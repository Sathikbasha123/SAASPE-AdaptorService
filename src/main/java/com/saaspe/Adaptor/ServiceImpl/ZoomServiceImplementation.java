package com.saaspe.Adaptor.ServiceImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saaspe.Adaptor.Advice.CommonResponse;
import com.saaspe.Adaptor.Advice.DataValidationException;
import com.saaspe.Adaptor.Advice.Response;
import com.saaspe.Adaptor.Constant.Constant;
import com.saaspe.Adaptor.Entity.AdaptorDetails;
import com.saaspe.Adaptor.Model.CreateZoomUserRequest;
import com.saaspe.Adaptor.Model.CreateZoomUserResponse;
import com.saaspe.Adaptor.Model.ZoomErrorResponse;
import com.saaspe.Adaptor.Model.ZoomPlanUsageResponse;
import com.saaspe.Adaptor.Model.ZoomPlanUsageResponse.PlanDetail;
import com.saaspe.Adaptor.Model.ZoomToken;
import com.saaspe.Adaptor.Model.ZoomgetUserlistResponse;
import com.saaspe.Adaptor.Repository.AdaptorDetailsRepository;
import com.saaspe.Adaptor.Service.ZoomService;

@Service
public class ZoomServiceImplementation implements ZoomService {

	@Value("${zoom.api.base-url}")
	private String zoomApiBaseUrl;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private AdaptorDetailsRepository adaptorDetails;
	
	@Override
	public CommonResponse generateAuthUri(String appId, String redirectUri) {
		AdaptorDetails adaptor = adaptorDetails.findByApplicationId(appId);
		String authUri = zoomApiBaseUrl +"/oauth/authorize?response_type=code&client_id="+ adaptor.getClientId()+
				"" + "&redirect_uri=" + redirectUri;
		return new CommonResponse(HttpStatus.OK, new Response("Authorization URL", authUri),
				"Authorization URL generated successfully");
	}
	
