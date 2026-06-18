package com.audio.gateway.config;

import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class TraceGatewayFilter implements GlobalFilter, Ordered {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString();
        }

        final String finalTraceId = traceId;
        MDC.put(TRACE_ID_HEADER, finalTraceId);
        MDC.put("traceId", finalTraceId);

        exchange.getResponse().getHeaders().add(TRACE_ID_HEADER, finalTraceId);

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(r -> r.header(TRACE_ID_HEADER, finalTraceId))
                .build();

        return chain.filter(mutatedExchange)
                .then(Mono.fromRunnable(MDC::clear));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
