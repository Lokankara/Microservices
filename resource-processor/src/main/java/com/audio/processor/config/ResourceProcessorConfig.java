package com.audio.processor.config;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@RefreshScope
public class ResourceProcessorConfig {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Value("${resource.service.url:http://localhost:8081}")
    private String resourceServiceUrl;

    @Value("${song.service.url:http://localhost:8082}")
    private String songServiceUrl;

    @Bean("resourceServiceClient")
    public WebClient resourceServiceClient() {
        return buildTraceableWebClient(resourceServiceUrl);
    }

    @Bean("songServiceClient")
    public WebClient songServiceClient() {
        return buildTraceableWebClient(songServiceUrl);
    }

    private WebClient buildTraceableWebClient(String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .filter(tracePropagationFilter())
                .build();
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
