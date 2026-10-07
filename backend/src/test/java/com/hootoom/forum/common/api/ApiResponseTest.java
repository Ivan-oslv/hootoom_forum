package com.hootoom.forum.common.api;

import com.hootoom.forum.common.web.RequestIdFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void shouldIncludeRequestIdInSuccessResponse() {
        MDC.put(RequestIdFilter.MDC_REQUEST_ID, "request-12345678");

        ApiResponse<String> response = ApiResponse.success("ok");

        assertThat(response.code()).isEqualTo("OK");
        assertThat(response.data()).isEqualTo("ok");
        assertThat(response.requestId()).isEqualTo("request-12345678");
    }
}
