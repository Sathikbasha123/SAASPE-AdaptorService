package com.saaspe.Adaptor.Service;

import java.io.IOException;


import com.saaspe.Adaptor.Advice.CommonResponse;
import com.saaspe.Adaptor.Advice.DataValidationException;
import com.saaspe.Adaptor.Model.CreateHubSpotUserRequest;
import com.saaspe.Adaptor.Model.CreateHubSpotUserResponse;
import com.saaspe.Adaptor.Model.HubSpotSubscriptionRequest;

public interface HubSpotService {

	CommonResponse createUnSubscription(HubSpotSubscriptionRequest hubSpotSubscriptionRequest, String appId);

	CommonResponse createSubscription(HubSpotSubscriptionRequest hubSpotSubscriptionRequest, String appId);

	CommonResponse getAllSubDefinitionHubSpot(String appId) throws IOException, DataValidationException;

	CommonResponse getUserSubDetailsHubSpot(String appId, String emailId) throws IOException, DataValidationException;

	CommonResponse getAccountInfoHubSpot(String appId) throws IOException, DataValidationException;

	CommonResponse getcountUsersFromHubSpot(String appId) throws IOException, DataValidationException;

	CreateHubSpotUserResponse createUser(CreateHubSpotUserRequest hubSpotUserRequest, String appId);

	CommonResponse getAllUsersFromHubSpot(String appId) throws IOException, DataValidationException;

	CommonResponse getRefreshToken(String appId) throws IOException;

	CommonResponse getToken(String appId) throws IOException;

	CommonResponse generateAuthUri(String appId);

	CommonResponse getSecurityActivityHubSpot(String appId, String userId) throws IOException, DataValidationException;

	CommonResponse getLoginActivityHubSpot(String appId, String userEmail) throws IOException, DataValidationException;

	CommonResponse deleteUserInHubSpot(String appId, String userEmail) throws DataValidationException;

}
