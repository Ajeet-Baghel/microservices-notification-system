package org.ajeet.user_service.service;

import org.ajeet.user_service.dto.CreateUserRequest;
import org.ajeet.user_service.dto.UserResponse;

public interface UserService {

    UserResponse createUser(CreateUserRequest request);
}
