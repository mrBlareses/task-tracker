package org.mushroom.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mushroom.enums.RoleType;
import org.mushroom.entity.Role;
import org.mushroom.entity.User;
import org.mushroom.exception.ConflictException;
import org.mushroom.exception.ForbiddenActionException;
import org.mushroom.exception.NotFoundException;
import org.mushroom.kafka.EventPublisher;
import org.mushroom.mapper.UserMapper;
import org.mushroom.model.UserRegistrationRequest;
import org.mushroom.model.UserRegistrationResponse;
import org.mushroom.model.UserUpdateRequest;
import org.mushroom.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
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
    private PasswordEncoder passwordEncoder;

    @Mock
    private Clock clock;

    @InjectMocks
    private UserService userService;

    @Mock
    private EventPublisher eventPublisher;

    private final UUID userId = UUID.randomUUID();
    private final String email = "test@example.com";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registerUser_ShouldReturnUserResponse() {
        UserRegistrationRequest request = new UserRegistrationRequest()
                .email(email)
                .password("password")
                .firstName("John")
                .lastName("Doe");

        User userEntity = User.builder()
                .email(email)
                .password("encodedPassword")
                .firstname("John")
                .lastname("Doe")
                .build();

        userEntity.getRoles().add(Role.builder().name(RoleType.USER).user(userEntity).build());

        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(clock.instant()).thenReturn(Instant.now());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(userRepository.save(any(User.class))).thenReturn(userEntity);

        UserRegistrationResponse responseDto = new UserRegistrationResponse()
                .email(email)
                .firstName("John")
                .lastName("Doe");

        when(userMapper.toRegistrationResponse(userEntity)).thenReturn(responseDto);

        UserRegistrationResponse response = userService.registerUser(request);

        assertThat(response.getEmail()).isEqualTo(email);
        assertThat(response.getFirstName()).isEqualTo("John");
        assertThat(response.getLastName()).isEqualTo("Doe");
    }

    @Test
    void registerUser_ShouldStillThrowConflictException() {
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(clock.instant()).thenReturn(Instant.now());
        when(passwordEncoder.encode(anyString())).thenReturn("12w3e4r5t6y7ui8");

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
    void updatedUser_ForbiddenWhenUpdatingOthersProfile() {
        User currentUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@test.com")
                .firstname("John")
                .lastname("Doe")
                .build();
        currentUser.getRoles().add(Role.builder().name(RoleType.USER).user(currentUser).build());

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(currentUser);
        SecurityContextHolder.getContext().setAuthentication(auth);

        UUID otherUserId = UUID.randomUUID();
        User otherUser = User.builder().id(otherUserId).email("other@test.com").build();
        when(userRepository.findById(otherUserId)).thenReturn(Optional.of(otherUser));

        UserUpdateRequest request = new UserUpdateRequest().firstName("test");

        ForbiddenActionException exception = assertThrows(ForbiddenActionException.class,
                () -> userService.updatedUser(otherUserId, request));

        assertThat(exception.getMessage())
                .isEqualTo("You can only update your profile");
    }

    @Test
    void deleteUser_ForbiddenWhenDeletingOthersAccount() {
        User currentUser = User.builder()
                .id(UUID.randomUUID())
                .email("current@test.com")
                .build();
        currentUser.getRoles().add(Role.builder().name(RoleType.USER).user(currentUser).build());

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(currentUser);
        SecurityContextHolder.getContext().setAuthentication(auth);

        UUID otherUserId = UUID.randomUUID();
        User otherUser = User.builder().id(otherUserId).email("other@test.com").build();
        when(userRepository.findById(otherUserId)).thenReturn(Optional.of(otherUser));

        ForbiddenActionException exception = assertThrows(ForbiddenActionException.class,
                () -> userService.deleteUser(otherUserId));

        assertThat(exception.getMessage())
                .isEqualTo("You can only delete your account");
    }

    @Test
    void updatedUser_ShouldThrowForbiddenActionExceptionWhenUpdatingOthers() {
        User currentUser = User.builder()
                .id(UUID.randomUUID())
                .email("current@test.com")
                .build();
        currentUser.getRoles().add(Role.builder().name(RoleType.USER).user(currentUser).build());

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(currentUser);
        SecurityContextHolder.getContext().setAuthentication(auth);

        UUID otherUserId = UUID.randomUUID();
        User otherUser = User.builder().id(otherUserId).email("other@test.com").build();
        when(userRepository.findById(otherUserId)).thenReturn(Optional.of(otherUser));

        UserUpdateRequest request = new UserUpdateRequest().firstName("Tom");

        ForbiddenActionException ex = assertThrows(ForbiddenActionException.class, () -> userService.updatedUser(otherUserId, request));
        assertThat(ex.getMessage()).isEqualTo("You can only update your profile");
    }

    @Test
    void getUserById_ShouldThrowNotFoundException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> userService.getUserById(userId));
        assertThat(ex.getMessage()).contains(userId.toString());
    }

    @Test
    void deleteUser_ShouldDeleteOwnAccount() {
        UUID currentUserId = UUID.randomUUID();
        User currentUser = User.builder()
                .id(currentUserId)
                .email("me@test.com")
                .build();

        currentUser.getRoles().add(Role.builder().name(RoleType.USER).user(currentUser).build());

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(currentUser);

        SecurityContextHolder.getContext().setAuthentication(auth);

        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        userService.deleteUser(currentUserId);
    }
}