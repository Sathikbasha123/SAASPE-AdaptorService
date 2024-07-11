package com.saaspe.Adaptor.Service;

import java.io.IOException;

import com.saaspe.Adaptor.Advice.CommonResponse;

public interface FreshdeskService {

	CommonResponse getAccountDetails(String appId);

	CommonResponse inviteUser(String appId, String userEmail, String userName) throws IOException;

	CommonResponse revokeUserAccess(String appId, String userEmail);

	CommonResponse searchUser(String appId,String userEmail);

	CommonResponse getUsersList(String appId);


}
