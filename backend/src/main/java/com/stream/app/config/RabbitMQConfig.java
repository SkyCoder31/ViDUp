package com.stream.app.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_NAME = "video_transcoding_queue";

    @Bean
    public Queue videoQueue() {
        // "true" means the queue survives a RabbitMQ restart (durable)
        return new Queue(QUEUE_NAME, true);
    }
}