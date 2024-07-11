package com.saaspe.Adaptor.ServiceImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.intuit.ipp.exception.BadRequestException;
import com.saaspe.Adaptor.Advice.CommonResponse;
import com.saaspe.Adaptor.Advice.DataValidationException;
import com.saaspe.Adaptor.Advice.Response;
import com.saaspe.Adaptor.Constant.Constant;
import com.saaspe.Adaptor.Entity.AdaptorDetails;
import com.saaspe.Adaptor.Model.License;
import com.saaspe.Adaptor.Model.Microsoft365CreateUserRequest;
import com.saaspe.Adaptor.Model.Microsoft365ErrorResponse;
import com.saaspe.Adaptor.Model.Microsoft365Token;
import com.saaspe.Adaptor.Model.Microsoft365getUserlistResponse;
import com.saaspe.Adaptor.Repository.AdaptorDetailsRepository;
import com.saaspe.Adaptor.Service.Microsoft365Service;

@Service
public class Microsoft365ServImpl implements Microsoft365Service {

	@Value("${microsoft365.api.base-url}")
	private String microsoftApiBaseUrl;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private AdaptorDetailsRepository adaptorDetails;

	@Override
	public CommonResponse generateAuthUri(String appId, String redirectUri) {
		AdaptorDetails adaptor = adaptorDetails.findByApplicationId(appId);
		String authUri = Constant.MICROSOFT_URL + adaptor.getTenantid() + "" + "/oauth2/v2.0/authorize?" + "client_id="
				+ adaptor.getClientId() + "" + "&response_type=code&redirect_uri=" + redirectUri
				+ "&scope=offline_access%20user.read%20mail.read&state=12345";
		return new CommonResponse(HttpStatus.OK, new Response("Authorization URL", authUri),
				"Authorization URL generated successfully");
	}

