package com.audio.resource.config;

import com.audio.resource.messaging.ResourceEventConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class ResourceEventListener {

    @Bean
    public Consumer<Long> processedResource(ResourceEventConsumer eventConsumer) {
        return eventConsumer::handleResourceProcessed;
    }
}
