package com.saaspe.Adaptor.Model;

import lombok.Data;
@Data
public class Microsoft365Token {
		private String token_type;
		private String scope;
		private Long expires_in;
		private Long ext_expires_in;
		private Long expires_on;
		private Long not_before;
		private Long resource;
		private String access_token;
		private String refresh_token;
		private String id_token;
		

	}

