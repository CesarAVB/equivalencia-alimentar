package br.com.sistema.alimentos.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Order(1)
@RequiredArgsConstructor
public class FrontendLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("frontend.http");
    private static final String GREEN = "\u001B[32m";
    private static final String RESET  = "\u001B[0m";

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String origin = request.getHeader("Origin");
        if (origin == null) {
            filterChain.doFilter(request, response);
            return;
        }

        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        long start = System.currentTimeMillis();

        try {
            filterChain.doFilter(request, wrappedResponse);
        } finally {
            long ms = System.currentTimeMillis() - start;

            String uri = request.getRequestURI();
            if (request.getQueryString() != null) {
                uri += "?" + request.getQueryString();
            }

            String body = prettyBody(wrappedResponse.getContentAsByteArray());

            log.info("{}[FRONTEND] {} {} -> status={} ({} ms) origin={} response={}{}",
                    GREEN,
                    request.getMethod(), uri,
                    wrappedResponse.getStatus(), ms,
                    origin,
                    body,
                    RESET);

            wrappedResponse.copyBodyToResponse();
        }
    }

    private String prettyBody(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return "(sem corpo)";
        String raw = new String(bytes, StandardCharsets.UTF_8).trim();
        if (raw.isEmpty()) return "(sem corpo)";
        try {
            Object parsed = objectMapper.readValue(raw, Object.class);
            return "\n" + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(parsed);
        } catch (Exception e) {
            return raw;
        }
    }
}
