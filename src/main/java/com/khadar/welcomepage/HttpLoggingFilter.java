package com.khadar.welcomepage;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class HttpLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger =
            LoggerFactory.getLogger(HttpLoggingFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        String method = request.getMethod();
        String uri = request.getRequestURI();

        try {

            // Continue processing the request
            filterChain.doFilter(request, response);

        } finally {

            long duration =
                    System.currentTimeMillis() - startTime;

            int status = response.getStatus();

            String message = String.format(
                    "HTTP Request - Method: %s, URI: %s, Status: %d, Duration: %d ms",
                    method,
                    uri,
                    status,
                    duration
            );

            // 5xx - Server errors
            if (status >= 500) {

                logger.error(message);

            }
            // 4xx - Client errors
            else if (status >= 400) {

                logger.warn(message);

            }
            // 2xx and 3xx - Success and redirects
            else {

                logger.info(message);

            }
        }
    }
}
