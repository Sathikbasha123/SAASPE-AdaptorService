package com.saaspe.Adaptor.Entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Entity
@Table(name = "saaspe_adaptors_details")
@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class AdaptorDetails {
	@Id
	private Long id;

	@Column(name = "APPLICATION_ID")
	private String applicationId;

	@Column(name = "APPLICATION_NAME")
	private String applicationName;

	@Column(name = "USER_NAME")
	private String username;

	@Column(name = "EMAIL")
	private String email;

	@Column(name = "AUTH_TYPE")
	private String authType;

	@Column(name = "API_TOKEN")
	private String apiToken;

	@Column(name = "ROLE")
	private String role;

	@Column(name = "CLIENT_ID")
	private String clientId;

	@Column(name = "CLIENT_SECRET")
	private String clientSecret;

	@Column(name = "DOMAIN_URL")
	private String domainUrl;

	@Column(name = "REDIRECT_URL")
	private String redirectUrl;

	@Column(name = "API_KEY")
	private String apiKey;

	@Column(name = "GROUP_ID")
	private long groupid;

	@Column(name = "GRANT_TOKEN")
	private String grantCode;

	@Column(name = "TENANT_ID")
	private String tenantid;

	@Column(name = "ORGANIZATION_NAME")
	private String organizationName;

	@Column(name = "REALM_ID")
	private String realmId;

	@Column(name = "REFRESHTOKEN_CREATEDON")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date refreshtokenCreatedOn;

}
