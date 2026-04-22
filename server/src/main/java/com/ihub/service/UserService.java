package com.ihub.service;

import com.ihub.dao.UserDao;
import com.ihub.dto.UserRequest;
import com.ihub.dto.UserResponse;
import com.ihub.model.User;
import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for user business logic
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserDao userDao;
    
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public UserService(UserDao userDao) {
		super();
		this.userDao = userDao;
	}

	/**
     * Create user
     */
    public UserResponse createUser(UserRequest request) {
    	
    	request.setPassword(encoder.encode(request.getPassword()));

        Long id = userDao.createUser(request);

        return new UserResponse(
                id,
                request.getName(),
                request.getEmail(),
                request.getRole()
        );
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