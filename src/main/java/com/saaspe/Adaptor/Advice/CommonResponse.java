package com.saaspe.Adaptor.Advice;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommonResponse {

	private HttpStatus status;
	private Response response;
	private Object message;
}
