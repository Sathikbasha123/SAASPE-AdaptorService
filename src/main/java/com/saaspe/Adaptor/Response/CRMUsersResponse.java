package com.saaspe.Adaptor.Response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.saaspe.Adaptor.Model.ProfilesAndRoles;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
public class CRMUsersResponse {

	private String country;
	private String id;
	private String state;
	private String country_locale;
	private String created_time;
	private String full_name;
	private String last_name;
	private String email;
	private String category;
	private ProfilesAndRoles profile;
	private ProfilesAndRoles role;
	
}
