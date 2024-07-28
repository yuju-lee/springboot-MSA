package com.sparta.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("spring-msa-member", r -> r.path("/member/**")
                        .uri("lb://spring-msa-member"))
                .route("spring-msa-member", r -> r.path("/auth/**")
                        .uri("lb://spring-msa-member"))
                .route("spring-msa-order", r -> r.path("/order/**")
                        .uri("lb://spring-msa-order"))
                .route("spring-msa-product", r -> r.path("/products/**")
                        .uri("lb://spring-msa-product"))
                .route("spring-msa-payment", r -> r.path("/payment/**")
                        .uri("lb://spring-msa-payment"))
                .build();
    }
}