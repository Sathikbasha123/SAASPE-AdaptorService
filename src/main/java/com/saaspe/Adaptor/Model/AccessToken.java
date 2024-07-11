package com.saaspe.Adaptor.Model;

import lombok.Data;

@Data
public class AccessToken {
	String access_token;
	String refresh_token;
	long expires_in;
	String scope;

}
