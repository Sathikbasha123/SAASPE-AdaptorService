package com.saaspe.Adaptor.ServiceImpl;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONObject;
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
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saaspe.Adaptor.Advice.CommonResponse;
import com.saaspe.Adaptor.Advice.Response;
import com.saaspe.Adaptor.Constant.Constant;
import com.saaspe.Adaptor.Constant.ZohoConstant;
import com.saaspe.Adaptor.Entity.AdaptorDetails;
import com.saaspe.Adaptor.Repository.AdaptorDetailsRepository;
import com.saaspe.Adaptor.Response.AccessTokenResponse;
import com.saaspe.Adaptor.Response.RefreshTokenResponse;
import com.saaspe.Adaptor.Response.ZohoPeopleErrorResponse;
import com.saaspe.Adaptor.Service.ZohoAnalyticsService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ZohoAnalyticsServiceImpl implements ZohoAnalyticsService {

	private final AdaptorDetailsRepository adaptorRepository;

	private final RestTemplate restTemplate;

	private final ObjectMapper objectMapper;

	@Override
	public CommonResponse getAccessToken(String appId, String code)
			throws IOException {
		AdaptorDetails applicationDetails = adaptorRepository.findByApplicationId(appId);
		if (applicationDetails == null) {
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.INVALID_ADAPTOR_APPLICATION, null),
					Constant.APPLICATION_NOT_CONNECTED);
		}
		String url = ZohoConstant.OAUTH_TOKEN_URL;
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
		builder.queryParam("grant_type", "authorization_code");
		builder.queryParam("client_id", applicationDetails.getClientId());
		builder.queryParam("client_secret", applicationDetails.getClientSecret());
		builder.queryParam("code", code);

		HttpHeaders headers = new HttpHeaders();
		ResponseEntity<String> response = restTemplate.exchange(builder.toUriString(), HttpMethod.POST,
				new HttpEntity<>(headers), String.class);
		if (response.getBody().contains(Constant.ERROR_KEY)) {

			ZohoPeopleErrorResponse errorResponse = objectMapper.readValue(response.getBody(),
					ZohoPeopleErrorResponse.class);
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.TOKEN_RESPONSE, null),
					errorResponse.getError());
		} else {
			AccessTokenResponse accessTokenResponse = objectMapper.readValue(response.getBody(),
					AccessTokenResponse.class);
			applicationDetails.setApiToken(accessTokenResponse.getRefresh_token());
			adaptorRepository.save(applicationDetails);
			return new CommonResponse(HttpStatus.OK, new Response(Constant.TOKEN_RESPONSE, accessTokenResponse),
					"Token generation successful");
		}

	}

	@Override
	public CommonResponse generateToken(String appId) throws IOException {
		AdaptorDetails applicationDetails = adaptorRepository.findByApplicationId(appId);

		if (applicationDetails == null) {
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.INVALID_ADAPTOR_APPLICATION, null),
					Constant.APPLICATION_NOT_CONNECTED);
		}
		String url = ZohoConstant.OAUTH_TOKEN_URL + "?refresh_token=" + applicationDetails.getApiToken() + "&client_id="
				+ applicationDetails.getClientId() + "&client_secret=" + applicationDetails.getClientSecret()
				+ "&grant_type=refresh_token";
		HttpHeaders headers = new HttpHeaders();
		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(headers),
				String.class);

		if (response.getBody().contains(Constant.ERROR_KEY)) {
			ZohoPeopleErrorResponse errorResponse = objectMapper.readValue(response.getBody(),
					ZohoPeopleErrorResponse.class);
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.TOKEN_RESPONSE, null),
					errorResponse.getError());
		} else {
			RefreshTokenResponse refreshTokenResponse = objectMapper.readValue(response.getBody(),
					RefreshTokenResponse.class);
			return new CommonResponse(HttpStatus.OK, new Response(Constant.TOKEN_RESPONSE, refreshTokenResponse),
					"Token generation successful");
		}

	}

	@Override
	public CommonResponse saveOrgDetails(String appId) throws IOException {
		AdaptorDetails applicationDetails = adaptorRepository.findByApplicationId(appId);
		String token = getToken(appId);
		if (applicationDetails == null) {
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.INVALID_ADAPTOR_APPLICATION, null),
					Constant.APPLICATION_NOT_CONNECTED);
		}
		String url = "https://analyticsapi.zoho.in/restapi/v2/orgs";
		HttpHeaders headers = new HttpHeaders();
		headers.set(Constant.AUTHORIZATION, Constant.BEARER + token);

		ResponseEntity<String> response = null;
		try {
			response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
			JSONObject jsonObject = new JSONObject(response.getBody());
			JSONObject dataObject = jsonObject.getJSONObject("data");
			JSONArray orgsList = dataObject.getJSONArray("orgs");
			String orgId = null;
			for (int i = 0; i < orgsList.length(); i++) {
				JSONObject organization = orgsList.getJSONObject(i);

				if (organization.get("role").toString().equalsIgnoreCase("Account Admin")) {

					orgId = organization.get("orgId").toString();
					applicationDetails.setTenantid(orgId);
					adaptorRepository.save(applicationDetails);
					break;
				}

			}
			if (applicationDetails.getTenantid() == null) {
				return new CommonResponse(HttpStatus.BAD_REQUEST,
						new Response("Save organization details response", null),
						"Failed to save organization details");
			}
			return new CommonResponse(HttpStatus.OK, new Response("Save organization details response", null),
					"Organization details saved successfully");

		} catch (HttpClientErrorException.Unauthorized e) {
			e.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Organization user list response", null),
					Constant.INVALID_OAUTH_TOKEN);
		} catch (HttpClientErrorException.BadRequest e) {
			e.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Organization user list response", null),
					"Exception occured");
		}

	}

	@Override
	public CommonResponse inviteUser(String appId, String userEmail) throws IOException {
		AdaptorDetails applicationDetails = adaptorRepository.findByApplicationId(appId);
		String token = getToken(appId);
		if (applicationDetails == null || token == null) {
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.INVALID_ADAPTOR_APPLICATION, null),
					Constant.APPLICATION_NOT_CONNECTED);
		}
		String url = Constant.ZOHOANALYTICS_USER_URL;
		HttpHeaders headers = new HttpHeaders();
		headers.set(Constant.AUTHORIZATION, Constant.BEARER + token);
		headers.set(Constant.ZOHO_ALAYTICS_HEADER, applicationDetails.getTenantid());
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		JSONObject jsonObject = new JSONObject();
		JSONArray emailArray = new JSONArray();
		emailArray.put(userEmail);
		jsonObject.put("emailIds", emailArray);
		map.add("CONFIG", jsonObject.toString());

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
		ResponseEntity<String> response = null;
		try {
			response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
			if (response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
				return new CommonResponse(HttpStatus.OK, new Response(Constant.INVITE_USER_RESPONSE, null),
						"User Invited successfully");
			}
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Invite user response", null),
					"Unable to invite user to Zoho Analytics Organization :"
							+ applicationDetails.getOrganizationName());

		} catch (HttpClientErrorException.Unauthorized e) {
			e.printStackTrace();
			JSONObject error = new JSONObject(e.getResponseBodyAsString());
			JSONObject message = new JSONObject(error.get("data").toString());
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.INVITE_USER_RESPONSE, message.get(Constant.ERROR_MESSAGE)),
					Constant.INVALID_OAUTH_TOKEN);
		} catch (HttpClientErrorException.BadRequest e) {
			e.printStackTrace();
			JSONObject error = new JSONObject(e.getResponseBodyAsString());
			JSONObject message = new JSONObject(error.get("data").toString());
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response(Constant.INVITE_USER_RESPONSE, message.get(Constant.ERROR_MESSAGE)), "Invalid data");

		}
	}

	@Override
	public CommonResponse getSubscriptionDetails(String appId) throws IOException {

		AdaptorDetails applicationDetails = adaptorRepository.findByApplicationId(appId);
		String token = getToken(appId);
		if (applicationDetails == null) {
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.INVALID_ADAPTOR_APPLICATION, null),
					Constant.APPLICATION_NOT_CONNECTED);
		}
		String url = "https://analyticsapi.zoho.in/restapi/v2/resources";
		HttpHeaders headers = new HttpHeaders();
		headers.set(Constant.AUTHORIZATION, Constant.BEARER + token);
		headers.set(Constant.ZOHO_ALAYTICS_HEADER, applicationDetails.getTenantid());
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		ResponseEntity<String> response = null;
		try {
			response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
			JsonNode data = objectMapper.readTree(response.getBody());
			JsonNode resourceDetails = data.get("data").get("resourceDetails");
			JSONObject result = new JSONObject();

			for (int i = 0; i < resourceDetails.size(); i++) {
				if (resourceDetails.get(i).get("resourceName").asText().equalsIgnoreCase("users")) {
					result.put("allocatedLicenseCount", resourceDetails.get(i).get(Constant.RESOURCE_USAGE).get("allocated"));
					result.put("usedLicenseCount", resourceDetails.get(i).get(Constant.RESOURCE_USAGE).get("used"));
					result.put("remainingLicenseCount", resourceDetails.get(i).get(Constant.RESOURCE_USAGE).get("remaining"));
				}
			}

			return new CommonResponse(HttpStatus.OK, new Response(Constant.SUB_DETAIL_RESPONSE, result.toMap()),
					"Subscription details retrieved successfully");

		} catch (HttpClientErrorException.Unauthorized e) {
			e.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.SUB_DETAIL_RESPONSE, null),
					Constant.INVALID_OAUTH_TOKEN);
		} catch (HttpClientErrorException.BadRequest e) {
			e.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.SUB_DETAIL_RESPONSE, null),
					"Exception occured during retrieving subscription details");
		}
	}

	public String getToken(String appId) throws IOException {
		AdaptorDetails applicationDetails = adaptorRepository.findByApplicationId(appId);
		if (applicationDetails == null) {
			return null;
		}
		String url = ZohoConstant.OAUTH_TOKEN_URL + "?refresh_token=" + applicationDetails.getApiToken() + "&client_id="
				+ applicationDetails.getClientId() + "&client_secret=" + applicationDetails.getClientSecret()
				+ "&grant_type=refresh_token";
		HttpHeaders headers = new HttpHeaders();
		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(headers),
				String.class);
		if (response.getBody().contains(Constant.ERROR_KEY)) {
			return null;
		} else {
			RefreshTokenResponse refreshTokenResponse = objectMapper.readValue(response.getBody(),
					RefreshTokenResponse.class);
			return refreshTokenResponse.getAccess_token().trim();
		}
	}

	@Override
	public CommonResponse getUsersList(String appId) throws IOException {

		AdaptorDetails applicationDetails = adaptorRepository.findByApplicationId(appId);
		String token = getToken(appId);
		if (applicationDetails == null) {
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.INVALID_ADAPTOR_APPLICATION, null),
					Constant.APPLICATION_NOT_CONNECTED);
		}
		String url = Constant.ZOHOANALYTICS_USER_URL;
		HttpHeaders headers = new HttpHeaders();
		headers.set(Constant.AUTHORIZATION, Constant.BEARER + token);
		headers.set(Constant.ZOHO_ALAYTICS_HEADER, applicationDetails.getTenantid());
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		ResponseEntity<String> response = null;
		try {
			response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
			JsonNode data = objectMapper.readTree(response.getBody());
			JsonNode resourceDetails = data.get("data").get("users");

			return new CommonResponse(HttpStatus.OK, new Response(Constant.USER_LIST_RESPONSE, resourceDetails),
					"Users List retrieved successfully");

		} catch (HttpClientErrorException.Unauthorized e) {
			e.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.USER_LIST_RESPONSE, null),
					Constant.INVALID_OAUTH_TOKEN);
		} catch (HttpClientErrorException.BadRequest e) {
			e.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.USER_LIST_RESPONSE, null),
					"Exception occured during retrieving user list details");
		}
	}

	@Override
	public CommonResponse revokeAccess(String appId, String userEmail) throws IOException {
		AdaptorDetails applicationDetails = adaptorRepository.findByApplicationId(appId);
		String token = getToken(appId);
		if (applicationDetails == null || token == null) {
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.INVALID_ADAPTOR_APPLICATION, null),
					Constant.APPLICATION_NOT_CONNECTED);
		}
		String url = Constant.ZOHOANALYTICS_USER_URL;
		HttpHeaders headers = new HttpHeaders();
		headers.set(Constant.AUTHORIZATION, Constant.BEARER + token);
		headers.set(Constant.ZOHO_ALAYTICS_HEADER, applicationDetails.getTenantid());
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		JSONObject jsonObject = new JSONObject();
		JSONArray emailArray = new JSONArray();
		emailArray.put(userEmail);
		jsonObject.put("emailIds", emailArray);
		map.add("CONFIG", jsonObject.toString());

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
		ResponseEntity<String> response = null;
		try {
			response = restTemplate.exchange(url, HttpMethod.DELETE, request, String.class);
			if (response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
				return new CommonResponse(HttpStatus.OK, new Response(Constant.INVITE_USER_RESPONSE, null),
						"User access revoked successfully");
			}
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Invite user response", null),
					"Unable to revoke user access from Zoho Analytics Organization :"
							+ applicationDetails.getOrganizationName());

		} catch (HttpClientErrorException.Unauthorized e) {
			e.printStackTrace();
			JSONObject error = new JSONObject(e.getResponseBodyAsString());
			JSONObject message = new JSONObject(error.get("data").toString());
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Revoke User access response", message.get(Constant.ERROR_MESSAGE)), Constant.INVALID_OAUTH_TOKEN);
		} catch (HttpClientErrorException.BadRequest e) {
			e.printStackTrace();
			JSONObject error = new JSONObject(e.getResponseBodyAsString());
			JSONObject message = new JSONObject(error.get("data").toString());
			return new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Revoke User access response", message.get(Constant.ERROR_MESSAGE)), "Invalid data");

		}
	}

	@Override
	public CommonResponse getOrganizationList(String appId) throws IOException {
		AdaptorDetails applicationDetails = adaptorRepository.findByApplicationId(appId);
		String token = getToken(appId);
		if (applicationDetails == null) {
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.INVALID_ADAPTOR_APPLICATION, null),
					Constant.APPLICATION_NOT_CONNECTED);
		}
		String url = "https://analyticsapi.zoho.in/restapi/v2/orgs";
		HttpHeaders headers = new HttpHeaders();
		headers.set(Constant.AUTHORIZATION, Constant.BEARER + token);

		ResponseEntity<String> response = null;
		try {
			response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

			JsonNode result = objectMapper.readTree(response.getBody());
			return new CommonResponse(HttpStatus.OK,
					new Response(Constant.ORG_LIST_RESPONSE, result.get("data").get("orgs")),
					"Organization details retireved successfully");

		} catch (HttpClientErrorException.Unauthorized e) {
			e.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.ORG_LIST_RESPONSE, null),
					Constant.INVALID_OAUTH_TOKEN);
		} catch (HttpClientErrorException.BadRequest e) {
			e.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.ORG_LIST_RESPONSE, null),
					"Exception occured");
		}
	}
}
