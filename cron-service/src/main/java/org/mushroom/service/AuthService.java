package org.mushroom.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mushroom.config.AuthProperties;
import org.mushroom.feign.AuthFeignClient;
import org.mushroom.model.AuthenticationResponse;
import org.mushroom.model.LoginRequest;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthFeignClient authFeignClient;
    private final AuthProperties authProperties;

    public AuthenticationResponse login() {
        log.info("Requesting access token from auth endpoint");

        AuthenticationResponse response = authFeignClient.login(
                LoginRequest.builder()
                        .email(authProperties.getEmail())
                        .password(authProperties.getPassword())
                        .build()
        );

        if (response == null || response.getToken() == null || response.getToken().isBlank()) {
            throw new IllegalStateException("Auth endpoint returned empty token");
        }

        log.info("Successfully obtained access token");
        return response;
    }
}