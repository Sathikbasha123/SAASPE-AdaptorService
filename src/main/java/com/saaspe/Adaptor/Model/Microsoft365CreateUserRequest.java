package com.saaspe.Adaptor.Model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
public class Microsoft365CreateUserRequest {
    @JsonProperty("accountEnabled")
    private boolean accountEnabled;

    @JsonProperty("mail")
    private String mail;

    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("mailNickname")
    private String mailNickname;

    @JsonProperty("userPrincipalName")
    private String userPrincipalName;

    @JsonProperty("passwordProfile")
    private PasswordProfile passwordProfile;

    @Data
    public static class PasswordProfile {
        @JsonProperty("forceChangePasswordNextSignIn")
        private boolean forceChangePasswordNextSignIn;

        @JsonProperty("password")
        private String password;
    }
}

