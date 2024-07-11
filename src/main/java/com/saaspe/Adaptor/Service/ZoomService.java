package com.saaspe.Adaptor.Service;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.saaspe.Adaptor.Advice.CommonResponse;
import com.saaspe.Adaptor.Advice.DataValidationException;
import com.saaspe.Adaptor.Model.CreateZoomUserRequest;
import com.saaspe.Adaptor.Model.CreateZoomUserResponse;


public interface ZoomService {

	CommonResponse generateAuthUri(String appId, String redirectUri);

	CommonResponse getToken(String appId, String code, Long uniqueId) throws IOException;

	CommonResponse getRefreshToken(String appId) throws IOException;

	CommonResponse getUserList(String appId) throws IOException, DataValidationException;

	CommonResponse deleteUserInZoom(String appId, String userEmail)
			throws JsonProcessingException, DataValidationException;

	CommonResponse getLicenseCount(String appId) throws IOException, DataValidationException;

	CreateZoomUserResponse createUser(CreateZoomUserRequest createZoomUserRequest, String appId) throws DataValidationException, IOException;

	
}
