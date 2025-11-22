package com.stream.app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Entity
@Table(name = "videos")
@Data // Lombok generates Getters, Setters, ToString
@NoArgsConstructor
@AllArgsConstructor
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String title;
    private String description;
    private String contentType; // e.g., "video/mp4"

    private String url; // S3 URL or File Path

    // We will use this later for the processing status
    // Values: "UPLOADED", "PROCESSING", "READY", "FAILED"
    private String processingStatus;
}