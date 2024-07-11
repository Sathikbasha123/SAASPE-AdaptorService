package com.saaspe.Adaptor.Model;

import java.util.Date;

import org.gitlab.api.models.GitlabAccessLevel;
import org.gitlab.api.models.GitlabGroupMember;
import org.gitlab.api.models.GitlabProjectMember;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GitlabGroupMemberResponse {
	private String name;
	private String username;
	private Integer id;
	private String state;
	private Date createdAt;
	private String avatarUrl;
	private GitlabAccessLevel accessLevel;

	public GitlabGroupMemberResponse(GitlabGroupMember groupMember) {
		this.name = groupMember.getName();
		this.id = groupMember.getId();
		this.state = groupMember.getState();
		this.accessLevel = groupMember.getAccessLevel();
		this.createdAt = groupMember.getCreatedAt();
		this.avatarUrl = groupMember.getAvatarUrl();
		this.username = groupMember.getUsername();
	}

	public GitlabGroupMemberResponse(GitlabProjectMember projectMember) {
		this.name = projectMember.getName();
		this.id = projectMember.getId();
		this.state = projectMember.getState();
		this.accessLevel = projectMember.getAccessLevel();
		this.createdAt = projectMember.getCreatedAt();
		this.avatarUrl = projectMember.getAvatarUrl();
		this.username = projectMember.getUsername();
	}
}
