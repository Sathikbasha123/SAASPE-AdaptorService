package com.saaspe.Adaptor.Service;

import java.io.IOException;

import com.saaspe.Adaptor.Advice.CommonResponse;

public interface SalesforceService {

	CommonResponse generateToken(String appId) throws IOException;

	CommonResponse getUserList(String appId) throws IOException;

	CommonResponse createUser(String appId, String userEmail, String userName, String firstName) throws IOException;

	CommonResponse removeUser(String appId, String userEmail, String userName) throws IOException;

	CommonResponse getLicenseDetails(String appId) throws IOException;

}
