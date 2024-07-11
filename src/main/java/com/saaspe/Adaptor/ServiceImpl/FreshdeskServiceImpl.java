package com.saaspe.Adaptor.ServiceImpl;

import java.io.IOException;

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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saaspe.Adaptor.Advice.CommonResponse;
import com.saaspe.Adaptor.Advice.Response;
import com.saaspe.Adaptor.Constant.Constant;
import com.saaspe.Adaptor.Entity.AdaptorDetails;
import com.saaspe.Adaptor.Model.FreshdeskUserResponse;
import com.saaspe.Adaptor.Repository.AdaptorDetailsRepository;
import com.saaspe.Adaptor.Service.FreshdeskService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FreshdeskServiceImpl implements FreshdeskService {

	private final AdaptorDetailsRepository adaptorRepository;

	private final RestTemplate restTemplate;

	private final ObjectMapper objectMapper;

	@Override
	public CommonResponse getAccountDetails(String appId) {

		AdaptorDetails applicationDetails = adaptorRepository.findByApplicationId(appId);
		if (applicationDetails == null) {
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.INVALID_ADAPTOR_APPLICATION, null),
					Constant.APPLICATION_NOT_CONNECTED);
		}
		String url = "https://{orgName}.freshdesk.com/api/v2/account".replace("{orgName}",
				applicationDetails.getOrganizationName());
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);

		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth(applicationDetails.getApiToken(), "X");
		try {
			ResponseEntity<String> response = restTemplate.exchange(builder.toUriString(), HttpMethod.GET,
					new HttpEntity<>(headers), String.class);
			JsonNode json = objectMapper.readTree(response.getBody());
			return new CommonResponse(HttpStatus.OK, new Response("Get Account details response", json),
					"Account details retrieved successfully");
		} catch (HttpClientErrorException.Unauthorized e) {
			return new CommonResponse(HttpStatus.UNAUTHORIZED, new Response("Get account details response", null),
					"Invalid credentials");
		} catch (Exception e) {
			e.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Get Account details", null),
					"Exception in get account details method");
		}

	}

	@Override
	public CommonResponse inviteUser(String appId, String userEmail, String userName) throws IOException {
		AdaptorDetails applicationDetails = adaptorRepository.findByApplicationId(appId);
		if (applicationDetails == null) {
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.INVALID_ADAPTOR_APPLICATION, null),
					Constant.APPLICATION_NOT_CONNECTED);
		}
		String url = "https://{orgName}.freshdesk.com/api/v2/agents".replace("{orgName}",
				applicationDetails.getOrganizationName());
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
		String requestBody = "{\r\n" + "    \"email\":\"" + userEmail + "\",\r\n" + "     \"name\":\"" + userName
				+ "\",\r\n" + "    \"ticket_scope\":1,\r\n" + "    \"occasional\":false\r\n" + "}";
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBasicAuth(applicationDetails.getApiToken(), "X");
		HttpEntity<String> request = new HttpEntity<String>(requestBody, headers);
		try {
			restTemplate.exchange(builder.toUriString(), HttpMethod.POST, request, String.class);
			return new CommonResponse(HttpStatus.OK, new Response("Create user response", null),
					"Account created successfully");
		} catch (HttpClientErrorException.Unauthorized e) {
			return new CommonResponse(HttpStatus.UNAUTHORIZED, new Response("Create User response", null),
					"Invalid credentials");
		} catch (HttpClientErrorException.Conflict e) {
			JsonNode json = objectMapper.readTree(e.getResponseBodyAsString());
			if (json.get("errors").get(0).get("additional_info").has("user_id")) {
				String userId = json.get("errors").get(0).get("additional_info").get("user_id").asText();
				String makeAgentUrl = "https://{orgName}.freshdesk.com/api/v2/contacts/".replace("{orgName}",
						applicationDetails.getOrganizationName()) + userId + "/make_agent";
				UriComponentsBuilder urlBuilder = UriComponentsBuilder.fromHttpUrl(makeAgentUrl);
				try {
					System.out.println("IN URL" + urlBuilder.toUriString());
					ResponseEntity<String> response = restTemplate.exchange(urlBuilder.toUriString(), HttpMethod.PUT,
							new HttpEntity<>(headers), String.class);
					System.out.println(response.getBody());
					return new CommonResponse(HttpStatus.OK, new Response("Create user response", null),
							"Account created successfully");
				} catch (Exception ex) {
					e.printStackTrace();
					return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Create User response", null),
							"Exception in create user method");
				}
			}
			String message = json.get("errors").get(0).get("field").asText() + " "
					+ json.get("errors").get(0).get("message").asText();
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Create user response", null), message);
		} catch (HttpClientErrorException.BadRequest e) {
			JsonNode json = objectMapper.readTree(e.getResponseBodyAsString());
			String message = json.get("errors").get(0).get("field").asText() + " "
					+ json.get("errors").get(0).get("message").asText();
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Create user response", null), message);
		} catch (Exception e) {
			e.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Create User response", null),
					"Exception in creat user method");
		}

	}

	@Override
	public CommonResponse revokeUserAccess(String appId, String userEmail) {
		AdaptorDetails applicationDetails = adaptorRepository.findByApplicationId(appId);
		if (applicationDetails == null) {
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.INVALID_ADAPTOR_APPLICATION, null),
					Constant.APPLICATION_NOT_CONNECTED);
		}

		FreshdeskUserResponse user = objectMapper.convertValue(searchUser(appId, userEmail).getResponse().getData(),
				FreshdeskUserResponse.class);

		String url = "https://{orgName}.freshdesk.com/api/v2/agents/{userId}"
				.replace("{orgName}", applicationDetails.getOrganizationName()).replace("{userId}", user.getId());
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);

		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth(applicationDetails.getApiToken(), "X");
		try {
			restTemplate.exchange(builder.toUriString(), HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
			return new CommonResponse(HttpStatus.OK, new Response("Revoke User access response ", null),
					"User access revoked successfully");
		} catch (HttpClientErrorException.Unauthorized e) {
			return new CommonResponse(HttpStatus.UNAUTHORIZED, new Response("Revoke user access response", null),
					"Invalid credentials");
		} catch (HttpClientErrorException.NotFound e) {
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Revoke user access response", null),
					"User not found in the application");
		} catch (Exception e) {
			e.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Revoke user access response", null),
					"Exception in revoke user access method");
		}

	}

	@Override
	public CommonResponse searchUser(String appId, String userEmail) {
		AdaptorDetails applicationDetails = adaptorRepository.findByApplicationId(appId);
		if (applicationDetails == null) {
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.INVALID_ADAPTOR_APPLICATION, null),
					Constant.APPLICATION_NOT_CONNECTED);
		}
		String url = "https://{orgName}.freshdesk.com/api/v2/agents/autocomplete?term=".replace("{orgName}",
				applicationDetails.getOrganizationName());
		url += userEmail;
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);

		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth(applicationDetails.getApiToken(), "X");
		try {
			ResponseEntity<String> response = restTemplate.exchange(builder.toUriString(), HttpMethod.GET,
					new HttpEntity<>(headers), String.class);
			JsonNode json = objectMapper.readTree(response.getBody());
			FreshdeskUserResponse userResponse = objectMapper.convertValue(json.get(0), FreshdeskUserResponse.class);
			return new CommonResponse(HttpStatus.OK, new Response("Search User response", userResponse),
					"User details retrieved successfully");
		} catch (HttpClientErrorException.Unauthorized e) {
			return new CommonResponse(HttpStatus.UNAUTHORIZED, new Response("Search user response", null),
					"Invalid credentials");
		} catch (Exception e) {
			e.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Search user response", null),
					"Exception in search user method");
		}
	}

	@Override
	public CommonResponse getUsersList(String appId) {
		AdaptorDetails applicationDetails = adaptorRepository.findByApplicationId(appId);
		if (applicationDetails == null) {
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.INVALID_ADAPTOR_APPLICATION, null),
					Constant.APPLICATION_NOT_CONNECTED);
		}
		String url = "https://{orgName}.freshdesk.com/api/v2/agents".replace("{orgName}",
				applicationDetails.getOrganizationName());
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);

		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth(applicationDetails.getApiToken(), "X");
		try {
			ResponseEntity<String> response = restTemplate.exchange(builder.toUriString(), HttpMethod.GET,
					new HttpEntity<>(headers), String.class);
			JsonNode json = objectMapper.readTree(response.getBody());
			return new CommonResponse(HttpStatus.OK, new Response("Get users list response", json),
					"User list retrieved successfully");
		} catch (HttpClientErrorException.Unauthorized e) {
			return new CommonResponse(HttpStatus.UNAUTHORIZED, new Response("Get users list response", null),
					"Invalid credentials");
		} catch (Exception e) {
			e.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Get users list response", null),
					"Exception in get list of users method");
		}
	}

}
