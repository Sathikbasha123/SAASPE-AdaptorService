package com.saaspe.Adaptor.Service;

import com.saaspe.Adaptor.Advice.CommonResponse;
import com.saaspe.Adaptor.Advice.DataValidationException;
import com.saaspe.Adaptor.Model.JiraCreateUserRequest;

 public interface JiraService {

	 CommonResponse createUser(JiraCreateUserRequest jiraCreateUserRequest, String appId);
	 
	 CommonResponse addUserToGroup(String productName, String accountId, String appId);
	 
	 CommonResponse getAllUsers(String appId) throws DataValidationException;
	 
	 CommonResponse removeUserFromGroup(String accountId, String appId);
	
	 CommonResponse deleteUser(String accountId, String appId);

	 CommonResponse getAllLicenseDetails(String appId);

	 CommonResponse getLicenseDetails(String key, String appId);

	 CommonResponse getAllAppDetail();

	 CommonResponse getAppDetail(String addonKey);

	 CommonResponse getAuditRecords(String appId);

	 CommonResponse getInstanceLicense(String appId);

	 CommonResponse getUserEmail(String accountId, String appId);

	 CommonResponse getUserGroups(String accountId, String appId);

	 CommonResponse getAllGroups(String appId);

	 CommonResponse getGroupMembers(String groupName, String appId);

	 CommonResponse getUserByAccountId(String accountId, String appId);



}
