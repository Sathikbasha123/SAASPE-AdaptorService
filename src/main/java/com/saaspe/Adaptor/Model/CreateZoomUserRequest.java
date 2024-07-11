package com.saaspe.Adaptor.Model;

import lombok.Data;

@Data
public class CreateZoomUserRequest {
    private String action;
    private UserInfo user_info;

    @Data
    public static class UserInfo {
        private String email;
        private String first_name;
        private String last_name;
        private String display_name;
        private String password;
        private int type;
    }
}



