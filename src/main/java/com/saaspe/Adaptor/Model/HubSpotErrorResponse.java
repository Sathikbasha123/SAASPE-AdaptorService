package com.saaspe.Adaptor.Model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Data;

@Data
@JsonInclude(Include.NON_NULL)
public class HubSpotErrorResponse {

	private String error;
	private String error_description;
	private String message;
}
