package org.ajeet.user_service.service;

import org.ajeet.user_service.dto.CreateUserRequest;
import org.ajeet.user_service.dto.UserResponse;
import org.ajeet.user_service.entity.User;
import org.ajeet.user_service.event.EventPublisher;
import org.ajeet.user_service.event.UserCreatedEvent;
import org.ajeet.user_service.exception.UserAlreadyExistsException;
import org.ajeet.user_service.mapper.UserMapper;
import org.ajeet.user_service.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final EventPublisher eventPublisher;

    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper, EventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("User already exists with email: " + request.getEmail());
        }
        User user = userMapper.toEntity(request);
        user.setCreatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);
        eventPublisher.publishUserCreated(new UserCreatedEvent(
                savedUser.getId(), savedUser.getName(), savedUser.getEmail(), savedUser.getCreatedAt()));
        return userMapper.toResponse(savedUser);
    }
}
