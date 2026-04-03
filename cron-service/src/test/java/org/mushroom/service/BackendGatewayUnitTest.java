package org.mushroom.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mushroom.feign.BackendTriggerFeignClient;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BackendGatewayUnitTest {

    @Mock
    private BackendTriggerFeignClient backendTriggerFeignClient;

    @InjectMocks
    private BackendGateway backendGateway;

    @Test
    @DisplayName("triggerDailyAnalytics — добавляет Bearer prefix и вызывает feign client")
    void triggerDailyAnalytics_ShouldCallFeignClientWithBearerToken() {
        backendGateway.triggerDailyAnalytics("token-1");

        verify(backendTriggerFeignClient).triggerDailyAnalytics("Bearer token-1");
    }
}