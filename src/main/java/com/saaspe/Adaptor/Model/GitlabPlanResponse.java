package com.saaspe.Adaptor.Model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class GitlabPlanResponse {

	private Integer groupId;
	private String planCode;
	private String planName;

	private int seatsInSubscription;
	private int seatsInUse;
	private int maxSeatsUsed;
	private int seatsOwed;

	@JsonFormat(pattern = "yyyy-MM-dd")
	private Date subscriptionStartDate;

	@JsonFormat(pattern = "yyyy-MM-dd")
	private Date subscriptionEndDate;

}
