package com.stream.app.repository;

import com.stream.app.model.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VideoRepository extends JpaRepository<Video, UUID> {
    // This gives us methods like .save(), .findById(), .findAll() for free
}