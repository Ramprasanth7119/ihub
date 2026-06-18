package com.ihub.service;

import com.ihub.dao.UserDao;
import com.ihub.dto.UserRequest;
import com.ihub.dto.UserResponse;
import com.ihub.exception.CustomException;
import com.ihub.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for user business logic
 */
@Service
public class UserService {

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserDao userDao, PasswordEncoder passwordEncoder) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
    }

	/**
     * Create user
     */
    public UserResponse createUser(UserRequest request) {
    	
    	request.setPassword(passwordEncoder.encode(request.getPassword()));

        Long id = userDao.createUser(request);

        return new UserResponse(
                id,
                request.getName(),
                request.getEmail(),
                request.getRole()
        );
    }

    /**
     * Get the currently authenticated user.
     */
    public UserResponse getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new CustomException("Authentication required");
        }
        String email = auth.getPrincipal().toString();
        try {
            User user = userDao.findByEmail(email);
            return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            throw new CustomException("User not found");
        }
    }

    /**
     * Get user by ID
     */
    public UserResponse getUser(Long id) {

        User user = userDao.getUserById(id);

        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }

    /**
     * Get all users
     */
    public List<UserResponse> getAllUsers() {

        return userDao.getAllUsers()
                .stream()
                .map(user -> new UserResponse(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getRole()
                ))
                .collect(Collectors.toList());
    }
    
    
}