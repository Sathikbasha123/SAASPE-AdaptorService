package com.saaspe.Adaptor.ServiceImpl;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
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
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.saaspe.Adaptor.Advice.CommonResponse;
import com.saaspe.Adaptor.Advice.Response;
import com.saaspe.Adaptor.Constant.Constant;
import com.saaspe.Adaptor.Constant.ZohoConstant;
import com.saaspe.Adaptor.Entity.AdaptorDetails;
import com.saaspe.Adaptor.Entity.AdaptorUserDetails;
import com.saaspe.Adaptor.Model.AdaptorFields;
import com.saaspe.Adaptor.Model.ZohoPeopleInviteRequest;
import com.saaspe.Adaptor.Model.ZohoPeopleProfile;
import com.saaspe.Adaptor.Repository.AdaptorDetailsRepository;
import com.saaspe.Adaptor.Repository.AdaptorUserDetailsRepository;
import com.saaspe.Adaptor.Response.AccessTokenResponse;
import com.saaspe.Adaptor.Response.RefreshTokenResponse;
import com.saaspe.Adaptor.Response.ZohoPeopleErrorResponse;
import com.saaspe.Adaptor.Service.SequenceGeneratorService;
import com.saaspe.Adaptor.Service.ZohoPeopleService;

@Service
public class ZohoPeopleServiceImplementation implements ZohoPeopleService {

	@Autowired
	AdaptorDetailsRepository adaptorRepository;

	@Autowired
	AdaptorUserDetailsRepository adaptorUserDetailsRepository;

	@Autowired
	SequenceGeneratorService sequenceGeneratorService;

	@Autowired
	RestTemplate restTemplate;


