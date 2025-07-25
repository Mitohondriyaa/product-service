package io.github.mitohondriyaa.product.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("!test")
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-api/**",
                    "/swagger-ui.html")
                .permitAll()
                .requestMatchers(HttpMethod.POST, "/api/product/**")
                .hasRole("PRODUCT_MANAGER")
                .requestMatchers(HttpMethod.PUT, "/api/product/**")
                .hasRole("PRODUCT_MANAGER")
                .requestMatchers(HttpMethod.DELETE, "/api/product/**")
                .hasRole("PRODUCT_MANAGER")
                .anyRequest()
                .authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(Customizer
                    .withDefaults()))
            .build();
    }
}