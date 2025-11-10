package org.mushroom.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.mushroom.api.UsersApi;
import org.mushroom.model.UserProfileResponse;
import org.mushroom.model.UserRegistrationRequest;
import org.mushroom.model.UserResponse;
import org.mushroom.model.UserUpdateRequest;
import org.mushroom.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class UserController implements UsersApi {
    private final UserService userService;

    @Override
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody UserRegistrationRequest request) {
        UserResponse response = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @Override
    public ResponseEntity<UserProfileResponse> getUserByEmail(@PathVariable String email) {
        UserProfileResponse profile = userService.getUserByEmail(email);
        return ResponseEntity.ok(profile);
    }

    @Override
    public ResponseEntity<UserProfileResponse> getUserById(@PathVariable UUID id) {
        UserProfileResponse profile = userService.getUserById(id);
        return ResponseEntity.ok(profile);
    }

    @Override
    public ResponseEntity<Void> deleteUser(@PathVariable("id") UUID userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<UserProfileResponse> updateUser(@PathVariable("id") UUID userId,
                                                          @Valid @RequestBody UserUpdateRequest userUpdateRequest) {
        UserProfileResponse updatedProfile = userService.updatedUser(userId, userUpdateRequest);
        return ResponseEntity.ok(updatedProfile);
    }
}
