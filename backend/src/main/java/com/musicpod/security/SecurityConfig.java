package com.musicpod.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http)
            throws Exception {

        http
                .csrf(
                        AbstractHttpConfigurer::disable
                )

                .sessionManagement(
                        session ->
                                session.sessionCreationPolicy(
                                        SessionCreationPolicy.STATELESS
                                )
                )

                .authorizeHttpRequests(
                        authorization ->
                                authorization

                                        .requestMatchers(
                                                "/api/v1/auth/register",
                                                "/api/v1/auth/login"
                                        )
                                        .permitAll()

                                        .requestMatchers(
                                        		"/actuator",
                                                "/actuator/health",
                                                "/actuator/info",
                                                "/actuator/metrics",
                                                "/actuator/metrics/**",
                                                "/actuator/prometheus"
                                        )
                                        .permitAll()

                                        .requestMatchers(
                                                "/api/v1/artists/**",
                                                "/api/v1/albums/**",
                                                "/api/v1/tracks/**"
                                        )
                                        .permitAll()

                                        .requestMatchers(
                                                "/error"
                                        )
                                        .permitAll()

                                        .anyRequest()
                                        .authenticated()
                )
                

                .oauth2ResourceServer(
                        oauth2 ->
                                oauth2.jwt(
                                        Customizer.withDefaults()
                                )
                );

        return http.build();
    }
}