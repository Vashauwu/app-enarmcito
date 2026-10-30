package com.empresa.medicina.common;

import java.util.List;

import org.springframework.http.HttpStatus;

/**
 * Error de negocio que se traduce directamente a una respuesta {@link ApiError}.
 */
public class ApiException extends RuntimeException {

	private final HttpStatus status;

	private final String code;

	private final List<ApiError.FieldError> fieldErrors;

	public ApiException(HttpStatus status, String code, String message) {
		this(status, code, message, List.of());
	}

	public ApiException(HttpStatus status, String code, String message, List<ApiError.FieldError> fieldErrors) {
		super(message);
		this.status = status;
		this.code = code;
		this.fieldErrors = fieldErrors;
	}

	public static ApiException badRequest(String code, String message) {
		return new ApiException(HttpStatus.BAD_REQUEST, code, message);
	}

	public static ApiException notFound(String code, String message) {
		return new ApiException(HttpStatus.NOT_FOUND, code, message);
	}

	public static ApiException forbidden(String code, String message) {
		return new ApiException(HttpStatus.FORBIDDEN, code, message);
	}

	public HttpStatus getStatus() {
		return this.status;
	}

	public String getCode() {
		return this.code;
	}

	public ApiError toApiError() {
		return new ApiError(this.code, getMessage(), this.fieldErrors);
	}

}
