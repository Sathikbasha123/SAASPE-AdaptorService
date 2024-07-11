package com.saaspe.Adaptor.Service;

import java.io.IOException;

import com.saaspe.Adaptor.Advice.CommonResponse;
import com.saaspe.Adaptor.Model.ZohoPeopleInviteRequest;

public interface ZohoPeopleService {

	CommonResponse getAuthUri(String appId);

	CommonResponse getAccessToken(String appId, String code)
			throws IOException;

	CommonResponse generateToken(String appId) throws IOException;

	CommonResponse inviteUser(String appId, ZohoPeopleInviteRequest inviteRequest) throws IOException;

	CommonResponse getAllUsers(String appId) throws IOException;

	CommonResponse findUserByEmail(String appId, String email) throws IOException;

	CommonResponse revokeLicense(String appId, String userEmail) throws IOException;

}
