package com.saaspe.Adaptor.Service;

import java.io.IOException;

import org.springframework.stereotype.Service;

import com.saaspe.Adaptor.Advice.CommonResponse;
import com.saaspe.Adaptor.Model.GitLabDeleteUserRequest;
import com.saaspe.Adaptor.Model.GitlabUserRequest;

@Service
public interface GitLabService {

	CommonResponse getAuthURL(String appId);

	CommonResponse getAccessToken(String appId) throws IOException;

	CommonResponse getProfileDetails(String appId) throws IOException;

	CommonResponse addMemberToGroup(GitlabUserRequest gitLabUserRequest,String appId) throws IllegalArgumentException, IOException;

	CommonResponse removeGroupMember(GitLabDeleteUserRequest deleteUserRequest,String appId);

	CommonResponse getGroups(String appId);

	CommonResponse getProjects(String appId);

	CommonResponse findUser(String userName,String appId);

	CommonResponse getAccessRoles();

	CommonResponse generateToken(String appId) throws IOException;

	CommonResponse getSubscriptionInfo(String appId);

	CommonResponse getAllUsers(String appId);

	CommonResponse getResourceMembers(String appId);

	CommonResponse deleteGroup(String appId);

	CommonResponse getInvitationList(String appId) throws IOException;

	CommonResponse revokeInvitation(String email,String appId);

}
