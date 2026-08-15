package com.ihub.service;

import com.ihub.dao.UserDao;
import com.ihub.dto.PasswordChangeRequest;
import com.ihub.dto.ProfileUpdateRequest;
import com.ihub.dto.UserRequest;
import com.ihub.dto.UserResponse;
import com.ihub.exception.ConflictException;
import com.ihub.exception.ForbiddenException;
import com.ihub.exception.NotFoundException;
import com.ihub.exception.UnauthorizedException;
import com.ihub.exception.UnprocessableEntityException;
import com.ihub.model.PagedResult;
import com.ihub.model.User;
import com.ihub.util.Pagination;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * User registration and profile management.
 */
@Service
public class UserService {

    /** Roles a visitor may choose when signing up. ADMIN is deliberately excluded. */
    private static final Set<String> SELF_SERVICE_ROLES = Set.of("CREATOR", "INVESTOR");

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserDao userDao, PasswordEncoder passwordEncoder) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new account.
     *
     * <p>{@code POST /api/users} is public, so the requested role is validated here:
     * without this check anyone could register themselves as an ADMIN and take over
     * the console. Creating an administrator requires an already-authenticated
     * administrator.</p>
     */
    @Transactional
    public UserResponse createUser(UserRequest request) {
        String role = request.getRole() != null ? request.getRole().toUpperCase(Locale.ROOT) : "";

        if (!SELF_SERVICE_ROLES.contains(role)) {
            User current = findAuthenticatedUser();
            boolean actingAdmin = current != null && "ADMIN".equalsIgnoreCase(current.getRole());
            if (!actingAdmin) {
                throw new ForbiddenException("You can only register as a CREATOR or an INVESTOR");
            }
        }

        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userDao.emailExists(email)) {
            throw new ConflictException("An account with this email already exists");
        }

        request.setEmail(email);
        request.setRole(role);
        request.setPassword(passwordEncoder.encode(request.getPassword()));

        Long id;
        try {
            id = userDao.createUser(request);
        } catch (DuplicateKeyException e) {
            // Lost the race against a concurrent signup with the same address.
            throw new ConflictException("An account with this email already exists");
        }

        return new UserResponse(id, request.getName(), email, role);
    }

    public UserResponse getCurrentUser() {
        User user = requireAuthenticatedUser();
        return toResponse(user, true);
    }

    /**
     * Returns a user by id.
     *
     * <p>The email address is only included when the caller is the user themselves or
     * an administrator. Other callers get the public profile — enough to attribute an
     * idea to its creator, without turning the endpoint into an email harvester.</p>
     */
    public UserResponse getUser(Long id) {
        User caller = requireAuthenticatedUser();

        User user;
        try {
            user = userDao.getUserById(id);
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("User not found");
        }

        boolean privileged = caller.getId().equals(id) || "ADMIN".equalsIgnoreCase(caller.getRole());
        return toResponse(user, privileged);
    }

    /** Full account listing. Restricted to administrators by {@code SecurityConfig}. */
    public PagedResult<UserResponse> getAllUsers(Integer page, Integer size) {
        int resolvedPage = Pagination.resolvePage(page);
        int resolvedSize = Pagination.resolveSize(size);

        List<UserResponse> content = userDao
                .getAllUsers(resolvedSize, Pagination.offset(resolvedPage, resolvedSize))
                .stream()
                .map(user -> toResponse(user, true))
                .toList();

        return new PagedResult<>(content, userDao.countUsers(), resolvedPage, resolvedSize);
    }

    @Transactional
    public UserResponse updateProfile(ProfileUpdateRequest request) {
        User user = requireAuthenticatedUser();

        // Email and role are intentionally not updatable here: email is the login
        // identity and role governs authorization, so neither may be self-assigned.
        userDao.updateProfile(user.getId(), request.getName().trim());

        User updated = userDao.getUserById(user.getId());
        return toResponse(updated, true);
    }

    @Transactional
    public void changePassword(PasswordChangeRequest request) {
        User user = requireAuthenticatedUser();

        String currentHash = userDao.findPasswordHash(user.getId());
        if (currentHash == null || !passwordEncoder.matches(request.getCurrentPassword(), currentHash)) {
            throw new UnprocessableEntityException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.getNewPassword(), currentHash)) {
            throw new UnprocessableEntityException("New password must differ from the current one");
        }

        userDao.updatePassword(user.getId(), passwordEncoder.encode(request.getNewPassword()));
    }

    private User requireAuthenticatedUser() {
        User user = findAuthenticatedUser();
        if (user == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return user;
    }

    /** Resolves the current principal, or {@code null} when the request is anonymous. */
    private User findAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            return null;
        }
        return userDao.findByEmail(auth.getName());
    }

    private UserResponse toResponse(User user, boolean includeEmail) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                includeEmail ? user.getEmail() : null,
                user.getRole()
        );
    }
}
