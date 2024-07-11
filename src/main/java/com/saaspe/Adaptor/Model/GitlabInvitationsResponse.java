package com.saaspe.Adaptor.Model;

import java.util.Date;

import lombok.Data;

@Data
public class GitlabInvitationsResponse {
	private Integer access_level;
	private Date created_at;
	private String invite_email;
	private String created_by_name;
}
