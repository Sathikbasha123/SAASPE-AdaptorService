package com.saaspe.Adaptor.ServiceImpl;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.saaspe.Adaptor.Advice.CommonResponse;
import com.saaspe.Adaptor.Advice.DataValidationException;
import com.saaspe.Adaptor.Advice.Response;
import com.saaspe.Adaptor.Constant.Constant;
import com.saaspe.Adaptor.Entity.AdaptorDetails;
import com.saaspe.Adaptor.Model.GitHubInviteRequestBody;
import com.saaspe.Adaptor.Model.RemoveUserRequest;
import com.saaspe.Adaptor.Model.Token;
import com.saaspe.Adaptor.Model.UserUpdateRequest;
import com.saaspe.Adaptor.Repository.AdaptorDetailsRepository;
import com.saaspe.Adaptor.Service.GitHubService;

@Service
public class GitHubServiceImpl implements GitHubService {

	@Value("${github.api.base-url}")
	private String githubApiBaseUrl;
	

	@Autowired
	private RestTemplate restTemplate;
	
	@Autowired
	private AdaptorDetailsRepository adaptorDetailsRepository;

	@Override
	public CommonResponse getAuthUri(String appId) {
		AdaptorDetails adaptor =  adaptorDetailsRepository.findByApplicationId(appId);
		
		String authUri = "https://github.com/login/oauth/authorize?client_id=" +adaptor.getClientId()
				+ "&scope=read:user,write:org,admin:org,read:org";
		return new CommonResponse(HttpStatus.OK, new Response("Authorization URL", authUri),
				"Authorization URL generated successfully");
	}

