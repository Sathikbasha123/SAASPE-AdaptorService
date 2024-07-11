package com.saaspe.Adaptor.Model;

import lombok.Data;

@Data
public class Token {
	private String token_type;
	private String access_token;
	private String refresh_token;
	private Long expires_in;

}
