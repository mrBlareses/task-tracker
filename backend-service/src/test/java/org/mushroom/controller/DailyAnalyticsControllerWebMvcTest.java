package org.mushroom.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mushroom.service.DailyAnalyticsService;
import org.mushroom.service.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = DailyAnalyticsController.class,
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = JwtAuthenticationFilter.class
                )
        }
)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class DailyAnalyticsControllerWebMvcTest {

    private static final String URL = "/api/v1/service/daily-analytics/run";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DailyAnalyticsService dailyAnalyticsService;

    @MockBean
    private Clock clock;

    @Test
    @DisplayName("Контроллер возвращает 202 при успешном запуске аналитики")
    void shouldReturn202_whenRequestIsValid() throws Exception {
        mockMvc.perform(post(URL))
                .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("Контроллер возвращает 405 если используется неверный HTTP метод")
    void shouldReturn405_whenWrongHttpMethod() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("Контроллер возвращает 404 если URL не существует")
    void shouldReturn404_whenWrongUrl() throws Exception {
        mockMvc.perform(post("/api/v1/service/wrong-endpoint"))
                .andExpect(status().isNotFound());
    }
}