package com.audio.resource.service;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ResourceEventPublisher {
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private final StreamBridge streamBridge;

    public ResourceEventPublisher(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2))
    public void publishUploadEvent(Long resourceId) {
        String traceId = MDC.get("traceId");
        if (traceId == null || traceId.isEmpty()) {
            traceId = MDC.get(TRACE_ID_HEADER);
        }

        Message<Long> message = MessageBuilder.withPayload(resourceId)
                .setHeader(TRACE_ID_HEADER, traceId != null ? traceId : "")
                .build();

        log.info("Publishing upload event for resource: {} with traceId: {}", resourceId, traceId);
        streamBridge.send("resourceUpload-out-0", message);
    }
}
