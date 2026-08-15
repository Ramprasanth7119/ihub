package com.ihub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Authentication is entirely JWT-based, so Spring Security's default in-memory user
 * is excluded — otherwise Boot generates and logs a random password on every start,
 * implying a login path that does not exist.
 */
@EnableScheduling
@EnableAsync
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class IhubApplication {

	public static void main(String[] args) {
		SpringApplication.run(IhubApplication.class, args);
	}

}
