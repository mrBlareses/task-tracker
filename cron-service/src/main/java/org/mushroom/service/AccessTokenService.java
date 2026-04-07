package org.mushroom.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mushroom.model.AuthenticationResponse;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccessTokenService {

    private final AuthService authService;

    private String cachedToken;

    public synchronized String getAccessToken() {
        if (cachedToken == null || cachedToken.isBlank()) {
            log.info("No cached access token found, requesting new token");

            AuthenticationResponse response = authService.login();
            cachedToken = response.getToken();

            log.info("Access token cached successfully");
        }

        return cachedToken;
    }

    public synchronized void evictToken() {
        log.warn("Evicting cached access token");
        cachedToken = null;
    }
}