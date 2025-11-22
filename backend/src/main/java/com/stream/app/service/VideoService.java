package com.stream.app.service;

import com.stream.app.config.RabbitMQConfig; // Import the config we just made
import com.stream.app.model.Video;
import com.stream.app.repository.VideoRepository;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class VideoService {

    private final MinioClient minioClient;
    private final VideoRepository videoRepository;
    private final RabbitTemplate rabbitTemplate; // Inject RabbitMQ helper

    @Value("${minio.bucket-name}")
    private String bucketName;

    public Video uploadVideo(MultipartFile file, String title, String description) {
        try {
            // Prepare file name
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            InputStream inputStream = file.getInputStream();

            // Upload to MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            // Save the Metadata (Status: UPLOADED)
            Video video = new Video();
            video.setTitle(title);
            video.setDescription(description);
            video.setContentType(file.getContentType());
            video.setUrl(fileName);
            video.setProcessingStatus("UPLOADED");

            Video savedVideo = videoRepository.save(video);

            // Send Message to Queue
            // We only send the Video ID. The worker will look up the rest.
            String videoId = savedVideo.getId().toString();
            rabbitTemplate.convertAndSend(RabbitMQConfig.QUEUE_NAME, videoId);

            System.out.println(">>> Message sent to Queue for Video ID: " + videoId);

            return savedVideo;

        } catch (Exception e) {
            throw new RuntimeException("Failed to upload video", e);
        }
    }
}