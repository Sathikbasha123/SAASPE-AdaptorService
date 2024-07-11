package com.saaspe.Adaptor.ServiceImpl;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saaspe.Adaptor.Advice.CommonResponse;
import com.saaspe.Adaptor.Advice.Response;
import com.saaspe.Adaptor.Constant.Constant;
import com.saaspe.Adaptor.Entity.AdaptorDetails;
import com.saaspe.Adaptor.Model.SalesForceUserResponse;
import com.saaspe.Adaptor.Repository.AdaptorDetailsRepository;
import com.saaspe.Adaptor.Service.SalesforceService;

@Service
public class SalesforceServiceImpl implements SalesforceService {

	@Autowired
	AdaptorDetailsRepository adaptorRepository;

	@Autowired
	RestTemplate restTemplate;

	@Autowired
	ObjectMapper objectMapper;

	@Override
	public CommonResponse generateToken(String appId) throws IOException {

		AdaptorDetails applicationDetails = adaptorRepository.findByApplicationId(appId);

		if (applicationDetails == null) {
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.INVALID_ADAPTOR_APPLICATION, null),
					Constant.APPLICATION_NOT_CONNECTED);
		}
		String url = "https://" + applicationDetails.getOrganizationName() + ".my.salesforce.com/services/oauth2/token";
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("grant_type", "client_credentials");
		map.add("client_id", applicationDetails.getClientId());
		map.add("client_secret", applicationDetails.getClientSecret());

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
		try {
			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
			JsonNode tokenResponse = objectMapper.readTree(response.getBody());
			if (response.getStatusCode().is2xxSuccessful()) {

				String userProfileUrl = "https://" + applicationDetails.getOrganizationName()
						+ ".my.salesforce.com/services/data/v60.0/query?q=SELECT Id,Name,UserType,UserLicenseId FROM Profile";

				HttpHeaders header = new HttpHeaders();
				header.add("Authorization", "Bearer " + tokenResponse.get("access_token").asText());
				ResponseEntity<String> profileResponse = restTemplate.exchange(userProfileUrl, HttpMethod.GET,
						new HttpEntity<>(header), String.class);

				JsonNode profile = objectMapper.readTree(profileResponse.getBody());

				JsonNode profileList = profile.get("records");
				boolean hasProfileId = false;
				for (int i = 0; i < profileList.size(); i++) {
					JsonNode profileId = profileList.get(i);
					if (profileId.get("Name").asText().equalsIgnoreCase("Standard User")) {
						applicationDetails.setTenantid(profileId.get("Id").asText());
						adaptorRepository.save(applicationDetails);
						hasProfileId = true;
						break;
					}
				}
				if (!hasProfileId) {
					return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Generate token response", null),
							"Invalid credentials");
				}
				return new CommonResponse(HttpStatus.OK,
						new Response("Generate token response", tokenResponse.get("access_token")),
						"Token generated successfully");
			} else {
				return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Generate token response", null),
						"Invalid credentials");
			}
		} catch (HttpClientErrorException.BadRequest e) {
			JsonNode tokenResponse = objectMapper.readTree(e.getResponseBodyAsString());
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Generate token response", null),
					tokenResponse.get("error"));
		}

	}

	@Override
	public CommonResponse getUserList(String appId) throws IOException {

		AdaptorDetails applicationDetails = adaptorRepository.findByApplicationId(appId);
		String token = getToken(appId);
		if (applicationDetails == null || token == null) {
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.INVALID_ADAPTOR_APPLICATION, null),
					Constant.APPLICATION_NOT_CONNECTED);
		}
		String url = "https://" + applicationDetails.getOrganizationName()
				+ ".my.salesforce.com/services/data/v60.0/query?q=SELECT+UserName,Id,Email,IsActive+FROM+User";
		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", "Bearer " + token);

		try {
			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
					String.class);
			JsonNode listResponse = objectMapper.readTree(response.getBody());
			List<SalesForceUserResponse> userListResponse = new LinkedList<>();
			for (int i = 0; i < listResponse.get("records").size(); i++) {
				JsonNode user = listResponse.get("records").get(i);
				SalesForceUserResponse userResponse = new SalesForceUserResponse();
				userResponse.setActive(user.get("IsActive").asBoolean());
				userResponse.setUserEmail(user.get("Email").asText());
				userResponse.setUserId(user.get("Id").asText());
				userResponse.setUserName(user.get("Username").asText());
				userListResponse.add(userResponse);

			}
			return new CommonResponse(HttpStatus.OK, new Response("User list response", userListResponse),
					"User list retrieved successfully");
		} catch (HttpClientErrorException.Unauthorized e) {
			JsonNode tokenResponse = objectMapper.readTree(e.getResponseBodyAsString());
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("User List response", null),
					tokenResponse.get(0).get("message"));
		} catch (HttpClientErrorException.BadRequest e) {
			JsonNode tokenResponse = objectMapper.readTree(e.getResponseBodyAsString());
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("User list response", null),
					tokenResponse.get("error"));
		}
	}

	@Override
	public CommonResponse createUser(String appId, String userEmail, String userName,String firstName) throws IOException {
		AdaptorDetails applicationDetails = adaptorRepository.findByApplicationId(appId);
		String token = getToken(appId);
		if (applicationDetails == null || token == null) {
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.INVALID_ADAPTOR_APPLICATION, null),
					Constant.APPLICATION_NOT_CONNECTED);
		}
		String url = "https://" + applicationDetails.getOrganizationName()
				+ ".my.salesforce.com/services/data/v60.0/composite";
		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", "Bearer " + token);
		headers.setContentType(MediaType.APPLICATION_JSON);
		String jsonPayload = "{\n" + "    \"allOrNone\": true,\n" + "    \"compositeRequest\": [\n" + "        {\n"
				+ "            \"method\": \"POST\",\n"
				+ "            \"url\": \"/services/data/v60.0/sobjects/User\",\n"
				+ "            \"referenceId\": \"NewUser\",\n" + "            \"body\": {\n"
				+ "                \"Username\": \"{username}\",\n" + "                \"LastName\": \"{firstName}\",\n"
				+ "                \"Email\": \"{email}\",\n" + "                \"Alias\": \"sa\",\n"
				+ "                \"CommunityNickname\": \"{communityNickname}\",\n"
				+ "                \"TimeZoneSidKey\": \"America/Los_Angeles\",\n"
				+ "                \"LocaleSidKey\": \"en_US\",\n"
				+ "                \"EmailEncodingKey\": \"ISO-8859-1\",\n"
				+ "                \"LanguageLocaleKey\": \"en_US\",\n"
				+ "                \"profileId\": \"{profileId}\"\n" + "            }\n" + "        },\n"
				+ "        {\n" + "            \"method\": \"DELETE\",\n"
				+ "            \"url\": \"/services/data/v60.0/sobjects/User/@{NewUser.id}/password\",\n"
				+ "            \"referenceId\": \"ResetPassword\"\n" + "        }\n" + "    ]\n" + "}";
		jsonPayload = jsonPayload.replace("{username}", userName + "_" + userEmail).replace("{email}", userEmail).replace("{firstName}",firstName)
				.replace("{communityNickname}", userName + "_" + userEmail).replace("{profileId}", applicationDetails.getTenantid());
		HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);

		try {
			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
			JsonNode listResponse = objectMapper.readTree(response.getBody());
			if (listResponse.get("compositeResponse").get(0).get("httpStatusCode").asText().equalsIgnoreCase("201"))
				return new CommonResponse(HttpStatus.OK,
						new Response("Create User response",
								"User created successfully "
										+ listResponse.get("compositeResponse").get(0).get("body").get("id").asText()),
						"User created successfully");
			else
				return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Create user resposne", null),
						listResponse.get("compositeResponse").get(0).get("body").get(0).get("message").asText());
		} catch (HttpClientErrorException.Unauthorized e) {
			JsonNode tokenResponse = objectMapper.readTree(e.getResponseBodyAsString());
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Create User response", null),
					tokenResponse.get(0).get("message"));
		} catch (HttpClientErrorException.BadRequest e) {
			JsonNode tokenResponse = objectMapper.readTree(e.getResponseBodyAsString());
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Create User response", null),
					tokenResponse.get("error"));
		}
	}

	@Override
	public CommonResponse removeUser(String appId, String userEmail, String userName) throws IOException {
		AdaptorDetails applicationDetails = adaptorRepository.findByApplicationId(appId);
		String token = getToken(appId);
		if (applicationDetails == null || token == null) {
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.INVALID_ADAPTOR_APPLICATION, null),
					Constant.APPLICATION_NOT_CONNECTED);
		}
		String userSearchUrl = "https://" + applicationDetails.getOrganizationName()
				+ ".my.salesforce.com/services/data/v60.0/query?q=SELECT+Id,Email,IsActive+FROM+User+ where + Email ='"
				+ userEmail + "'and Username ='" + userName + "_" + userEmail + "'";
		HttpHeaders header = new HttpHeaders();
		header.add("Authorization", "Bearer " + token);
		HttpEntity<String> searchRequest = new HttpEntity<>(header);
		String userId = null;
		try {
			ResponseEntity<String> response = restTemplate.exchange(userSearchUrl, HttpMethod.GET, searchRequest,
					String.class);
			if (!response.getStatusCode().is2xxSuccessful()) {
				return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Remove User response", null),
						"Exception in remove user response");
			}
			userId = objectMapper.readTree(response.getBody()).get("records").get(0).get("Id").asText();
			String url = "https://" + applicationDetails.getOrganizationName()
					+ ".my.salesforce.com/services/data/v60.0/sobjects/User/" + userId;
			HttpHeaders headers = new HttpHeaders();
			headers.add("Authorization", "Bearer " + token);
			headers.setContentType(MediaType.APPLICATION_JSON);
			String jsonPayload = "{ \"IsActive\":false\r\n" + "}";
			HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);
			RestTemplate rest = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
			try {
				ResponseEntity<String> response1 = rest.exchange(url, HttpMethod.PATCH, request, String.class);
				if (response1.getStatusCodeValue() == 204)
					return new CommonResponse(HttpStatus.OK, new Response("Remove user response", null),
							"User removed from salesforce successfully");
				else
					return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Remove user response", null),
							"Exception in remove user response");
			} catch (HttpClientErrorException.Unauthorized e) {
				JsonNode tokenResponse = objectMapper.readTree(e.getResponseBodyAsString());
				return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Remove User response", null),
						tokenResponse.get(0).get("message"));
			} catch (HttpClientErrorException.BadRequest e) {
				JsonNode tokenResponse = objectMapper.readTree(e.getResponseBodyAsString());
				return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Remove User response", null),
						tokenResponse.get("error"));
			}
		} catch (HttpClientErrorException.Unauthorized e) {
			JsonNode tokenResponse = objectMapper.readTree(e.getResponseBodyAsString());
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Remove User response", null),
					tokenResponse.get(0).get("message"));
		} catch (HttpClientErrorException.BadRequest e) {
			JsonNode tokenResponse = objectMapper.readTree(e.getResponseBodyAsString());
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Remove User response", null),
					tokenResponse.get("error"));
		}
	}

	public String getToken(String appId) {
		AdaptorDetails applicationDetails = adaptorRepository.findByApplicationId(appId);
		String url = "https://" + applicationDetails.getOrganizationName() + ".my.salesforce.com/services/oauth2/token";
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("grant_type", "client_credentials");
		map.add("client_id", applicationDetails.getClientId());
		map.add("client_secret", applicationDetails.getClientSecret());

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
		try {
			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
			JsonNode tokenResponse = objectMapper.readTree(response.getBody());
			if (response.getStatusCode().is2xxSuccessful()) {

				return tokenResponse.get("access_token").asText();
			} else {
				return null;
			}
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public CommonResponse getLicenseDetails(String appId) throws IOException {
		AdaptorDetails applicationDetails = adaptorRepository.findByApplicationId(appId);
		String token = getToken(appId);
		if (applicationDetails == null || token == null) {
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.INVALID_ADAPTOR_APPLICATION, null),
					Constant.APPLICATION_NOT_CONNECTED);
		}
		String licenseListUrl = "https://" + applicationDetails.getOrganizationName()
				+ ".my.salesforce.com/services/data/v60.0/query?q=SELECT+LicenseDefinitionKey,MasterLabel,Id+ FROM+UserLicense";
		HttpHeaders header = new HttpHeaders();
		header.add("Authorization", "Bearer " + token);
		HttpEntity<String> searchRequest = new HttpEntity<>(header);
		String licenseId = null;
		try {
			ResponseEntity<String> response = restTemplate.exchange(licenseListUrl, HttpMethod.GET, searchRequest,
					String.class);
			JsonNode licenseResponse = objectMapper.readTree(response.getBody()).get("records");
			for (int i = 0; i < licenseResponse.size(); i++) {
				if (licenseResponse.get(i).get("MasterLabel").asText().equalsIgnoreCase("Salesforce")) {
					licenseId = licenseResponse.get(i).get("Id").asText();
					break;
				}
			}
			if (!response.getStatusCode().is2xxSuccessful()) {
				return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("License details response", null),
						"Exception in get license details response");
			}
			String url = "https://" + applicationDetails.getOrganizationName()
					+ ".my.salesforce.com/services/data/v60.0/sobjects/UserLicense/" + licenseId;
			HttpHeaders headers = new HttpHeaders();
			headers.add("Authorization", "Bearer " + token);
			headers.setContentType(MediaType.APPLICATION_JSON);
			RestTemplate rest = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
			try {
				ResponseEntity<String> response1 = rest.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
						String.class);
				if (response1.getStatusCodeValue() == 200) {
					JsonNode license = objectMapper.readTree(response1.getBody());
					JSONObject json = new JSONObject();
					json.put("LicenseId", license.get("LicenseId"));
					json.put("TotalLicenses", license.get("TotalLicenses"));
					json.put("UsedLicenses", license.get("UsedLicenses"));
					json.put("MasterLabel", license.get("MasterLabel"));
					return new CommonResponse(HttpStatus.OK, new Response("License details response", json.toMap()),
							"License details retrieved successfully");
				} else
					return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("License details response", null),
							"Exception in License details  response");
			} catch (HttpClientErrorException.Unauthorized e) {
				e.printStackTrace();
				JsonNode tokenResponse = objectMapper.readTree(e.getResponseBodyAsString());
				return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("License details response", null),
						tokenResponse.get(0).get("message"));
			} catch (HttpClientErrorException.BadRequest e) {
				e.printStackTrace();
				JsonNode tokenResponse = objectMapper.readTree(e.getResponseBodyAsString());
				return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("License details response", null),
						tokenResponse.get("error"));
			}
		} catch (HttpClientErrorException.Unauthorized e) {
			JsonNode tokenResponse = objectMapper.readTree(e.getResponseBodyAsString());
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("License details response", null),
					tokenResponse.get(0).get("message"));
		} catch (HttpClientErrorException.BadRequest e) {
			JsonNode tokenResponse = objectMapper.readTree(e.getResponseBodyAsString());
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("License details response", null),
					tokenResponse.get("error"));
		}
	}

}
