package com.saaspe.Adaptor.Model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(content = Include.NON_NULL)
public class AddUserZohoCRMRequest {

	private AddUserRequestCRM[] users;
	
}
