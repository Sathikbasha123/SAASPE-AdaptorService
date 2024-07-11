package com.saaspe.Adaptor.Model;

import org.gitlab.api.models.GitlabGroup;
import org.gitlab.api.models.GitlabProject;
import org.gitlab.api.models.GitlabUser;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GitlabResource {

	private Integer id;
	private String name;

	public GitlabResource(GitlabGroup group) {
		this.id = group.getId();
		this.name = group.getName();
	}

	public GitlabResource(GitlabProject group) {
		this.id = group.getId();
		this.name = group.getName();
	}

	public GitlabResource(GitlabUser group) {
		this.id = group.getId();
		this.name = group.getName();
	}

	


	
}
