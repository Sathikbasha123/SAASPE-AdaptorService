package com.saaspe.Adaptor.Model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ZoomgetUserlistResponse {
	
	@JsonProperty("id")
	private String id; 
	@JsonProperty("display_name")
	private String display_name;
	@JsonProperty("first_name")
    private String first_name;
	@JsonProperty("last_name")
    private String last_name;
	@JsonProperty("type")
    private Integer type;
	@JsonProperty("email")
	private String email;
	@JsonProperty("dept")
	private String dept;
	@JsonProperty("status")
    private String status;
	@JsonProperty("role_id")
    private Integer role_id;
	@JsonProperty("language")
	private String language;                            
	@JsonProperty("last_login_time")
    private String	last_login_time;
	@JsonProperty("created_at")
	private String created_at;
	@JsonProperty("user_created_at")
	private String user_created_at;
	
}
