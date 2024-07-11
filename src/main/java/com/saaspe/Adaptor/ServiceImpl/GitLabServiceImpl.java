package com.saaspe.Adaptor.ServiceImpl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.gitlab.api.AuthMethod;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.GitlabAPIException;
import org.gitlab.api.TokenType;
import org.gitlab.api.models.GitlabGroup;
import org.gitlab.api.models.GitlabGroupMember;
import org.gitlab.api.models.GitlabProject;
import org.gitlab.api.models.GitlabUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.ParseException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.saaspe.Adaptor.Advice.CommonResponse;
import com.saaspe.Adaptor.Advice.Response;
import com.saaspe.Adaptor.Constant.Constant;
import com.saaspe.Adaptor.Constant.GitlabConstant;
import com.saaspe.Adaptor.Entity.AdaptorDetails;
import com.saaspe.Adaptor.Model.AccessToken;
import com.saaspe.Adaptor.Model.GitLabDeleteUserRequest;
import com.saaspe.Adaptor.Model.GitLabUserProfileResponse;
import com.saaspe.Adaptor.Model.GitlabAccessRole;
import com.saaspe.Adaptor.Model.GitlabErrorResponse;
import com.saaspe.Adaptor.Model.GitlabGroupMemberResponse;
import com.saaspe.Adaptor.Model.GitlabInvitationsResponse;
import com.saaspe.Adaptor.Model.GitlabPlanResponse;
import com.saaspe.Adaptor.Model.GitlabResource;
import com.saaspe.Adaptor.Model.GitlabUserRequest;
import com.saaspe.Adaptor.Repository.AdaptorDetailsRepository;
import com.saaspe.Adaptor.Service.GitLabService;

@Service
public class GitLabServiceImpl implements GitLabService {

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private AdaptorDetailsRepository adaptorDetails;

	@Override
	public CommonResponse getAuthURL(String appId) {
		AdaptorDetails adaptor = adaptorDetails.findByApplicationId(appId);
		String oAuthURL = "https://gitlab.com/oauth/authorize?" + Constant.CLIENT_ID + adaptor.getClientId() + ""
				+ "&redirect_uri=" + adaptor.getRedirectUrl() + ""
				+ "&response_type=code&scope=api+read_user+read_api+admin_mode";

		return new CommonResponse(HttpStatus.OK, new Response("Fetch OAuth URL", oAuthURL),
				"OAuth URL returned successfully");
	}

