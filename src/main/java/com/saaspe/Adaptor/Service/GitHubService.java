package com.saaspe.Adaptor.Service;


import com.saaspe.Adaptor.Advice.CommonResponse;
import com.saaspe.Adaptor.Advice.DataValidationException;
import com.saaspe.Adaptor.Model.GitHubInviteRequestBody;
import com.saaspe.Adaptor.Model.RemoveUserRequest;
import com.saaspe.Adaptor.Model.UserUpdateRequest;

public interface GitHubService {
	
    CommonResponse getAuthUri(String appId) ;

	CommonResponse getToken(String appId);
	
	CommonResponse getUserDetails(String appId) throws DataValidationException ;
	
    CommonResponse inviteUserToOrganization(String appId, GitHubInviteRequestBody inviteRequestBody) throws DataValidationException;
    
    CommonResponse getOrganizationMembers(String appId) throws DataValidationException;

    CommonResponse getActionsBilling(String appId) throws DataValidationException;
    
    CommonResponse getPackagesBilling(String appId) throws DataValidationException;

    CommonResponse getSharedStorageBilling(String appId) throws DataValidationException;
    
    CommonResponse removeOrganizationMember(String appId, RemoveUserRequest removUserRequest) throws DataValidationException;

    CommonResponse updateMembership(String appId, UserUpdateRequest userUpdateRequest) throws DataValidationException;

    CommonResponse getOrgDetails(String appId) throws DataValidationException;
    
}
