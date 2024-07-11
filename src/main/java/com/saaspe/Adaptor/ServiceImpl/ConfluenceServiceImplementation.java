		package com.saaspe.Adaptor.ServiceImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.saaspe.Adaptor.Advice.CommonResponse;
import com.saaspe.Adaptor.Advice.DataValidationException;
import com.saaspe.Adaptor.Advice.Response;
import com.saaspe.Adaptor.Constant.Constant;
import com.saaspe.Adaptor.Entity.AdaptorDetails;
import com.saaspe.Adaptor.Entity.AtlassianUsers;
import com.saaspe.Adaptor.Entity.AtlassianGroups;
import com.saaspe.Adaptor.Model.ConfluenceCreateUser;
import com.saaspe.Adaptor.Model.ConfluenceGetUserListResponse;
import com.saaspe.Adaptor.Model.ConfluenceUsersbyGroup;
import com.saaspe.Adaptor.Repository.AdaptorDetailsRepository;
import com.saaspe.Adaptor.Repository.AtlassianGroupRepository;
import com.saaspe.Adaptor.Repository.AtlassianUsersRepository;
import com.saaspe.Adaptor.Service.ConfluenceService;

@Service
public class ConfluenceServiceImplementation implements ConfluenceService {

	@Autowired
	private AdaptorDetailsRepository adaptorDetailsRepsitory;

	@Autowired
	private AtlassianGroupRepository atlassianGroupRepository;

	@Autowired
	private AtlassianUsersRepository atlassianUsersRepository;

	@Autowired
	private RestTemplate restTemplate;

	@Value("${AtlassianConfluence.api.base-url}")
	private String confluenceApiBaseUrl;

