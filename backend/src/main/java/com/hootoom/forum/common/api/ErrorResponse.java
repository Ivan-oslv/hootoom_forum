package com.hootoom.forum.common.api;

import java.util.List;

public record ErrorResponse(
        String code,
        String message,
        List<ValidationError> details,
        String requestId
) {
}
