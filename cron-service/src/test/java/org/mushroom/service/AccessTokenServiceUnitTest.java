package org.mushroom.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mushroom.model.AuthenticationResponse;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessTokenServiceUnitTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AccessTokenService accessTokenService;

    @Test
    @DisplayName("getAccessToken — получает токен и кеширует его")
    void getAccessToken_ShouldCacheToken() {
        when(authService.login()).thenReturn(AuthenticationResponse.builder()
                .token("cached-token")
                .build());

        String first = accessTokenService.getAccessToken();
        String second = accessTokenService.getAccessToken();

        assertThat(first).isEqualTo("cached-token");
        assertThat(second).isEqualTo("cached-token");

        verify(authService, times(1)).login();
    }

    @Test
    @DisplayName("getAccessToken — если cachedToken blank, запрашивает новый токен")
    void getAccessToken_ShouldRequestNewToken_WhenCachedTokenIsBlank() {
        ReflectionTestUtils.setField(accessTokenService, "cachedToken", "   ");

        when(authService.login()).thenReturn(AuthenticationResponse.builder()
                .token("fresh-token")
                .build());

        String actual = accessTokenService.getAccessToken();

        assertThat(actual).isEqualTo("fresh-token");
        verify(authService, times(1)).login();
    }

    @Test
    @DisplayName("evictToken — очищает кеш и следующий вызов получает новый токен")
    void evictToken_ShouldClearCache() {
        when(authService.login())
                .thenReturn(AuthenticationResponse.builder().token("token-1").build())
                .thenReturn(AuthenticationResponse.builder().token("token-2").build());

        String first = accessTokenService.getAccessToken();
        accessTokenService.evictToken();
        String second = accessTokenService.getAccessToken();

        assertThat(first).isEqualTo("token-1");
        assertThat(second).isEqualTo("token-2");

        verify(authService, times(2)).login();
    }
}