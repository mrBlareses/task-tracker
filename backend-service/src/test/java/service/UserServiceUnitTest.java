package service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mushroom.entity.User;
import org.mushroom.exception.ConflictException;
import org.mushroom.exception.NotFoundException;
import org.mushroom.mapper.UserMapper;
import org.mushroom.model.UserRegistrationRequest;
import org.mushroom.model.UserUpdateRequest;
import org.mushroom.repository.UserRepository;
import org.mushroom.service.UserService;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private Clock clock;

    @InjectMocks
    private UserService userService;

    private final UUID userId = UUID.randomUUID();
    private final String email = "test@example.com";

    @BeforeEach
    void setupClock() {
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(clock.instant()).thenReturn(Instant.now());
    }


    @Test
    void registerUser_ShouldStillThrowConflictException() {
        setupClock();

        UserRegistrationRequest request = new UserRegistrationRequest()
                .email(email)
                .password("password")
                .firstName("John")
                .lastName("Doe");

        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("could not execute statement"));

        assertThrows(ConflictException.class, () -> userService.registerUser(request));
    }

    @Test
    void getUserById_ShouldThrowNotFoundExceptionWithCorrectId() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> userService.getUserById(userId));

        assertTrue(exception.getMessage().contains(userId.toString()));
    }

    @Test
    void getUserById_ShouldPropagateIt() {
        when(userRepository.findById(userId))
                .thenThrow(new RuntimeException("Database connection failed"));

        assertThrows(RuntimeException.class, () -> userService.getUserById(userId));
    }

    @Test
    void updateUserd_ShouldThrowNotFoundException() {
        UserUpdateRequest updateRequest = new UserUpdateRequest()
                .email("new@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> userService.updatedUser(userId, updateRequest));

        assertTrue(exception.getMessage().contains(userId.toString()));
    }

    @Test
    void updateUser_ShouldThrowConflictException() {
        setupClock();
        UserUpdateRequest updateRequest = new UserUpdateRequest()
                .email("existing@example.com");

        User existingUser = User.builder()
                .id(userId)
                .email(email)
                .firstname("John")
                .lastname("Doe")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(existingUser))
                .thenThrow(new DataIntegrityViolationException("Duplicate email"));

        assertThrows(ConflictException.class,
                () -> userService.updatedUser(userId, updateRequest));
    }

    @Test
    void updateUser_ShouldPropagateIt() {
        setupClock();
        UserUpdateRequest updateRequest = new UserUpdateRequest()
                .email("new@example.com");

        User existingUser = User.builder()
                .id(userId)
                .email(email)
                .firstname("John")
                .lastname("Doe")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(existingUser))
                .thenThrow(new RuntimeException("Unexpected error"));

        assertThrows(RuntimeException.class,
                () -> userService.updatedUser(userId, updateRequest));
    }

    @Test
    void registerUser_ShouldNotBeCaught() {
        setupClock();
        UserRegistrationRequest request = new UserRegistrationRequest()
                .email(email)
                .password("password")
                .firstName("John")
                .lastName("Doe");

        String expectedMessage = "Some other database error";
        when(userRepository.save(any(User.class)))
                .thenThrow(new RuntimeException(expectedMessage));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.registerUser(request));

        assertEquals(expectedMessage, exception.getMessage());
    }
}