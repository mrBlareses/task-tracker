package service;
import org.junit.jupiter.api.Test;
import org.mushroom.entity.User;
import org.mushroom.exception.ConflictException;
import org.mushroom.exception.NotFoundException;
import org.mushroom.mapper.UserMapper;
import org.mushroom.model.UserProfileResponse;
import org.mushroom.model.UserRegistrationRequest;
import org.mushroom.model.UserUpdateRequest;
import org.mushroom.repository.UserRepository;
import org.mushroom.service.UserService;
import org.mushroom.util.DateTimeUtil;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class UserServiceUnitTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final DateTimeUtil dateTimeUtil = mock(DateTimeUtil.class);

    private final UserService userService = new UserService(
            userRepository,
            userMapper,
            dateTimeUtil
    );

    @Test
    void registerUser_success() {
        UserRegistrationRequest request = new UserRegistrationRequest()
                .email("test@mail.com")
                .firstName("John")
                .lastName("Doe")
                .password("pass");

        OffsetDateTime now = OffsetDateTime.now();
        when(dateTimeUtil.nowOffsetDateTime()).thenReturn(now);

        User saved = User.builder()
                .email("test@mail.com")
                .firstname("John")
                .lastname("Doe")
                .password("pass")
                .createdAt(now)
                .updatedAt(now)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(saved);

        var response = userService.registerUser(request);

        assertEquals("test@mail.com", response.getEmail());
        assertEquals("John", response.getFirstName());
        assertEquals("Doe", response.getLastName());
    }

    @Test
    void registerUser_duplicateEmail_shouldThrowConflict() {
        UserRegistrationRequest request = new UserRegistrationRequest()
                .email("exists@mail.com")
                .password("pass");

        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate"));

        assertThrows(ConflictException.class,
                () -> userService.registerUser(request));
    }
    @Test
    void getUserByEmail_success() {
        User user = new User();
        user.setEmail("test@mail.com");

        UserProfileResponse profile = new UserProfileResponse();
        profile.setEmail("test@mail.com");

        when(userRepository.findByEmail("test@mail.com"))
                .thenReturn(Optional.of(user));

        when(userMapper.toProfileResponse(user))
                .thenReturn(profile);

        UserProfileResponse response = userService.getUserByEmail("test@mail.com");

        assertEquals("test@mail.com", response.getEmail());
    }
    @Test
    void getUserByEmail_notFound() {
        when(userRepository.findByEmail("missing@mail.com"))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> userService.getUserByEmail("missing@mail.com"));
    }

    @Test
    void deleteUser_success() {
        UUID id = UUID.randomUUID();

        when(userRepository.existsById(id)).thenReturn(true);

        userService.deleteUser(id);

        verify(userRepository).deleteById(id);
    }

    @Test
    void deleteUser_notFound() {
        UUID id = UUID.randomUUID();

        when(userRepository.existsById(id)).thenReturn(false);

        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> userService.deleteUser(id));
    }

    @Test
    void updatedUser_success() {
        UUID id = UUID.randomUUID();
        User existing = new User();
        existing.setId(id);

        UserUpdateRequest req = new UserUpdateRequest();
        req.setFirstName("NewFirstName");

        User updated = new User();
        updated.setId(id);
        updated.setFirstname("NewFirstName");

        UserProfileResponse profile = new UserProfileResponse();
        profile.setId(id);
        profile.setFirstName("NewFirstName");

        when(userRepository.findById(id)).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(updated);
        when(userMapper.toProfileResponse(updated)).thenReturn(profile);

        UserProfileResponse response = userService.updatedUser(id, req);

        assertEquals("NewFirstName", response.getFirstName());
        assertEquals(id, response.getId());
    }
}