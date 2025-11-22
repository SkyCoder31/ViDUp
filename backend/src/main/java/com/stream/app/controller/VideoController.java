package com.stream.app.controller;

import com.stream.app.model.Video;
import com.stream.app.service.VideoService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;
    private final MinioClient minioClient; // Inject MinIO Client directly for streaming

    @Value("${minio.bucket-name}")
    private String bucketName;

    @PostMapping("/upload")
    public ResponseEntity<Video> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("description") String description) {

        Video savedVideo = videoService.uploadVideo(file, title, description);
        return new ResponseEntity<>(savedVideo, HttpStatus.CREATED);
    }

    @GetMapping("/{videoId}/{filename}")
    public ResponseEntity<InputStreamResource> streamVideoFile(
            @PathVariable String videoId,
            @PathVariable String filename) {

        try {
            //Security Check (Placeholder)
            // In a real production setup, we would check:
            // - Is the user logged in?
            // - Did the user pay for this video?
            // if (!authService.canWatch(user, videoId)) return forbidden();

            // Resolve the path in MinIO
            // Our structure is: processed/{videoId}/{filename}
            String objectPath = "processed/" + videoId + "/" + filename;

            //Fetching from MinIO (Privately)
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectPath)
                            .build()
            );

            // Determining Content Type
            MediaType contentType = MediaType.APPLICATION_OCTET_STREAM;
            if (filename.endsWith(".m3u8")) {
                contentType = MediaType.parseMediaType("application/x-mpegURL");
            } else if (filename.endsWith(".ts")) {
                contentType = MediaType.parseMediaType("video/MP2T");
            }

            //Streaming the data back to the user
            return ResponseEntity.ok()
                    .contentType(contentType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(new InputStreamResource(stream));

        } catch (Exception e) {
            System.err.println("Error streaming file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}