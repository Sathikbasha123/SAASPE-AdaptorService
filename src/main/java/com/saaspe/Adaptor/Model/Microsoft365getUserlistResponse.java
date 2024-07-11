package com.saaspe.Adaptor.Model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Microsoft365getUserlistResponse {
	
	@JsonProperty("displayName")
	private String displayName;
	@JsonProperty("givenName")
    private String givenName;
	@JsonProperty("jobTitle")
    private String	jobTitle;
	@JsonProperty("mail")
	private String mail;
	@JsonProperty("mobilePhone")
	private String mobilePhone;
	@JsonProperty("officeLocation")
    private String officeLocation;
	@JsonProperty("preferredLanguage")
    private String	preferredLanguage;
	@JsonProperty("surname")
	private String surname;                            
	@JsonProperty("userPrincipalName")
    private String	userPrincipalName;
	@JsonProperty("id")
	private String id; 
	@JsonProperty("businessPhones")
	private List<String> businessPhones;
		
}
