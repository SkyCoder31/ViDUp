package com.stream.app.service;

import com.stream.app.model.Video;
import com.stream.app.repository.VideoRepository;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class TranscodingService {

    private final MinioClient minioClient;
    private final VideoRepository videoRepository;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Transactional
    public void transcodeVideo(String videoId) {
        String tempDirName = "transcode_" + UUID.randomUUID();
        Path tempPath = Paths.get(System.getProperty("java.io.tmpdir"), tempDirName);

        try {
            Video video = videoRepository.findById(UUID.fromString(videoId))
                    .orElseThrow(() -> new RuntimeException("Video not found: " + videoId));

            video.setProcessingStatus("PROCESSING");
            videoRepository.save(video);
            System.out.println(">>> Starting transcoding for: " + videoId);

            Files.createDirectories(tempPath);

            //Download Source
            String originalFileName = video.getUrl();
            Path localInputPath = tempPath.resolve("input.mp4");

            try (InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(originalFileName)
                            .build())) {
                Files.copy(stream, localInputPath);
            }

            Path hlsOutputPath = tempPath.resolve("master.m3u8");

            //Prepare Command
            // In Production (Docker), "ffmpeg" will be in the PATH.
            // We keep the CMD wrapper for Windows dev, but remove hardcoded paths.
            List<String> command = new ArrayList<>();

            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                command.add("cmd.exe");
                command.add("/c");
            }

            command.add("ffmpeg"); // We rely on the system PATH now
            command.add("-i");
            command.add(localInputPath.toString());
            command.add("-codec:v");
            command.add("libx264");
            command.add("-codec:a");
            command.add("aac");
            command.add("-hls_time");
            command.add("10");
            command.add("-hls_playlist_type");
            command.add("vod");
            command.add("-hls_segment_filename");
            command.add(tempPath.resolve("segment_%03d.ts").toString());
            command.add(hlsOutputPath.toString());

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Uncomment for debug logs
                    // System.out.println("[FFmpeg] " + line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("FFmpeg failed with exit code " + exitCode);
            }

            //Upload segments
            try (Stream<Path> paths = Files.walk(tempPath)) {
                paths.filter(Files::isRegularFile)
                        .filter(p -> !p.equals(localInputPath))
                        .forEach(path -> {
                            try {
                                String objectName = "processed/" + videoId + "/" + path.getFileName().toString();
                                minioClient.uploadObject(
                                        UploadObjectArgs.builder()
                                                .bucket(bucketName)
                                                .object(objectName)
                                                .filename(path.toString())
                                                .contentType(getContentType(path))
                                                .build()
                                );
                            } catch (Exception e) {
                                System.err.println("Failed to upload segment: " + e.getMessage());
                            }
                        });
            }

            //Update DB
            String hlsUrl = "processed/" + videoId + "/master.m3u8";
            video.setUrl(hlsUrl);
            video.setProcessingStatus("READY");
            videoRepository.save(video);

            System.out.println(">>> Transcoding Complete for Video: " + videoId);

        } catch (Exception e) {
            System.err.println("Error transcoding video " + videoId + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            deleteDirectory(tempPath.toFile());
        }
    }

    private String getContentType(Path path) {
        String name = path.getFileName().toString();
        if (name.endsWith(".m3u8")) return "application/x-mpegURL";
        if (name.endsWith(".ts")) return "video/MP2T";
        return "application/octet-stream";
    }

    private void deleteDirectory(File file) {
        if (file.isDirectory()) {
            File[] entries = file.listFiles();
            if (entries != null) {
                for (File entry : entries) {
                    deleteDirectory(entry);
                }
            }
        }
        file.delete();
    }
}