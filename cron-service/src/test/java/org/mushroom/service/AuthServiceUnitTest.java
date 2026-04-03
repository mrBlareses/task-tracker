package org.mushroom.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mushroom.config.AuthProperties;
import org.mushroom.feign.AuthFeignClient;
import org.mushroom.model.AuthenticationResponse;
import org.mushroom.model.LoginRequest;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceUnitTest {

    @Mock
    private AuthFeignClient authFeignClient;

    @Mock
    private AuthProperties authProperties;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("login — возвращает токен если auth endpoint ответил успешно")
    void login_ShouldReturnToken() {
        when(authProperties.getEmail()).thenReturn("service@local");
        when(authProperties.getPassword()).thenReturn("service123");

        AuthenticationResponse response = AuthenticationResponse.builder()
                .token("jwt-token")
                .type("Bearer")
                .expiresIn(3600L)
                .build();

        when(authFeignClient.login(any(LoginRequest.class))).thenReturn(response);

        AuthenticationResponse actual = authService.login();

        assertThat(actual).isSameAs(response);

        ArgumentCaptor<LoginRequest> captor = ArgumentCaptor.forClass(LoginRequest.class);
        verify(authFeignClient).login(captor.capture());

        assertThat(captor.getValue().getEmail()).isEqualTo("service@local");
        assertThat(captor.getValue().getPassword()).isEqualTo("service123");
    }

    @Test
    @DisplayName("login — бросает IllegalStateException если response null")
    void login_ShouldThrow_WhenResponseIsNull() {
        when(authProperties.getEmail()).thenReturn("service@local");
        when(authProperties.getPassword()).thenReturn("service123");
        when(authFeignClient.login(any(LoginRequest.class))).thenReturn(null);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> authService.login()
        );

        assertThat(ex.getMessage()).isEqualTo("Auth endpoint returned empty token");
    }

    @Test
    @DisplayName("login — бросает IllegalStateException если token null")
    void login_ShouldThrow_WhenTokenIsNull() {
        when(authProperties.getEmail()).thenReturn("service@local");
        when(authProperties.getPassword()).thenReturn("service123");
        when(authFeignClient.login(any(LoginRequest.class)))
                .thenReturn(AuthenticationResponse.builder().token(null).build());

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> authService.login()
        );

        assertThat(ex.getMessage()).isEqualTo("Auth endpoint returned empty token");
    }

    @Test
    @DisplayName("login — бросает IllegalStateException если токен пустой")
    void login_ShouldThrow_WhenTokenIsBlank() {
        when(authProperties.getEmail()).thenReturn("service@local");
        when(authProperties.getPassword()).thenReturn("service123");

        when(authFeignClient.login(any(LoginRequest.class)))
                .thenReturn(AuthenticationResponse.builder().token(" ").build());

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> authService.login()
        );

        assertThat(ex.getMessage()).isEqualTo("Auth endpoint returned empty token");
    }
}