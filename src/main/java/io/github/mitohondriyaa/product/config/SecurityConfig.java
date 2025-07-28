package io.github.mitohondriyaa.product.config;

import io.github.mitohondriyaa.product.converter.KeycloakRealmRoleConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

@Configuration
@Profile("!test")
public class SecurityConfig {
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write(
                """
                    {
                        "code": "UNAUTHORIZED",
                        "message": "User not authenticated"
                    }
                    """
            );
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json");
            response.getWriter().write("""
                    {
                        "code": "FORBIDDEN",
                        "message": "User not authorized"
                    }
                """);
        };
    }

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
                .authenticationEntryPoint(authenticationEntryPoint())
                .accessDeniedHandler(accessDeniedHandler())
                .jwt(jwt -> jwt.jwtAuthenticationConverter(new KeycloakRealmRoleConverter())))
            .build();
    }
}