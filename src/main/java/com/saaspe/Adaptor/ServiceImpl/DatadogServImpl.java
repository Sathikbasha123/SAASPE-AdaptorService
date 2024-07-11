package com.saaspe.Adaptor.ServiceImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.saaspe.Adaptor.Model.DatadogCreateUserResponse;
import com.saaspe.Adaptor.Model.DatadogGetUserResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saaspe.Adaptor.Advice.CommonResponse;
import com.saaspe.Adaptor.Advice.DataValidationException;
import com.saaspe.Adaptor.Advice.Response;
import com.saaspe.Adaptor.Constant.Constant;
import com.saaspe.Adaptor.Entity.AdaptorDetails;
import com.saaspe.Adaptor.Repository.AdaptorDetailsRepository;
import com.saaspe.Adaptor.Service.DatadogService;

@Service
public class DatadogServImpl implements DatadogService {

	@Autowired
	private AdaptorDetailsRepository adaptorDetailsRepository;

	@Autowired
	private RestTemplate restTemplate;

	@Value("${Datadog.api.base-url}")
	private String datadogApiBaseUrl;

	public CommonResponse createUser(String userEmail, String appId) {
	    try {
		AdaptorDetails adaptorDetails = adaptorDetailsRepository.findByApplicationId(appId);
	    if (adaptorDetails == null) {
	        return new CommonResponse(HttpStatus.NOT_FOUND,
	                new Response(Constant.USER_CREATION, "Adaptor details not found"), "Adaptor details not found");
	    }
	    
	    String url = datadogApiBaseUrl + "/api/v2/users";
	    HttpHeaders headers = new HttpHeaders();
	    headers.set("dd-api-key", adaptorDetails.getApiKey());
	    headers.set("dd-application-key", adaptorDetails.getApiToken());
	    headers.setContentType(MediaType.APPLICATION_JSON);
	    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		String jsonPayload = "{\n" + "  \"data\": {\n" + "    \"type\": \"users\",\n" + "    \"attributes\": {\n"
				+ "      \"name\": \" \",\n" + "      \"email\": \"" + userEmail + "\"\n" + "    }\n" + "  }\n" + "}";
		
	        HttpEntity<String> requestEntity = new HttpEntity<>(jsonPayload, headers);
	        ResponseEntity<DatadogCreateUserResponse> responseEntity = restTemplate.postForEntity(url, requestEntity,
	                DatadogCreateUserResponse.class);
	        DatadogCreateUserResponse createUserResponse = responseEntity.getBody();
	        String userId = createUserResponse.getData().getId(); 

	        if (responseEntity.getStatusCode().is2xxSuccessful()) {
	            ResponseEntity<String> invitationResponse = sendInvitation(appId, userId);
	            if (invitationResponse.getStatusCode().is2xxSuccessful()) {
	                return new CommonResponse(HttpStatus.CREATED,
	                        new Response(Constant.USER_CREATION, "User Created Successfully"),
	                        "User Created Successfully");
	            } else {
	                return new CommonResponse(invitationResponse.getStatusCode(),
	                        new Response(Constant.USER_CREATION, invitationResponse.getBody()),
	                        "Error during invitation API call");
	            }
	        } else {
	            return new CommonResponse(responseEntity.getStatusCode(),
	                    new Response(Constant.USER_CREATION, responseEntity.getBody().toString()),
	                    "Error during user creation API call");
	        }
	    }
	    catch (HttpClientErrorException.Conflict e) {
	        try {
	            ResponseEntity<String> getUsersResponse = getUsers(appId);
	            if (getUsersResponse.getStatusCode().is2xxSuccessful()) {
	                ObjectMapper mapper = new ObjectMapper();
	                JsonNode usersNode = mapper.readTree(getUsersResponse.getBody()).get("data");
	                String userId1 = null;
	                for (JsonNode userNode : usersNode) {
	                    String email = userNode.get("attributes").get("email").asText();
	                    if (email.equals(userEmail)) { 
	                        userId1 = userNode.get("id").asText();
	                        break;
	                    }
	                }
	                if (userId1 != null) {
	                    ResponseEntity<String> updateUserResponse = updateUser(appId, userId1);
	                    if (updateUserResponse.getStatusCode().is2xxSuccessful()) {
	                        return new CommonResponse(HttpStatus.OK,
	                                new Response("User activated", "User already exists"),
	                                "User updated successfully");
	                    } else {
	                        return new CommonResponse(updateUserResponse.getStatusCode(),
	                                new Response("error in update user", updateUserResponse.getBody()),
	                                "Error during user update API call");
	                    }
	                } else {
	                    return new CommonResponse(HttpStatus.NOT_FOUND,
	                            new Response("user not found", "User not found"),
	                            "User not found");
	                }
	            } else {
	                return new CommonResponse(getUsersResponse.getStatusCode(),
	                        new Response("error in get user", getUsersResponse.getBody()),
	                        "Error during getUsers API call");
	            }
	        }catch (Exception e1) {
	        return new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR,
	                new Response(Constant.USER_CREATION, "An unexpected error occurred: " + e1.getMessage()),
	                "An unexpected error occurred");
	    }
	    }   
	}


	private ResponseEntity<String> sendInvitation(String appId, String userId) {
		try {
			AdaptorDetails adaptorDetails = adaptorDetailsRepository.findByApplicationId(appId);
			if (adaptorDetails == null) {
				return new ResponseEntity<>("Adaptor details not found", HttpStatus.NOT_FOUND);
			}
			String url = datadogApiBaseUrl + "/api/v2/user_invitations";
			HttpHeaders headers = new HttpHeaders();
			headers.set("dd-api-key", adaptorDetails.getApiKey());
			headers.set("dd-application-key", adaptorDetails.getApiToken());
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

			String jsonPayload = "{\n" + "\"data\":[\n" + "{\n" + " \"type\": \"user_invitations\",\n"
					+ "\"relationships\": {\n" + " \"user\": {\n" + " \"data\": {\n" + " \"type\": \"users\",\n"
					+ " \"id\": \"" + userId + "\"\n" + " }\n" + "}\n" + "}\n" + " }\n" + "  ]\n" + "}";

			HttpEntity<String> requestEntity = new HttpEntity<>(jsonPayload, headers);
			ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, requestEntity, String.class);
			return responseEntity;
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private ResponseEntity<String> updateUser(String appId, String userId1) {
		try {
			AdaptorDetails adaptorDetails = adaptorDetailsRepository.findByApplicationId(appId);
			if (adaptorDetails == null) {
				return new ResponseEntity<>("Adaptor details not found", HttpStatus.NOT_FOUND);
			}
			HttpHeaders headers = new HttpHeaders();
			headers.set("dd-api-key", adaptorDetails.getApiKey());
			headers.set("dd-application-key", adaptorDetails.getApiToken());
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

			String jsonPayload = "{\n" + "\"data\": \n" + "{\n" + " \"type\": \"users\",\n" + " \"id\": \"" + userId1
					+ "\",\n" + " \"attributes\": {\n" + " \"disabled\": false\n" + "}\n" + "}\n" + "\n" + "}";

			HttpEntity<String> requestEntity = new HttpEntity<>(jsonPayload, headers);
			RestTemplate restTemplates = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
			ResponseEntity<String> response = restTemplates.exchange(datadogApiBaseUrl + "/api/v2/users/" + userId1,
					HttpMethod.PATCH, requestEntity, String.class);
			return response;
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	private ResponseEntity<String> getUsers(String appId) {
		try {
			AdaptorDetails adaptorDetails = adaptorDetailsRepository.findByApplicationId(appId);
			if (adaptorDetails == null) {
				return new ResponseEntity<>("Adaptor details not found", HttpStatus.NOT_FOUND);
			}
			HttpHeaders headers = new HttpHeaders();
			headers.set("dd-api-key", adaptorDetails.getApiKey());
			headers.set("dd-application-key", adaptorDetails.getApiToken());
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

			HttpEntity<String> requestEntity = new HttpEntity<>(headers);
			RestTemplate restTemplates = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
			ResponseEntity<String> response = restTemplates.exchange(datadogApiBaseUrl + "/api/v2/users",
					HttpMethod.GET, requestEntity, String.class);
			return response;
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	
	

	public CommonResponse getUser(String appId) throws DataValidationException, IOException {
		AdaptorDetails adaptorDetails = adaptorDetailsRepository.findByApplicationId(appId);
		if (adaptorDetails == null) {
			return new CommonResponse(HttpStatus.NOT_FOUND,
					new Response("Adaptor details not found while getting user", "Adaptor details not found"),
					"Adaptor details not found for the particular application");
		}
		ObjectMapper mapper = new ObjectMapper();
		HttpHeaders headers = new HttpHeaders();
		headers.set("dd-api-key", adaptorDetails.getApiKey());
		headers.set("dd-application-key", adaptorDetails.getApiToken());
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		HttpEntity<String> requestEntity = new HttpEntity<>(headers);
		try {
			ResponseEntity<String> response = restTemplate.exchange(datadogApiBaseUrl + "/api/v2/users", HttpMethod.GET,
					requestEntity, String.class);

			if (response.getStatusCode().is2xxSuccessful()) {
				List<DatadogGetUserResponse> datadogUserResponse = mapToDatadogGetUserResponse(response.getBody());
				return new CommonResponse(HttpStatus.OK, new Response("Get UserList", datadogUserResponse),
						"User List retrieved Successfully");

			} else {
				throw new DataValidationException(HttpStatus.BAD_REQUEST,
						"Failed to fetch profile details from Datadog. Status code: " + response.getStatusCode(), null);
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
					new Response("Get all user details Response", errorResponse), "Get all user details Failed");
		} catch (RestClientException ex) {
			ex.printStackTrace();
			return new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					new Response("Get all user details Response", null), "Internal Server Error");
		}
	}

	private List<DatadogGetUserResponse> mapToDatadogGetUserResponse(String jsonResponse)
			throws DataValidationException, IOException {
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode rootNode = objectMapper.readTree(jsonResponse);
			JsonNode dataNode = rootNode.path("data");
			List<DatadogGetUserResponse> userList = new ArrayList<>();
			for (JsonNode userDataNode : dataNode) {
				String type = userDataNode.path("type").asText();
				String id = userDataNode.path("id").asText();
				String name = userDataNode.path("attributes").path("name").asText();
				String email = userDataNode.path("attributes").path("email").asText();
				boolean verified = userDataNode.path("attributes").path("verified").asBoolean();
				boolean disabled = userDataNode.path("attributes").path("disabled").asBoolean();
				String status = userDataNode.path("attributes").path("status").asText();

				DatadogGetUserResponse userResponse = new DatadogGetUserResponse(type, id, name, email, verified,
						disabled, status);
				userList.add(userResponse);
			}
			return userList;
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			throw new DataValidationException(HttpStatus.BAD_REQUEST, "Failed to parse JSON response", null);
		}
	}

	public CommonResponse deleteUser(String appId, String userEmail) {
		try {
			AdaptorDetails adaptorDetails = adaptorDetailsRepository.findByApplicationId(appId);
			if (adaptorDetails == null) {
				return new CommonResponse(HttpStatus.NOT_FOUND,
						new Response("Adaptor details not found while deleting the user", "Adaptor details not found"),
						"Adaptor details not found for the particular application");
			}
			HttpHeaders headers = new HttpHeaders();
			headers.set("dd-api-key", adaptorDetails.getApiKey());
			headers.set("dd-application-key", adaptorDetails.getApiToken());
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
			HttpEntity<String> requestEntity = new HttpEntity<>(headers);
			String userId = getUserIDByEmail(userEmail, appId);
			restTemplate.exchange(datadogApiBaseUrl + "/api/v2/users/" + userId,
					HttpMethod.DELETE, requestEntity, String.class);

		} catch (HttpClientErrorException e) {
			return new CommonResponse(e.getStatusCode(), new Response("DeleteUser", e.getResponseBodyAsString()),
					"Error during API call");
		} catch (Exception e) {
			return new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR, new Response("DeleteUser", e.getMessage()),
					"An unexpected error occurred");
		}

		return new CommonResponse(HttpStatus.OK, new Response("User deleted", "User deleted successfully"),
				"User deleted successfully");
	}

	private String getUserIDByEmail(String userEmail, String appId) throws DataValidationException, IOException {
		AdaptorDetails adaptorDetails = adaptorDetailsRepository.findByApplicationId(appId);
		HttpHeaders headers = new HttpHeaders();
		headers.set("dd-api-key", adaptorDetails.getApiKey());
		headers.set("dd-application-key", adaptorDetails.getApiToken());
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		HttpEntity<String> requestEntity = new HttpEntity<>(headers);
		try {
			ResponseEntity<String> response = restTemplate.exchange(datadogApiBaseUrl + "/api/v2/users", HttpMethod.GET,
					requestEntity, String.class);
			if (response.getStatusCode().is2xxSuccessful()) {
				List<DatadogGetUserResponse> userList = mapToDatadogGetUserResponse(response.getBody());
				Optional<DatadogGetUserResponse> userOptional = userList.stream()
						.filter(user -> userEmail.equalsIgnoreCase(user.getEmail())).findFirst();
				if (userOptional.isPresent()) {
					return userOptional.get().getId();
				} else {
					throw new DataValidationException(HttpStatus.BAD_REQUEST, "User not found with email: " + userEmail,
							null);
				}
			} else {
				throw new DataValidationException(HttpStatus.BAD_REQUEST,
						"Failed to fetch user details from Datadog. Status code: " + response.getStatusCode(), null);
			}
		} catch (HttpClientErrorException.BadRequest ex) {
			return "Bad request: " + ex.getRawStatusCode() + ", " + ex.getResponseBodyAsString();
		} catch (RestClientException ex) {

			return "RestClientException: " + ex.getMessage();
		}

	}

}