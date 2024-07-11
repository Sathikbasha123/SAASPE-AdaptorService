package com.saaspe.Adaptor.Model;

import java.util.Date;

import org.gitlab.api.models.GitlabGroupMember;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GitLabUserProfileResponse {

	private String name;
	private Integer id;
	private String state;
	private String email;
	private Date createdAt;
	private String avatarUrl;
	private Date currentSignInAt;
	private Date lastActivityOn;
	private String username;

	public GitLabUserProfileResponse(GitlabGroupMember user) {
		this.name = user.getName();
		this.id = user.getId();
		this.state = user.getState();
		this.email = user.getEmail();
		this.createdAt = user.getCreatedAt();
		this.avatarUrl = user.getAvatarUrl();
		this.currentSignInAt = user.getCurrentSignInAt();
		this.lastActivityOn = user.getLastActivityOn();
		this.username = user.getUsername();
	}
}
