package com.owl2gendl.api;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.owl2gendl.api.dto.ErrorResponseDto;

/**
 * Converts request problems into clean JSON error bodies instead of raw stack traces / bare 500s. Bean
 * Validation failures (@Valid on the DTOs) and our own {@link InvalidRequestException}/{@link
 * IllegalArgumentException} (unknown construct/variant names, malformed job/variant ids in path variables)
 * all become 400s with a readable message; anything unexpected becomes a generic 500 that doesn't leak
 * internals to the client.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponseDto> handleValidation(MethodArgumentNotValidException e) {
		List<String> details = e.getBindingResult().getFieldErrors().stream()
				.map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
				.toList();
		return ResponseEntity.badRequest().body(new ErrorResponseDto("Invalid request", details));
	}

	@ExceptionHandler({InvalidRequestException.class, IllegalArgumentException.class})
	public ResponseEntity<ErrorResponseDto> handleInvalidRequest(RuntimeException e) {
		return ResponseEntity.badRequest().body(new ErrorResponseDto(e.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponseDto> handleUnexpected(Exception e) {
		log.error("Unhandled exception", e);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ErrorResponseDto("An unexpected error occurred"));
	}
}
