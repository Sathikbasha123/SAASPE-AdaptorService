package com.saaspe.Adaptor.Advice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApplicationExceptionHandler {
	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<CommonResponse> handleMissingServletRequestParameterException(
			MissingServletRequestParameterException ex) {
		return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
				new Response("Missing Form fields", null), ex.getParameterName() + " field is missing"),
				HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(MissingRequestHeaderException.class)
	public ResponseEntity<CommonResponse> handleMissingRequestHeaderException(MissingRequestHeaderException ex) {
		return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
				new Response("Missing Form fields", null), "Missing header : " + ex.getHeaderName()),
				HttpStatus.BAD_REQUEST);
	}
	
}