	@Override
	public CommonResponse getAccessToken(String appId) throws IOException {
		AdaptorDetails adaptor = adaptorDetails.findByApplicationId(appId);

		String accessTokenURL = Constant.GITLAB_URL + Constant.CLIENT_ID + adaptor.getClientId() + "&"
				+ Constant.CLIENT_SECRET + adaptor.getClientSecret() + "" + "&code=" + adaptor.getApiKey()
				+ "&grant_type=authorization_code&redirect_uri=" + adaptor.getRedirectUrl();

		RestTemplate restTemplate = new RestTemplate();
		try {
			ResponseEntity<AccessToken> response = restTemplate.postForEntity(accessTokenURL, null, AccessToken.class);
			adaptor.setApiToken(response.getBody().getRefresh_token());
			adaptorDetails.save(adaptor);
			return new CommonResponse(HttpStatus.OK, new Response("Fetch Access Token", response.getBody()),
					"Access token generated successfully");
		} catch (HttpClientErrorException.BadRequest e) {
			e.printStackTrace();
			String responseBody = e.getResponseBodyAsString();
			GitlabErrorResponse error = objectMapper.readValue(responseBody, GitlabErrorResponse.class);
			return new CommonResponse(e.getStatusCode(), new Response(error.getError(), error.getError_description()),
					"Invalid or Expired grant/client details");
		} catch (Exception e) {
			e.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Fetch Access Token", null),
					"Exception in Access Token API");
		}
	}

	@Override
	public CommonResponse generateToken(String appId) throws IOException {

		AdaptorDetails adaptor = adaptorDetails.findByApplicationId(appId);

		String accessTokenURL = Constant.GITLAB_URL + Constant.CLIENT_ID + adaptor.getClientId() + "&"
				+ Constant.CLIENT_SECRET + adaptor.getClientSecret() + "" + "&refresh_token=" + adaptor.getApiToken() + ""
				+ "&grant_type=refresh_token&redirect_uri=" + adaptor.getRedirectUrl();

		RestTemplate restTemplate = new RestTemplate();
		try {
			ResponseEntity<AccessToken> response = restTemplate.postForEntity(accessTokenURL, null, AccessToken.class);
			adaptor.setApiToken(response.getBody().getRefresh_token());
			adaptorDetails.save(adaptor);
			return new CommonResponse(HttpStatus.OK, new Response("Generate Token", response.getBody()),
					"Access token generated successfully");
		} catch (HttpClientErrorException.BadRequest e) {
			String responseBody = e.getResponseBodyAsString();
			GitlabErrorResponse error = objectMapper.readValue(responseBody, GitlabErrorResponse.class);
			return new CommonResponse(e.getStatusCode(), new Response(error.getError(), error.getError_description()),
					"Invalid or Expired grant/client details");
		} catch (Exception e) {
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Fetch Generate Token", "Invalid/Expired/Revoked Grant"),
					"Kindly check the grant type/code/client details");
		}
	}

	@Override
	public CommonResponse getProfileDetails(String appId) throws IOException {

		try {
		AdaptorDetails applicationDetails = adaptorDetails.findByApplicationId(appId);
			if (applicationDetails == null) {
				return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.INVALID_ADAPTOR_APPLICATION, null),
						Constant.APPLICATION_NOT_CONNECTED);
			}
			GitlabAPI gitLabAPI = GitlabAPI.connect(Constant.GITLAB_BASE_URL, applicationDetails.getApiToken(),
					TokenType.PRIVATE_TOKEN, AuthMethod.HEADER);

			GitLabUserProfileResponse userProfileResponse = objectMapper.convertValue(gitLabAPI.getUser(),
					GitLabUserProfileResponse.class);
			return new CommonResponse(HttpStatus.OK, new Response("Get Profile details", userProfileResponse),
					"Profile details fetched successfully");
		} catch (GitlabAPIException e) {
			String message ;
			GitlabErrorResponse error = objectMapper.readValue(e.getMessage(), GitlabErrorResponse.class);
			if (error.getMessage() != null)
				message = error.getMessage();
			else
				message = error.getError_description();
			if (error.getError() == null)
				error.setError("Token Expired");
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(error.getError(), null), message);
		}

	}

	@Override
	public CommonResponse addMemberToGroup(GitlabUserRequest gitLabUserRequest, String appId) throws IOException {

		AdaptorDetails applicationDetails = adaptorDetails.findByApplicationId(appId);
		if (applicationDetails == null) {
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.INVALID_ADAPTOR_APPLICATION, null),
					Constant.APPLICATION_NOT_CONNECTED);
		}
		StringBuffer inviteUrl = new StringBuffer("https://gitlab.com/api/v4/");
		inviteUrl.append("groups/" + applicationDetails.getGroupid());
		inviteUrl.append("/invitations");
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(inviteUrl.toString());
		builder.queryParam("email", gitLabUserRequest.getEmail());
		builder.queryParam("access_level", gitLabUserRequest.getAccessLevel());
		HttpHeaders headers = new HttpHeaders();
		headers.add(Constant.AUTHORIZATION, Constant.BEARER + applicationDetails.getApiToken());
		HttpEntity<HttpHeaders> requestEntity = new HttpEntity<>(headers);
		RestTemplate restTemplate = new RestTemplate();
		try {
			ResponseEntity<String> response = restTemplate.exchange(builder.toUriString(), HttpMethod.POST,
					requestEntity, String.class);
			if (response.getStatusCode() == HttpStatus.CREATED) {
				JsonNode json = objectMapper.readTree(response.getBody());
				if (json.get("status").asText().equalsIgnoreCase("error")) {
					return new CommonResponse(HttpStatus.OK,
							new Response("Add gitlab user response",json.get("message")),
							"License limit reached");
				}
				return new CommonResponse(HttpStatus.OK, new Response("Add User to gitlab", null),
						"User invited to gitlab successfully");
			} else
				return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Add user response", null),
						"Exception in add user to gitlab");
		} catch (HttpClientErrorException e) {
			e.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Add user response", null),
					"Exception in add user to gitlab");
		}

	}

	@Override
	public CommonResponse removeGroupMember(GitLabDeleteUserRequest deleteUserRequest, String appId) {
		try {
			AdaptorDetails applicationDetails = adaptorDetails.findByApplicationId(appId);
			if (applicationDetails == null) {
				return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.INVALID_ADAPTOR_APPLICATION, null),
						Constant.APPLICATION_NOT_CONNECTED);
			}

			GitlabAPI gitLabAPI = GitlabAPI.connect(Constant.GITLAB_BASE_URL, applicationDetails.getApiToken(),
					TokenType.PRIVATE_TOKEN, AuthMethod.HEADER);
			boolean isValidMember = isMember(deleteUserRequest.getUserId(), (int) applicationDetails.getGroupid(),
					gitLabAPI);
			if (!isValidMember) {
				return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Remove User", null),
						"User is not a memeber of the group");
			}
			String deleteUrl = Constant.GITLAB_GROUP_URL + ((int) applicationDetails.getGroupid())
					+ "/members/" + deleteUserRequest.getUserId();
			HttpHeaders headers = new HttpHeaders();
			headers.set(Constant.AUTHORIZATION, Constant.BEARER + applicationDetails.getApiToken());
			HttpEntity<String> requestEntity = new HttpEntity<>(headers);
			RestTemplate restTemplate = new RestTemplate();
			ResponseEntity<Object> response = restTemplate.exchange(deleteUrl, HttpMethod.DELETE, requestEntity,
					Object.class);
			if (response.getStatusCode().is2xxSuccessful()) {
				return new CommonResponse(HttpStatus.OK, new Response("Remove Member", null),
						"User successfully removed");
			} else {
				return new CommonResponse(HttpStatus.valueOf(response.getStatusCodeValue()),
						new Response("Remove User", null), response.getBody());
			}
		} catch (UncheckedIOException e) {
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Remove  Gitlab Member", null),
					"Token Expired. Kindly reauthorize");
		}
	}

	public boolean isMember(Integer userId, Integer groupId, GitlabAPI gitlabAPI) {

		List<GitlabGroupMember> groupMembers = gitlabAPI.getGroupMembers(groupId);
		for (GitlabGroupMember groupMember : groupMembers) {
			if (groupMember.getId().equals(userId))
				return true;
		}
		return false;

	}

	@Override
	public CommonResponse getGroups(String appId) {

		try {
			AdaptorDetails applicationDetails = adaptorDetails.findByApplicationId(appId);
			if (applicationDetails == null) {
				return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.INVALID_ADAPTOR_APPLICATION, null),
						Constant.APPLICATION_NOT_CONNECTED);
			}

			GitlabAPI gitLabAPI = GitlabAPI.connect(Constant.GITLAB_BASE_URL, applicationDetails.getApiToken(),
					TokenType.PRIVATE_TOKEN, AuthMethod.HEADER);
			List<GitlabGroup> userGroups = gitLabAPI.getGroups();
			if (!userGroups.isEmpty()) {
				List<GitlabResource> groups = userGroups.stream().map(GitlabResource::new).collect(Collectors.toList());
				return new CommonResponse(HttpStatus.OK, new Response("User grops details", groups),
						"User groups details retrieved successfully");
			} else {
				return new CommonResponse(HttpStatus.OK, new Response("Fetch user groups", null),
						"No groups present under the user");
			}
		} catch (UncheckedIOException | GitlabAPIException e) {
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.INVALID_TOKEN, null), "Token expired");
		} catch (IOException e) {
			e.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Get User group", null),
					"Exception in user groups");
		}

	}

	@Override
	public CommonResponse getProjects(String appId) {
		try {
			AdaptorDetails applicationDetails = adaptorDetails.findByApplicationId(appId);
			if (applicationDetails == null) {
				return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.INVALID_ADAPTOR_APPLICATION, null),
						Constant.APPLICATION_NOT_CONNECTED);
			}

			GitlabAPI gitLabAPI = GitlabAPI.connect(Constant.GITLAB_BASE_URL, applicationDetails.getApiToken(),
					TokenType.ACCESS_TOKEN, AuthMethod.HEADER);
			List<GitlabProject> projects = gitLabAPI.getGroupProjects((int) applicationDetails.getGroupid());
			if (!projects.isEmpty()) {
				List<GitlabResource> groups = projects.stream().map(GitlabResource::new).collect(Collectors.toList());
				return new CommonResponse(HttpStatus.OK, new Response(Constant.FETCH_PROJECT_LIST, groups),
						"Project details retrieved successfully");
			} else {
				return new CommonResponse(HttpStatus.OK, new Response(Constant.FETCH_PROJECT_LIST, null),
						"No projects present under the group");
			}
		} catch (UncheckedIOException e) {
			e.printStackTrace();
			if (e.getCause() instanceof org.gitlab.api.GitlabAPIException) {
				return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.INVALID_TOKEN, null),
						Constant.TOKEN_EXPIRY);
			} else if (e.getCause() instanceof java.io.FileNotFoundException) {
				return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Invalid Group", null),
						"Group not found");
			} else
				return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.FETCH_PROJECT_LIST, null),
						"Exception in projects");

		} catch (Exception e) {
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.FETCH_PROJECT_LIST, null),
					"Exception in projects");
		}
	}

	@Override
	public CommonResponse findUser(String userName, String appId) {

		try {
			AdaptorDetails applicationDetails = adaptorDetails.findByApplicationId(appId);
			if (applicationDetails == null) {
				return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.INVALID_ADAPTOR_APPLICATION, null),
						Constant.APPLICATION_NOT_CONNECTED);
			}

			GitlabAPI gitLabAPI = GitlabAPI.connect(Constant.GITLAB_BASE_URL, applicationDetails.getApiToken(),
					TokenType.PRIVATE_TOKEN, AuthMethod.HEADER);

			List<GitlabUser> users = gitLabAPI.findUsers(userName);
			if (!users.isEmpty()) {
				List<GitlabResource> usersList = users.stream().map(GitlabResource::new).collect(Collectors.toList());
				return new CommonResponse(HttpStatus.OK, new Response(Constant.FIND_USER, usersList),
						"User details retrieved successfully");
			} else {
				return new CommonResponse(HttpStatus.NO_CONTENT, new Response("Invalid username", null),
						"No User found with the username");
			}
		} catch (GitlabAPIException e) {
			String message ;
			GitlabErrorResponse error = new Gson().fromJson(e.getMessage(), GitlabErrorResponse.class);
			if (error.getMessage() != null)
				message = error.getMessage();
			else
				message = error.getError_description();
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.FIND_USER, error.getError()), message);
		} catch (IOException e) {
			e.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.FIND_USER, null),
					"Exception in Find user by username");
		}
	}

	@Override
	public CommonResponse getAccessRoles() {
		List<String> accessRoleValues = GitlabConstant.accessRole;
		List<Integer> accessRoles = GitlabConstant.accessValues;
		List<GitlabAccessRole> response = new LinkedList<>();
		int i = 0;
		for (String accessRole : accessRoleValues) {
			response.add(new GitlabAccessRole(accessRole, accessRoles.get(i++)));
		}
		return new CommonResponse(HttpStatus.OK, new Response("Get Access roles", response),
				"Access roles list retrieved successfully");
	}

	@Override
	public CommonResponse getSubscriptionInfo(String appId) {
		AdaptorDetails applicationDetails = adaptorDetails.findByApplicationId(appId);
		if (applicationDetails == null) {
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.INVALID_ADAPTOR_APPLICATION, null),
					Constant.APPLICATION_NOT_CONNECTED);
		}

		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBearerAuth(applicationDetails.getApiToken());
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl("https://gitlab.com/api/v4/namespaces/");
		String url = builder.toUriString() + "" + ((int) applicationDetails.getGroupid()) + "/gitlab_subscription";
		try {
			ResponseEntity<String> apiResponse = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
					String.class);
			JsonNode groupPlanResponse = objectMapper.readTree(apiResponse.getBody());
			GitlabPlanResponse planResponse = new GitlabPlanResponse();
			planResponse.setGroupId((int) applicationDetails.getGroupid());
			planResponse.setPlanCode(groupPlanResponse.path("plan").path("code").asText());
			planResponse.setPlanName(groupPlanResponse.path("plan").path("name").asText());
			planResponse.setMaxSeatsUsed(groupPlanResponse.path(Constant.USAGE).path("max_seats_used").asInt());
			planResponse.setSeatsInSubscription(groupPlanResponse.path(Constant.USAGE).path("seats_in_subscription").asInt());
			planResponse.setSeatsInUse(groupPlanResponse.path(Constant.USAGE).path("seats_in_use").asInt());
			planResponse.setSeatsOwed(groupPlanResponse.path(Constant.USAGE).path("seats_owed").asInt());
			if (!groupPlanResponse.path(Constant.BILLING).path("subscription_start_date").asText().equalsIgnoreCase("null"))
				planResponse.setSubscriptionStartDate(new SimpleDateFormat("yyyy-MM-dd")
						.parse(groupPlanResponse.path(Constant.BILLING).path("subscription_start_date").asText()));
			if (!groupPlanResponse.path(Constant.BILLING).path("subscription_end_date").asText().equalsIgnoreCase("null"))
				planResponse.setSubscriptionEndDate(new SimpleDateFormat("yyyy-MM-dd")
						.parse(groupPlanResponse.path(Constant.BILLING).path("subscription_end_date").asText()));
			return new CommonResponse(HttpStatus.OK, new Response("Group's Subscription Info", planResponse),
					"Group's subscription info retrieved successfully");
		} catch (HttpClientErrorException.BadRequest e) {
			return new Gson().fromJson(e.getResponseBodyAsString(), CommonResponse.class);
		} catch (HttpClientErrorException.Unauthorized e) {
			GitlabErrorResponse errorResponse = new Gson().fromJson(e.getResponseBodyAsString(),
					GitlabErrorResponse.class);
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(errorResponse.getError(), null),
					errorResponse.getError_description());
		} catch (ParseException e) {
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Exception in Group Subscription API", null),
					"Exception in Group subscritpion details");
		} catch (Exception e) {
			e.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Get Group Subscription Info", null),
					"Exception occured in Group Subscrption Info");
		}

	}

	@Override
	public CommonResponse getAllUsers(String appId) {
		try {
			AdaptorDetails applicationDetails = adaptorDetails.findByApplicationId(appId);
			if (applicationDetails == null) {
				return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.INVALID_ADAPTOR_APPLICATION, null),
						Constant.APPLICATION_NOT_CONNECTED);
			}
			GitlabAPI gitLabAPI = GitlabAPI.connect(Constant.GITLAB_BASE_URL, applicationDetails.getApiToken(),
					TokenType.PRIVATE_TOKEN, AuthMethod.HEADER);
			List<GitlabGroupMember> users = new LinkedList<>();
			for (GitlabGroupMember user : gitLabAPI.getGroupMembers((int) applicationDetails.getGroupid())) {
				users.add(user);
			}
			List<GitlabGroupMemberResponse> usersList = users.stream().map(GitlabGroupMemberResponse::new)
					.collect(Collectors.toList());
			return new CommonResponse(HttpStatus.OK, new Response("Get All users", usersList),
					"Users list returned successfully");
		} catch (UncheckedIOException e) {
			e.printStackTrace();
			if (e.getCause() instanceof org.gitlab.api.GitlabAPIException) {
				return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.INVALID_TOKEN, null),
						Constant.TOKEN_EXPIRY);
			} else
				return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Get all users", null),
						"Exception in get all users");
		}
	}

	@Override
	public CommonResponse getResourceMembers(String appId) {

		try {
			AdaptorDetails applicationDetails = adaptorDetails.findByApplicationId(appId);
			if (applicationDetails == null) {
				return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.INVALID_ADAPTOR_APPLICATION, null),
						Constant.APPLICATION_NOT_CONNECTED);
			}

			GitlabAPI gitLabAPI = GitlabAPI.connect(Constant.GITLAB_BASE_URL, applicationDetails.getApiToken(),
					TokenType.PRIVATE_TOKEN, AuthMethod.HEADER);
			AdaptorDetails adaptor = adaptorDetails.findByApplicationId(appId);

			List<GitlabGroupMember> gitlabMembers = gitLabAPI.getGroupMembers((int) adaptor.getGroupid());

			List<GitlabGroupMemberResponse> groupMembers = gitlabMembers.stream().map(GitlabGroupMemberResponse::new)
					.collect(Collectors.toList());
			return new CommonResponse(HttpStatus.OK, new Response("Get Group members", groupMembers),
					"Group member details retrieved successfully");

		} catch (UncheckedIOException e) {
			e.printStackTrace();
			if (e.getCause() instanceof org.gitlab.api.GitlabAPIException) {
				return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.INVALID_TOKEN, null),
						Constant.TOKEN_EXPIRY);
			} else
				return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Get all users", null),
						"Exception in get all users");
		}
	}

	@Override
	public CommonResponse deleteGroup(String appId) {
		try {
			AdaptorDetails applicationDetails = adaptorDetails.findByApplicationId(appId);
			if (applicationDetails == null) {
				return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.INVALID_ADAPTOR_APPLICATION, null),
						Constant.APPLICATION_NOT_CONNECTED);
			}

			GitlabAPI gitLabAPI = GitlabAPI.connect(Constant.GITLAB_BASE_URL, applicationDetails.getApiToken(),
					TokenType.ACCESS_TOKEN, AuthMethod.HEADER);
			AdaptorDetails adaptor = adaptorDetails.findByApplicationId(appId);

			gitLabAPI.deleteGroup((int) adaptor.getGroupid());

			return new CommonResponse(HttpStatus.OK, new Response("Delete Group", null),
					"Group marked for deletion successfully");
		} catch (FileNotFoundException e) {
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Delete group", null), "Group not found");
		}

		catch (UncheckedIOException e) {
			e.printStackTrace();
			if (e.getCause() instanceof org.gitlab.api.GitlabAPIException) {
				return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.INVALID_TOKEN, null),
						Constant.TOKEN_EXPIRY);
			} else
				return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Delete group response", null),
						"Exception in delete group api");
		} catch (IOException e) {
			e.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Delete Group response", null),
					"Exception in delete group api");
		}
	}

	@Override
	public CommonResponse getInvitationList(String appId) throws IOException {

		AdaptorDetails applicationDetails = adaptorDetails.findByApplicationId(appId);
		if (applicationDetails == null) {
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.INVALID_ADAPTOR_APPLICATION, null),
					Constant.APPLICATION_NOT_CONNECTED);
		}

		String invitationsListUrl = Constant.GITLAB_GROUP_URL + ((int) applicationDetails.getGroupid())
				+ "/invitations";
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(invitationsListUrl);
		HttpHeaders headers = new HttpHeaders();
		headers.add(Constant.AUTHORIZATION, Constant.BEARER + applicationDetails.getApiToken());
		HttpEntity<HttpHeaders> requestEntity = new HttpEntity<>(headers);
		RestTemplate restTemplate = new RestTemplate();
		try {
			ResponseEntity<Object> response = restTemplate.exchange(builder.toUriString(), HttpMethod.GET,
					requestEntity, Object.class);
			List<GitlabInvitationsResponse> users = objectMapper.readValue(
					objectMapper.writeValueAsString(response.getBody()),
					new TypeReference<List<GitlabInvitationsResponse>>() {
					});
			if (response.getStatusCode() == HttpStatus.OK) {
				return new CommonResponse(HttpStatus.OK, new Response(Constant.INVITAION_LIST_RESPONSE, users),
						"Invitation list retrieved successfully");
			} else
				return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.INVITAION_LIST_RESPONSE, null),
						"Exception in invitation list method");
		} catch (HttpClientErrorException | JsonProcessingException e) {
			e.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.INVITAION_LIST_RESPONSE, null),
					"Exception in get invitation list");
		}  
	}

	@Override
	public CommonResponse revokeInvitation(String email, String appId) {

		AdaptorDetails applicationDetails = adaptorDetails.findByApplicationId(appId);
		if (applicationDetails == null) {
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.INVALID_ADAPTOR_APPLICATION, null),
					Constant.APPLICATION_NOT_CONNECTED);
		}

		String revokeInvitation = Constant.GITLAB_GROUP_URL + ((int) applicationDetails.getGroupid())
				+ "/invitations/" + email;
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(revokeInvitation);
		HttpHeaders headers = new HttpHeaders();
		headers.add(Constant.AUTHORIZATION, Constant.BEARER + applicationDetails.getApiToken());
		HttpEntity<HttpHeaders> requestEntity = new HttpEntity<>(headers);
		RestTemplate restTemplate = new RestTemplate();
		try {
			ResponseEntity<Object> response = restTemplate.exchange(builder.toUriString(), HttpMethod.DELETE,
					requestEntity, Object.class);
			if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
				return new CommonResponse(HttpStatus.OK, new Response("Revoke user invitation response", null),
						"Invitation revoked successfully");
			} else if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
				return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Revoke Invitation response", null),
						"No invitation found for this user");
			} else
				return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Revoke invitation response", null),
						"Exception in revoke invitation list method");
		} catch (HttpClientErrorException e) {
			e.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Revoke user invitation response", null),
					"Exception in revoke invitation");
		}
	}

	public String getRefreshToken(String appId) {
		AdaptorDetails adaptor = adaptorDetails.findByApplicationId(appId);
		String accessTokenURL = Constant.GITLAB_URL + Constant.CLIENT_ID + adaptor.getClientId() + "&"
				+ Constant.CLIENT_SECRET + adaptor.getClientSecret() + "" + "&refresh_token=" + adaptor.getApiToken()
				+ "&grant_type=refresh_token&redirect_uri=" + adaptor.getRedirectUrl();
		RestTemplate restTemplate = new RestTemplate();
		try {
			ResponseEntity<AccessToken> response = restTemplate.postForEntity(accessTokenURL, null, AccessToken.class);
			if (response.getStatusCode() == HttpStatus.OK) {
				adaptor.setApiToken(response.getBody().getRefresh_token());
				adaptorDetails.save(adaptor);
				return response.getBody().getAccess_token();
			} else {
				return null;
			}
		} catch (Exception e) {
			return null;
		}
	}

}
