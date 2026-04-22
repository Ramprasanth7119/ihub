package com.ihub.service;

import com.ihub.dao.AuthDao;
import com.ihub.dto.LoginRequest;
import com.ihub.dto.AuthResponse;
import com.ihub.exception.CustomException;
import com.ihub.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthDao authDao;
    private final JwtUtil jwtUtil;
    
    

    public AuthService(AuthDao authDao, JwtUtil jwtUtil) {
		super();
		this.authDao = authDao;
		this.jwtUtil = jwtUtil;
	}

	private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthResponse login(LoginRequest request) {

        Map<String, Object> user = authDao.getUserByEmail(request.getEmail());

        if (user == null) {
            throw new CustomException("User not found");
        }

        String dbPassword = (String) user.get("password");

        if (!encoder.matches(request.getPassword(), dbPassword)) {
            throw new CustomException("Invalid credentials");
        }
        
        System.out.println("Inside auth");
        System.out.println(request.getEmail());
        System.out.println("IN: "+user.get("email"));

        String token = jwtUtil.generateToken(
                (String) user.get("email"),
                (String) user.get("role")
        );
        System.out.println("Token generated: " + token);

        return new AuthResponse(token);
    }
}