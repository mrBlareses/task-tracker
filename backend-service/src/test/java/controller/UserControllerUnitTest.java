package controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mushroom.controller.UserController;
import org.mushroom.model.UserProfileResponse;
import org.mushroom.model.UserRegistrationRequest;
import org.mushroom.model.UserResponse;
import org.mushroom.model.UserUpdateRequest;
import org.mushroom.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserControllerUnitTest {

    private UserService userService;
    private UserController userController;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        userController = new UserController(userService);
    }

    @Test
    void registerUser_success() {
        UserRegistrationRequest request = new UserRegistrationRequest()
                .email("test@mail.com")
                .firstName("John")
                .lastName("Doe")
                .password("pass");

        UserResponse response = new UserResponse()
                .email("test@mail.com")
                .firstName("John")
                .lastName("Doe");

        when(userService.registerUser(request)).thenReturn(response);

        ResponseEntity<UserResponse> result = userController.registerUser(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    void registerUser_conflict() {
        UserRegistrationRequest request = new UserRegistrationRequest()
                .email("exists@mail.com")
                .password("pass");

        when(userService.registerUser(request))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate email"));

        assertThrows(ResponseStatusException.class,
                () -> userController.registerUser(request));
    }

    @Test
    void getUserByEmail_success() {
        String email = "test@mail.com";

        UserProfileResponse profile = new UserProfileResponse()
                .email(email);

        when(userService.getUserByEmail(email)).thenReturn(profile);

        ResponseEntity<UserProfileResponse> result = userController.getUserByEmail(email);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(profile, result.getBody());
    }

    @Test
    void getUserByEmail_notFound() {
        String email = "missing@mail.com";

        when(userService.getUserByEmail(email))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        assertThrows(ResponseStatusException.class,
                () -> userController.getUserByEmail(email));
    }

    @Test
    void getUserById_success() {
        UUID id = UUID.randomUUID();

        UserProfileResponse profile = new UserProfileResponse()
                .id(id);

        when(userService.getUserById(id)).thenReturn(profile);

        ResponseEntity<UserProfileResponse> result = userController.getUserById(id);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(profile, result.getBody());
    }

    @Test
    void getUserById_notFound() {
        UUID id = UUID.randomUUID();

        when(userService.getUserById(id))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        assertThrows(ResponseStatusException.class,
                () -> userController.getUserById(id));
    }

    @Test
    void deleteUser_success() {
        UUID id = UUID.randomUUID();

        doNothing().when(userService).deleteUser(id);

        ResponseEntity<Void> result = userController.deleteUser(id);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        assertNull(result.getBody());
    }

    @Test
    void deleteUser_notFound() {
        UUID id = UUID.randomUUID();

        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
                .when(userService).deleteUser(id);

        assertThrows(ResponseStatusException.class,
                () -> userController.deleteUser(id));
    }

    @Test
    void updateUser_success() {
        UUID id = UUID.randomUUID();

        UserUpdateRequest request = new UserUpdateRequest()
                .firstName("NewFirstName");

        UserProfileResponse updatedProfile = new UserProfileResponse()
                .id(id)
                .firstName("NewFirstName");

        when(userService.updatedUser(id, request)).thenReturn(updatedProfile);

        ResponseEntity<UserProfileResponse> result = userController.updateUser(id, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(updatedProfile, result.getBody());
    }

    @Test
    void updateUser_notFound() {
        UUID id = UUID.randomUUID();

        UserUpdateRequest request = new UserUpdateRequest()
                .firstName("NewFirstName");

        when(userService.updatedUser(id, request))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        assertThrows(ResponseStatusException.class,
                () -> userController.updateUser(id, request));
    }
}
