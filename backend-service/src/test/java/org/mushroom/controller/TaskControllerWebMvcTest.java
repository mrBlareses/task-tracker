package org.mushroom.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mushroom.mapper.TaskMapper;
import org.mushroom.model.task.TaskCreateRequest;
import org.mushroom.model.task.TaskResponse;
import org.mushroom.model.task.TaskUpdateRequest;
import org.mushroom.service.JwtAuthenticationFilter;
import org.mushroom.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = TaskController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import(TaskControllerWebMvcTest.TestClockConfig.class)
@ActiveProfiles("test")
class TaskControllerWebMvcTest {

    private static final String BASE_URL = "/api/v1/tasks";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean TaskService taskService;
    @MockBean TaskMapper taskMapper;

    @TestConfiguration
    static class TestClockConfig {
        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }
    }

    @Test
    @DisplayName("Получение задачи возвращает 200 если задача существует")
    void shouldReturn200_whenTaskExists() throws Exception {
        UUID id = UUID.randomUUID();

        when(taskService.getTaskById(id)).thenReturn(new org.mushroom.entity.Task());
        when(taskMapper.toResponse(any())).thenReturn(new TaskResponse());

        mockMvc.perform(get(BASE_URL + "/" + id))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Создание задачи возвращает 201 при корректных данных")
    void shouldReturn201_whenCreateTask() throws Exception {
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle("My task");

        TaskResponse response = new TaskResponse();
        response.setId(UUID.randomUUID());

        when(taskService.createTask(any(TaskCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Обновление задачи возвращает 200 при корректных данных")
    void shouldReturn200_whenUpdateTask() throws Exception {
        UUID id = UUID.randomUUID();

        TaskUpdateRequest request = new TaskUpdateRequest();
        request.setTitle("Updated title");
        request.setStatus(TaskUpdateRequest.StatusEnum.TODO);

        when(taskService.updateTask(eq(id), any(TaskUpdateRequest.class)))
                .thenReturn(new TaskResponse());

        mockMvc.perform(put(BASE_URL + "/" + id)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Удаление задачи возвращает 204 если задача удалена")
    void shouldReturn204_whenDeleteTask() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete(BASE_URL + "/" + id))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Создание задачи возвращает 400 при невалидных данных")
    void shouldReturn400_whenCreateTaskInvalidBody() throws Exception {
        TaskCreateRequest request = new TaskCreateRequest();

        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Обновление задачи возвращает 400 при невалидных данных")
    void shouldReturn400_whenUpdateTaskInvalidBody() throws Exception {
        UUID id = UUID.randomUUID();

        TaskUpdateRequest request = new TaskUpdateRequest();

        mockMvc.perform(put(BASE_URL + "/" + id)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Создание задачи возвращает 500 если сервис выбрасывает исключение")
    void shouldReturn500_whenServiceThrowsException() throws Exception {
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle("My task");

        doThrow(new RuntimeException("boom"))
                .when(taskService)
                .createTask(any(TaskCreateRequest.class));

        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }
}