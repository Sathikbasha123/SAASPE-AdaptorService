package com.saaspe.Adaptor.Model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GitlabAccessRole {
	private String accessRole;
	private int accessLevel;
}
