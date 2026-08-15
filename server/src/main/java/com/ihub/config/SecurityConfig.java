package com.ihub.config;

import com.ihub.security.JwtFilter;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final ObjectMapper objectMapper;
    private final String allowedOrigins;

    public SecurityConfig(
            JwtFilter jwtFilter,
            ObjectMapper objectMapper,
            @Value("${ihub.cors.allowed-origins:http://localhost:3000}") String allowedOrigins) {
        this.jwtFilter = jwtFilter;
        this.objectMapper = objectMapper;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            // CSRF is not applicable: the API is stateless and authenticated by a
            // bearer token that browsers do not attach automatically.
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(headers -> headers
                    .frameOptions(frame -> frame.deny())
                    .contentTypeOptions(contentType -> {})
                    .httpStrictTransportSecurity(hsts -> hsts
                            .includeSubDomains(true)
                            .maxAgeInSeconds(31536000))
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) ->
                        writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required", request.getRequestURI()))
                .accessDeniedHandler((request, response, accessDeniedException) ->
                        writeError(response, HttpServletResponse.SC_FORBIDDEN, "Access denied", request.getRequestURI()))
            )
            .authorizeHttpRequests(auth -> auth

                // Container health probes
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()

                // Public — authentication & registration
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/users").permitAll()

                // Public — discovery (read-only)
                .requestMatchers(HttpMethod.GET, "/api/ideas/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auctions/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/search/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()

                // STOMP handshake. Subscriptions are authorised per-destination in
                // WebSocketConfig's inbound interceptor, which requires a JWT on CONNECT.
                .requestMatchers("/ws/**").permitAll()

                // Creator — idea mutations & auction creation
                .requestMatchers(HttpMethod.POST, "/api/ideas/*/publish").hasRole("CREATOR")
                .requestMatchers(HttpMethod.POST, "/api/ideas/**").hasRole("CREATOR")
                .requestMatchers(HttpMethod.PUT, "/api/ideas/**").hasRole("CREATOR")
                .requestMatchers(HttpMethod.PATCH, "/api/ideas/**").hasRole("CREATOR")
                .requestMatchers(HttpMethod.DELETE, "/api/ideas/**").hasRole("CREATOR")
                .requestMatchers(HttpMethod.POST, "/api/auctions/*/close").hasAnyRole("CREATOR", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/auctions/*/start").hasAnyRole("CREATOR", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/auctions").hasRole("CREATOR")

                // Bidding — read-only public, placing a bid requires an investor
                .requestMatchers(HttpMethod.GET, "/api/bids/auction/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/bids/my").hasRole("INVESTOR")
                .requestMatchers(HttpMethod.POST, "/api/bids").hasRole("INVESTOR")

                // In-app notifications (all authenticated roles)
                .requestMatchers("/api/notifications/**").hasAnyRole("ADMIN", "CREATOR", "INVESTOR")

                // Admin console
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // Listing every user exposes names, emails and roles platform-wide,
                // so it is admin-only. /me is self-service; /{id} is authorised
                // per-record in UserService (self or admin).
                .requestMatchers(HttpMethod.GET, "/api/users").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/users/**").hasAnyRole("ADMIN", "CREATOR", "INVESTOR")
                .requestMatchers(HttpMethod.PUT, "/api/users/me").hasAnyRole("ADMIN", "CREATOR", "INVESTOR")

                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Origins come from {@code ihub.cors.allowed-origins} (env {@code FRONTEND_URL}).
     * Wildcards are intentionally unsupported: credentialed requests require an
     * explicit origin list, and the deployed frontend URL is always known.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        // Paging metadata is returned in headers, so clients must be able to read them.
        configuration.setExposedHeaders(List.of("X-Total-Count", "X-Page", "X-Page-Size", "X-Total-Pages"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        source.registerCorsConfiguration("/ws/**", configuration);
        return source;
    }

    private void writeError(HttpServletResponse response, int status, String message, String path) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(),
                new ErrorResponse(status, status == 401 ? "Unauthorized" : "Forbidden", message, path));
    }
}
