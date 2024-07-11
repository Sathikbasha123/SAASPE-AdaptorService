package com.saaspe.Adaptor.Model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor

public class HubSpotGetUserlistResponse {
	
	@JsonProperty("id")
	private String id;
	@JsonProperty("email")
    private String email;
	@JsonProperty("primaryTeamId")
    private String	primaryTeamId;
	@JsonProperty("superAdmin")
	private boolean superAdmin;

}