	@Override
	public CommonResponse getToken(String appId) {
		AdaptorDetails adaptor =  adaptorDetailsRepository.findByApplicationId(appId);

		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.add("client_id",adaptor.getClientId() );
		body.add("client_secret", adaptor.getClientSecret());
		body.add("code", adaptor.getApiKey());

		HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

		try {
			ResponseEntity<Token> response = restTemplate.postForEntity("https://github.com/login/oauth/access_token",
					requestEntity, Token.class);

			Token token = response.getBody();

			return new CommonResponse(HttpStatus.OK, new Response("Access Token", token),
					"Access token generated successfully");
		} catch (Exception e) {
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Access Token", "Invalid/Expired/Revoked Grant"),
					"Kindly check the grant type/code/client details");
		}
	}

	@Override
	public CommonResponse getUserDetails(String appId) throws DataValidationException {
		AdaptorDetails adaptor =  adaptorDetailsRepository.findByApplicationId(appId);
		HttpHeaders headers = new HttpHeaders();
		headers.set(Constant.AUTHORIZATION, Constant.BEARER+adaptor.getApiToken());
		headers.set(Constant.ACCEPT, Constant.APPLICATION_ACCEPT);
		headers.set(Constant.GITHUB_API_VERSION, Constant.GITHUB_VERSION);

		HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);

		ResponseEntity<Object> response = restTemplate.exchange(githubApiBaseUrl + "/user", HttpMethod.GET,
				requestEntity, Object.class);

		if (response.getStatusCode().is2xxSuccessful()) {
			Object userDetails = response.getBody();
			return new CommonResponse(HttpStatus.OK, new Response("GitHub User details", userDetails),
					"GitHub user details fetched successfully");
		} else {
			throw new DataValidationException(HttpStatus.BAD_REQUEST,"Failed to fetch GitHub user details. Status code: " + response.getStatusCode(),null);
		}
	}
	
	  @Override
	    public CommonResponse inviteUserToOrganization(String appId, GitHubInviteRequestBody inviteRequestBody) throws DataValidationException {
	        HttpHeaders headers = new HttpHeaders();
			AdaptorDetails adaptor =  adaptorDetailsRepository.findByApplicationId(appId);

			headers.set(Constant.AUTHORIZATION,Constant.BEARER+adaptor.getApiToken());
	        headers.set(Constant.ACCEPT, Constant.APPLICATION_ACCEPT);
	        headers.set(Constant.GITHUB_API_VERSION, Constant.GITHUB_VERSION);

	        HttpEntity<GitHubInviteRequestBody> requestEntity = new HttpEntity<>(inviteRequestBody, headers);

	        try {
	            ResponseEntity<Object> response = restTemplate.exchange(
	                    githubApiBaseUrl + Constant.ORGS + adaptor.getOrganizationName() + "/invitations",
	                    HttpMethod.POST,
	                    requestEntity,
	                    Object.class
	            );

	            if (response.getStatusCode().is2xxSuccessful()) {
	                Object inviteResponse = response.getBody();
	                return new CommonResponse(HttpStatus.OK, new Response("GitHub Invitation Response", inviteResponse),
	                        "User invited to GitHub organization successfully");
	            } else {
	                throw new DataValidationException(HttpStatus.BAD_REQUEST,"Failed to invite user to GitHub organization. Status code: " + response.getStatusCode(),null);
	            }
	        } catch (HttpClientErrorException e) {
	            return new CommonResponse(HttpStatus.BAD_REQUEST,
	                    new Response("GitHub Invitation Response", e.getResponseBodyAsString()),
	                    "Failed to invite user to GitHub organization");
	        }
	    }
	  
	    @Override
	    public CommonResponse getOrganizationMembers(String appId) throws DataValidationException {
			AdaptorDetails adaptor =  adaptorDetailsRepository.findByApplicationId(appId);
	        HttpHeaders headers = new HttpHeaders();
			headers.set(Constant.AUTHORIZATION, Constant.BEARER+adaptor.getApiToken());
	        headers.set(Constant.ACCEPT, Constant.APPLICATION_ACCEPT);
	        headers.set(Constant.GITHUB_API_VERSION, Constant.GITHUB_VERSION);

	        HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);

	        try {
	            ResponseEntity<Object> response = restTemplate.exchange(
	                    githubApiBaseUrl + Constant.ORGS + adaptor.getOrganizationName() + "/members",
	                    HttpMethod.GET,
	                    requestEntity,
	                    Object.class
	            );

	            if (response.getStatusCode().is2xxSuccessful()) {
	                Object membersResponse = response.getBody();
	                return new CommonResponse(HttpStatus.OK, new Response("GitHub Organization Members", membersResponse),
	                        "GitHub organization members fetched successfully");
	            } else {
	                throw new DataValidationException(HttpStatus.BAD_REQUEST,"Failed to fetch GitHub organization members. Status code: " + response.getStatusCode(),null);
	            }
	        } catch (HttpClientErrorException e) {
	            return new CommonResponse(HttpStatus.BAD_REQUEST,
	                    new Response("GitHub Organization Members", e.getResponseBodyAsString()),
	                    "Failed to fetch GitHub organization members");
	        }
	    }
	  
	   @Override
	    public CommonResponse getActionsBilling(String appId) throws DataValidationException {
			AdaptorDetails adaptor =  adaptorDetailsRepository.findByApplicationId(appId);

	        HttpHeaders headers = new HttpHeaders();
			headers.set(Constant.AUTHORIZATION, Constant.BEARER+adaptor.getApiToken());
	        headers.set(Constant.ACCEPT, Constant.APPLICATION_ACCEPT);
	        headers.set(Constant.GITHUB_API_VERSION, Constant.GITHUB_VERSION);

	        HttpEntity<Object> requestEntity = new HttpEntity<>(null, headers);

	        try {
	            ResponseEntity<Object> response = restTemplate.exchange(
	                    githubApiBaseUrl + Constant.ORGS + adaptor.getOrganizationName() + "/settings/billing/actions",
	                    HttpMethod.GET,
	                    requestEntity,
	                    Object.class
	            );

	            if (response.getStatusCode().is2xxSuccessful()) {
	                Object billingActionsResponse = response.getBody();
	                return new CommonResponse(HttpStatus.OK, new Response(Constant.GITHUB_BILLING_ACTIONS, billingActionsResponse),
	                        "GitHub organization billing actions fetched successfully");
	            } else {
	                throw new DataValidationException(HttpStatus.BAD_REQUEST,"Failed to fetch GitHub organization billing actions. Status code: " + response.getStatusCode(), null);
	            }
	        } catch (HttpClientErrorException e) {
	            return new CommonResponse(HttpStatus.BAD_REQUEST,
	                    new Response(Constant.GITHUB_BILLING_ACTIONS, e.getResponseBodyAsString()),
	                    "Failed to fetch GitHub organization billing actions");
	        }
	    }
	   
	   @Override
	    public CommonResponse getPackagesBilling(String appId) throws DataValidationException {
			AdaptorDetails adaptor =  adaptorDetailsRepository.findByApplicationId(appId);

	        HttpHeaders headers = new HttpHeaders();
			headers.set(Constant.AUTHORIZATION, Constant.BEARER+adaptor.getApiToken());
	        headers.set(Constant.ACCEPT, Constant.APPLICATION_ACCEPT);
	        headers.set(Constant.GITHUB_API_VERSION, Constant.GITHUB_VERSION);

	        HttpEntity<Object> requestEntity = new HttpEntity<>(null, headers);

	        try {
	            ResponseEntity<Object> response = restTemplate.exchange(
	                    githubApiBaseUrl + Constant.ORGS + adaptor.getOrganizationName() + "/settings/billing/packages",
	                    HttpMethod.GET,
	                    requestEntity,
	                    Object.class
	            );

	            if (response.getStatusCode().is2xxSuccessful()) {
	                Object billingPackagesResponse = response.getBody();
	                return new CommonResponse(HttpStatus.OK, new Response("GitHub Packages Billing", billingPackagesResponse),
	                        "GitHub organization packages billing  fetched successfully");
	            } else {
	                throw new DataValidationException(HttpStatus.BAD_REQUEST,"Failed to fetch GitHub organization packages billings. Status code: " + response.getStatusCode(), null);
	            }
	        } catch (HttpClientErrorException e) {
	            return new CommonResponse(HttpStatus.BAD_REQUEST,
	                    new Response(Constant.GITHUB_BILLING_ACTIONS, e.getResponseBodyAsString()),
	                    "Failed to fetch GitHub organization packages billings");
	        }
	    }
	   
	   @Override
	    public CommonResponse getSharedStorageBilling(String appId) throws DataValidationException {
			AdaptorDetails adaptor =  adaptorDetailsRepository.findByApplicationId(appId);

	        HttpHeaders headers = new HttpHeaders();
			headers.set(Constant.AUTHORIZATION, Constant.BEARER+adaptor.getApiToken());
	        headers.set(Constant.ACCEPT, Constant.APPLICATION_ACCEPT);
	        headers.set(Constant.GITHUB_API_VERSION, Constant.GITHUB_VERSION);

	        HttpEntity<Object> requestEntity = new HttpEntity<>(null, headers);

	        try {
	            ResponseEntity<Object> response = restTemplate.exchange(
	                    githubApiBaseUrl + Constant.ORGS + adaptor.getOrganizationName() + "/settings/billing/shared-storage",
	                    HttpMethod.GET,
	                    requestEntity,
	                    Object.class
	            );

	            if (response.getStatusCode().is2xxSuccessful()) {
	                Object billingSharedStorageResponse = response.getBody();
	                return new CommonResponse(HttpStatus.OK, new Response("GitHub Shared Storage Billing", billingSharedStorageResponse),
	                        "GitHub organization shared storage billing fetched successfully");
	            } else {
	                throw new DataValidationException(HttpStatus.BAD_REQUEST,"Failed to fetch GitHub organization shared storage billings. Status code: " + response.getStatusCode(),null);
	            }
	        } catch (HttpClientErrorException e) {
	            return new CommonResponse(HttpStatus.BAD_REQUEST,
	                    new Response("GitHub Shared Storage Billing", e.getResponseBodyAsString()),
	                    "Failed to fetch GitHub organization shared storage billings");
	        }
	    }
	   
        @Override
	    public CommonResponse removeOrganizationMember(String appId, RemoveUserRequest removeUserRequest) throws DataValidationException {
			AdaptorDetails adaptor =  adaptorDetailsRepository.findByApplicationId(appId);

	        HttpHeaders headers = new HttpHeaders();
			headers.set(Constant.AUTHORIZATION, Constant.BEARER+adaptor.getApiToken());
	        headers.set(Constant.ACCEPT, Constant.APPLICATION_ACCEPT);
	        headers.set(Constant.GITHUB_API_VERSION, Constant.GITHUB_VERSION);

	        HttpEntity<Object> requestEntity = new HttpEntity<>(removeUserRequest, headers);

	        try {
	            ResponseEntity<Object> response = restTemplate.exchange(
	                    githubApiBaseUrl + Constant.ORGS + adaptor.getOrganizationName() + "/members/" + removeUserRequest.getUserName(),
	                    HttpMethod.DELETE,
	                    requestEntity,
	                    Object.class
	            );

	            if (response.getStatusCode().is2xxSuccessful()) {
	                Object removeMemberResponse = response.getBody();
	                return new CommonResponse(HttpStatus.OK, new Response("GitHub Remove Member", removeMemberResponse),
	                        "GitHub organization member removed successfully");
	            } else {
	                throw new DataValidationException(HttpStatus.BAD_REQUEST,"Failed to remove GitHub organization member. Status code: " + response.getStatusCode(),null);
	            }
	        } catch (HttpClientErrorException e) {
	            return new CommonResponse(HttpStatus.BAD_REQUEST,
	                    new Response("GitHub Remove Member", e.getResponseBodyAsString()),
	                    "Failed to remove GitHub organization member");
	        }
	    }
	   
	   @Override
	    public CommonResponse updateMembership(String appId, UserUpdateRequest updateUserRequest) throws DataValidationException {
			AdaptorDetails adaptor =  adaptorDetailsRepository.findByApplicationId(appId);

			String userName=updateUserRequest.getUserName();
	        HttpHeaders headers = new HttpHeaders();
			headers.set(Constant.AUTHORIZATION, Constant.BEARER+adaptor.getApiToken());
	        headers.set(Constant.ACCEPT, Constant.APPLICATION_ACCEPT);
	        headers.set(Constant.GITHUB_API_VERSION, Constant.GITHUB_VERSION);

	        HttpEntity<Object> requestEntity = new HttpEntity<>(updateUserRequest, headers);

	        try {
	            ResponseEntity<Object> response = restTemplate.exchange(
	                    githubApiBaseUrl + Constant.ORGS + adaptor.getOrganizationName() + "/memberships/" + userName,
	                    HttpMethod.PUT,
	                    requestEntity,
	                    Object.class
	            );

	            if (response.getStatusCode().is2xxSuccessful()) {
	                Object updateMembershipResponse = response.getBody();
	                return new CommonResponse(HttpStatus.OK, new Response("GitHub Update Membership", updateMembershipResponse),
	                        "GitHub organization membership updated successfully");
	            } else {
	                throw new DataValidationException(HttpStatus.BAD_REQUEST,"Failed to update GitHub organization membership. Status code: " + response.getStatusCode(),null);
	            }
	        } catch (HttpClientErrorException e) {
	            return new CommonResponse(HttpStatus.BAD_REQUEST,
	                    new Response("GitHub Update Membership", e.getResponseBodyAsString()),
	                    "Failed to update GitHub organization membership");
	        }
	    }
	   
	   @Override
	    public CommonResponse getOrgDetails(String appId) throws DataValidationException {
			AdaptorDetails adaptor =  adaptorDetailsRepository.findByApplicationId(appId);

	        HttpHeaders headers = new HttpHeaders();
			headers.set(Constant.AUTHORIZATION, Constant.BEARER+adaptor.getApiToken());
	        headers.set(Constant.ACCEPT, Constant.APPLICATION_ACCEPT);
	        headers.set(Constant.GITHUB_API_VERSION, Constant.GITHUB_VERSION);

	        HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);

	        try {
	            ResponseEntity<Object> response = restTemplate.exchange(
	                    githubApiBaseUrl + Constant.ORGS + adaptor.getOrganizationName(),
	                    HttpMethod.GET,
	                    requestEntity,
	                    Object.class
	            );

	            if (response.getStatusCode().is2xxSuccessful()) {
	                Object orgDetails = response.getBody();
	                return new CommonResponse(HttpStatus.OK, new Response("GitHub Org details", orgDetails),
	                        "GitHub org details fetched successfully");
	            } else {
	                throw new DataValidationException(HttpStatus.BAD_REQUEST,"Failed to fetch GitHub org details. Status code: " + response.getStatusCode(),null);
	            }
	        } catch (HttpClientErrorException e) {
	            return new CommonResponse(HttpStatus.BAD_REQUEST,
	                    new Response("GitHub Org details", e.getResponseBodyAsString()),
	                    "Failed to fetch GitHub org details");
	        }
	    }}