	@Override
	public CommonResponse getAuthUri(String appId) {

		AdaptorDetails applicationDetails = adaptorRepository.findByApplicationId(appId);

		if (applicationDetails == null) {
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.INVALID_ADAPTOR_APPLICATION, null),
					Constant.APPLICATION_NOT_CONNECTED);
		}
		String redirectURL = ZohoConstant.GRANT_TOKEN_URL + "?scope=ZOHOPEOPLE.forms.ALL&client_id="
				+ applicationDetails.getClientId() + "&response_type=code&prompt=consent&access_type=offline";

		return new CommonResponse(HttpStatus.OK, new Response("Authorization URI response", redirectURL),
				"Auth URI retrieved successfully");
	}

	@Override
	public CommonResponse getAccessToken(String appId, String code) throws IOException {
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

		ObjectMapper objectMapper = new ObjectMapper();
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
		ObjectMapper objectMapper = new ObjectMapper();
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
	public CommonResponse inviteUser(String appId, ZohoPeopleInviteRequest inviteRequest) throws IOException {
		AdaptorDetails applicationDetails = adaptorRepository.findByApplicationId(appId);
		String token = getToken(appId);
		if (applicationDetails == null || token == null) {
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.INVALID_ADAPTOR_APPLICATION, null),
					Constant.APPLICATION_NOT_CONNECTED);
		}
		String url = "https://people.zoho.in/people/api/forms/json/employee/insertRecord";
		String inviteUser = String.format("{FirstName:'%s',LastName:'%s',EmailID:'%s',EmployeeID:%s\r\n" + "}",
				inviteRequest.getFirstName(), inviteRequest.getLastName(), inviteRequest.getEmailID(),
				inviteRequest.getEmployeeID());
		HttpHeaders headers = new HttpHeaders();
		headers.set(Constant.AUTHORIZATION, Constant.BEARER + token);
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("inputData", inviteUser);
		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
		ResponseEntity<String> response = null;
		try {
			response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode jsonNode = objectMapper.readTree(response.getBody());
			if (response.getBody().contains("\"errors\":")) {
				if (jsonNode.get(Constant.RESPONSE).get("errors").getNodeType().equals(JsonNodeType.OBJECT) && jsonNode
						.get(Constant.RESPONSE).get("errors").get("code").asText().equalsIgnoreCase("7412")) {
					return new CommonResponse(HttpStatus.BAD_REQUEST,
							new Response(Constant.INVITE_USER_RESPONSE, "Please Upgrade to add more Employees"),
							"License not available");
				} else
					return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.INVITE_USER_RESPONSE, null),
							"Duplicate Employee ID / Email ID already exists");
			}
			String userId = jsonNode.get(Constant.RESPONSE).get(Constant.RESULT).get("pkId").asText();
			boolean userExist = adaptorUserDetailsRepository.existsByUserEmail(inviteRequest.getEmailID());
			if (userExist) {
				AdaptorUserDetails userDetail = adaptorUserDetailsRepository
						.findByUserEmail(inviteRequest.getEmailID());
				userDetail.setUserEmail(inviteRequest.getEmailID());
				AdaptorFields field = new AdaptorFields();
				field.setApplicationId(appId);
				field.setApplicationName(Constant.ZOHO_PEOPLE);
				field.setUserId(userId);
				userDetail.getFields().add(field);
				adaptorUserDetailsRepository.save(userDetail);
			} else {
				AdaptorUserDetails userDetail = new AdaptorUserDetails();
				userDetail.setUserEmail(inviteRequest.getEmailID());
				Long id = sequenceGeneratorService.generateSequence(AdaptorUserDetails.SEQUENCE_NAME);
				userDetail.setId(id);
				List<AdaptorFields> fields = new LinkedList<>();
				AdaptorFields field = new AdaptorFields();
				field.setApplicationId(appId);
				field.setApplicationName(Constant.ZOHO_PEOPLE);
				field.setUserId(userId);
				fields.add(field);
				userDetail.setFields(fields);
				adaptorUserDetailsRepository.save(userDetail);
			}
			return new CommonResponse(HttpStatus.OK,
					new Response(Constant.INVITE_USER_RESPONSE, "User with " + userId + " invited"),
					"User Invited successfully");

		} catch (HttpClientErrorException.Unauthorized e) {
			e.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.INVITE_USER_RESPONSE, null),
					Constant.INVALID_OAUTH_TOKEN);
		} catch (HttpClientErrorException.BadRequest e) {
			e.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.INVITE_USER_RESPONSE, null),
					"Invalid data");

		}
	}

	@Override
	public CommonResponse getAllUsers(String appId) throws IOException {

		AdaptorDetails applicationDetails = adaptorRepository.findByApplicationId(appId);
		String token = getToken(appId);
		if (applicationDetails == null || token == null) {
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.INVALID_ADAPTOR_APPLICATION, null),
					Constant.APPLICATION_NOT_CONNECTED);
		}

		String url = "https://people.zoho.in/people/api/forms/employee/getRecords";
		HttpHeaders headers = new HttpHeaders();
		String inviteUser = "{searchField: EmailID, searchOperator: Is, searchText: usertalent8@gmail.com\r\n" + "}";
		headers.set(Constant.AUTHORIZATION, Constant.BEARER + token);
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("searchParams ", inviteUser);
		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
		List<ZohoPeopleProfile> userList = new LinkedList<>();

		ResponseEntity<String> response = null;
		try {
			response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
			JSONObject jsonObject = new JSONObject(response.getBody());
			JSONObject response1 = jsonObject.getJSONObject(Constant.RESPONSE);
			JSONArray resultArray = response1.getJSONArray(Constant.RESULT);
			for (int i = 0; i < resultArray.length(); i++) {
				JSONObject item = resultArray.getJSONObject(i);
				String key = item.keys().next(); // Get the dynamic key
				if (item.get(key) instanceof JSONArray) {
					JSONArray innerArray = item.getJSONArray(key);
					for (int j = 0; j < innerArray.length(); j++) {
						JSONObject innerObject = innerArray.getJSONObject(j);
						ZohoPeopleProfile user = new ZohoPeopleProfile();
						user.setEmail(innerObject.getString("EmailID"));
						user.setZohoID(innerObject.get("Zoho_ID").toString());
						user.setFirstName(innerObject.getString("FirstName"));
						user.setStatus(innerObject.getString("Employeestatus"));
						userList.add(user);
					}
				}
			}
			return new CommonResponse(HttpStatus.OK, new Response(Constant.ORG_USER_LIST_RESPONSE, userList),
					"User list retrieved successfully");

		} catch (HttpClientErrorException.Unauthorized e) {
			e.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.ORG_USER_LIST_RESPONSE, null),
					Constant.INVALID_OAUTH_TOKEN);
		} catch (HttpClientErrorException.BadRequest e) {
			e.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.ORG_USER_LIST_RESPONSE, null),
					Constant.EXCEPTION_OCURRED);

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
		ObjectMapper objectMapper = new ObjectMapper();
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
	public CommonResponse findUserByEmail(String appId, String email) throws IOException {
		AdaptorDetails applicationDetails = adaptorRepository.findByApplicationId(appId);
		String token = getToken(appId);
		if (applicationDetails == null || token == null) {
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Invalid adaptor application", null),
					Constant.APPLICATION_NOT_CONNECTED);
		}
		String url = "https://people.zoho.in/people/api/forms/employee/getRecords";
		String inviteUser = String.format("{searchField: EmailID, searchOperator: Is, searchText:%s}", email);
		HttpHeaders headers = new HttpHeaders();
		headers.set(Constant.AUTHORIZATION, Constant.BEARER + token);
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("searchParams", inviteUser);
		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

		ResponseEntity<String> response = null;
		try {
			response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
			ObjectMapper objectMapper = new ObjectMapper();
			if (response.getBody().contains(Constant.ERROR_KEY)) {
				ZohoPeopleErrorResponse errorResponse = objectMapper.readValue(response.getBody(),
						ZohoPeopleErrorResponse.class);
				return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.TOKEN_RESPONSE, null),
						errorResponse.getError());
			}
			JSONObject jsonObject = new JSONObject(response.getBody());
			JSONObject response1 = jsonObject.getJSONObject(Constant.RESPONSE);
			JSONArray resultArray = response1.getJSONArray(Constant.RESULT);
			ZohoPeopleProfile profile = new ZohoPeopleProfile();
			for (int i = 0; i < resultArray.length(); i++) {
				JSONObject item = resultArray.getJSONObject(i);
				String key = item.keys().next();
				JSONArray innerArray = item.getJSONArray(key);
				JSONObject innerObject = innerArray.getJSONObject(0);
				profile.setZohoID(innerObject.get("Zoho_ID").toString());
				profile.setEmail(innerObject.getString("EmailID"));
				profile.setFirstName(innerObject.getString("FirstName"));
				profile.setStatus(innerObject.getString("Employeestatus"));
			}
			return new CommonResponse(HttpStatus.OK, new Response(Constant.ORG_USER_LIST_RESPONSE, profile),
					"User list retrieved successfully");

		} catch (HttpClientErrorException.Unauthorized e) {
			e.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.ORG_USER_LIST_RESPONSE, null),
					Constant.INVALID_OAUTH_TOKEN);
		} catch (HttpClientErrorException.BadRequest e) {
			e.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.ORG_USER_LIST_RESPONSE, null),
					Constant.EXCEPTION_OCURRED);

		}
	}

	@Override
	public CommonResponse revokeLicense(String appId, String userEmail) throws IOException {
		AdaptorDetails applicationDetails = adaptorRepository.findByApplicationId(appId);
		String token = getToken(appId);
		if (applicationDetails == null || token == null) {
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.INVALID_ADAPTOR_APPLICATION, null),
					Constant.APPLICATION_NOT_CONNECTED);
		}

		String zohoID = null;

		if (adaptorUserDetailsRepository.existsByUserEmail(userEmail)) {
			AdaptorUserDetails userDetails = adaptorUserDetailsRepository.findByUserEmail(userEmail);
			int index = 0;
			for (int i = 0; i < userDetails.getFields().size(); i++) {
				AdaptorFields field = userDetails.getFields().get(i);
				if (field.getApplicationName().equalsIgnoreCase(Constant.ZOHO_PEOPLE)
						&& field.getApplicationId().equalsIgnoreCase(appId)) {
					zohoID = field.getUserId();
					index = i;
					break;
				}
			}
			userDetails.getFields().remove(index);
			adaptorUserDetailsRepository.save(userDetails);
		}
		if (zohoID == null) {
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Revoke License response", null),
					"User doesn't have a license in Zoho People");
		}

		CommonResponse profileResponse = findUserByEmail(appId, userEmail);
		ObjectMapper objectMapper = new ObjectMapper();
