package org.ajeet.user_service.controller;

import org.ajeet.user_service.dto.CreateUserRequest;
import org.ajeet.user_service.dto.UserResponse;
import org.ajeet.user_service.exception.UserAlreadyExistsException;
import org.ajeet.user_service.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void createsUserForAuthenticatedRequest() throws Exception {
        when(userService.createUser(any(CreateUserRequest.class)))
                .thenReturn(new UserResponse(42L, "Test User", "user@example.com", LocalDateTime.now()));

        mockMvc.perform(post("/api/users")
                        .with(jwt())
                        .contentType("application/json")
                        .content("""
                                {"name":"Test User","email":"user@example.com"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/users/42"))
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.email").value("user@example.com"));
    }

    @Test
    void returnsStructuredValidationErrors() throws Exception {
        mockMvc.perform(post("/api/users")
                        .with(jwt())
                        .contentType("application/json")
                        .content("""
                                {"name":"","email":"invalid"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.path").value("/api/users"))
                .andExpect(jsonPath("$.validationErrors.name").value("Name is required"))
                .andExpect(jsonPath("$.validationErrors.email").value("Email must be valid"));
    }

    @Test
    void returnsConflictForDuplicateEmail() throws Exception {
        when(userService.createUser(any(CreateUserRequest.class)))
                .thenThrow(new UserAlreadyExistsException("User already exists with email: user@example.com"));

        mockMvc.perform(post("/api/users")
                        .with(jwt())
                        .contentType("application/json")
                        .content("""
                                {"name":"Test User","email":"user@example.com"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message")
                        .value("User already exists with email: user@example.com"));
    }

    @Test
    void returnsBadRequestForMalformedJson() throws Exception {
        mockMvc.perform(post("/api/users")
                        .with(jwt())
                        .contentType("application/json")
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is malformed"));
    }
}
