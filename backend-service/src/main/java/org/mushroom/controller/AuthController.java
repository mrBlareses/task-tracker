package org.mushroom.controller;

import lombok.RequiredArgsConstructor;
import org.mushroom.api.AuthenticationApi;
import org.mushroom.model.AuthenticationResponse;
import org.mushroom.model.LoginRequest;
import org.mushroom.model.LogoutResponse;
import org.mushroom.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
public class AuthController implements AuthenticationApi {
    private final AuthService authService;

    @Override
    public ResponseEntity<AuthenticationResponse> loginUser(LoginRequest request) {
        AuthenticationResponse response = authService.authenticate(request);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<LogoutResponse> logoutUser() {
        LogoutResponse response = authService.logout();
        return ResponseEntity.ok(response);
    }
}
