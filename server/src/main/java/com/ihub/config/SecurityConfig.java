package com.ihub.config;

import com.ihub.security.JwtFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import com.ihub.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtFilter jwtFilter, ObjectMapper objectMapper) {
        this.jwtFilter = jwtFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) ->
                        writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required", request.getRequestURI()))
                .accessDeniedHandler((request, response, accessDeniedException) ->
                        writeError(response, HttpServletResponse.SC_FORBIDDEN, "Access denied", request.getRequestURI()))
            )
            .authorizeHttpRequests(auth -> auth

                // Public — authentication & registration
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/users").permitAll()

                // Public — discovery (read-only)
                .requestMatchers(HttpMethod.GET, "/api/ideas/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auctions/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/search/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()

                // WebSocket handshake (STOMP auth can be added in Phase 6)
                .requestMatchers("/ws/**").permitAll()

                // Creator — idea mutations & auction creation
                .requestMatchers(HttpMethod.POST, "/api/ideas/*/publish").hasRole("CREATOR")
                .requestMatchers(HttpMethod.POST, "/api/ideas/**").hasRole("CREATOR")
                .requestMatchers(HttpMethod.PUT, "/api/ideas/**").hasRole("CREATOR")
                .requestMatchers(HttpMethod.PATCH, "/api/ideas/**").hasRole("CREATOR")
                .requestMatchers(HttpMethod.DELETE, "/api/ideas/**").hasRole("CREATOR")
                .requestMatchers(HttpMethod.POST, "/api/auctions/*/close").hasAnyRole("CREATOR", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/auctions/*/start").hasRole("CREATOR")
                .requestMatchers(HttpMethod.POST, "/api/auctions").hasRole("CREATOR")

                // Bidding — read-only public, place bid requires investor
                .requestMatchers(HttpMethod.GET, "/api/bids/auction/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/bids").hasRole("INVESTOR")

                // In-app notifications (all authenticated roles)
                .requestMatchers("/api/notifications/**").hasAnyRole("ADMIN", "CREATOR", "INVESTOR")

                // Admin — user management & monitoring (Phase 7)
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/users/**").hasAnyRole("ADMIN", "CREATOR", "INVESTOR")

                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void writeError(HttpServletResponse response, int status, String message, String path) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(),
                new ErrorResponse(status, status == 401 ? "Unauthorized" : "Forbidden", message, path));
    }
}
