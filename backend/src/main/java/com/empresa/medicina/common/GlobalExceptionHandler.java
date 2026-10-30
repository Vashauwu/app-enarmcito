package com.empresa.medicina.common;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Traduce las excepciones a respuestas {@link ApiError} con un formato uniforme.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

	private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	ResponseEntity<ApiError> handleApiException(ApiException ex) {
		return ResponseEntity.status(ex.getStatus()).body(ex.toApiError());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
		List<ApiError.FieldError> fieldErrors = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.map((error) -> new ApiError.FieldError(error.getField(), error.getDefaultMessage()))
			.toList();
		return ResponseEntity.badRequest()
			.body(new ApiError("VALIDACION", "Revisa los datos marcados en el formulario.", fieldErrors));
	}

	@ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class })
	ResponseEntity<ApiError> handleUnreadable(Exception ex) {
		return ResponseEntity.badRequest().body(new ApiError("SOLICITUD_INVALIDA", "La solicitud no es válida."));
	}

	@ExceptionHandler(NoResourceFoundException.class)
	ResponseEntity<ApiError> handleNoResource(NoResourceFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
			.body(new ApiError("NO_ENCONTRADO", "El recurso solicitado no existe."));
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
		return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
			.body(new ApiError("METODO_NO_PERMITIDO", "Método HTTP no permitido para este recurso."));
	}

	/** Permiso insuficiente detectado por {@code @PreAuthorize}. */
	@ExceptionHandler(AccessDeniedException.class)
	ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
			.body(new ApiError("ACCESO_DENEGADO", "No tienes permiso para realizar esta acción."));
	}

	/** Dos personas modificaron el mismo registro a la vez. */
	@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
	ResponseEntity<ApiError> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
			.body(new ApiError("CONFLICTO_EDICION",
					"Otra persona modificó este registro al mismo tiempo. Recarga e inténtalo de nuevo."));
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiError> handleUnexpected(Exception ex) {
		logger.error("Error no controlado", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(new ApiError("ERROR_INTERNO", "Ocurrió un error inesperado. Inténtalo de nuevo."));
	}

}
