package com.saaspe.Adaptor.Advice;

import org.springframework.http.HttpStatus;

import com.zoho.crm.api.util.Choice;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DataValidationException extends Exception {
	
	private static final long serialVersionUID = 1L;
	public DataValidationException(Choice<String> status, String message2, String value) {
	}
	private HttpStatus statusCode;
	private String message;
	private String errorCode;
	

}