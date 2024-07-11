package com.saaspe.Adaptor.Model;

import lombok.Data;

import java.util.List;
@Data
public class ConfluenceUsersbyGroup {
    private List<UserGroup> results;

    @Data
    public static class UserGroup {
        private String type;
        private String name;
        private String id;
    }
}

