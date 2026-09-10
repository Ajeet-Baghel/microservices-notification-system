package org.ajeet.user_service.service;

import org.ajeet.user_service.dto.CreateUserRequest;
import org.ajeet.user_service.dto.UserResponse;
import org.ajeet.user_service.entity.User;
import org.ajeet.user_service.event.EventPublisher;
import org.ajeet.user_service.event.UserCreatedEvent;
import org.ajeet.user_service.exception.UserAlreadyExistsException;
import org.ajeet.user_service.mapper.UserMapper;
import org.ajeet.user_service.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceImplTests {

    @Test
    void publishesEventAfterSavingUser() {
        UserRepository repository = mock(UserRepository.class);
        EventPublisher publisher = mock(EventPublisher.class);
        UserServiceImpl service = new UserServiceImpl(repository, new UserMapper(), publisher);
        when(repository.existsByEmail("user@example.com")).thenReturn(false);
        when(repository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(42L);
            return user;
        });

        UserResponse response = service.createUser(new CreateUserRequest("Test User", "user@example.com"));

        ArgumentCaptor<UserCreatedEvent> eventCaptor = ArgumentCaptor.forClass(UserCreatedEvent.class);
        verify(publisher).publishUserCreated(eventCaptor.capture());
        assertThat(response.getId()).isEqualTo(42L);
        assertThat(eventCaptor.getValue().id()).isEqualTo(42L);
        assertThat(eventCaptor.getValue().email()).isEqualTo("user@example.com");
        assertThat(eventCaptor.getValue().createdAt()).isNotNull();
    }

    @Test
    void rejectsDuplicateEmailWithoutSavingOrPublishing() {
        UserRepository repository = mock(UserRepository.class);
        EventPublisher publisher = mock(EventPublisher.class);
        UserServiceImpl service = new UserServiceImpl(repository, new UserMapper(), publisher);
        when(repository.existsByEmail("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.createUser(new CreateUserRequest("Test User", "user@example.com")))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessage("User already exists with email: user@example.com");
        verify(repository, never()).save(any(User.class));
        verify(publisher, never()).publishUserCreated(any(UserCreatedEvent.class));
    }
}