	@Override
	@Transactional
	public CommonResponse createUser(ConfluenceCreateUser confluenceCreateUser, String appId) {
	    try {
	        AdaptorDetails adaptorDetails = adaptorDetailsRepsitory.findByApplicationId(appId);
	        if (adaptorDetails == null) {
	            return new CommonResponse(HttpStatus.NOT_FOUND,
	                    new Response(Constant.USER_CREATION, "Adaptor details not found"), "Adaptor details not found");
	        }
	        String url = confluenceApiBaseUrl + "/rest/api/3/user";
	        HttpHeaders headers = new HttpHeaders();
	        headers.setBasicAuth(adaptorDetails.getEmail(), adaptorDetails.getApiToken());
	        headers.setContentType(MediaType.APPLICATION_JSON);
	        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
	        HttpEntity<ConfluenceCreateUser> requestEntity = new HttpEntity<>(confluenceCreateUser, headers);
	        ResponseEntity<ConfluenceGetUserListResponse> responseEntity = restTemplate.postForEntity(url,
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

	        List<String> groupIds = new ArrayList<>();
	        List<String> groupNames = new ArrayList<>();
	        getUserByGroup(accountId, headers, groupIds, groupNames);

	        AtlassianUsers atlassianUser = new AtlassianUsers(accountId,confluenceCreateUser.getEmailAddress());
	        atlassianUsersRepository.save(atlassianUser);


	        return new CommonResponse(HttpStatus.CREATED, new Response(Constant.USER_CREATION, jsonResponse.toString()),
	                "User created successfully");

	    } catch (HttpClientErrorException e) {
	        return new CommonResponse(e.getStatusCode(),
	                new Response(Constant.USER_CREATION, e.getResponseBodyAsString()), "Error during API call");
	    } catch (DataAccessException e) {
	        return new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR,
	                new Response(Constant.USER_CREATION, "Database access error: " + e.getMessage()), "Database access error");
	    } catch (Exception e) {
	        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
	        return new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR,
	                new Response(Constant.USER_CREATION, "An unexpected error occurred: " + e.getMessage()),
	                "An unexpected error occurred");
	    }
	}


	private void getUserByGroup(String accountId,
	        HttpHeaders headers, List<String> groupIds, List<String> groupNames) throws DataValidationException{
	    try {
	        String url = confluenceApiBaseUrl + "/wiki/rest/api/user/memberof?accountId=" + accountId;
	        headers.setContentType(MediaType.APPLICATION_JSON);
	        HttpEntity<?> requestEntity = new HttpEntity<>(headers);
	        Thread.sleep(15000);
	        ResponseEntity<ConfluenceUsersbyGroup> responseEntity = restTemplate.exchange(url, HttpMethod.GET,
	                requestEntity, ConfluenceUsersbyGroup.class);
	        if (responseEntity.getStatusCode().is2xxSuccessful()) {

	            ConfluenceUsersbyGroup confluenceUsersbyGroup = responseEntity.getBody();
	            if (confluenceUsersbyGroup != null && confluenceUsersbyGroup.getResults() != null) {

	                for (ConfluenceUsersbyGroup.UserGroup group : confluenceUsersbyGroup.getResults()) {
	                    AtlassianGroups existingGroup = atlassianGroupRepository.findByGroupIdAndAccountId(group.getId(), accountId);
	                    if (existingGroup == null) {
	                        groupIds.add(group.getId());
	                        groupNames.add(group.getName());

	                        AtlassianGroups atlassianGroup = new AtlassianGroups(null, group.getId(), group.getName(), accountId);
	                        atlassianGroupRepository.save(atlassianGroup);
	                    } else {
	                        groupIds.add(existingGroup.getGroupId());
	                        groupNames.add(existingGroup.getGroupName());
	                    }
	                }

	                for (ConfluenceUsersbyGroup.UserGroup group : confluenceUsersbyGroup.getResults()) {
	                    if (group.getName().startsWith("jira-software-users-")) {
	                        removeUserFromGroup(group.getId(), accountId, headers);
	                    }
	                }
	            }
	        } else {
	            throw new DataValidationException(HttpStatus.BAD_REQUEST,"Failed to fetch user groups from Confluence API",null);
	        }
	    } catch (HttpClientErrorException e) {
	        throw e;
	    } catch (DataAccessException e) {
	        throw new DataValidationException(HttpStatus.BAD_REQUEST,"Database access error while storing user groups: " + e.getMessage(),null);
	    } catch (Exception e) {
	    	Thread.currentThread().interrupt();
	        throw new DataValidationException(HttpStatus.BAD_REQUEST,"An unexpected error occurred while fetching user groups: " + e.getMessage(), null);
	    }
	}


	private void removeUserFromGroup(String groupId, String accountId, HttpHeaders headers) throws DataValidationException {
	    try {
	        String url = confluenceApiBaseUrl + "/wiki/rest/api/group/userByGroupId?groupId=" + groupId + "&accountId="
	                + accountId;
	        HttpHeaders deleteHeaders = new HttpHeaders();
	        deleteHeaders.addAll(headers);
	        HttpEntity<?> requestEntity = new HttpEntity<>(deleteHeaders);
	        ResponseEntity<Void> responseEntity = restTemplate.exchange(url, HttpMethod.DELETE, requestEntity,
	                Void.class);
	        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
	            throw new DataValidationException(HttpStatus.BAD_REQUEST,
	                    "Failed to remove user from group. Status code: " + responseEntity.getStatusCodeValue(),null);
	        } else {
	            atlassianGroupRepository.deleteByGroupIdAndAccountId(groupId, accountId);
	        }
	    } catch (HttpClientErrorException e) {
	        throw e;
	    } catch (Exception e) {
	        throw new DataValidationException(HttpStatus.BAD_REQUEST,"An unexpected error occurred while removing user from group: " + e.getMessage(), null);
	    }
	}



	@Override
	public CommonResponse getUserList(String appId) throws DataValidationException {
		ObjectMapper mapper = new ObjectMapper();
		AdaptorDetails adaptorDetails = adaptorDetailsRepsitory.findByApplicationId(appId);
		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth(adaptorDetails.getEmail(), adaptorDetails.getApiToken());
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> requestEntity = new HttpEntity<>(headers);
		try {
			ResponseEntity<String> response = restTemplate.exchange(confluenceApiBaseUrl + "/rest/api/2/users/search?maxResults=1000",
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
	public CommonResponse deleteUser(String accountId, String appId) {
	    AdaptorDetails adaptorDetails = adaptorDetailsRepsitory.findByApplicationId(appId);
	    List<String> groupIds = new ArrayList<>();
	    List<AtlassianGroups> atlassianGroupsList = atlassianGroupRepository.findAll();

	    for (AtlassianGroups group : atlassianGroupsList) {
	        if (group.getGroupName().startsWith("confluence-users-")) {
	            groupIds.add(group.getGroupId());
	        }
	    }
      
	    if (groupIds.isEmpty()) {
	        return new CommonResponse(HttpStatus.NOT_FOUND,
	                new Response("Group Not Found", "No groups starting with 'confluence-users-' found in the database"),
	                "No groups found");
	    }
	    for (String groupId : groupIds) {
	        String url = confluenceApiBaseUrl + "/rest/api/3/group/user?groupId=" + groupId + "&accountId=" + accountId;
	        HttpHeaders headers = new HttpHeaders();
	        headers.setBasicAuth(adaptorDetails.getEmail(), adaptorDetails.getApiToken());
	        HttpEntity<HttpHeaders> requestEntity = new HttpEntity<>(headers);
	        try {
	            ResponseEntity<String> responseEntity = new RestTemplate().exchange(url, HttpMethod.DELETE, requestEntity,
	                    String.class);
	            if (responseEntity.getStatusCode().is2xxSuccessful()) {
	            	   return new CommonResponse(responseEntity.getStatusCode(),
	                        new Response("User Removed from Group","User removed from the Confluence group"),
	                        "User removed from group successfully");

	            }
	        } catch (HttpClientErrorException e) {
	            return new CommonResponse(e.getStatusCode(),
	                    new Response("DeleteUser", e.getResponseBodyAsString()), "Error during API call");
	        } catch (Exception e) {
	            return new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR,
	                    new Response("DeleteUser", e.getMessage()), "An unexpected error occurred");
	        }
	    }
	    return new CommonResponse(HttpStatus.OK,
	            new Response("User Removed from Groups", "User successfully removed from groups"),
	            "User removed from groups successfully");
	}
}


