package com.saaspe.Adaptor.ServiceImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import com.saaspe.Adaptor.Advice.CommonResponse;
import com.saaspe.Adaptor.Advice.DataValidationException;
import com.saaspe.Adaptor.Advice.Response;
import com.saaspe.Adaptor.Constant.Constant;
import com.saaspe.Adaptor.Entity.AdaptorDetails;
import com.saaspe.Adaptor.Model.AccessToken;
import com.saaspe.Adaptor.Model.CreateHubSpotUserRequest;
import com.saaspe.Adaptor.Model.CreateHubSpotUserResponse;
import com.saaspe.Adaptor.Model.HubSpotErrorResponse;
import com.saaspe.Adaptor.Model.HubSpotGetUserlistResponse;
import com.saaspe.Adaptor.Model.HubSpotSubscriptionRequest;
import com.saaspe.Adaptor.Model.Token;
import com.saaspe.Adaptor.Repository.AdaptorDetailsRepository;
import com.saaspe.Adaptor.Service.HubSpotService;

@Service
public class HubSpotServImpl implements HubSpotService {
	@Value("${hubspot.api.base-url}")
	private String hubspotApiBaseUrl;
	@Autowired
	private RestTemplate restTemplate;
	
	@Autowired
	private AdaptorDetailsRepository adaptorDetails;

	@Override
	public CommonResponse generateAuthUri(String appId) {
		AdaptorDetails adaptor = adaptorDetails.findByApplicationId(appId);
		String authUri = "https://app.hubspot.com/oauth/authorize?"+Constant.CLIENT_ID + adaptor.getClientId() + ""
				+"&scope=crm.lists.read+crm.lists.write+settings.users.read+settings.users.write+account-info.security.read+oauth+communication_preferences.read_write+communication_preferences.read&redirect_uri="+ adaptor.getRedirectUrl() + "";
		return new CommonResponse(HttpStatus.OK, new Response("Authorization URL", authUri),
				"Authorization URL generated successfully");
	}

	

