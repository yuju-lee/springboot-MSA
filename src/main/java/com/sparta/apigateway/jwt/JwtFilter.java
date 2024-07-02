package com.sparta.apigateway.jwt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.ArrayList;

@Component
public class JwtFilter implements WebFilter, ApplicationContextAware {
    private static final Logger logger = LoggerFactory.getLogger(JwtFilter.class);
    private final JwtTokenProvider jwtTokenProvider;
    private ApplicationContext applicationContext;

    private static final String[] EXCLUDED_PATHS = {"/", "/auth/login", "/member/signup", "/products",
            "/products/detail/**", "member/find-password", "member/reset-password", "/member/verify**"};

    public JwtFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String requestPath = exchange.getRequest().getURI().getPath();
        logger.info("Request path: {}", requestPath);
        AntPathMatcher pathMatcher = new AntPathMatcher();

        for (String excludedPath : EXCLUDED_PATHS) {
            if (pathMatcher.match(excludedPath, requestPath)) {
                logger.info("Path {} is excluded from JWT filtering", requestPath);
                return chain.filter(exchange);
            }
        }

        String token = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        logger.info("Authorization header: {}", token);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            String email = jwtTokenProvider.getEmailFromToken(token);
            logger.info("Extracted email from token: {}", email);

            if (email != null) {
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        new User(email, "", new ArrayList<>()), null, new ArrayList<>()
                );
                return chain.filter(exchange.mutate()
                                .request(exchange.getRequest().mutate()
                                        .header("X-Authenticated-User", email)
                                        .build())
                                .build())
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
            } else {
                logger.error("Failed to extract email from token");
            }
        } else {
            logger.error("Invalid or expired token");
        }

        return chain.filter(exchange);
    }
}