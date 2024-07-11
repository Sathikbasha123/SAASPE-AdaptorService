package com.saaspe.Adaptor.ServiceImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.saaspe.Adaptor.Advice.CommonResponse;
import com.saaspe.Adaptor.Advice.DataValidationException;
import com.saaspe.Adaptor.Advice.Response;
import com.saaspe.Adaptor.Constant.Constant;
import com.saaspe.Adaptor.Entity.AdaptorDetails;
import com.saaspe.Adaptor.Entity.AtlassianUsers;
import com.saaspe.Adaptor.Model.ConfluenceGetUserListResponse;
import com.saaspe.Adaptor.Model.JiraCreateUserRequest;
import com.saaspe.Adaptor.Model.JiraGroups;
import com.saaspe.Adaptor.Model.JiraGroups.Group;
import com.saaspe.Adaptor.Repository.AdaptorDetailsRepository;
import com.saaspe.Adaptor.Repository.AtlassianUsersRepository;
import com.saaspe.Adaptor.Service.JiraService;

@Service
public class JIRAServiceImplementation implements JiraService {

	@Autowired
	private AdaptorDetailsRepository adaptorDetailsRepsitory;
	
	@Autowired
	private AtlassianUsersRepository atlassianUsersRepository;
	
	@Autowired
	private RestTemplate restTemplate;

	@Value("${AtlassianJira.your.api.url}")
	private String jiraApiUrl;

