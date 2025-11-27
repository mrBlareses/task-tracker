package org.mushroom.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.mushroom.api.UsersApi;
import org.mushroom.model.*;
import org.mushroom.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class UserController implements UsersApi {
    private final UserService userService;

    @Override
    public ResponseEntity<UserResponse> registerUser(UserRegistrationRequest request) {
        UserResponse response = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<UserResponse> getUserById(UUID id) {
        UserResponse profile = userService.getUserById(id);
        return ResponseEntity.ok(profile);
    }

    @Override
    public ResponseEntity<Void> deleteUser(UUID userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<UserResponse> updateUser(UUID userId, @Valid UserUpdateRequest userUpdateRequest) {
        UserResponse updatedProfile = userService.updatedUser(userId, userUpdateRequest);
        return ResponseEntity.ok(updatedProfile);
    }
}
