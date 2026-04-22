package com.ihub.controller;

import com.ihub.dto.UserRequest;
import com.ihub.dto.UserResponse;
import com.ihub.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for user APIs
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    
    

    public UserController(UserService userService) {
		super();
		this.userService = userService;
	}

	/**
     * Create new user
     */
    @PostMapping
    public UserResponse createUser(@RequestBody UserRequest request) {
        return userService.createUser(request);
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