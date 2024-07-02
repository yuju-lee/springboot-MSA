package com.sparta.apigateway;

import com.sparta.apigateway.jwt.JwtFilter;
import com.sparta.apigateway.handler.JwtAccessDeniedHandler;
import com.sparta.apigateway.filter.JwtAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .cors(withDefaults())
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .headers(headers -> headers.frameOptions(ServerHttpSecurity.HeaderSpec.FrameOptionsSpec::disable))
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .authorizeExchange(authorizeExchange -> authorizeExchange
                        .pathMatchers("/", "/auth/login", "/member/signup", "/products", "/products/detail/**", "member/find-password", "member/reset-password", "/member/verify/**").permitAll()
                        .anyExchange().authenticated())
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
                        .accessDeniedHandler(new JwtAccessDeniedHandler()))
                .addFilterAt(jwtFilter, SecurityWebFiltersOrder.AUTHORIZATION);

        return http.build();
    }
}