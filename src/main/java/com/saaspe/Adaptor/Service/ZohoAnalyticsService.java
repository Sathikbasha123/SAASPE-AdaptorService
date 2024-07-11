package com.saaspe.Adaptor.Service;

import java.io.IOException;

import com.saaspe.Adaptor.Advice.CommonResponse;

public interface ZohoAnalyticsService {
	CommonResponse getAccessToken(String appId, String code)
			throws IOException;

	CommonResponse generateToken(String appId) throws IOException;

	CommonResponse saveOrgDetails(String appId) throws IOException;

	CommonResponse inviteUser(String appId, String userEmail) throws IOException;

	CommonResponse getSubscriptionDetails(String appId) throws IOException;

	CommonResponse getUsersList(String appId) throws IOException;

	CommonResponse revokeAccess(String appId, String userEmail) throws IOException;

	CommonResponse getOrganizationList(String appId) throws IOException;

}
