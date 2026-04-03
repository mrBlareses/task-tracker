package org.mushroom.controller;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.mushroom.entity.enums.JobExecutionStatus;
import org.mushroom.entity.enums.TriggerType;


import org.mushroom.exception.GlobalExceptionHandler;
import org.mushroom.model.JobExecutionResponse;
import org.mushroom.service.DailyAnalyticsTriggerService;
import org.mushroom.service.JobExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mushroom.entity.enums.TriggerType.MANUAL;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DailyAnalyticsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
class DailyAnalyticsControllerWebMvcTest {

    private static final String RUN_URL = "/api/v1/internal/cron/daily-analytics/run";
    private static final String EXECUTIONS_URL = "/api/v1/internal/cron/daily-analytics/executions";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DailyAnalyticsTriggerService dailyAnalyticsTriggerService;

    @MockBean
    private JobExecutionService jobExecutionService;

    @Test
    @DisplayName("POST /run — возвращает 202 и вызывает trigger с MANUAL")
    void runDailyAnalyticsManually_ShouldReturnAccepted() throws Exception {
        mockMvc.perform(post(RUN_URL))
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));

        verify(dailyAnalyticsTriggerService).trigger(MANUAL);
    }

    @Test
    @DisplayName("GET /executions — возвращает 200 и список execution history")
    void getRecentExecutions_ShouldReturnExecutionList() throws Exception {
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        OffsetDateTime startedAt = OffsetDateTime.parse("2026-04-02T12:00:00+03:00");
        OffsetDateTime finishedAt = OffsetDateTime.parse("2026-04-02T12:01:00+03:00");

        List<JobExecutionResponse> responses = List.of(
                JobExecutionResponse.builder()
                        .id(id)
                        .jobName("DAILY_ANALYTICS")
                        .status(JobExecutionStatus.SUCCESS)
                        .triggerType(TriggerType.CRON)
                        .startedAt(startedAt)
                        .finishedAt(finishedAt)
                        .build()
        );

        when(jobExecutionService.getRecentExecutions("DAILY_ANALYTICS")).thenReturn(responses);

        mockMvc.perform(get(EXECUTIONS_URL).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$[0].jobName").value("DAILY_ANALYTICS"))
                .andExpect(jsonPath("$[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$[0].triggerType").value("CRON"))
                .andExpect(jsonPath("$[0].startedAt").value("2026-04-02T12:00:00+03:00"))
                .andExpect(jsonPath("$[0].finishedAt").value("2026-04-02T12:01:00+03:00"));
    }

    @Test
    @DisplayName("GET /executions — возвращает 200 и пустой список")
    void getRecentExecutions_ShouldReturnEmptyList() throws Exception {
        when(jobExecutionService.getRecentExecutions("DAILY_ANALYTICS")).thenReturn(List.of());

        mockMvc.perform(get(EXECUTIONS_URL).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("POST /run — возвращает 400 если сервис выбрасывает IllegalArgumentException")
    void runDailyAnalyticsManually_ShouldReturn400_WhenIllegalArgumentThrown() throws Exception {
        doThrow(new IllegalArgumentException("bad request"))
                .when(dailyAnalyticsTriggerService)
                .trigger(MANUAL);

        mockMvc.perform(post(RUN_URL))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("bad request"))
                .andExpect(jsonPath("$.path").value(RUN_URL))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("POST /run — возвращает 500 если сервис выбрасывает IllegalStateException")
    void runDailyAnalyticsManually_ShouldReturn500_WhenIllegalStateThrown() throws Exception {
        doThrow(new IllegalStateException("internal error"))
                .when(dailyAnalyticsTriggerService)
                .trigger(MANUAL);

        mockMvc.perform(post(RUN_URL))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("internal error"))
                .andExpect(jsonPath("$.path").value(RUN_URL))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("POST /run — возвращает 503 если сервис выбрасывает FeignException 503")
    void runDailyAnalyticsManually_ShouldReturn503_WhenFeign503Thrown() throws Exception {
        doThrow(feignException(503))
                .when(dailyAnalyticsTriggerService)
                .trigger(MANUAL);

        mockMvc.perform(post(RUN_URL))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("Service Unavailable"))
                .andExpect(jsonPath("$.message").value("Backend service is temporarily unavailable"))
                .andExpect(jsonPath("$.path").value(RUN_URL))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("GET /run — возвращает 405 при неверном HTTP method")
    void runDailyAnalyticsManually_ShouldReturn405_WhenWrongMethod() throws Exception {
        mockMvc.perform(get(RUN_URL))
                .andExpect(status().isMethodNotAllowed());
    }

    private FeignException feignException(int status) {
        return FeignException.errorStatus(
                "triggerDailyAnalytics",
                feign.Response.builder()
                        .status(status)
                        .reason("error")
                        .request(request())
                        .headers(Collections.emptyMap())
                        .body(new byte[0])
                        .build()
        );
    }

    private Request request() {
        return Request.create(
                Request.HttpMethod.POST,
                "http://backend-service:8081/api/v1/service/daily-analytics/run",
                Collections.emptyMap(),
                null,
                StandardCharsets.UTF_8,
                new RequestTemplate()
        );
    }
}