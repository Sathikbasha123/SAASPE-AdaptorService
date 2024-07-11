package com.saaspe.Adaptor.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccessTokenResponse {
    
	 private String access_token;
	 private String refresh_token;
	 private String scope;
	 private String api_domain;
	 private String token_type;
	 private String expires_in;
}
