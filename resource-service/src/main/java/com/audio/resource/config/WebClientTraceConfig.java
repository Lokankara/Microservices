package com.audio.resource.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Configuration
public class WebClientTraceConfig {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .filter(tracePropagationFilter());
    }

    private ExchangeFilterFunction tracePropagationFilter() {
        return (request, next) -> {
            String traceId = MDC.get("traceId");
            if (traceId == null || traceId.isEmpty()) {
                traceId = MDC.get(TRACE_ID_HEADER);
            }
            if (traceId != null && !traceId.isEmpty()) {
                ClientRequest mutatedRequest = ClientRequest.from(request)
                        .header(TRACE_ID_HEADER, traceId)
                        .build();
                return next.exchange(mutatedRequest);
            }
            return next.exchange(request);
        };
    }
}
