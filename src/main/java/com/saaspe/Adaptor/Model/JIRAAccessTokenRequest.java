package com.saaspe.Adaptor.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class JIRAAccessTokenRequest {
	private String clientId;
	private String clientSecret;
	private String code;
	private String redirectURL;
}