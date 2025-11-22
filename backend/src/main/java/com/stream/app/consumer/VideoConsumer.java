package com.stream.app.consumer;

import com.stream.app.config.RabbitMQConfig;
import com.stream.app.service.TranscodingService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VideoConsumer {

    private final TranscodingService transcodingService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void processVideo(String videoId) {
        System.out.println(">>> Received Message from Queue: " + videoId);

        // Delegating the heavy work to the service
        transcodingService.transcodeVideo(videoId);
    }
}