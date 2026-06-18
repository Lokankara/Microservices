package com.audio.processor.messaging;

import com.audio.processor.service.ResourceProcessorService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Component
public class ResourceEventConsumer {

    private final ResourceProcessorService resourceProcessorService;

    public ResourceEventConsumer(ResourceProcessorService resourceProcessorService) {
        this.resourceProcessorService = resourceProcessorService;
    }

    @Bean
    public Consumer<Map<String, Object>> processResource() {
        return message -> {
            Long resourceId = Long.valueOf(message.get("payload").toString());
            @SuppressWarnings("unchecked")
            Map<String, Object> headers = (Map<String, Object>) message.get("headers");

            String traceId = null;
            if (headers != null) {
                Object headerTraceId = headers.get("X-Trace-Id");
                if (headerTraceId != null) {
                    traceId = headerTraceId.toString();
                }
            }

            if (traceId != null && !traceId.isEmpty()) {
                MDC.put("X-Trace-Id", traceId);
                MDC.put("traceId", traceId);
            }

            try {
                log.info("Received processing event for resource: {} with traceId: {}", resourceId, traceId);
                resourceProcessorService.process(resourceId);
            } finally {
                MDC.clear();
            }
        };
    }
}
