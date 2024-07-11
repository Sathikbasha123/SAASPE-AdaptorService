package com.saaspe.Adaptor.Service;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.intuit.ipp.exception.BadRequestException;
import com.saaspe.Adaptor.Advice.CommonResponse;
import com.saaspe.Adaptor.Advice.DataValidationException;
import com.saaspe.Adaptor.Model.Microsoft365getUserlistResponse;

public interface Microsoft365Service {

	CommonResponse generateAuthUri(String appId, String redirectUri);

	CommonResponse getToken(String appId, String code, Long uniqueId)
			throws IOException;

	CommonResponse getRefreshToken(String appId) throws IOException;

	CommonResponse getUserList(String appId) throws IOException, DataValidationException;


	Microsoft365getUserlistResponse updateUserByEmail(String userEmail,
			Microsoft365getUserlistResponse microsoft365UpdateUserRequest, String appId)
			throws JsonProcessingException, DataValidationException;

	CommonResponse deleteUserInMicrosoft365(String appId, String userEmail)
			throws JsonProcessingException, DataValidationException;

	CommonResponse getSubscribedSku(String appId) throws IOException, DataValidationException;

	Microsoft365getUserlistResponse assignLicense(String userEmail, String appId, String productName)
	        throws BadRequestException, DataValidationException, IOException, InterruptedException;



	CommonResponse getUserLicenseDetails(String userEmail, String appId) throws IOException, DataValidationException;

	Microsoft365getUserlistResponse createUser(String userEmail, String appId) throws JsonProcessingException;

	Microsoft365getUserlistResponse unAssignLicense(String userEmail, String appId, String productName)
			throws JsonProcessingException, BadRequestException, DataValidationException;

	

}