	@Override
	public CommonResponse createUser(JiraCreateUserRequest jiraCreateUserRequest, String appId) {
	    AdaptorDetails adaptorDetails = adaptorDetailsRepsitory.findByApplicationId(appId);
	    String createUserUrl = jiraApiUrl + "/rest/api/3/user";

	    HttpHeaders headers = new HttpHeaders();
	    headers.setBasicAuth(adaptorDetails.getEmail(), adaptorDetails.getApiToken());
	    headers.setContentType(MediaType.APPLICATION_JSON);
	    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
	    jiraCreateUserRequest.setProducts(Collections.singletonList("jira-software"));
	    HttpEntity<JiraCreateUserRequest> requestEntity = new HttpEntity<>(jiraCreateUserRequest, headers);
	    try {
	        ResponseEntity<ConfluenceGetUserListResponse> responseEntity = restTemplate.exchange(createUserUrl, HttpMethod.POST,
	                requestEntity, ConfluenceGetUserListResponse.class);
	        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
	            String responseBody = responseEntity.getBody() != null ? responseEntity.getBody().toString() : "";
	            return new CommonResponse(responseEntity.getStatusCode(), new Response(Constant.USER_CREATION, responseBody),
	                    "Failed to create user");
	        }
	        
	        ConfluenceGetUserListResponse jsonResponse = responseEntity.getBody();
	        String accountId = jsonResponse != null ? jsonResponse.getAccountId() : null;

	        if (accountId == null || accountId.isEmpty()) {
	            return new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR,
	                    new Response(Constant.USER_CREATION, "Account ID is null or empty"),
	                    "Failed to create user - Account ID is null or empty");
	        }
	                    AtlassianUsers jiraUser = new AtlassianUsers(accountId, jiraCreateUserRequest.getEmailAddress());
	                    atlassianUsersRepository.save(jiraUser);
	                } 
	            
	        
	     catch (HttpClientErrorException e) {
	        return new CommonResponse(e.getStatusCode(),
	                new Response(Constant.USER_CREATION, e.getResponseBodyAsString()), Constant.JIRA_ERROR_MESSAGE);
	    } catch (Exception e) {
	        return new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR, new Response(Constant.USER_CREATION, e.getMessage()),
	                Constant.UNEXPECTED_ERROR);
	    }
	    return new CommonResponse(HttpStatus.CREATED, new Response(Constant.USER_CREATION, "User Created"),
                "User created successfully");
	}
	
	@Override
	public CommonResponse addUserToGroup(String productName, String accountId, String appId) {
	    try {
	        AdaptorDetails adaptorDetails = adaptorDetailsRepsitory.findByApplicationId(appId);
	        HttpHeaders headers = new HttpHeaders();
	        headers.setBasicAuth(adaptorDetails.getEmail(), adaptorDetails.getApiToken());
	        headers.setContentType(MediaType.APPLICATION_JSON);
	        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

	        ResponseEntity<JiraGroups> responseEntity = restTemplate.exchange(
	                jiraApiUrl + "/rest/api/3/groups/picker", HttpMethod.GET,
	                requestEntity, JiraGroups.class);
	        
	        JiraGroups groupsResponse = responseEntity.getBody();
	        List<JiraGroups.Group> groups = groupsResponse.getGroups();

	        List<String> jiraSoftwareUsersGroups = groups.stream()
	                .filter(group -> group.getName().startsWith("jira-software-users-"))
	                .map(JiraGroups.Group::getName)
	                .collect(Collectors.toList());

	        for (String jiraSoftwareUsersGroup : jiraSoftwareUsersGroups) {
	            String addUserToGroupUrl = jiraApiUrl + "/rest/api/3/group/user?groupname=" + jiraSoftwareUsersGroup;
	            String jsonPayload = " "+ accountId + " ";

	       
	            HttpEntity<String> requestEntityWithBody = new HttpEntity<>(jsonPayload, headers);
	            ResponseEntity<String> addUserResponse = restTemplate.exchange(addUserToGroupUrl, HttpMethod.POST, requestEntityWithBody, String.class);
	           
	            if (addUserResponse.getStatusCode() != HttpStatus.OK) {
	               
	                return new CommonResponse(addUserResponse.getStatusCode(),
	                        new Response(Constant.USER_ADDED_TO_GROUP, addUserResponse.getBody()), Constant.JIRA_ERROR_MESSAGE);
	            }
	        }
	        
	        return new CommonResponse(HttpStatus.OK,
	                new Response("User added to Groups", "User successfully added to groups"),
	                "User added to groups successfully");

	    } catch (HttpClientErrorException e) {
	        return new CommonResponse(e.getStatusCode(),
	                new Response(Constant.USER_ADDED_TO_GROUP, e.getResponseBodyAsString()), Constant.JIRA_ERROR_MESSAGE);
	    } catch (Exception e) {
	        return new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR,
	                new Response(Constant.USER_ADDED_TO_GROUP, e.getMessage()), Constant.UNEXPECTED_ERROR);
	    }
	}

	@Override
	public CommonResponse getAllUsers(String appId) throws DataValidationException {
		ObjectMapper mapper = new ObjectMapper();
		AdaptorDetails adaptorDetails = adaptorDetailsRepsitory.findByApplicationId(appId);
		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth(adaptorDetails.getEmail(), adaptorDetails.getApiToken());
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> requestEntity = new HttpEntity<>(headers);
		try {
			ResponseEntity<String> response = restTemplate.exchange(jiraApiUrl + "/rest/api/3/users/search?maxResults=1000",
					HttpMethod.GET, requestEntity, String.class);

			if (response.getStatusCode().is2xxSuccessful()) {
				List<ConfluenceGetUserListResponse> microsoft365UserResponse = mapToConfluenceGetUserListResponse(
						response.getBody());
				return new CommonResponse(HttpStatus.OK, new Response("Get userList", microsoft365UserResponse),
						"User List retrived Successfully");

			} else {
				throw new DataValidationException(HttpStatus.BAD_REQUEST,
						"Failed to fetch profile details from Confluence. Status code: " + response.getStatusCode(),null);
			}
		} catch (HttpClientErrorException.BadRequest ex) {
			ex.printStackTrace();
			String responseBody = ex.getResponseBodyAsString();
			Object errorResponse = null;
			try {
				errorResponse = mapper.readValue(responseBody, Object.class);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Get all user details Response", errorResponse), " Get all user details Failed");
		} catch (RestClientException ex) {
			ex.printStackTrace();
			return new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					new Response("Get all user details Response", null), "Internal Server Error");
		}
	}

	private List<ConfluenceGetUserListResponse> mapToConfluenceGetUserListResponse(String jsonResponse) throws DataValidationException {
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode rootNode = objectMapper.readTree(jsonResponse);
			List<ConfluenceGetUserListResponse> userlist = new ArrayList<>();
			for (JsonNode userDataNode : rootNode) {
				ConfluenceGetUserListResponse confluenceUserResponse = new ConfluenceGetUserListResponse();
				confluenceUserResponse.setSelf(userDataNode.path("self").asText());
				confluenceUserResponse.setAccountId(userDataNode.path("accountId").asText());
				confluenceUserResponse.setAccountType(userDataNode.path("accountType").asText());
				confluenceUserResponse.setDisplayName(userDataNode.path("displayName").asText());
				confluenceUserResponse.setActive(userDataNode.path("active").asBoolean());
				userlist.add(confluenceUserResponse);
			}
			return userlist;
		} catch (Exception e) {
			e.printStackTrace();
			throw new DataValidationException(HttpStatus.BAD_REQUEST, "Failed to map JSON response", null);
		}
	}

	
	@Override
	public CommonResponse removeUserFromGroup(String accountId, String appId) {
	   try {
		AdaptorDetails adaptorDetails = adaptorDetailsRepsitory.findByApplicationId(appId);
	    HttpHeaders headers = new HttpHeaders();
		
	    headers.setBasicAuth(adaptorDetails.getEmail(), adaptorDetails.getApiToken());
	    HttpEntity<?> requestEntity = new HttpEntity<>(headers);

	        ResponseEntity<JiraGroups> responseEntity = restTemplate.exchange(
	                jiraApiUrl + "/rest/api/3/groups/picker", HttpMethod.GET,
	                requestEntity, JiraGroups.class);
	       
	        JiraGroups groupsResponse = responseEntity.getBody();
	        ArrayList<Group> responseData = (ArrayList<Group>) groupsResponse.getGroups();
	        String jsonString = new JSONArray(responseData).toString();
	        JsonArray jsonArray = JsonParser.parseString(jsonString).getAsJsonArray();

	        List<String> allGroups = new ArrayList<>();
	        for (Object element : jsonArray) {
	            JsonObject groupObject = ((JsonElement) element).getAsJsonObject();
	            String groupName = groupObject.get("name").getAsString();
	            allGroups.add(groupName);
	        }

	        List<String> groupsToRemove = new ArrayList<>();
	        String productName="JIRA";
	        deleteUsers(allGroups, productName, groupsToRemove);

	        for (String group : groupsToRemove) {
	            String removeUserUrl = jiraApiUrl + "/rest/api/3/group/user?groupname=" + group + "&accountId=" + accountId;
	            restTemplate.exchange(removeUserUrl,
	                    HttpMethod.DELETE, requestEntity, CommonResponse.class);
	            return new CommonResponse(HttpStatus.OK,
	    	            new Response("User Removed from Groups", "User successfully removed from groups"),
	    	            "User removed from groups successfully");
	        }

	    } catch (HttpClientErrorException e) {
	        return new CommonResponse(e.getStatusCode(),
	                new Response(Constant.USER_REMOVED, e.getResponseBodyAsString()), Constant.JIRA_ERROR_MESSAGE);
	    } catch (Exception e) {
	        return new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR,
	                new Response(Constant.USER_REMOVED, e.getMessage()), Constant.UNEXPECTED_ERROR);
	    }
	    return new CommonResponse(HttpStatus.OK,
	            new Response("User Removed from Groups", "User successfully removed from groups"),
	            "User removed from groups successfully");
	}

	
	private void deleteUsers(List<String> allGroups, String productName, List<String> jiraSoftwareUsersGroups) {
		for (String groups : allGroups) {
			if (productName.equalsIgnoreCase("JIRA")) { 
				if (groups.startsWith("jira-software-users-")) {
					jiraSoftwareUsersGroups.add(groups);
				}
			} else if (productName.equalsIgnoreCase("Confluece") && (groups.startsWith("confluence-users-"))) {
				jiraSoftwareUsersGroups.add(groups);

			}
		}
	}

	@Override
	public CommonResponse getAuditRecords(String appId) {
		AdaptorDetails adaptorDetails = adaptorDetailsRepsitory.findByApplicationId(appId);
		String url = jiraApiUrl + "/rest/api/3/auditing/record";

		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth(adaptorDetails.getEmail(), adaptorDetails.getApiToken());
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

		HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

		try {
			ResponseEntity<String> responseEntity = new RestTemplate().exchange(url, HttpMethod.GET, requestEntity,
					String.class);

			if (responseEntity.getStatusCode().is2xxSuccessful()) {
				return new CommonResponse(responseEntity.getStatusCode(),
						new Response(Constant.AUDIT_RESPONSE, responseEntity.getBody()),
						"Audit records retrieval successful");
			} else {
				return new CommonResponse(responseEntity.getStatusCode(),
						new Response(Constant.AUDIT_RESPONSE, responseEntity.getBody()), "Failed to get audit records");
			}
		} catch (HttpClientErrorException e) {
			return new CommonResponse(e.getStatusCode(),
					new Response(Constant.AUDIT_RESPONSE, e.getResponseBodyAsString()), Constant.JIRA_ERROR_MESSAGE);
		} catch (Exception e) {
			return new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					new Response(Constant.AUDIT_RESPONSE, e.getMessage()), Constant.UNEXPECTED_ERROR);
		}
	}

	@Override
	public CommonResponse getInstanceLicense(String appId) {
		AdaptorDetails adaptorDetails = adaptorDetailsRepsitory.findByApplicationId(appId);
		String url = jiraApiUrl + "/rest/api/3/instance/license";

		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth(adaptorDetails.getEmail(), adaptorDetails.getApiToken());
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

		HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

		try {
			ResponseEntity<String> responseEntity = new RestTemplate().exchange(url, HttpMethod.GET, requestEntity,
					String.class);

			if (responseEntity.getStatusCode().is2xxSuccessful()) {
				return new CommonResponse(responseEntity.getStatusCode(),
						new Response(Constant.GET_INSTANCE_LICENSE, responseEntity.getBody()), Constant.REQUEST_SUCCESSFUL);
			} else {
				return new CommonResponse(responseEntity.getStatusCode(),
						new Response(Constant.GET_INSTANCE_LICENSE, responseEntity.getBody()),
						"Failed to get instance license");
			}
		} catch (HttpClientErrorException e) {
			return new CommonResponse(e.getStatusCode(),
					new Response(Constant.GET_INSTANCE_LICENSE, e.getResponseBodyAsString()), Constant.JIRA_ERROR_MESSAGE);
		} catch (Exception e) {
			return new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					new Response(Constant.GET_INSTANCE_LICENSE, e.getMessage()), Constant.UNEXPECTED_ERROR);
		}
	}

	


	@Override
	public CommonResponse deleteUser(String accountId, String appId) {
		AdaptorDetails adaptorDetails = adaptorDetailsRepsitory.findByApplicationId(appId);
		String url = jiraApiUrl + "/rest/api/3/user?accountId=" + accountId;

		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth(adaptorDetails.getEmail(), adaptorDetails.getApiToken());
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

		HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

		try {
			ResponseEntity<Void> responseEntity = new RestTemplate().exchange(url, HttpMethod.DELETE, requestEntity,
					Void.class);

			if (responseEntity.getStatusCode() == (HttpStatus.NO_CONTENT)) {
				return new CommonResponse(responseEntity.getStatusCode(),
						new Response(Constant.USER_DELETION, responseEntity.getBody()), "User deleted successfully");
			} else {
				return new CommonResponse(responseEntity.getStatusCode(),
						new Response(Constant.USER_DELETION, responseEntity.getBody()), "Failed to delete user");
			}
		} catch (HttpClientErrorException e) {
			return new CommonResponse(e.getStatusCode(),
					new Response(Constant.USER_DELETION, e.getResponseBodyAsString()), Constant.JIRA_ERROR_MESSAGE);
		} catch (Exception e) {
			return new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR, new Response(Constant.USER_DELETION, e.getMessage()),
					Constant.UNEXPECTED_ERROR);
		}
	}

	@Override
	public CommonResponse getAllLicenseDetails(String appId) {
		AdaptorDetails adaptorDetails = adaptorDetailsRepsitory.findByApplicationId(appId);
		String url = jiraApiUrl + "/rest/api/3/applicationrole";

		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth(adaptorDetails.getEmail(), adaptorDetails.getApiToken());
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

		HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
		RestTemplate restTemplate = new RestTemplate();
		try {
			ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, requestEntity,
					String.class);
			return new CommonResponse(HttpStatus.OK, new Response("GetAllLicense Details", responseEntity.getBody()),
					Constant.REQUEST_SUCCESSFUL);
		} catch (HttpClientErrorException e) {
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Get All License Details", e.getResponseBodyAsString()),
					"Error in the request. Please check your authorization header.");
		} catch (Exception e) {
			return new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					new Response("Get All License Details", e.getMessage()),
					"An error occurred while processing the request");
		}
	}

	@Override
	public CommonResponse getLicenseDetails(String key, String appId) {
		AdaptorDetails adaptorDetails = adaptorDetailsRepsitory.findByApplicationId(appId);
		String url = jiraApiUrl + "/rest/api/3/applicationrole/{key}";

		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth(adaptorDetails.getEmail(), adaptorDetails.getApiToken());
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

		HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

		Map<String, String> uriVariables = new HashMap<>();
		uriVariables.put("key", key);

		RestTemplate restTemplate = new RestTemplate();
		try {
			ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, requestEntity,
					String.class, uriVariables);
			return new CommonResponse(HttpStatus.OK, new Response("GetLicense Details", responseEntity.getBody()),
					Constant.REQUEST_SUCCESSFUL);
		} catch (HttpClientErrorException e) {
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Get License Details", e.getResponseBodyAsString()),
					"Error in the request. Please check your authorization header.");
		} catch (Exception e) {
			return new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					new Response("Get License Details", e.getMessage()),
					"An error occurred while processing the request");
		}
	}

	@Override
	public CommonResponse getAllAppDetail() {
		String url = "https://marketplace.atlassian.com/rest/2/addons";
		HttpHeaders headers = new HttpHeaders();
		headers.set(Constant.ACCEPT, Constant.APPLICATION_JSON);

		HttpEntity<String> requestEntity = new HttpEntity<>(headers);

		try {
			ResponseEntity<String> responseEntity = new RestTemplate().exchange(url, HttpMethod.GET, requestEntity,
					String.class);

			if (responseEntity.getStatusCode().is2xxSuccessful()) {
				return new CommonResponse(responseEntity.getStatusCode(),
						new Response("Get All Applications Details", responseEntity.getBody()), Constant.REQUEST_SUCCESSFUL);
			} else {
				return new CommonResponse(responseEntity.getStatusCode(),
						new Response(Constant.ERROR, responseEntity.getBody()), "Failed to fetch Applications Details");
			}
		} catch (HttpClientErrorException e) {
			return new CommonResponse(e.getStatusCode(), new Response(Constant.ERROR, e.getResponseBodyAsString()),
					Constant.JIRA_ERROR_MESSAGE);
		} catch (Exception e) {
			return new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR, new Response(Constant.ERROR, e.getMessage()),
					Constant.UNEXPECTED_ERROR);
		}
	}

	@Override
	public CommonResponse getAppDetail(String addonKey) {
		String url = "https://marketplace.atlassian.com/rest/2/addons/" + addonKey;

		HttpHeaders headers = new HttpHeaders();
		headers.set(Constant.ACCEPT, Constant.APPLICATION_JSON);

		HttpEntity<String> requestEntity = new HttpEntity<>(headers);

		try {
			ResponseEntity<String> responseEntity = new RestTemplate().exchange(url, HttpMethod.GET, requestEntity,
					String.class);
			if (responseEntity.getStatusCode().is2xxSuccessful()) {
				return new CommonResponse(responseEntity.getStatusCode(),
						new Response("Get Application Details", responseEntity.getBody()), Constant.REQUEST_SUCCESSFUL);
			} else {
				return new CommonResponse(responseEntity.getStatusCode(),
						new Response(Constant.ERROR, responseEntity.getBody()), "Failed to fetch Application Details");
			}
		} catch (HttpClientErrorException e) {
			return new CommonResponse(e.getStatusCode(),
					new Response(Constant.ERROR, e.getResponseBodyAsString()), Constant.JIRA_ERROR_MESSAGE);
		} catch (Exception e) {
			return new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR, new Response(Constant.ERROR, e.getMessage()),
					Constant.UNEXPECTED_ERROR);
		}
	}

	@Override
	public CommonResponse getUserEmail(String accountId, String appId) {
		AdaptorDetails adaptorDetails = adaptorDetailsRepsitory.findByApplicationId(appId);
		String url = jiraApiUrl + "/rest/api/3/user/email?accountId=" + accountId;

		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth(adaptorDetails.getEmail(), adaptorDetails.getApiToken());
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

		HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

		try {
			ResponseEntity<String> responseEntity = new RestTemplate().exchange(url, HttpMethod.GET, requestEntity,
					String.class);

			if (responseEntity.getStatusCode().is2xxSuccessful()) {
				return new CommonResponse(responseEntity.getStatusCode(),
						new Response(Constant.USER_EMAIL_RETRIEVAL, responseEntity.getBody()),
						"User email retrieved successfully");
			} else {
				return new CommonResponse(responseEntity.getStatusCode(),
						new Response(Constant.USER_EMAIL_RETRIEVAL, responseEntity.getBody()),
						"Failed to retrieve user email");
			}
		} catch (HttpClientErrorException e) {
			return new CommonResponse(e.getStatusCode(),
					new Response(Constant.USER_EMAIL_RETRIEVAL, e.getResponseBodyAsString()), Constant.JIRA_ERROR_MESSAGE);
		} catch (Exception e) {
			return new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					new Response(Constant.USER_EMAIL_RETRIEVAL, e.getMessage()), Constant.UNEXPECTED_ERROR);
		}
	}

	@Override
	public CommonResponse getUserGroups(String accountId, String appId) {
		AdaptorDetails adaptorDetails = adaptorDetailsRepsitory.findByApplicationId(appId);
		String url = jiraApiUrl + "/rest/api/3/user/groups?accountId=" + accountId;

		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth(adaptorDetails.getEmail(), adaptorDetails.getApiToken());
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

		HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

		try {
			ResponseEntity<String> responseEntity = new RestTemplate().exchange(url, HttpMethod.GET, requestEntity,
					String.class);

			if (responseEntity.getStatusCode().is2xxSuccessful()) {
				return new CommonResponse(responseEntity.getStatusCode(),
						new Response(Constant.USER_GROUPS_RETRIEVAL, responseEntity.getBody()),
						"User groups retrieved successfully");
			} else {
				return new CommonResponse(responseEntity.getStatusCode(),
						new Response(Constant.USER_GROUPS_RETRIEVAL, responseEntity.getBody()),
						"Failed to retrieve user groups");
			}
		} catch (HttpClientErrorException e) {
			return new CommonResponse(e.getStatusCode(),
					new Response(Constant.USER_GROUPS_RETRIEVAL, e.getResponseBodyAsString()), Constant.JIRA_ERROR_MESSAGE);
		} catch (Exception e) {
			return new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					new Response(Constant.USER_GROUPS_RETRIEVAL, e.getMessage()), Constant.UNEXPECTED_ERROR);
		}
	}

	@Override
	public CommonResponse getAllGroups(String appId) {
		AdaptorDetails adaptorDetails = adaptorDetailsRepsitory.findByApplicationId(appId);
		String url = jiraApiUrl + "/rest/api/3/groups/picker";

		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth(adaptorDetails.getEmail(), adaptorDetails.getApiToken());
		headers.set(Constant.ACCEPT, Constant.APPLICATION_JSON);

		HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

		try {
			ResponseEntity<String> responseEntity = new RestTemplate().exchange(url, HttpMethod.GET, requestEntity,
					String.class);
			if (responseEntity.getStatusCode().is2xxSuccessful()) {
				return new CommonResponse(responseEntity.getStatusCode(),
						new Response(Constant.GROUPS_RETRIEVAL, responseEntity.getBody()),
						"All Groups retrieved successfully");
			} else {
				return new CommonResponse(responseEntity.getStatusCode(),
						new Response("Groups Picker Retrieval", responseEntity.getBody()), "Failed to retrieve groups");
			}
		} catch (HttpClientErrorException e) {
			return new CommonResponse(e.getStatusCode(),
					new Response(Constant.GROUPS_RETRIEVAL, e.getResponseBodyAsString()), Constant.JIRA_ERROR_MESSAGE);
		} catch (Exception e) {
			return new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					new Response(Constant.GROUPS_RETRIEVAL, e.getMessage()), Constant.UNEXPECTED_ERROR);
		}
	}

	@Override
	public CommonResponse getGroupMembers(String groupName, String appId) {
		AdaptorDetails adaptorDetails = adaptorDetailsRepsitory.findByApplicationId(appId);
		String url = jiraApiUrl + "/rest/api/3/group/member?groupname=" + groupName;

		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth(adaptorDetails.getEmail(), adaptorDetails.getApiToken());
		headers.set(Constant.ACCEPT, Constant.APPLICATION_JSON);

		HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

		try {
			ResponseEntity<String> responseEntity = new RestTemplate().exchange(url, HttpMethod.GET, requestEntity,
					String.class);

			if (responseEntity.getStatusCode().is2xxSuccessful()) {
				return new CommonResponse(responseEntity.getStatusCode(),
						new Response(Constant.GROUP_MEMBERS_RETRIEVAL, responseEntity.getBody()),
						"Group members retrieved successfully");
			} else {
				return new CommonResponse(responseEntity.getStatusCode(),
						new Response(Constant.GROUP_MEMBERS_RETRIEVAL, responseEntity.getBody()),
						"Failed to retrieve group members");
			}
		} catch (HttpClientErrorException e) {
			return new CommonResponse(e.getStatusCode(),
					new Response(Constant.GROUP_MEMBERS_RETRIEVAL, e.getResponseBodyAsString()), Constant.JIRA_ERROR_MESSAGE);
		} catch (Exception e) {
			return new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					new Response(Constant.GROUP_MEMBERS_RETRIEVAL, e.getMessage()), Constant.UNEXPECTED_ERROR);
		}
	}

	


	

	@Override
	public CommonResponse getUserByAccountId(String accountId, String appId) {
		AdaptorDetails adaptorDetails = adaptorDetailsRepsitory.findByApplicationId(appId);

		String url = jiraApiUrl + "/rest/api/3/user?accountId=" + accountId;
		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth(adaptorDetails.getEmail(), adaptorDetails.getApiToken());
		headers.set(Constant.ACCEPT, Constant.APPLICATION_JSON);

		HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

		try {
			ResponseEntity<String> responseEntity = new RestTemplate().exchange(url, HttpMethod.GET, requestEntity,
					String.class);

			if (responseEntity.getStatusCode().is2xxSuccessful()) {
				return new CommonResponse(responseEntity.getStatusCode(),
						new Response(Constant.USER_RETRIVAL, responseEntity.getBody()), "User retrieved successfully");
			} else {
				return new CommonResponse(responseEntity.getStatusCode(),
						new Response(Constant.USER_RETRIVAL, responseEntity.getBody()), "Failed to retrieve user");
			}
		} catch (HttpClientErrorException e) {
			return new CommonResponse(e.getStatusCode(),
					new Response(Constant.USER_RETRIVAL, e.getResponseBodyAsString()), Constant.JIRA_ERROR_MESSAGE);
		} catch (Exception e) {
			return new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR, new Response(Constant.USER_RETRIVAL, e.getMessage()),
					Constant.UNEXPECTED_ERROR);
		}
	}
}
