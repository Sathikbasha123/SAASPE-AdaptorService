package com.saaspe.Adaptor.Response;

import java.util.TimeZone;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrganizationnResponse {

	private String country;
	private String city;
	private String description;
	private String domainName;
	private String currency;
	private String organizationId;
	private String employeesCount;
	private String organizationState;
	private String zipCode;
	private String website;
	private String phone;
	private TimeZone timeZone;
	private String LicensePurchased;
	private String trialType;
	private String trialExpiry;
	private boolean licensePaid;
	private String companyName;
	private String primaryEmail;
	private String paidType;
	
	
}
