package com.saaspe.Adaptor.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuickBooksToken {
	
	private String accessToken;
	private String refreshToken;
	private Long expiresIn;
	private Long xRefreshTokenExpiresIn;

}
