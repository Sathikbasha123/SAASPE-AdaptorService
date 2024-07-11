package com.saaspe.Adaptor.Model;

import lombok.Data;

@Data
public class ZoomToken {
	private String token_type;
	private String scope;
	private Long expires_in;
	private String access_token;
	private String refresh_token;	
}