//		JsonNode json = 
		ZohoPeopleProfile userProfile = objectMapper.readValue(
				objectMapper.writeValueAsString(profileResponse.getResponse().getData()), ZohoPeopleProfile.class);
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<MultiValueMap<String, String>> request;
		String url;
		if (!userProfile.getStatus().equalsIgnoreCase("Active")) {
			url = "https://people.zoho.in/api/deleteRecords?formLinkName=employee&recordIds=" + userProfile.getZohoID();
			headers.set(Constant.AUTHORIZATION, Constant.BEARER + token);
			MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
			request = new HttpEntity<>(map, headers);
		} else {
			url = "https://people.zoho.in/people/api/forms/json/employee/updateRecord";
			headers.set(Constant.AUTHORIZATION, Constant.BEARER + token);
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
			map.add("inputData", "{Employeestatus:'Resigned'}");
			map.add("recordId", zohoID);
			request = new HttpEntity<>(map, headers);
		}
		ResponseEntity<String> response = null;
		try {
			response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
			JsonNode jsonNode = objectMapper.readTree(response.getBody());
			if (response.getBody().contains(Constant.ERROR_KEY)) {
				ZohoPeopleErrorResponse errorResponse = objectMapper.readValue(response.getBody(),
						ZohoPeopleErrorResponse.class);
				return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.TOKEN_RESPONSE, null),
						errorResponse.getError());
			}
			return new CommonResponse(HttpStatus.OK,
					new Response("Revoke license response", jsonNode.get(Constant.RESPONSE).get(Constant.RESULT)),
					"User license revoked successfully");

		} catch (HttpClientErrorException.Unauthorized e) {
			e.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.ORG_USER_LIST_RESPONSE, null),
					Constant.INVALID_OAUTH_TOKEN);
		} catch (HttpClientErrorException.BadRequest e) {
			e.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.ORG_USER_LIST_RESPONSE, null),
					Constant.EXCEPTION_OCURRED);

		}
	}
}
