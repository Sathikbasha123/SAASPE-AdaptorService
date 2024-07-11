package com.saaspe.Adaptor.Model;

import lombok.Data;

@Data
public class ConfluenceGetUserListResponse {
	
	private String self;
	private String accountId;
	private String accountType;
	private String displayName;
	private Boolean active;

}
