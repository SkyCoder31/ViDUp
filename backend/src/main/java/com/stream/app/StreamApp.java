package com.stream.app;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class StreamApp {

    public static void main(String[] args) {
        SpringApplication.run(StreamApp.class, args);
    }

    @GetMapping("/")
    public String healthCheck() {
        return "Stream Engine Backend is Running on Windows!";
    }

    @Bean
    public CommandLineRunner init(MinioClient minioClient) {
        return args -> {
            // Check if bucket exists
            // "video-storage" is the name we defined in application.yml
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket("video-storage").build());

            if (!found) {
                // Create it if it doesn't exist
                minioClient.makeBucket(MakeBucketArgs.builder().bucket("video-storage").build());
                System.out.println(">>> Bucket 'video-storage' created successfully!");
            } else {
                System.out.println(">>> Bucket 'video-storage' already exists.");
            }
        };
    }
}