	@Override
	public CommonResponse getToken(String appId) throws IOException {
		AdaptorDetails adaptor = adaptorDetails.findByApplicationId(appId);
		
		String accessTokenURL =  Constant.HUBSPOT_URL + Constant.CLIENT_ID + adaptor.getClientId() + "&"
				+ Constant.CLIENT_SECRET + adaptor.getClientSecret() + "" + "&code=" + adaptor.getApiKey()
				+ "&grant_type=authorization_code&redirect_uri=" + adaptor.getRedirectUrl();
	    try {
			ResponseEntity<Token> response = restTemplate.postForEntity(accessTokenURL, null, Token.class);
			adaptor.setApiToken(response.getBody().getRefresh_token());
			adaptorDetails.save(adaptor);
			return new CommonResponse(HttpStatus.OK, new Response("Fetch Access Token", response.getBody()),
					"Access token generated successfully");
		} catch (HttpClientErrorException.BadRequest e) {
			e.printStackTrace();
			String responseBody = e.getResponseBodyAsString();
			ObjectMapper objectMapper = new ObjectMapper();
			HubSpotErrorResponse error = objectMapper.readValue(responseBody, HubSpotErrorResponse.class);
			return new CommonResponse(e.getStatusCode(), new Response(error.getError(), error.getError_description()),
					"Invalid or Expired grant/client details");
		} catch (Exception e) {
			e.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Fetch Access Token", null),
					"Exception in Access Token API");
		}
	}
	
	
	@Override
	public CommonResponse getRefreshToken(String appId) throws IOException {
		
		AdaptorDetails adaptor = adaptorDetails.findByApplicationId(appId);
		
		String accessTokenURL = Constant.HUBSPOT_URL + Constant.CLIENT_ID + adaptor.getClientId() + "&"
				+ Constant.CLIENT_SECRET + adaptor.getClientSecret() + "" + "&refresh_token=" + adaptor.getApiToken() + ""
				+ "&grant_type=refresh_token";

	    try {
			ResponseEntity<Token> response = restTemplate.postForEntity(accessTokenURL, null, Token.class);
			adaptor.setApiToken(response.getBody().getRefresh_token());
			adaptorDetails.save(adaptor);
			return new CommonResponse(HttpStatus.OK, new Response("Generate Token", response.getBody()),
					"Access token generated successfully");
		} catch (HttpClientErrorException.BadRequest e) {
			String responseBody = e.getResponseBodyAsString();
			ObjectMapper objectMapper = new ObjectMapper();
			HubSpotErrorResponse error = objectMapper.readValue(responseBody, HubSpotErrorResponse.class);
			
			return new CommonResponse(e.getStatusCode(), new Response(error.getError(), error.getError_description()),
					"Invalid or Expired grant/client details");
		} catch (Exception e) {
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Fetch Generate Token", "Invalid/Expired/Revoked Grant"),
					"Kindly check the grant type/code/client details");
		}
	}

	
	@Override
	public CommonResponse getAllUsersFromHubSpot(String appId) throws IOException, DataValidationException {
	    ObjectMapper mapper = new ObjectMapper();
	    HttpHeaders headers = new HttpHeaders();
	    AdaptorDetails appdetails = adaptorDetails.findByApplicationId(appId);
	    headers.set(Constant.AUTHORIZATION, Constant.BEARER + appdetails.getApiToken());
	    HttpEntity<String> requestEntity = new HttpEntity<>(headers);
	    try {
	        ResponseEntity<String> response = restTemplate.exchange(
	                hubspotApiBaseUrl + Constant.HUBSPOT_USER_URL,
	                HttpMethod.GET, requestEntity, String.class);
          
	        if (response.getStatusCode().is2xxSuccessful()) {
	        	List<HubSpotGetUserlistResponse> hubSpotUserResponse= mapToHubSpotGetUserlistResponse  (response.getBody());
	    		return new CommonResponse(HttpStatus.OK,new Response("",hubSpotUserResponse),"");

	        } else {
	            throw new DataValidationException(HttpStatus.BAD_REQUEST,
	                    "Failed to fetch profile details from HubSpot. Status code: " + response.getStatusCode(),null);
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
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Get all user details Response", errorResponse),
					" Get all user details Failed");
		} catch (RestClientException ex) {
			ex.printStackTrace();
			return new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					new Response("Get all user details Response", null), Constant.INTERNAL_SERVER_ERROR);
		}
	}
			
	private List<HubSpotGetUserlistResponse> mapToHubSpotGetUserlistResponse (String jsonResponse) throws DataValidationException {
		try {

			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode rootNode = objectMapper.readTree(jsonResponse);
			List<HubSpotGetUserlistResponse> userlist = new ArrayList<>();
			for (int i = 0; i < rootNode.get(Constant.RESULTS).size(); i++) {
				JsonNode userDataNode = rootNode.get(Constant.RESULTS).get(i);
				HubSpotGetUserlistResponse hubSpotUserResponse = new  HubSpotGetUserlistResponse();
				hubSpotUserResponse.setId(userDataNode.path("id").asText());
				hubSpotUserResponse.setEmail(userDataNode.path("email").asText());
				hubSpotUserResponse.setSuperAdmin(userDataNode.path("superAdmin").asBoolean());
				hubSpotUserResponse.setPrimaryTeamId(userDataNode.path("primaryTeamId").asText());
				userlist.add(hubSpotUserResponse);
			}
			return userlist;
		}

		catch (Exception e) {
			e.printStackTrace();
			throw new DataValidationException(HttpStatus.BAD_REQUEST,"Failed to map JSON response", null);
		}

	}
	
	@Override
	public CreateHubSpotUserResponse createUser(CreateHubSpotUserRequest hubSpotUserRequest, String appId) {
		String hubSpotApiUrl = "https://api.hubapi.com/settings/v3/users";
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		AdaptorDetails appdetails = adaptorDetails.findByApplicationId(appId);
		headers.set(Constant.AUTHORIZATION, Constant.BEARER + appdetails.getApiToken());
		HttpEntity<CreateHubSpotUserRequest> requestEntity = new HttpEntity<>(hubSpotUserRequest, headers);
		return restTemplate.postForObject(hubSpotApiUrl, requestEntity, CreateHubSpotUserResponse.class);
	}
	
	
	@Override
	public CommonResponse deleteUserInHubSpot(String appId,String userEmail) throws DataValidationException {
	    HttpHeaders headers = new HttpHeaders();
	    AdaptorDetails appdetails = adaptorDetails.findByApplicationId(appId);
	    headers.set(Constant.AUTHORIZATION, Constant.BEARER + appdetails.getApiToken());
	    HttpEntity<String> requestEntity = new HttpEntity<>(headers);
	    String userId = getUserIDByEmail(userEmail, appdetails.getApiToken()); 
	    ResponseEntity<Object> response = restTemplate.exchange(
	            hubspotApiBaseUrl + "/settings/v3/users/" + userId,
	            HttpMethod.DELETE, requestEntity, Object.class);

	    if (response.getStatusCode().is2xxSuccessful()) {
	        return new CommonResponse(HttpStatus.OK, new Response("User deleted successfully", null),
	                "User deleted successfully");
	    } else {
	        return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Failed to delete user", null),
	                "Failed to delete user. Status code: " + response.getStatusCode());
	    }
	}

	private String getUserIDByEmail(String userEmail,String accessToken) throws DataValidationException {
		
	    HttpHeaders headers = new HttpHeaders();
	    headers.set(Constant.AUTHORIZATION, Constant.BEARER + accessToken);
	    HttpEntity<String> requestEntity = new HttpEntity<>(headers);

	    try {
	        ResponseEntity<String> response = restTemplate.exchange(
	                hubspotApiBaseUrl + Constant.HUBSPOT_USER_URL,
	                HttpMethod.GET, requestEntity, String.class);

	        if (response.getStatusCode().is2xxSuccessful()) {
	            List<HubSpotGetUserlistResponse> userList = mapToHubSpotGetUserlistResponse(response.getBody());
	            Optional<HubSpotGetUserlistResponse> userOptional = userList.stream()
	                    .filter(user -> userEmail.equalsIgnoreCase(user.getEmail()))
	                    .findFirst();

	            if (userOptional.isPresent()) {
	                return userOptional.get().getId();
	            } else {
	                throw new DataValidationException(HttpStatus.BAD_REQUEST,"User not found with email: " + userEmail,null);
	            }
	        } else {
	            throw new DataValidationException(HttpStatus.BAD_REQUEST,
	                    "Failed to fetch user details from HubSpot. Status code: " + response.getStatusCode(),null);
	        }
	    }catch (HttpClientErrorException.BadRequest ex) {
	          
	            return "Bad request: " + ex.getRawStatusCode() + ", " + ex.getResponseBodyAsString();
	        } catch (RestClientException ex) {
	           
	            return "RestClientException: " + ex.getMessage();
	        }
	    
	
	}



	


	@Override
	public CommonResponse getcountUsersFromHubSpot(String appId) throws IOException, DataValidationException {
		ObjectMapper mapper = new ObjectMapper();
		HttpHeaders headers = new HttpHeaders();
		AdaptorDetails appdetails = adaptorDetails.findByApplicationId(appId);
		headers.set(Constant.AUTHORIZATION, Constant.BEARER + appdetails.getApiToken());
		HttpEntity<String> requestEntity = new HttpEntity<>(headers);
		
		try {
		ResponseEntity<Object> response = restTemplate.exchange(hubspotApiBaseUrl + Constant.HUBSPOT_USER_URL,
				HttpMethod.GET, requestEntity, Object.class);
		if (response.getStatusCode().is2xxSuccessful()) {
			Object profileDetails = response.getBody();
			if (profileDetails instanceof Map) {
				Map<?, ?> resultMap = (Map<?, ?>) profileDetails;
				if (resultMap.containsKey(Constant.RESULTS) && resultMap.get(Constant.RESULTS) instanceof List) {
					List<?> userList = (List<?>) resultMap.get(Constant.RESULTS);
					int count = userList.size();
					return new CommonResponse(HttpStatus.OK, new Response("License Count", count),
							"License Count fetched successfully");
				} else {
					throw new DataValidationException(HttpStatus.BAD_REQUEST,"Unexpected response format from HubSpot API",null);
				}
			} else {
				throw new DataValidationException(HttpStatus.BAD_REQUEST,"Unexpected response format from HubSpot API",null);
			}
		} else {
			throw new DataValidationException(HttpStatus.BAD_REQUEST,
					"Failed to fetch user count details from HubSpot. Status code: " + response.getStatusCode(),null);
		}
	} catch (HttpClientErrorException.BadRequest ex) {
        String responseBody = ex.getResponseBodyAsString();
        Object errorResponse = null;
        try {
            errorResponse = mapper.readValue(responseBody, Object.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.ACCESS_TOKEN_RESPONSE, errorResponse),
                Constant.INVALID_GRANT);
    } catch (RestClientException ex) {
        ex.printStackTrace();
        return new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                new Response(Constant.ACCESS_TOKEN_RESPONSE, null), Constant.INTERNAL_SERVER_ERROR);
    }
	}

	@Override
	public CommonResponse getLoginActivityHubSpot(String appId, String userEmail) throws IOException, DataValidationException {
	    ObjectMapper mapper = new ObjectMapper();
	    HttpHeaders headers = new HttpHeaders();
	    AdaptorDetails appdetails = adaptorDetails.findByApplicationId(appId);
	    headers.set(Constant.AUTHORIZATION, Constant.BEARER + appdetails.getApiToken());
	    HttpEntity<String> requestEntity = new HttpEntity<>(headers);
	    String userId = getUserIDByEmail(userEmail, appdetails.getApiToken()); 
	 
	    String auditLogsEndpoint = hubspotApiBaseUrl + "/account-info/v3/activity/login?limit=10&userId=" + userId;
	  
	    try {
	        ResponseEntity<Object> response = restTemplate.exchange(auditLogsEndpoint, HttpMethod.GET, requestEntity,
	                Object.class);
            
	        if (response.getStatusCode().is2xxSuccessful()) {
	            Object auditLogsDetails = response.getBody();
	            return new CommonResponse(HttpStatus.OK, new Response("Audit Logs details", auditLogsDetails),
	                    "Audit logs fetched successfully");
	        } else {
	            throw new DataValidationException(HttpStatus.BAD_REQUEST,
	                    "Failed to fetch audit logs from HubSpot. Status code: " + response.getStatusCode(),null);
	        }
	    } catch (HttpClientErrorException.BadRequest ex) {
	        String responseBody = ex.getResponseBodyAsString();
	        Object errorResponse = mapper.readValue(responseBody, Object.class);
	        return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.ACCESS_TOKEN_RESPONSE, errorResponse),
	        		Constant.INVALID_GRANT);
	    } catch (RestClientException ex) {
	        ex.printStackTrace();
	        return new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR,
	                new Response(Constant.ACCESS_TOKEN_RESPONSE, null), Constant.INTERNAL_SERVER_ERROR);
	    }
	}


	@Override
	public CommonResponse getSecurityActivityHubSpot(String appId, String userEmail) throws IOException, DataValidationException {
		ObjectMapper mapper = new ObjectMapper();
		HttpHeaders headers = new HttpHeaders();
		AdaptorDetails appdetails = adaptorDetails.findByApplicationId(appId);
		 headers.set(Constant.AUTHORIZATION, Constant.BEARER + appdetails.getApiToken());
		HttpEntity<String> requestEntity = new HttpEntity<>(headers);
		String userId = getUserIDByEmail(userEmail, appdetails.getApiToken()); 
		String auditLogsEndpoint = hubspotApiBaseUrl + "/account-info/v3/activity/security?limit=10&userId="
				+ userId;
		try {
		ResponseEntity<Object> response = restTemplate.exchange(auditLogsEndpoint, HttpMethod.GET, requestEntity,
				Object.class);

		if (response.getStatusCode().is2xxSuccessful()) {
			Object auditLogsDetails = response.getBody();
			return new CommonResponse(HttpStatus.OK, new Response("Audit Logs details", auditLogsDetails),
					"Audit logs fetched successfully");
		} else {
			throw new DataValidationException(HttpStatus.BAD_REQUEST,
					"Failed to fetch audit logs from HubSpot. Status code: " + response.getStatusCode(),null);
		}
	}catch (HttpClientErrorException.BadRequest ex) {
        String responseBody = ex.getResponseBodyAsString();
        Object errorResponse = null;
        try {
            errorResponse = mapper.readValue(responseBody, Object.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.ACCESS_TOKEN_RESPONSE, errorResponse),
        		Constant.INVALID_GRANT);
    } catch (RestClientException ex) {
        ex.printStackTrace();
        return new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                new Response(Constant.ACCESS_TOKEN_RESPONSE, null), Constant.INTERNAL_SERVER_ERROR);
    }
	}


	@Override
	public CommonResponse getAccountInfoHubSpot(String appId) throws IOException, DataValidationException {
		ObjectMapper mapper = new ObjectMapper();
		HttpHeaders headers = new HttpHeaders();
		AdaptorDetails appdetails = adaptorDetails.findByApplicationId(appId);
		headers.set(Constant.AUTHORIZATION, Constant.BEARER + appdetails.getApiToken());
		HttpEntity<String> requestEntity = new HttpEntity<>(headers);
		String auditLogsEndpoint = hubspotApiBaseUrl + "/account-info/v3/details";
		try {
		ResponseEntity<Object> response = restTemplate.exchange(auditLogsEndpoint, HttpMethod.GET, requestEntity,
				Object.class);

		if (response.getStatusCode().is2xxSuccessful()) {
			Object auditLogsDetails = response.getBody();
			return new CommonResponse(HttpStatus.OK, new Response("Account details", auditLogsDetails),
					"Account Details fetched successfully");
		} else {
			throw new DataValidationException(HttpStatus.BAD_REQUEST,
					"Failed to fetch Account info audit logs from HubSpot. Status code: " + response.getStatusCode(),null);
		}
	}catch (HttpClientErrorException.BadRequest ex) {
        String responseBody = ex.getResponseBodyAsString();
        Object errorResponse = null;
        try {
            errorResponse = mapper.readValue(responseBody, Object.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.ACCESS_TOKEN_RESPONSE, errorResponse),
        		Constant.INVALID_GRANT);
    } catch (RestClientException ex) {
        ex.printStackTrace();
        return new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                new Response(Constant.ACCESS_TOKEN_RESPONSE, null), Constant.INTERNAL_SERVER_ERROR);
    }
	}

	@Override
	public CommonResponse getUserSubDetailsHubSpot(String appId, String emailId) throws IOException, DataValidationException {
		ObjectMapper mapper = new ObjectMapper();
		HttpHeaders headers = new HttpHeaders();
		AdaptorDetails appdetails = adaptorDetails.findByApplicationId(appId);
		headers.set(Constant.AUTHORIZATION, Constant.BEARER + appdetails.getApiToken());
		HttpEntity<String> requestEntity = new HttpEntity<>(headers);
		String auditLogsEndpoint = hubspotApiBaseUrl + "/communication-preferences/v3/status/email/" + emailId;
		try {
		ResponseEntity<Object> response = restTemplate.exchange(auditLogsEndpoint, HttpMethod.GET, requestEntity,
				Object.class);

		if (response.getStatusCode().is2xxSuccessful()) {
			Object auditLogsDetails = response.getBody();
			return new CommonResponse(HttpStatus.OK, new Response("User Subscription details", auditLogsDetails),
					"User Subscriptions details fetched successfully");
		} else {
			throw new DataValidationException(HttpStatus.BAD_REQUEST,
					"Failed to fetch User Subscription details from HubSpot. Status code: " + response.getStatusCode(),null);
		}
	}catch (HttpClientErrorException.BadRequest ex) {
        String responseBody = ex.getResponseBodyAsString();
        Object errorResponse = null;
        try {
            errorResponse = mapper.readValue(responseBody, Object.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.ACCESS_TOKEN_RESPONSE, errorResponse),
        		Constant.INVALID_GRANT);
    } catch (RestClientException ex) {
        ex.printStackTrace();
        return new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                new Response(Constant.ACCESS_TOKEN_RESPONSE, null), Constant.INTERNAL_SERVER_ERROR);
    }
	}

	@Override
	public CommonResponse getAllSubDefinitionHubSpot(String appId) throws IOException, DataValidationException {
		ObjectMapper mapper = new ObjectMapper();
		HttpHeaders headers = new HttpHeaders();
		AdaptorDetails appdetails = adaptorDetails.findByApplicationId(appId);
		headers.set(Constant.AUTHORIZATION, Constant.BEARER + appdetails.getApiToken());
		HttpEntity<String> requestEntity = new HttpEntity<>(headers);
		String auditLogsEndpoint = hubspotApiBaseUrl + "/communication-preferences/v3/definitions";
		try {
		ResponseEntity<Object> response = restTemplate.exchange(auditLogsEndpoint, HttpMethod.GET, requestEntity,
				Object.class);

		if (response.getStatusCode().is2xxSuccessful()) {
			Object auditLogsDetails = response.getBody();
			return new CommonResponse(HttpStatus.OK, new Response("Subscription Definitions", auditLogsDetails),
					"Subscription Definitions fetched successfully");
		} else {
			throw new DataValidationException(HttpStatus.BAD_REQUEST,
					"Failed to fetch Subscription definition from HubSpot. Status code: " + response.getStatusCode(),null);
		}
	}catch (HttpClientErrorException.BadRequest ex) {
        String responseBody = ex.getResponseBodyAsString();
        Object errorResponse = null;
        try {
            errorResponse = mapper.readValue(responseBody, Object.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.ACCESS_TOKEN_RESPONSE, errorResponse),
        		Constant.INVALID_GRANT);
    } catch (RestClientException ex) {
        ex.printStackTrace();
        return new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                new Response(Constant.ACCESS_TOKEN_RESPONSE, null), Constant.INTERNAL_SERVER_ERROR);
    }
	}

	@Override
	public CommonResponse createSubscription(HubSpotSubscriptionRequest hubSpotSubscriptionRequest,
			String appId) {
	    ObjectMapper mapper = new ObjectMapper();
	    String hubSpotApiUrl = "https://api.hubapi.com/communication-preferences/v3/subscribe";
	    HttpHeaders headers = new HttpHeaders();
	    AdaptorDetails appdetails = adaptorDetails.findByApplicationId(appId);
		headers.set(Constant.AUTHORIZATION, Constant.BEARER + appdetails.getApiToken());
	    headers.setContentType(MediaType.APPLICATION_JSON);
	    HttpEntity<HubSpotSubscriptionRequest> requestEntity = new HttpEntity<>(hubSpotSubscriptionRequest, headers);
	    try {
	        return restTemplate.postForObject(hubSpotApiUrl, requestEntity, CommonResponse.class);
	    } catch (HttpClientErrorException.BadRequest ex) {
	        String responseBody = ex.getResponseBodyAsString();
	        Object errorResponse = null;
	        try {
	            errorResponse = mapper.readValue(responseBody, Object.class);
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	        return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.ACCESS_TOKEN_RESPONSE, errorResponse),
	        		Constant.INVALID_GRANT);
	    } catch (RestClientException ex) {
	        ex.printStackTrace();
	        return new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR,
	                new Response(Constant.ACCESS_TOKEN_RESPONSE, null), Constant.INTERNAL_SERVER_ERROR);
	    }
	}

	@Override
	public CommonResponse createUnSubscription(HubSpotSubscriptionRequest hubSpotSubscriptionRequest,
			String appId) {
	    ObjectMapper mapper = new ObjectMapper();
	    String hubSpotApiUrl = "https://api.hubapi.com/communication-preferences/v3/unsubscribe";
	    HttpHeaders headers = new HttpHeaders();
	    headers.setContentType(MediaType.APPLICATION_JSON);
	    AdaptorDetails appdetails = adaptorDetails.findByApplicationId(appId);
	    headers.set(Constant.AUTHORIZATION, Constant.BEARER + appdetails.getApiToken());
	    HttpEntity<HubSpotSubscriptionRequest> requestEntity = new HttpEntity<>(hubSpotSubscriptionRequest, headers);
	    try {
	        return restTemplate.postForObject(hubSpotApiUrl, requestEntity, CommonResponse.class);
	    } catch (HttpClientErrorException.BadRequest ex) {
	        String responseBody = ex.getResponseBodyAsString();
	        Object errorResponse = null;
	        try {
	            errorResponse = mapper.readValue(responseBody, Object.class);
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	        return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.ACCESS_TOKEN_RESPONSE, errorResponse),
	        		Constant.INVALID_GRANT);
	    } catch (RestClientException ex) {
	        ex.printStackTrace();
	        return new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR,
	                new Response(Constant.ACCESS_TOKEN_RESPONSE, null), Constant.INTERNAL_SERVER_ERROR);
	    }
	}
	
	
	public String getRefreshToken1(String appId) {
		AdaptorDetails adaptor = adaptorDetails.findByApplicationId(appId);
		String accessTokenURL = Constant.HUBSPOT_URL + Constant.CLIENT_ID + adaptor.getClientId() + "&"
				+ Constant.CLIENT_SECRET + adaptor.getClientSecret() + "" + "&refresh_token=" + adaptor.getApiToken() + ""
				+ "&grant_type=refresh_token";
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