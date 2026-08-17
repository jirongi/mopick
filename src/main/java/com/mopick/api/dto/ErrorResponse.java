package com.mopick.api.dto;

/** 공통 오류 형식. */
public record ErrorResponse(String code, String message, String requestId) {
}
