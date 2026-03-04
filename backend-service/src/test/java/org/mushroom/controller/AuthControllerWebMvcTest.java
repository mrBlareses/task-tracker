package org.mushroom.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mushroom.model.AuthenticationResponse;
import org.mushroom.model.LoginRequest;
import org.mushroom.model.LogoutResponse;
import org.mushroom.service.AuthService;
import org.mushroom.service.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.ZoneId;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AuthController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import(AuthControllerWebMvcTest.TestClockConfig.class)
@ActiveProfiles("test")
class AuthControllerWebMvcTest {

    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String LOGOUT_URL = "/api/v1/auth/logout";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    AuthService authService;

    @TestConfiguration
    static class TestClockConfig {
        @Bean
        Clock clock() {
            return Clock.system(ZoneId.of("UTC+3"));
        }
    }

    @Test
    @DisplayName("Контроллер возвращает 200 при успешной авторизации")
    void shouldReturn200_whenLoginSuccess() throws Exception {

        LoginRequest request = new LoginRequest();
        request.setEmail("user@test.com");
        request.setPassword("password123");

        when(authService.authenticate(any(LoginRequest.class)))
                .thenReturn(new AuthenticationResponse());

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Контроллер возвращает 200 при успешном выходе из системы")
    void shouldReturn200_whenLogoutSuccess() throws Exception {

        when(authService.logout())
                .thenReturn(new LogoutResponse());

        mockMvc.perform(post(LOGOUT_URL))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Контроллер возвращает 400 при невалидном запросе авторизации")
    void shouldReturn400_whenLoginInvalid() throws Exception {

        LoginRequest request = new LoginRequest();

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }


    @Test
    @DisplayName("Контроллер возвращает 500 если сервис авторизации выбрасывает исключение")
    void shouldReturn500_whenLoginServiceThrows() throws Exception {

        LoginRequest request = new LoginRequest();
        request.setEmail("user@test.com");
        request.setPassword("password123");

        doThrow(new RuntimeException("boom"))
                .when(authService)
                .authenticate(any(LoginRequest.class));

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Контроллер возвращает 500 если сервис выхода выбрасывает исключение")
    void shouldReturn500_whenLogoutServiceThrows() throws Exception {

        doThrow(new RuntimeException("boom"))
                .when(authService)
                .logout();

        mockMvc.perform(post(LOGOUT_URL))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Контроллер возвращает 405 при использовании неверного HTTP метода")
    void shouldReturn405_whenWrongMethod() throws Exception {

        mockMvc.perform(get(LOGIN_URL))
                .andExpect(status().isMethodNotAllowed());
    }
}
