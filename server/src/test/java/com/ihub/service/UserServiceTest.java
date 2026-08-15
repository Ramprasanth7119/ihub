package com.ihub.service;

import com.ihub.dao.UserDao;
import com.ihub.dto.UserRequest;
import com.ihub.dto.UserResponse;
import com.ihub.exception.ConflictException;
import com.ihub.exception.ForbiddenException;
import com.ihub.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Registration guards and PII exposure rules.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

    @Mock private UserDao userDao;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userDao, passwordEncoder);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("an anonymous visitor cannot register themselves as an ADMIN")
    void rejectsAnonymousAdminRegistration() {
        assertThatThrownBy(() -> userService.createUser(request("ADMIN")))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("CREATOR or an INVESTOR");

        verify(userDao, never()).createUser(any());
    }

    @Test
    @DisplayName("an authenticated admin may create another admin")
    void allowsAdminToCreateAdmin() {
        authenticateAs("boss@ihub.test", "ADMIN");
        when(userDao.emailExists("new@ihub.test")).thenReturn(false);
        when(userDao.createUser(any())).thenReturn(50L);

        UserResponse response = userService.createUser(request("ADMIN"));

        assertThat(response.getRole()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("a creator cannot escalate by creating an admin")
    void rejectsCreatorCreatingAdmin() {
        authenticateAs("creator@ihub.test", "CREATOR");

        assertThatThrownBy(() -> userService.createUser(request("ADMIN")))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("self-service signup as CREATOR or INVESTOR is allowed and normalises the email")
    void allowsSelfServiceRoles() {
        when(userDao.emailExists("new@ihub.test")).thenReturn(false);
        when(userDao.createUser(any())).thenReturn(51L);

        UserRequest request = request("INVESTOR");
        request.setEmail("  NEW@IHub.Test  ");

        UserResponse response = userService.createUser(request);

        assertThat(response.getEmail()).isEqualTo("new@ihub.test");
    }

    @Test
    @DisplayName("a duplicate email is a 409, not a 500 from the unique index")
    void rejectsDuplicateEmail() {
        when(userDao.emailExists("new@ihub.test")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request("CREATOR")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("the stored password is hashed, never the plaintext")
    void hashesPassword() {
        when(userDao.emailExists("new@ihub.test")).thenReturn(false);
        when(userDao.createUser(any())).thenAnswer(invocation -> {
            UserRequest saved = invocation.getArgument(0);
            assertThat(saved.getPassword()).isNotEqualTo("Passw0rd!23");
            assertThat(passwordEncoder.matches("Passw0rd!23", saved.getPassword())).isTrue();
            return 52L;
        });

        userService.createUser(request("CREATOR"));

        verify(userDao).createUser(any());
    }

    @Test
    @DisplayName("another user's email is withheld, but their name is not")
    void redactsOtherUsersEmail() {
        authenticateAs("investor@ihub.test", "INVESTOR");
        when(userDao.getUserById(2L)).thenReturn(user(2L, "someone@ihub.test", "CREATOR"));

        UserResponse response = userService.getUser(2L);

        assertThat(response.getEmail()).isNull();
        assertThat(response.getName()).isEqualTo("Someone");
    }

    @Test
    @DisplayName("an admin sees the full record including email")
    void adminSeesEmail() {
        authenticateAs("boss@ihub.test", "ADMIN");
        when(userDao.getUserById(2L)).thenReturn(user(2L, "someone@ihub.test", "CREATOR"));

        assertThat(userService.getUser(2L).getEmail()).isEqualTo("someone@ihub.test");
    }

    @Test
    @DisplayName("a user always sees their own email")
    void selfSeesOwnEmail() {
        authenticateAs("investor@ihub.test", "INVESTOR");
        when(userDao.getUserById(1L)).thenReturn(user(1L, "investor@ihub.test", "INVESTOR"));

        assertThat(userService.getUser(1L).getEmail()).isEqualTo("investor@ihub.test");
    }

    // ------------------------------------------------------------------ helpers

    private UserRequest request(String role) {
        UserRequest request = new UserRequest();
        request.setName("New User");
        request.setEmail("new@ihub.test");
        request.setPassword("Passw0rd!23");
        request.setRole(role);
        return request;
    }

    private User user(Long id, String email, String role) {
        User user = new User();
        user.setId(id);
        user.setName("Someone");
        user.setEmail(email);
        user.setRole(role);
        return user;
    }

    private void authenticateAs(String email, String role) {
        User user = new User();
        user.setId("ADMIN".equals(role) ? 99L : 1L);
        user.setName("Auth User");
        user.setEmail(email);
        user.setRole(role);
        when(userDao.findByEmail(email)).thenReturn(user);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, List.of()));
    }
}