	@Override
	public CommonResponse getToken(String appId, String code, Long uniqueId) throws IOException {

		AdaptorDetails adaptor = adaptorDetails.findBySequenceId(uniqueId);

		MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
		formData.add(Constant.CLIENTID, adaptor.getClientId());
		formData.add(Constant.CLIENTSECRET, adaptor.getClientSecret());
		formData.add("code", code);
		formData.add("redirect_uri", adaptor.getRedirectUrl());
		formData.add(Constant.GRANT_TYPE, "authorization_code");
		formData.add(Constant.SCOPE, Constant.MICROSOFT_SCOPE);

		String accesstokenEndpoint = Constant.MICROSOFT_URL + adaptor.getTenantid() + Constant.MICROSOFT_TOKEN_URL;
		try {
			ResponseEntity<Microsoft365Token> response = restTemplate.postForEntity(accesstokenEndpoint, formData,
					Microsoft365Token.class);
			adaptor.setApiToken(response.getBody().getRefresh_token());
			adaptor.setRefreshtokenCreatedOn(new Date());
			adaptorDetails.save(adaptor);

			return new CommonResponse(HttpStatus.OK, new Response("Generate Token", response.getBody()),
					"Access token generated successfully");

		} catch (HttpClientErrorException.BadRequest e) {
			e.printStackTrace();
			String responseBody = e.getResponseBodyAsString();
			ObjectMapper objectMapper = new ObjectMapper();
			Microsoft365ErrorResponse error = objectMapper.readValue(responseBody, Microsoft365ErrorResponse.class);

			return new CommonResponse(e.getStatusCode(), new Response(error.getError(), error.getError_description()),
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

		MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
		formData.add(Constant.CLIENTID, adaptor.getClientId());
		formData.add(Constant.CLIENTSECRET, adaptor.getClientSecret());
		formData.add(Constant.REFRESH_TOKEN, adaptor.getApiToken());
		formData.add(Constant.SCOPE, Constant.MICROSOFT_SCOPE);
		formData.add(Constant.GRANT_TYPE, Constant.REFRESH_TOKEN);

		String refreshtokenEndpoint = Constant.MICROSOFT_URL + adaptor.getTenantid() + Constant.MICROSOFT_TOKEN_URL;

		try {
			ResponseEntity<Microsoft365Token> response = restTemplate.postForEntity(refreshtokenEndpoint, formData,
					Microsoft365Token.class);
			adaptor.setApiToken(response.getBody().getRefresh_token());
			adaptor.setRefreshtokenCreatedOn(new Date());
			adaptorDetails.save(adaptor);

			return new CommonResponse(HttpStatus.OK, new Response("Refresh Token", response.getBody()),
					"Refresh token generated successfully");

		} catch (HttpClientErrorException.BadRequest e) {
			e.printStackTrace();
			String responseBody = e.getResponseBodyAsString();
			ObjectMapper objectMapper = new ObjectMapper();
			Microsoft365ErrorResponse error = objectMapper.readValue(responseBody, Microsoft365ErrorResponse.class);

			return new CommonResponse(e.getStatusCode(), new Response(error.getError(), error.getError_description()),
					"Invalid or Expired grant/client details");

		} catch (Exception e) {
			e.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Fetch Refresh Token", "Invalid/Expired/Revoked Grant"),
					"Kindly check the grant type/code/client details");
		}
	}

	@Override
	public CommonResponse getUserList(String appId) throws IOException, DataValidationException {
		ObjectMapper mapper = new ObjectMapper();
		HttpHeaders headers = new HttpHeaders();
		String accessToken = getRefreshToken1(appId);
		headers.set(Constant.AUTHORIZATION, Constant.BEARER + accessToken);

		HttpEntity<String> requestEntity = new HttpEntity<>(headers);

		try {
			ResponseEntity<String> response = restTemplate.exchange(microsoftApiBaseUrl + "/v1.0/users?$top=999",
					HttpMethod.GET, requestEntity, String.class);

			if (response.getStatusCode().is2xxSuccessful()) {
				List<Microsoft365getUserlistResponse> microsoft365UserResponse = mapToMicrosoft365getUserlistResponse(
						response.getBody());
				return new CommonResponse(HttpStatus.OK, new Response("Get userList", microsoft365UserResponse),
						"User List retrived Successfully");
			} else {
				throw new DataValidationException(HttpStatus.BAD_REQUEST,
						"Failed to fetch profile details from Microsoft365. Status code: " + response.getStatusCode(),
						null);
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
					new Response("Get all user details Response", null), Constant.INTERNAL_SERVER_ERROR);
		    }
	}

	private List<Microsoft365getUserlistResponse> mapToMicrosoft365getUserlistResponse(String jsonResponse)
			throws DataValidationException {
		try {

			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode rootNode = objectMapper.readTree(jsonResponse);
			List<Microsoft365getUserlistResponse> userlist = new ArrayList<>();
			for (int i = 0; i < rootNode.get(Constant.VALUE).size(); i++) {
				JsonNode userDataNode = rootNode.get(Constant.VALUE).get(i);
				Microsoft365getUserlistResponse microsoft365UserResponse = new Microsoft365getUserlistResponse();
				microsoft365UserResponse.setDisplayName(userDataNode.path("displayName").asText());
				microsoft365UserResponse.setGivenName(userDataNode.path("givenName").asText());
				microsoft365UserResponse.setJobTitle(userDataNode.path("jobTitle").asText());
				microsoft365UserResponse.setMail(userDataNode.path("mail").asText());
				microsoft365UserResponse.setMobilePhone(userDataNode.path("mobilePhone").asText());
				microsoft365UserResponse.setOfficeLocation(userDataNode.path("officeLocation").asText());
				microsoft365UserResponse.setPreferredLanguage(userDataNode.path("preferredLanguage").asText());
				microsoft365UserResponse.setSurname(userDataNode.path("surname").asText());
				microsoft365UserResponse.setUserPrincipalName(userDataNode.path("userPrincipalName").asText());
				microsoft365UserResponse.setId(userDataNode.path("id").asText());
				JsonNode businessPhonesNode = userDataNode.path("businessPhones");
				List<String> businessPhonesList = new ArrayList<>();
				if (businessPhonesNode.isArray()) {
					for (JsonNode phoneNode : businessPhonesNode) {
						businessPhonesList.add(phoneNode.asText());
					}
				}
				microsoft365UserResponse.setBusinessPhones(businessPhonesList);
				userlist.add(microsoft365UserResponse);
			}
			return userlist;
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new DataValidationException(HttpStatus.BAD_REQUEST, "Failed to map JSON response", null);
		}

	}
	
	@Override
	public Microsoft365getUserlistResponse createUser(String userEmail, String appId) throws JsonProcessingException {
	    Microsoft365CreateUserRequest request = microsoft365CreateUserRequest(userEmail); 
	    String createMicrosoftUserUrl = microsoftApiBaseUrl + "/v1.0/users";
	    HttpHeaders headers = new HttpHeaders();
	    headers.setContentType(MediaType.APPLICATION_JSON);
	    String accessToken = getRefreshToken1(appId);
	    headers.set(Constant.AUTHORIZATION, Constant.BEARER + accessToken);
	    HttpEntity<Microsoft365CreateUserRequest> requestEntity = new HttpEntity<>(request, headers);
	    return restTemplate.postForObject(createMicrosoftUserUrl, requestEntity, Microsoft365getUserlistResponse.class);
	}

	private Microsoft365CreateUserRequest microsoft365CreateUserRequest(String userEmail) {
	    Microsoft365CreateUserRequest request = new Microsoft365CreateUserRequest();
	    request.setDisplayName(userEmail.substring(0, userEmail.indexOf('@'))); 
	    request.setMail(userEmail);
	    request.setUserPrincipalName(userEmail);
	    request.setMailNickname(userEmail.substring(0, userEmail.indexOf('@'))); 
	    request.setAccountEnabled(true);
	    Microsoft365CreateUserRequest.PasswordProfile passwordProfile = new Microsoft365CreateUserRequest.PasswordProfile();
	    passwordProfile.setForceChangePasswordNextSignIn(false);
	    passwordProfile.setPassword("abc@123456");
	    request.setPasswordProfile(passwordProfile);

	    return request;
	}


	@Override
	public Microsoft365getUserlistResponse updateUserByEmail(String userEmail,
			Microsoft365getUserlistResponse microsoft365UpdateUserRequest, String appId)
			throws JsonProcessingException, DataValidationException {
		String userId = getUserIDByEmail(userEmail, getRefreshToken1(appId));
		if (userId == null || userId.isEmpty()) {
			throw new DataValidationException(HttpStatus.BAD_REQUEST, Constant.USER_NOT_FOUND + userEmail, null);
		}
		String updateMicrosoftUserUrl = microsoftApiBaseUrl + Constant.MICROSOFT_USER_URL + userId;
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		String accessToken = getRefreshToken1(appId);
		headers.set(Constant.AUTHORIZATION, Constant.BEARER + accessToken);
		Map<String, Object> updateFields = new HashMap<>();
		if (microsoft365UpdateUserRequest.getDisplayName() != null) {
			updateFields.put("displayName", microsoft365UpdateUserRequest.getDisplayName());
		}
		if (microsoft365UpdateUserRequest.getBusinessPhones() != null) {
			updateFields.put("businessPhones", microsoft365UpdateUserRequest.getBusinessPhones());
		}
		if (microsoft365UpdateUserRequest.getGivenName() != null) {
			updateFields.put("givenName", microsoft365UpdateUserRequest.getGivenName());
		}
		if (microsoft365UpdateUserRequest.getJobTitle() != null) {
			updateFields.put("jobTitle", microsoft365UpdateUserRequest.getJobTitle());
		}
		if (microsoft365UpdateUserRequest.getMail() != null) {
			updateFields.put("Mail", microsoft365UpdateUserRequest.getMail());
		}
		if (microsoft365UpdateUserRequest.getMobilePhone() != null) {
			updateFields.put("mobilePhone", microsoft365UpdateUserRequest.getMobilePhone());
		}
		if (microsoft365UpdateUserRequest.getOfficeLocation() != null) {
			updateFields.put("officeLocation", microsoft365UpdateUserRequest.getOfficeLocation());
		}
		if (microsoft365UpdateUserRequest.getPreferredLanguage() != null) {
			updateFields.put("preferredLanguage", microsoft365UpdateUserRequest.getPreferredLanguage());
		}
		if (microsoft365UpdateUserRequest.getSurname() != null) {
			updateFields.put("surname", microsoft365UpdateUserRequest.getSurname());
		}
		if (microsoft365UpdateUserRequest.getUserPrincipalName() != null) {
			updateFields.put("userPrincipalName", microsoft365UpdateUserRequest.getUserPrincipalName());
		}
		if (microsoft365UpdateUserRequest.getId() != null) {
			updateFields.put("id", microsoft365UpdateUserRequest.getId());
		}
		ObjectNode requestBody = new ObjectMapper().valueToTree(updateFields);
		HttpEntity<ObjectNode> requestEntity = new HttpEntity<>(requestBody, headers);
		RestTemplate restTemplates = new RestTemplate(new HttpComponentsClientHttpRequestFactory());

		ResponseEntity<Microsoft365getUserlistResponse> responseEntity = restTemplates.exchange(updateMicrosoftUserUrl,
				HttpMethod.PATCH, requestEntity, Microsoft365getUserlistResponse.class);

		if (responseEntity.getStatusCode().is2xxSuccessful()) {

			return responseEntity.getBody();
		} else {
			throw new DataValidationException(HttpStatus.BAD_REQUEST,
					"Failed to update user. Status code: " + responseEntity.getStatusCodeValue(), null);
		}
	}

	@Override
	public CommonResponse deleteUserInMicrosoft365(String appId, String userEmail)
			throws JsonProcessingException, DataValidationException {
		HttpHeaders headers = new HttpHeaders();
		String accessToken = getRefreshToken1(appId);
		headers.set(Constant.AUTHORIZATION, Constant.BEARER + accessToken);
		HttpEntity<String> requestEntity = new HttpEntity<>(headers);
		String userId = getUserIDByEmail(userEmail, accessToken);
		ResponseEntity<Object> response = restTemplate.exchange(
				microsoftApiBaseUrl + Constant.MICROSOFT_USER_URL + userId, HttpMethod.DELETE, requestEntity,
				Object.class);

		if (response.getStatusCode().is2xxSuccessful()) {
			return new CommonResponse(HttpStatus.OK, new Response("User deleted successfully", null),
					"User deleted successfully");
		} else {
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Failed to delete user", null),
					"Failed to delete user. Status code: " + response.getStatusCode());
		}
	}

