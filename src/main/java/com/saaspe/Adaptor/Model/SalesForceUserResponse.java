package com.saaspe.Adaptor.Model;

import lombok.Data;

@Data
public class SalesForceUserResponse {

	private String userName;
	private String userId;
	private String userEmail;
	private boolean isActive;

}