	@Override
	public CommonResponse getToken(String appId, String code, Long uniqueId) throws IOException {

		AdaptorDetails adaptor = adaptorDetails.findBySequenceId(uniqueId);

		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add(Constant.CLIENTID, adaptor.getClientId());
		params.add(Constant.CLIENTSECRET, adaptor.getClientSecret());
		params.add("code", code);
		params.add("redirect_uri", adaptor.getRedirectUrl());
		params.add(Constant.GRANT_TYPE, "authorization_code");

		String accesstokenEndpoint = zoomApiBaseUrl + "/oauth/token";
		try {
			ResponseEntity<ZoomToken> response = restTemplate.postForEntity(accesstokenEndpoint, params,
					ZoomToken.class);
			adaptor.setApiToken(response.getBody().getRefresh_token());
			adaptor.setRefreshtokenCreatedOn(new Date());
			adaptorDetails.save(adaptor);
			return new CommonResponse(HttpStatus.OK, new Response("Generate Token", response.getBody()),
					"Access token generated successfully");

		} catch (HttpClientErrorException.BadRequest e) {
			e.printStackTrace();
			String responseBody = e.getResponseBodyAsString();
			ObjectMapper objectMapper = new ObjectMapper();
			ZoomErrorResponse error = objectMapper.readValue(responseBody, ZoomErrorResponse.class);
			return new CommonResponse(e.getStatusCode(), new Response(error.getError(), error.getReason()),
					"Invalid or Expired grant/client details");

		} catch (Exception e) {
			e.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Fetch Generate Token", "Invalid/Expired/Revoked Grant"),
					"Kindly check the grant type/code/client details");
		}
	}

	
	@Override
	public CommonResponse getRefreshToken(String appId) throws IOException {

		AdaptorDetails adaptor = adaptorDetails.findByApplicationId(appId);

		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add(Constant.CLIENTID, adaptor.getClientId());
		params.add(Constant.CLIENTSECRET, adaptor.getClientSecret());
		params.add(Constant.REFRESH_TOKEN, adaptor.getApiToken());
		params.add(Constant.GRANT_TYPE, Constant.REFRESH_TOKEN);

		String refreshtokenEndpoint = zoomApiBaseUrl + "/oauth/token";
		
		try {
			ResponseEntity<ZoomToken> response = restTemplate.postForEntity(refreshtokenEndpoint, params,
					ZoomToken.class);
			adaptor.setApiToken(response.getBody().getRefresh_token());
			adaptor.setRefreshtokenCreatedOn(new Date());
			adaptorDetails.save(adaptor);

			return new CommonResponse(HttpStatus.OK, new Response("Refresh Token", response.getBody()),
					"Refresh token generated successfully");

		} catch (HttpClientErrorException.BadRequest e) {
			e.printStackTrace();
			String responseBody = e.getResponseBodyAsString();
			ObjectMapper objectMapper = new ObjectMapper();
			ZoomErrorResponse error = objectMapper.readValue(responseBody, ZoomErrorResponse.class);
			return new CommonResponse(e.getStatusCode(), new Response(error.getError(), error.getReason()),
					"Invalid or Expired grant/client details");

		} catch (Exception e) {
			e.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Fetch Refresh Token", "Invalid/Expired/Revoked Grant"),
					"Kindly check the grant type/code/client details");
		}
	}
	
	
	public String getRefreshToken1(String appId) {
		AdaptorDetails adaptor = adaptorDetails.findByApplicationId(appId);
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add(Constant.CLIENTID, adaptor.getClientId());
	    params.add(Constant.CLIENTSECRET, adaptor.getClientSecret());
		params.add(Constant.REFRESH_TOKEN, adaptor.getApiToken());
		params.add(Constant.GRANT_TYPE, Constant.REFRESH_TOKEN);

		String refreshtokenEndpoint = zoomApiBaseUrl + "/oauth/token";
		try {
			ResponseEntity<ZoomToken> response = restTemplate.postForEntity(refreshtokenEndpoint, params,
					ZoomToken.class);
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
	

	
	@Override
	public CommonResponse getUserList(String appId) throws IOException, DataValidationException {
	    HttpHeaders headers = new HttpHeaders();
	    String accessToken = getRefreshToken1(appId);
	    headers.set(Constant.AUTHORIZATION, Constant.BEARER + accessToken);

	    try {
	        List<ZoomgetUserlistResponse> activeUsers = fetchUsersWithStatus(headers, "active");
	        List<ZoomgetUserlistResponse> pendingUsers = fetchUsersWithStatus(headers, "pending");
	        List<ZoomgetUserlistResponse> allUsers = new ArrayList<>();
	        allUsers.addAll(activeUsers);
	        allUsers.addAll(pendingUsers);

	        return new CommonResponse(HttpStatus.OK, new Response("Get userList", allUsers),
	                "User List retrieved Successfully");
	        
	    } catch (DataValidationException ex) {
	        ex.printStackTrace();
	        return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Get userList", null),
	                "Failed to retrieve user list");
	    }
	}

	private List<ZoomgetUserlistResponse> fetchUsersWithStatus(HttpHeaders headers, String status)
	        throws DataValidationException {
	    HttpEntity<String> requestEntity = new HttpEntity<>(headers);
	    try {
	        ResponseEntity<String> response = restTemplate.exchange(zoomApiBaseUrl + "/v2/users?status=" + status,
	                HttpMethod.GET, requestEntity, String.class);

	        if (response.getStatusCode().is2xxSuccessful()) {
	            return mapToZoomgetUserlistResponse(response.getBody());
	        } else {
	            throw new DataValidationException(HttpStatus.BAD_REQUEST,
	                    "Failed to fetch users with status '" + status + "'. Status code: " + response.getStatusCode(),
	                    null);
	        }
	    } catch (HttpClientErrorException.BadRequest ex) {
	        ex.printStackTrace();
	        String responseBody = ex.getResponseBodyAsString();
	        throw new DataValidationException(HttpStatus.BAD_REQUEST, "Failed to fetch users with status '" + status + "'",
	                responseBody);
	    } catch (RestClientException ex) {
	        ex.printStackTrace();
	        throw new DataValidationException(HttpStatus.INTERNAL_SERVER_ERROR, Constant.INTERNAL_SERVER_ERROR, null);
	    }
	}


	private List<ZoomgetUserlistResponse> mapToZoomgetUserlistResponse(String jsonResponse)
			throws DataValidationException {
		try {

			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode rootNode = objectMapper.readTree(jsonResponse);
			List<ZoomgetUserlistResponse> userlist = new ArrayList<>();
			for (int i = 0; i < rootNode.get(Constant.USERS).size(); i++) {
				JsonNode userDataNode = rootNode.get(Constant.USERS).get(i);
				ZoomgetUserlistResponse zoomUserResponse = new ZoomgetUserlistResponse();
				zoomUserResponse.setId(userDataNode.path("id").asText());
				zoomUserResponse.setFirst_name(userDataNode.path("first_name").asText());
				zoomUserResponse.setLast_name(userDataNode.path("last_name").asText());
				zoomUserResponse.setEmail(userDataNode.path("email").asText());
				zoomUserResponse.setDept(userDataNode.path("Dept").asText());
				zoomUserResponse.setDisplay_name(userDataNode.path("display_name").asText());
				zoomUserResponse.setLanguage(userDataNode.path("language").asText());
				zoomUserResponse.setLast_login_time(userDataNode.path("last_login_time").asText());
				zoomUserResponse.setUser_created_at(userDataNode.path("user_created_at").asText());
				zoomUserResponse.setType(userDataNode.path("Type").asInt());
				zoomUserResponse.setStatus(userDataNode.path("status").asText());
				zoomUserResponse.setRole_id(userDataNode.path("role_id").asInt());
				zoomUserResponse.setCreated_at(userDataNode.path("created_at").asText());
				userlist.add(zoomUserResponse);
			}
			return userlist;
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new DataValidationException(HttpStatus.BAD_REQUEST, "Failed to map JSON response", null);
		}

	}
	

	@Override
	public CreateZoomUserResponse createUser(CreateZoomUserRequest createZoomUserRequest, String appId) throws DataValidationException, IOException {
	    
	    CommonResponse licenseResponse = getLicenseCount(appId);
	    ZoomPlanUsageResponse zoomPlanUsageResponse = (ZoomPlanUsageResponse) licenseResponse.getResponse().getData();
	    PlanDetail planDetail = zoomPlanUsageResponse.getPlanBase(); 

	    if (planDetail.getUsage() < planDetail.getHosts()) {
	        String createUserUrl = zoomApiBaseUrl + "/v2/users";
	        HttpHeaders headers = new HttpHeaders();
	        headers.setContentType(MediaType.APPLICATION_JSON);
	        String accessToken = getRefreshToken1(appId);
	        headers.set(Constant.AUTHORIZATION, Constant.BEARER + accessToken);
	        HttpEntity<CreateZoomUserRequest> requestEntity = new HttpEntity<>(createZoomUserRequest, headers);
	        
	        try {
	            return restTemplate.postForObject(createUserUrl, requestEntity, CreateZoomUserResponse.class);
	        } catch (HttpClientErrorException e) {
	            if (e.getStatusCode().equals(HttpStatus.BAD_REQUEST)) {
	                throw new DataValidationException(HttpStatus.BAD_REQUEST, "There is no license available to assign for users", null);
	            } else {
	                throw e;
	            }
	        }
	    } else {
	        throw new DataValidationException(HttpStatus.BAD_REQUEST, "There is no license available to assign for users", null);
	    }	 
	}



	@Override
	public CommonResponse deleteUserInZoom(String appId, String userEmail)
			throws JsonProcessingException, DataValidationException {
		HttpHeaders headers = new HttpHeaders();
		String accessToken = getRefreshToken1(appId);
		headers.set(Constant.AUTHORIZATION, Constant.BEARER + accessToken);
		HttpEntity<String> requestEntity = new HttpEntity<>(headers);
		ResponseEntity<Object> response = restTemplate.exchange(
				zoomApiBaseUrl +"/v2/users/"+ userEmail, HttpMethod.DELETE, requestEntity,
				Object.class);

		if (response.getStatusCode().is2xxSuccessful()) {
			return new CommonResponse(HttpStatus.OK, new Response("User deleted successfully", null),
					"User deleted successfully");
		} else {
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Failed to delete user", null),
					"Failed to delete user. Status code: " + response.getStatusCode());
		}
	}
	
	@Override
	public CommonResponse getLicenseCount(String appId) throws IOException, DataValidationException {
	    HttpHeaders headers = new HttpHeaders();
	    headers.setContentType(MediaType.APPLICATION_JSON);
	    String accessToken = getRefreshToken1(appId);
	    headers.set(Constant.AUTHORIZATION, Constant.BEARER + accessToken);
	    HttpEntity<String> requestEntity = new HttpEntity<>(headers);
	    
	    String licenseCountUrl = zoomApiBaseUrl + "/v2/accounts/me/plans/usage";
	  
	    try {
	        ResponseEntity<ZoomPlanUsageResponse> response = restTemplate.exchange(licenseCountUrl, HttpMethod.GET, requestEntity,
	        		ZoomPlanUsageResponse.class);
	        if (response.getStatusCode().is2xxSuccessful()) {
	            Object licenseCountDetails = response.getBody();
	            return new CommonResponse(HttpStatus.OK, new Response("License count details", licenseCountDetails),
	                    "License count fetched successfully");
	        } else {
	            throw new DataValidationException(HttpStatus.BAD_REQUEST,
	                    "Failed to fetch license count from zoom. Status code: " + response.getStatusCode(),null);
	        }
	    } catch (HttpClientErrorException.BadRequest ex) {
	        ex.printStackTrace();
	        String responseBody = ex.getResponseBodyAsString();
	        throw new DataValidationException(HttpStatus.BAD_REQUEST, "Failed to fetch license count",
	                responseBody);
	    } catch (RestClientException ex) {
	        ex.printStackTrace();
	        throw new DataValidationException(HttpStatus.INTERNAL_SERVER_ERROR, Constant.INTERNAL_SERVER_ERROR, null);
	    }
	}

}
