package com.saaspe.Adaptor.Model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Data;

@Data
@JsonInclude(Include.NON_NULL)
public class Microsoft365ErrorResponse {

	private String error;
	private String error_description;
	private String[] error_codes;
	private String timestamp;
	private String trace_id;
	private String correlation_id;
	private String error_uri;

}