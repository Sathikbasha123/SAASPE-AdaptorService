package com.saaspe.Adaptor.Model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class GitlabGroupSubsResponse {

	private String code;
	private String name;
	private boolean trial;
	private Boolean auto_renew;
	private boolean upgradable;
	private boolean exclude_guests;

	private int seats_in_subscription;
	private int seats_in_use;
	private int max_seats_used;
	private int seats_owed;
	@JsonFormat(pattern = "yyyy-MM-dd")
	private Date subscription_start_date;

	@JsonFormat(pattern = "yyyy-MM-dd")
	private Date subscription_end_date;

	@JsonFormat(pattern = "yyyy-MM-dd")
	private Date trial_ends_on;

}
