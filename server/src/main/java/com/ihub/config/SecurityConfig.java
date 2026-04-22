package com.ihub.config;

import com.ihub.security.JwtFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration with JWT protection
 */
@Configuration
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    
    public SecurityConfig(JwtFilter jwtFilter) {
		super();
		this.jwtFilter = jwtFilter;
	}

	@Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth

                // Public APIs
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/users").permitAll()
                .requestMatchers("/ws/**").permitAll()

                // Role-based APIs
                .requestMatchers("/api/ideas/**").hasRole("CREATOR")
                .requestMatchers("/api/bids/**").hasRole("INVESTOR")

                // Everything else
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}