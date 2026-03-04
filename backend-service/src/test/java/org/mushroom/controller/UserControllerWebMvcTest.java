package org.mushroom.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mushroom.model.UserRegistrationRequest;
import org.mushroom.model.UserRegistrationResponse;
import org.mushroom.model.UserResponse;
import org.mushroom.model.UserUpdateRequest;
import org.mushroom.service.JwtAuthenticationFilter;
import org.mushroom.service.UserService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = UserController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import(UserControllerWebMvcTest.TestClockConfig.class)
@ActiveProfiles("test")
class UserControllerWebMvcTest {

    private static final String REGISTER_URL = "/api/v1/users/register";
    private static final String USER_BY_ID_URL = "/api/v1/users/{id}";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    UserService userService;

    @TestConfiguration
    static class TestClockConfig {
        @Bean
        Clock clock() {
            return Clock.system(ZoneId.of("UTC"));
        }
    }

    @Test
    @DisplayName("Регистрация пользователя возвращает 201 при корректных данных")
    void shouldReturn201_whenRegisterUserValid() throws Exception {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setEmail("user@example.com");
        request.setPassword("password123");
        request.setFirstName("John");
        request.setLastName("Doe");

        UserRegistrationResponse response = new UserRegistrationResponse();
        response.setId(UUID.randomUUID());
        response.setEmail("user@example.com");
        response.setFirstName("John");
        response.setLastName("Doe");

        when(userService.registerUser(any(UserRegistrationRequest.class))).thenReturn(response);

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Получение пользователя возвращает 200 если пользователь существует")
    void shouldReturn200_whenGetUserById() throws Exception {
        UUID id = UUID.randomUUID();

        when(userService.getUserById(id)).thenReturn(new UserResponse());

        mockMvc.perform(get(USER_BY_ID_URL, id))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Обновление пользователя возвращает 200 при корректных данных")
    void shouldReturn200_whenUpdateUserValid() throws Exception {
        UUID id = UUID.randomUUID();

        UserUpdateRequest request = new UserUpdateRequest();
        request.setEmail("new-email@example.com");
        request.setFirstName("Jane");
        request.setLastName("Smith");

        when(userService.updatedUser(eq(id), any(UserUpdateRequest.class))).thenReturn(new UserResponse());

        mockMvc.perform(put(USER_BY_ID_URL, id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Удаление пользователя возвращает 204 если пользователь удален")
    void shouldReturn204_whenDeleteUser() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete(USER_BY_ID_URL, id))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Регистрация пользователя возвращает 400 при невалидных данных")
    void shouldReturn400_whenRegisterUserInvalid() throws Exception {
        UserRegistrationRequest request = new UserRegistrationRequest();

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Регистрация возвращает 500 если Content-Type отсутствует и ошибка обрабатывается GlobalExceptionHandler")
    void shouldReturn500_whenRegisterUserNoContentType() throws Exception {
        mockMvc.perform(post("/api/v1/users/register")
                        .content("{}"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Регистрация пользователя возвращает 500 если сервис выбрасывает исключение")
    void shouldReturn500_whenRegisterUserServiceThrows() throws Exception {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setEmail("user@example.com");
        request.setPassword("password123");

        doThrow(new RuntimeException("boom"))
                .when(userService).registerUser(any(UserRegistrationRequest.class));

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }
}