package com.saaspe.Adaptor.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class JIRARefreshTokenRequest {

    private String clientId;
    private String clientSecret;
    private String refreshToken;
    
}
