package com.saaspe.Adaptor.Model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Data;

@Data
@JsonInclude(Include.NON_NULL)
public class AdaptorFields {

	private String applicationName;
	private String applicationId;
	private String userId;
	private String userName;
}
