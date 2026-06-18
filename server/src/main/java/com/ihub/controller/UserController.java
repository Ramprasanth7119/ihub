package com.ihub.controller;

import com.ihub.dto.UserRequest;
import com.ihub.dto.UserResponse;
import com.ihub.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for user APIs
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

	/**
     * Create new user
     */
    @PostMapping
    public UserResponse createUser(@Valid @RequestBody UserRequest request) {
        return userService.createUser(request);
    }

    /**
     * Get the currently authenticated user
     */
    @GetMapping("/me")
    public UserResponse getCurrentUser() {
        return userService.getCurrentUser();
    }

    /**
     * Get user by ID
     */
    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable Long id) {
        return userService.getUser(id);
    }

    /**
     * Get all users
     */
    @GetMapping
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers();
    }
}