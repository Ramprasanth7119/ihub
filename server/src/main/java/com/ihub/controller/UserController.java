package com.ihub.controller;

import com.ihub.dto.PasswordChangeRequest;
import com.ihub.dto.ProfileUpdateRequest;
import com.ihub.dto.UserRequest;
import com.ihub.dto.UserResponse;
import com.ihub.service.UserService;
import com.ihub.util.PagedResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * User registration and profile APIs.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** Public registration. The service rejects any attempt to self-assign ADMIN. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody UserRequest request) {
        return userService.createUser(request);
    }

    @GetMapping("/me")
    public UserResponse getCurrentUser() {
        return userService.getCurrentUser();
    }

    @PutMapping("/me")
    public UserResponse updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        return userService.updateProfile(request);
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        userService.changePassword(request);
    }

    /**
     * Public profile of a single user. The email address is only included for the
     * user themselves or for an administrator.
     */
    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable Long id) {
        return userService.getUser(id);
    }

    /** Full account listing — admin only (enforced in {@code SecurityConfig}). */
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return PagedResponse.of(userService.getAllUsers(page, size));
    }
}
