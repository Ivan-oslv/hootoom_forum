package com.hootoom.forum.common.api;

public record ValidationError(String field, String reason) {
}
