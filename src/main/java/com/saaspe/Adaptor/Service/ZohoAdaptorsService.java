package com.saaspe.Adaptor.Service;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.saaspe.Adaptor.Advice.CommonResponse;
import com.saaspe.Adaptor.Advice.DataValidationException;
import com.saaspe.Adaptor.Model.CommonZohoCRMRequest;

public interface ZohoAdaptorsService {

	public void getGrantToken(HttpServletResponse response, String appId) throws IOException, DataValidationException;

	public CommonResponse getaccessToken(String appId, String code)
			throws JsonProcessingException, DataValidationException;

	public CommonResponse generateRefreshToken(String appId)
			throws JsonProcessingException, DataValidationException;

	public CommonResponse addUserToCRM(String accesstoken, CommonZohoCRMRequest request) throws IOException, DataValidationException;

	public CommonResponse getUserFromCRM(String appId, String userType)
			throws DataValidationException, IOException;

	public CommonResponse updateUserInCRM(String appId, CommonZohoCRMRequest request) throws DataValidationException, IOException;

	public CommonResponse deleteUserInCRM(String appId, String userId)
			throws DataValidationException, IOException;

	public CommonResponse getUserFromCRMById(String appId, String userId)
			throws DataValidationException, IOException;

	public CommonResponse getOrganizationInCRM(String appId);

	public CommonResponse getUserProfiles(String appId);

	public CommonResponse getUserRoles(String appId);

	public CommonResponse getLicenseDetails(String appId)
			throws DataValidationException, IOException;

	public CommonResponse getUserId(String email, String userType, String appId) throws DataValidationException;

	public CommonResponse constructURL(String appId) throws IOException, DataValidationException;

}
