package com.ihub.controller;

import com.ihub.dto.LoginRequest;
import com.ihub.dto.AuthResponse;
import com.ihub.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
		super();
		this.authService = authService;
	}



	@PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }
}