	public String getRefreshToken1(String appId) {
		AdaptorDetails adaptor = adaptorDetails.findByApplicationId(appId);
		MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
		formData.add(Constant.CLIENTID, adaptor.getClientId());
		formData.add(Constant.CLIENTSECRET, adaptor.getClientSecret());
		formData.add(Constant.REFRESH_TOKEN, adaptor.getApiToken());
		formData.add(Constant.SCOPE, Constant.MICROSOFT_SCOPE);
		formData.add(Constant.GRANT_TYPE, Constant.REFRESH_TOKEN);

		String refreshtokenEndpoint = Constant.MICROSOFT_URL + adaptor.getTenantid() + Constant.MICROSOFT_TOKEN_URL;
		try {
			ResponseEntity<Microsoft365Token> response = restTemplate.postForEntity(refreshtokenEndpoint, formData,
					Microsoft365Token.class);
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

	private String getUserIDByEmail(String userPrincipalName, String accessToken) throws DataValidationException {
		HttpHeaders headers = new HttpHeaders();
		headers.set(Constant.AUTHORIZATION, Constant.BEARER + accessToken);
		HttpEntity<String> requestEntity = new HttpEntity<>(headers);

		try {
			ResponseEntity<String> response = restTemplate.exchange(microsoftApiBaseUrl + "/v1.0/users?$top=999",
					HttpMethod.GET, requestEntity, String.class);

			if (response.getStatusCode().is2xxSuccessful()) {
				List<Microsoft365getUserlistResponse> userList = mapToMicrosoft365getUserlistResponse(
						response.getBody());
				Optional<Microsoft365getUserlistResponse> userOptional = userList.stream()
						.filter(user -> userPrincipalName.equalsIgnoreCase(user.getUserPrincipalName())).findFirst();

				if (userOptional.isPresent()) {
					return userOptional.get().getId();
				} else {

					throw new DataValidationException(HttpStatus.BAD_REQUEST, Constant.USER_NOT_FOUND + userPrincipalName,
							null);

				}
			} else {
				throw new DataValidationException(HttpStatus.BAD_REQUEST,
						"Failed to fetch profile details from HubSpot. Status code: " + response.getStatusCode(), null);
			}
		} catch (HttpClientErrorException.BadRequest ex) {
			return "Bad request: " + ex.getRawStatusCode() + ", " + ex.getResponseBodyAsString();
		} catch (RestClientException ex) {
			return "RestClientException: " + ex.getMessage();
		}

	}

	public CommonResponse getSubscribedSku(String appId) throws IOException, DataValidationException {
	    try {
	        String accessToken = getRefreshToken1(appId);
	        HttpHeaders headers = new HttpHeaders();
	        headers.set(Constant.AUTHORIZATION, Constant.BEARER + accessToken);
	        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
	        ResponseEntity<String> response = restTemplate.exchange(microsoftApiBaseUrl + "/v1.0/subscribedSkus",
	                HttpMethod.GET, requestEntity, String.class);

	        if (response.getStatusCode().is2xxSuccessful()) {
	            ObjectMapper mapper = new ObjectMapper();
	            JsonNode microsoftSubscribedskuResponse = mapper.readTree(response.getBody());
	            JsonNode values = microsoftSubscribedskuResponse.get(Constant.VALUE);
	            List<Map<String, Object>> skuDetails = new ArrayList<>();
	            
	           
	            for (int i = 0; i < values.size(); i++) {
	                JsonNode sku = values.get(i);
	                int enabledUnits = sku.path("prepaidUnits").path("enabled").asInt();
	                int consumedUnits = sku.path("consumedUnits").asInt();
	          
	                int availableUnits = enabledUnits - consumedUnits;
	                
	                
	                Map<String, Object> skuDetail = new HashMap<>();
	                skuDetail.put("skuId", sku.path("skuId").asText());
	                skuDetail.put("availableUnits", availableUnits);
	                skuDetails.add(skuDetail);
	            }
	            return new CommonResponse(HttpStatus.OK, new Response("SubscribedSku retrieved successfully", skuDetails),
	                    "SubscribedSku retrieved successfully");
	        } else {
	            throw new DataValidationException(HttpStatus.BAD_REQUEST,
	                    "Failed to fetch subscribed sku details from Microsoft. Status code: "
	                            + response.getStatusCode(),
	                    null);
	        }
	    } catch (JsonProcessingException e) {
	        e.printStackTrace();
	        throw new DataValidationException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error",
	                Constant.INTERNAL_SERVER_ERROR);
	    } catch (RestClientException ex) {
	        ex.printStackTrace();
	        throw new DataValidationException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error",
	                Constant.INTERNAL_SERVER_ERROR);
	    }
	}


	
	public CommonResponse getUserLicenseDetails(String userEmail, String appId)
			throws IOException, DataValidationException {
		String userId = getUserIDByEmail(userEmail, getRefreshToken1(appId));
		if (userId == null || userId.isEmpty()) {
			throw new DataValidationException(HttpStatus.BAD_REQUEST, Constant.USER_NOT_FOUND + userEmail, null);
		}
		String accessToken = getRefreshToken1(appId);
		HttpHeaders headers = new HttpHeaders();
		headers.set(Constant.AUTHORIZATION, Constant.BEARER + accessToken);
		HttpEntity<String> requestEntity = new HttpEntity<>(headers);
		ResponseEntity<String> response = restTemplate.exchange(
				microsoftApiBaseUrl + "/v1.0/users/" + userId + "/licenseDetails", HttpMethod.GET, requestEntity,
				String.class);
		try {
			if (response.getStatusCode().is2xxSuccessful()) {
				ObjectMapper mapper = new ObjectMapper();
				JsonNode microsoftUserLicenseResponse = mapper.readTree(response.getBody());
				JsonNode values = microsoftUserLicenseResponse.get("value");
				List<String> skuIds = new ArrayList<>();
				for (JsonNode value : values) {
					skuIds.add(value.get("skuId").asText());
				}
				return new CommonResponse(HttpStatus.OK, new Response("UserLicenseDetails retrieved successfully", skuIds),
						"User deleted successfully");
			} else {
				throw new DataValidationException(HttpStatus.BAD_REQUEST,
						"Failed to fetch subscribed sku details from Microsoft. Status code: "
								+ response.getStatusCode(),
						null);
			}
		}catch (JsonProcessingException e) {
			e.printStackTrace();
			throw new DataValidationException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error",
					Constant.INTERNAL_SERVER_ERROR);
		}catch (RestClientException ex) {
			ex.printStackTrace();
			throw new DataValidationException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error",
					Constant.INTERNAL_SERVER_ERROR);
		}
	}


	
	
	public Microsoft365getUserlistResponse assignLicense(String userEmail, String appId, String productName)
	        throws BadRequestException, DataValidationException, IOException, InterruptedException {
	   
	    String skuId = License.getIdByName(productName);
	    if (skuId == null) {
	        throw new IllegalArgumentException("Invalid product name: " + productName);
	    }
	   
	    int availableUnits = checkLicenseAvailability(appId, skuId);
	    if (availableUnits > 0) {
	        boolean userExists = checkIfUserExists(userEmail, appId);
	        if (userExists) {
	            return assignLicenseAfterDelay(userEmail, appId, skuId);
	        } else {
	            Microsoft365getUserlistResponse createUserResponse = createUser(userEmail, appId);

	            if (createUserResponse == null || createUserResponse.getUserPrincipalName() == null) {
	                throw new DataValidationException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create user", null);
	            }

	            ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
	            executorService.schedule(() -> {
	                try {
	                    assignLicenseAfterDelay(userEmail, appId, skuId);
	                } catch (Exception e) {
	                    e.printStackTrace();
	                }
	            }, 180, TimeUnit.SECONDS);

	            return createUserResponse;
	        }
	    } else {
	        throw new DataValidationException(HttpStatus.NOT_FOUND, "No license available to assign", null);
	    }
	}


	private int checkLicenseAvailability(String appId, String skuId) throws IOException, DataValidationException {
	    try {
	        CommonResponse subscribedSkuResponse = getSubscribedSku(appId);
	        List<Map<String, Object>> skuDetails = (List<Map<String, Object>>) subscribedSkuResponse.getResponse().getData();
	       
	        for (Map<String, Object> sku : skuDetails) {
	            String subscribedSkuId = (String) sku.get("skuId");
	            if (subscribedSkuId.equals(skuId)) {
	                return (int) sku.get("availableUnits");
	            }
	        }
	        return 0; 
	    } catch (Exception e) {
	        e.printStackTrace();
	        throw new DataValidationException(HttpStatus.INTERNAL_SERVER_ERROR, "Error checking license availability", null);
	    }
	}


    private Microsoft365getUserlistResponse assignLicenseAfterDelay(String userEmail, String appId, String skuId) {
    try {
        String userId = getUserIDByEmail(userEmail, getRefreshToken1(appId));
        if (userId == null || userId.isEmpty()) {
            throw new DataValidationException(HttpStatus.BAD_REQUEST, Constant.USER_NOT_FOUND + userEmail, null);
        }


        String jsonPayload = "{\n" +
                "  \"addLicenses\": [\n" +
                "    {\n" +
                "      \"skuId\": \"" + skuId + "\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"removeLicenses\": []\n" +
                "}";

        String assignLicenseUrl = microsoftApiBaseUrl + Constant.MICROSOFT_USER_URL + userId + "/assignLicense";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String accessToken = getRefreshToken1(appId);
        headers.set(Constant.AUTHORIZATION, Constant.BEARER + accessToken);

        HttpEntity<String> requestEntity = new HttpEntity<>(jsonPayload, headers);
        ResponseEntity<Microsoft365getUserlistResponse> responseEntity = restTemplate
                .postForEntity(assignLicenseUrl, requestEntity, Microsoft365getUserlistResponse.class);
        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
            throw new DataValidationException(HttpStatus.BAD_REQUEST,
                    "Failed to assign license to user. Status code: " + responseEntity.getStatusCodeValue(), null);
        }
        
        return responseEntity.getBody();
    } catch (Exception e) {
        e.printStackTrace();
        return null;
    }
}


	private boolean checkIfUserExists(String userEmail, String appId) throws IOException, DataValidationException {
	    CommonResponse userListResponse = getUserList(appId);
	    List<Microsoft365getUserlistResponse> userList = (List<Microsoft365getUserlistResponse>) userListResponse.getResponse().getData();
	    for (Microsoft365getUserlistResponse user : userList) {
	        if (user.getUserPrincipalName().equalsIgnoreCase(userEmail)) {
	            return true; 
	        }
	    }
	    return false; 
	}


	@Override
	public Microsoft365getUserlistResponse unAssignLicense(String userEmail, String appId, String productName)
	        throws JsonProcessingException, BadRequestException, DataValidationException {
	    try {
	        String userId = getUserIDByEmail(userEmail, getRefreshToken1(appId));
	        if (userId == null || userId.isEmpty()) {
	            throw new DataValidationException(HttpStatus.BAD_REQUEST, Constant.USER_NOT_FOUND + userEmail, null);
	        }

	        String skuId = License.getIdByName(productName);
	        if (skuId == null) {
	            throw new IllegalArgumentException("Invalid product name: " + productName);
	        }

	        String unAssignLicenseUrl = microsoftApiBaseUrl + Constant.MICROSOFT_USER_URL + userId + "/assignLicense";
	        HttpHeaders headers = new HttpHeaders();
	        headers.setContentType(MediaType.APPLICATION_JSON);
	        String accessToken = getRefreshToken1(appId);
	        headers.set(Constant.AUTHORIZATION, Constant.BEARER + accessToken);

	        String jsonPayload = "{\n" +
	                "  \"addLicenses\": [],\n" +
	                "  \"removeLicenses\": [\n" +
	                "    \"" + skuId + "\"\n" +
	                "  ]\n" +
	                "}";

	        HttpEntity<String> requestEntity = new HttpEntity<>(jsonPayload, headers);
	        Microsoft365getUserlistResponse response = restTemplate.postForObject(unAssignLicenseUrl, requestEntity, Microsoft365getUserlistResponse.class);

	        CommonResponse deleteUserResponse = deleteUserInMicrosoft365(appId, userEmail);
	        if (deleteUserResponse.getStatus().is2xxSuccessful()) {
	            return response;
	        } else {
	            System.out.println("Failed to delete user: " + userEmail + ". Error: " + deleteUserResponse.getMessage());
	            return response;
	        }
	    } catch (HttpClientErrorException.BadRequest ex) {
	        String responseBody = ex.getResponseBodyAsString();
	        if (responseBody.contains("Cannot convert the literal")) {
	            throw new BadRequestException("User does not have the specified license to unassign.", ex);
	        } else {
	            ObjectMapper mapper = new ObjectMapper();
	            Microsoft365getUserlistResponse errorResponse;
	            try {
	                errorResponse = mapper.readValue(responseBody, Microsoft365getUserlistResponse.class);
	            } catch (IOException ioException) {
	                ioException.printStackTrace();
	                errorResponse = null;
	            }
	            return errorResponse;
	        }
	    }
	}
}