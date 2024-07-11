package com.saaspe.Adaptor.Service;

import org.springframework.stereotype.Service;

import com.intuit.ipp.exception.FMSException;
import com.intuit.oauth2.exception.InvalidRequestException;
import com.intuit.oauth2.exception.OAuthException;
import com.saaspe.Adaptor.Advice.CommonResponse;
import com.saaspe.Adaptor.Model.QuickBooksUserRequest;

@Service
public interface QuickBooksService {

	CommonResponse getAuthoriztionCode(String appId,String redirectUri)throws InvalidRequestException;

	CommonResponse getAccessToken(String appId, String authCode, String realmId, Long uniqueId) throws OAuthException;

	CommonResponse getUsers(String appId) throws FMSException;

	CommonResponse getInfo(String appId) throws FMSException;

	CommonResponse addUsers(String appId, QuickBooksUserRequest userRequest) throws FMSException;

	CommonResponse getLicenseCount(String appId) throws FMSException;

	CommonResponse deleteUser(String appId, String id) throws FMSException;

	CommonResponse generateRefreshToken(String appId) throws OAuthException;

	CommonResponse userDetailsByEmail(String appId, String email) throws FMSException;

}
