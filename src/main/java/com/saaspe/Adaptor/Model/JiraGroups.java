package com.saaspe.Adaptor.Model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class JiraGroups {
    private String header;
    private int total;
    @JsonProperty("groups")
    private List<Group> groups;


@Data
@NoArgsConstructor
public static class Group {
    private String name;
    private String groupId;
}
}