package com.ihub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class IhubApplication {

	public static void main(String[] args) {
		SpringApplication.run(IhubApplication.class, args);
	}

}
