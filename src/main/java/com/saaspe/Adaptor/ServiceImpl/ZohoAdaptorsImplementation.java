package com.saaspe.Adaptor.ServiceImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.saaspe.Adaptor.Advice.CommonResponse;
import com.saaspe.Adaptor.Advice.DataValidationException;
import com.saaspe.Adaptor.Advice.Response;
import com.saaspe.Adaptor.Constant.Constant;
import com.saaspe.Adaptor.Constant.ZohoConstant;
import com.saaspe.Adaptor.Entity.AdaptorDetails;
import com.saaspe.Adaptor.Model.CommonZohoCRMRequest;
import com.saaspe.Adaptor.Model.Details;
import com.saaspe.Adaptor.Model.ProfilesAndRoles;
import com.saaspe.Adaptor.Repository.AdaptorDetailsRepository;
import com.saaspe.Adaptor.Response.CRMUserLicenseResponse;
import com.saaspe.Adaptor.Response.CRMUserResponse;
import com.saaspe.Adaptor.Response.CRMUsersResponse;
import com.saaspe.Adaptor.Response.CommonUserResponse;
import com.saaspe.Adaptor.Response.OrganizationnResponse;
import com.saaspe.Adaptor.Response.RefreshTokenResponse;
import com.saaspe.Adaptor.Response.UserIdResponse;
import com.saaspe.Adaptor.Response.ZohoCRMErrorResponse;
import com.saaspe.Adaptor.Response.AccessTokenResponse;
import com.saaspe.Adaptor.Service.ZohoAdaptorsService;
import com.zoho.api.authenticator.OAuthToken;
import com.zoho.api.authenticator.Token;
import com.zoho.crm.api.Initializer;
import com.zoho.crm.api.ParameterMap;
import com.zoho.crm.api.dc.DataCenter.Environment;
import com.zoho.crm.api.dc.INDataCenter;
import com.zoho.crm.api.org.APIException;
import com.zoho.crm.api.org.LicenseDetails;
import com.zoho.crm.api.org.Org;
import com.zoho.crm.api.org.OrgOperations;
import com.zoho.crm.api.org.ResponseHandler;
import com.zoho.crm.api.org.ResponseWrapper;
import com.zoho.crm.api.profiles.ProfileWrapper;
import com.zoho.crm.api.profiles.ProfilesOperations;
import com.zoho.crm.api.roles.RolesOperations;
import com.zoho.crm.api.util.APIResponse;

@Service
public class ZohoAdaptorsImplementation implements ZohoAdaptorsService {

	private WebClient webClient = WebClient.create();

	@Autowired
	AdaptorDetailsRepository adapRepo;

	@Override
	public void getGrantToken(HttpServletResponse response, String appId) throws IOException, DataValidationException {

		AdaptorDetails applicationDetails = adapRepo.findByApplicationId(appId);

		String url = String.format("%s?scope=%s&client_id=%s&response_type=%s&redirect_uri=%s&access_type=%s",
				ZohoConstant.GRANT_TOKEN_URL,
				"ZohoCRM.users.ALL,ZohoCRM.modules.ALL,ZohoCRM.settings.ALL,ZohoCRM.org.ALL,ZohoCRM.features.READ",
				applicationDetails.getClientId(), "code", applicationDetails.getRedirectUrl(), "offline");

		response.sendRedirect(url);
	}

	@Override
	public CommonResponse getaccessToken(String appId, String code)
			throws JsonProcessingException, DataValidationException {

		AdaptorDetails applicationDetails = adapRepo.findByApplicationId(appId);

		String url = String.format("%s?grant_type=%s&client_id=%s&client_secret=%s&code=%s", ZohoConstant.OAUTH_TOKEN_URL,
				"authorization_code", applicationDetails.getClientId(), applicationDetails.getClientSecret(), code);
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			String response = webClient.post().uri(url).retrieve().bodyToMono(String.class).block();

			if (response!=null && response.contains("\"error\":")) {
				ZohoCRMErrorResponse errorResponse = objectMapper.readValue(response, ZohoCRMErrorResponse.class);
				return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.TOKEN_RESPONSE, null),
						errorResponse.getError());
			} else {
				AccessTokenResponse accessTokenResponse = objectMapper.readValue(response, AccessTokenResponse.class);
				applicationDetails.setApiToken(accessTokenResponse.getRefresh_token());
				adapRepo.save(applicationDetails);
				return new CommonResponse(HttpStatus.OK, new Response(Constant.TOKEN_RESPONSE, accessTokenResponse),
						"Token generation successful");
			}
		}

		catch (Exception e) {
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.TOKEN_RESPONSE, null),
					"Cannot process the request");
		}
	}

	@Override
	public CommonResponse generateRefreshToken(String appId)
			throws JsonProcessingException, DataValidationException {

		AdaptorDetails applicationDetails = adapRepo.findByApplicationId(appId);

		String url = String.format("%s?refresh_token=%s&client_id=%s&client_secret=%s&grant_type=%s",
				ZohoConstant.OAUTH_TOKEN_URL, applicationDetails.getApiToken(), applicationDetails.getClientId(),
				applicationDetails.getClientSecret(), "refresh_token");
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			String response = webClient.post().uri(url).retrieve().bodyToMono(String.class).block();
			if (response!=null && response.contains("\"error\":")) {

				ZohoCRMErrorResponse errorResponse = objectMapper.readValue(response, ZohoCRMErrorResponse.class);
				return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.TOKEN_RESPONSE, null),
						errorResponse.getError());
			} else {
				RefreshTokenResponse refreshTokenResponse = objectMapper.readValue(response,
						RefreshTokenResponse.class);

				return new CommonResponse(HttpStatus.OK, new Response(Constant.TOKEN_RESPONSE, refreshTokenResponse),
						"Token generation successful");
			}
		} catch (IOException e) {
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.TOKEN_RESPONSE, null),
					"Cannot process the request");
		}

	}

	@Override
	public CommonResponse addUserToCRM(String appId, CommonZohoCRMRequest addRequest) throws IOException, DataValidationException{

		ObjectMapper mapper = new ObjectMapper();

		CommonResponse commonResponse = generateRefreshToken(appId);
		RefreshTokenResponse tokenResponse = mapper.readValue(
				mapper.writeValueAsString(commonResponse.getResponse().getData()), RefreshTokenResponse.class);
		HttpHeaders headers = new HttpHeaders();
		headers.set(Constant.AUTHORIZATION, Constant.BEARER + tokenResponse.getAccess_token().trim());
		HttpEntity<CommonZohoCRMRequest> requestEntity = new HttpEntity<>(addRequest, headers);
		RestTemplate restTemplate = new RestTemplate();
		try {

			if (addRequest.getUsers().get(0).getFirst_name() == null || addRequest.getUsers().get(0).getEmail() == null
					|| addRequest.getUsers().get(0).getProfile() == null
					|| addRequest.getUsers().get(0).getRole() == null) {
				throw new DataValidationException(HttpStatus.BAD_REQUEST, "Mandatory fields missing", "400");

			}

			ResponseEntity<String> response = restTemplate.exchange(ZohoConstant.USER_URL, HttpMethod.POST,
					requestEntity, String.class);

			ObjectMapper objectMapper = new ObjectMapper();
			CRMUserResponse crmUserResponse = objectMapper.readValue(response.getBody(), CRMUserResponse.class);

			if (response.getStatusCode().is2xxSuccessful()) {
				return new CommonResponse(HttpStatus.OK,
						new Response(Constant.ADD_USER_RESPONSE,
								"User added with ID : " + crmUserResponse.getUsers().get(0).getDetails().getId()),
						"User added to organization successfully");
			} else {
				return new CommonResponse(HttpStatus.valueOf(response.getStatusCodeValue()),
						new Response("Add user to organization", null), response.getBody());
			}
		} catch (HttpClientErrorException.Unauthorized e) {

			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.ADD_USER_RESPONSE, null),
					"Invalid oauth token");

		} catch (HttpClientErrorException.BadRequest e) {
			e.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.ADD_USER_RESPONSE, null), "Invalid data");

		} catch (RestClientException ex) {
			ex.printStackTrace();
			return new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR, new Response(Constant.ADD_USER_RESPONSE, null),
					ex.getLocalizedMessage());
		}
	}

	@Override
	public CommonResponse getUserFromCRM(String appId, String userType) throws DataValidationException, IOException {

		ObjectMapper mapper = new ObjectMapper();
		CommonResponse commonResponse = generateRefreshToken(appId);
		RefreshTokenResponse tokenResponse = mapper.readValue(
				mapper.writeValueAsString(commonResponse.getResponse().getData()), RefreshTokenResponse.class);
		HttpHeaders headers = new HttpHeaders();
		headers.set(Constant.AUTHORIZATION, Constant.BEARER + tokenResponse.getAccess_token().trim());
		HttpEntity<String> requestEntity = new HttpEntity<>(headers);
		RestTemplate restTemplate = new RestTemplate();
		try {
			ResponseEntity<String> response = restTemplate.exchange(ZohoConstant.USER_URL + "?type=" + userType,
					HttpMethod.GET, requestEntity, String.class);

			if (response.getStatusCode().is2xxSuccessful()) {
				List<CRMUsersResponse> crmUsersResponse = mapToCRMUsersResponse(response.getBody());
				return new CommonResponse(HttpStatus.OK,
						new Response(Constant.ORG_USER_DETAILS, crmUsersResponse),
						"User details fetched successfully");
			} else {
				return new CommonResponse(HttpStatus.valueOf(response.getStatusCodeValue()),
						new Response("Get User details", null), response.getBody());
			}
		} catch (HttpClientErrorException e) {
			CommonUserResponse error = new Gson().fromJson(e.getResponseBodyAsString(), CommonUserResponse.class);
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.ORG_USER_DETAILS, null),
					error.getMessage());

		} catch (RestClientException ex) {
			ex.printStackTrace();
			return new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					new Response(Constant.ORG_USER_DETAILS, null), ex.getLocalizedMessage());
		}
	}

	@Override
	public CommonResponse getUserFromCRMById(String appId, String userId) throws DataValidationException, IOException {

		ObjectMapper mapper = new ObjectMapper();
		CommonResponse commonResponse = generateRefreshToken(appId);
		RefreshTokenResponse tokenResponse = mapper.readValue(
				mapper.writeValueAsString(commonResponse.getResponse().getData()), RefreshTokenResponse.class);

		HttpHeaders headers = new HttpHeaders();
		headers.set(Constant.AUTHORIZATION, Constant.BEARER + tokenResponse.getAccess_token().trim());
		HttpEntity<String> requestEntity = new HttpEntity<>(headers);
		RestTemplate restTemplate = new RestTemplate();
		try {
			ResponseEntity<String> response = restTemplate.exchange(ZohoConstant.USER_URL + "/" + userId, HttpMethod.GET,
					requestEntity, String.class);

			if (response.getStatusCode().toString().equals("204 NO_CONTENT")) {
				return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.USER_DETAILS_RESPONSE, null),
						"Invalid user credentias provided");
			}
			if (response.getStatusCode().is2xxSuccessful()) {
				List<CRMUsersResponse> crmUsersResponse = mapToCRMUsersResponse(response.getBody());
				return new CommonResponse(HttpStatus.OK, new Response(Constant.USER_DETAILS_RESPONSE, crmUsersResponse),
						"User details fetched successfully");
			} else {
				return new CommonResponse(HttpStatus.valueOf(response.getStatusCodeValue()),
						new Response(Constant.USER_DETAILS_RESPONSE, null), response.getBody());
			}
		} catch (HttpClientErrorException.Unauthorized e) {
			CommonUserResponse error = new Gson().fromJson(e.getResponseBodyAsString(), CommonUserResponse.class);
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.USER_DETAILS_RESPONSE, null),
					error.getMessage());

		} catch (RestClientException ex) {
			ex.printStackTrace();
			return new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR, new Response(Constant.USER_DETAILS_RESPONSE, null),
					ex.getLocalizedMessage());
		}

	}

	@Override
	public CommonResponse updateUserInCRM(String appId, CommonZohoCRMRequest addRequest) throws DataValidationException, IOException {

		ObjectMapper mapper = new ObjectMapper();
		CommonResponse commonResponse = generateRefreshToken(appId);
		RefreshTokenResponse tokenResponse = mapper.readValue(
				mapper.writeValueAsString(commonResponse.getResponse().getData()), RefreshTokenResponse.class);
		HttpHeaders headers = new HttpHeaders();
		headers.set(Constant.AUTHORIZATION, Constant.BEARER + tokenResponse.getAccess_token().trim());
		HttpEntity<CommonZohoCRMRequest> requestEntity = new HttpEntity<>(addRequest, headers);
		RestTemplate restTemplate = new RestTemplate();
		try {
			if (addRequest.getUsers().get(0).getId() == null || addRequest.getUsers().get(0).getEmail() == null
					|| addRequest.getUsers().get(0).getProfile() == null
					|| addRequest.getUsers().get(0).getRole() == null
					|| addRequest.getUsers().get(0).getFirst_name() == null) {
				throw new DataValidationException(HttpStatus.BAD_REQUEST, "Mandatory fields missing", "400");

			}
			if (!ZohoConstant.ROLES.contains(addRequest.getUsers().get(0).getRole())
					|| !ZohoConstant.PROFILES.contains(addRequest.getUsers().get(0).getProfile())) {
				throw new DataValidationException(HttpStatus.BAD_REQUEST, "Invalid Roles/Profiles provided", "400");
			}

			ResponseEntity<String> response = restTemplate.exchange(ZohoConstant.USER_URL, HttpMethod.PUT, requestEntity,
					String.class);
			ObjectMapper objectMapper = new ObjectMapper();
			CRMUserResponse crmUserResponse = objectMapper.readValue(response.getBody(), CRMUserResponse.class);

			if (response.getStatusCode().is2xxSuccessful()) {
				return new CommonResponse(HttpStatus.OK, new Response(Constant.UPDATE_USER_RESPONSE, null),
						"User successfully updated with ID : "
								+ crmUserResponse.getUsers().get(0).getDetails().getId());
			} else {
				return new CommonResponse(HttpStatus.valueOf(response.getStatusCodeValue()),
						new Response(Constant.UPDATE_USER_RESPONSE, null), response.getBody());
			}
		} catch (HttpClientErrorException.BadRequest e) {

			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.UPDATE_USER_RESPONSE, null),
					"Invalid data");

		} catch (HttpClientErrorException.Unauthorized e) {

			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.UPDATE_USER_RESPONSE, null),
					"invalid oauth token");

		} catch (RestClientException ex) {
			ex.printStackTrace();
			return new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR, new Response(Constant.UPDATE_USER_RESPONSE, null),
					ex.getLocalizedMessage());
		}

	}

	@Override
	public CommonResponse deleteUserInCRM(String appId, String userId) throws DataValidationException, IOException {

		ObjectMapper mapper = new ObjectMapper();
		CommonResponse commonResponse = generateRefreshToken(appId);
		RefreshTokenResponse tokenResponse = mapper.readValue(
				mapper.writeValueAsString(commonResponse.getResponse().getData()), RefreshTokenResponse.class);
		HttpHeaders headers = new HttpHeaders();
		headers.set(Constant.AUTHORIZATION, Constant.BEARER + tokenResponse.getAccess_token().trim());
		HttpEntity<?> requestEntity = new HttpEntity<>(headers);
		RestTemplate restTemplate = new RestTemplate();

		try {
			ResponseEntity<String> response = restTemplate.exchange(ZohoConstant.USER_URL + "/" + userId,
					HttpMethod.DELETE, requestEntity, String.class);

			if (response.getStatusCode().is2xxSuccessful()) {
				ObjectMapper objectMapper = new ObjectMapper();
				CRMUserResponse crmUserResponse = objectMapper.readValue(response.getBody(), CRMUserResponse.class);
				return new CommonResponse(HttpStatus.OK,
						new Response(Constant.DELETE_USER_RESPONSE,
								"User deleted with ID : " + crmUserResponse.getUsers().get(0).getDetails().getId()),
						"User deleted from organization successfully");
			} else {
				return new CommonResponse(HttpStatus.valueOf(response.getStatusCodeValue()),
						new Response(Constant.DELETE_USER_RESPONSE, null), response.getBody());
			}

		} catch (HttpClientErrorException.Unauthorized e) {
			CommonUserResponse error = new Gson().fromJson(e.getResponseBodyAsString(), CommonUserResponse.class);
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.DELETE_USER_RESPONSE, null),
					error.getMessage());

		} catch (RestClientException ex) {
			ex.printStackTrace();
			return new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR, new Response(Constant.DELETE_USER_RESPONSE, null),
					ex.getLocalizedMessage());
		} catch (IOException e) {
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.DELETE_USER_RESPONSE, null),
					"Invalid input.");
		}
	}

	private List<CRMUsersResponse> mapToCRMUsersResponse(String jsonResponse) throws DataValidationException {
		try {

			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode rootNode = objectMapper.readTree(jsonResponse);
			List<CRMUsersResponse> userlist = new ArrayList<>();
			for (int i = 0; i < rootNode.get("users").size(); i++) {
				JsonNode userDataNode = rootNode.get("users").get(i);
				CRMUsersResponse crmUsersResponse = new CRMUsersResponse();
				crmUsersResponse.setCountry(userDataNode.path("country").asText());
				crmUsersResponse.setId(userDataNode.path("id").asText());
				crmUsersResponse.setState(userDataNode.path("state").asText());
				crmUsersResponse.setCountry_locale(userDataNode.path("country_locale").asText());
				crmUsersResponse.setCreated_time(userDataNode.path("created_time").asText());
				crmUsersResponse.setFull_name(userDataNode.path("full_name").asText());
				crmUsersResponse.setLast_name(userDataNode.path("last_name").asText());
				crmUsersResponse.setEmail(userDataNode.path("email").asText());
				crmUsersResponse.setCategory(userDataNode.path("category").asText());
				crmUsersResponse.setProfile(new ProfilesAndRoles(userDataNode.path("profile").path("id").asText(),
						userDataNode.path("profile").path("name").asText(), null, null));
				crmUsersResponse.setRole(new ProfilesAndRoles(null, null, userDataNode.path("role").path("id").asText(),
						userDataNode.path("role").path("name").asText()));
				userlist.add(crmUsersResponse);
			}
			return userlist;
		}

		catch (Exception e) {
			throw new DataValidationException(HttpStatus.BAD_REQUEST,"Failed to map JSON Response", null);
		}

	}

	@Override
	public CommonResponse getOrganizationInCRM(String appId) {

		try {
			Environment environment = INDataCenter.PRODUCTION;

			AdaptorDetails applicationDetails = adapRepo.findByApplicationId(appId);

			Token token = new OAuthToken.Builder().clientID(applicationDetails.getClientId())
					.clientSecret(applicationDetails.getClientSecret()).refreshToken(applicationDetails.getApiToken())
					.build();
			new Initializer.Builder().environment(environment).token(token).initialize();
			OrgOperations orgOperations = new OrgOperations();
			APIResponse<ResponseHandler> response = orgOperations.getOrganization();

			if (response != null &&  (response.isExpected())) {
					ResponseHandler responseHandler = response.getObject();
					if (responseHandler instanceof ResponseWrapper) {
						ResponseWrapper responseWrapper = (ResponseWrapper) responseHandler;
						List<com.zoho.crm.api.org.Org> orgs = responseWrapper.getOrg();
						List<OrganizationnResponse> orgList = new ArrayList<>();
						for (com.zoho.crm.api.org.Org org : orgs) {
							OrganizationnResponse organizationResponse = new OrganizationnResponse();
							organizationResponse.setCountry(org.getCountry());
							organizationResponse.setCity(org.getCity());
							organizationResponse.setDescription(org.getDescription());
							organizationResponse.setCurrency(org.getCurrency());
							organizationResponse.setOrganizationId(org.getId());
							organizationResponse.setOrganizationState(org.getState());
							organizationResponse.setEmployeesCount(org.getEmployeeCount());
							organizationResponse.setZipCode(org.getZip());
							organizationResponse.setWebsite(org.getWebsite());
							organizationResponse.setPhone(org.getPhone());
							organizationResponse.setTimeZone(org.getTimeZone());
							organizationResponse.setCompanyName(org.getCompanyName());
							organizationResponse.setPrimaryEmail(org.getPrimaryEmail());
							organizationResponse.setDomainName(org.getDomainName());
							LicenseDetails licenseDetails = org.getLicenseDetails();
							if (licenseDetails != null) {
								organizationResponse
										.setLicensePurchased(licenseDetails.getUsersLicensePurchased().toString());
								organizationResponse.setPaidType(licenseDetails.getPaidType());
								organizationResponse.setTrialExpiry(licenseDetails.getTrialExpiry());
								organizationResponse.setLicensePaid(licenseDetails.getPaid());
								organizationResponse.setTrialType(licenseDetails.getTrialType());
							}
							orgList.add(organizationResponse);
						}
						return new CommonResponse(HttpStatus.OK, new Response(Constant.ORG_DETAILS_RESPONSE, orgList),
								"Organization details fetch Successfull");
					} else {
						APIException exception = (APIException) responseHandler;
						return new CommonResponse(HttpStatus.BAD_REQUEST,
								new Response(Constant.ORG_DETAILS_RESPONSE, null), exception.getMessage());
					}
				

			}
		} catch (Exception e) {
			e.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.ORG_DETAILS_RESPONSE, null),
					e.getMessage());
		}
		return null;
	}

	@Override
	public CommonResponse getUserProfiles(String appId) {
		try {
			Environment environment = INDataCenter.PRODUCTION;

			AdaptorDetails applicationDetails = adapRepo.findByApplicationId(appId);

			Token token = new OAuthToken.Builder().clientID(applicationDetails.getClientId())
					.clientSecret(applicationDetails.getClientSecret()).refreshToken(applicationDetails.getApiToken())
					.build();
			new Initializer.Builder().environment(environment).token(token).initialize();

			ProfilesOperations profilesOperations = new ProfilesOperations();
			ParameterMap paramInstance = new ParameterMap();
			APIResponse<com.zoho.crm.api.profiles.ResponseHandler> response = profilesOperations
					.getProfiles(paramInstance);
			List<ProfilesAndRoles> profileList = new ArrayList<>();
			if (response != null) {

				if (Arrays.asList(204, 304).contains(response.getStatusCode())) {
					return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.PROFILE_DETAILS_RESPONSE, null),
							"Profile details fetch unsuccessfull");
				}
				if (response.isExpected()) {
					com.zoho.crm.api.profiles.ResponseHandler responseHandler = response.getObject();
					if (responseHandler instanceof ProfileWrapper) {

						ProfileWrapper responseWrapper = (ProfileWrapper) responseHandler;
						List<com.zoho.crm.api.profiles.Profile> profiles = responseWrapper.getProfiles();

						for (com.zoho.crm.api.profiles.Profile profile : profiles) {
							ProfilesAndRoles profileDetails = new ProfilesAndRoles();
							profileDetails.setProfile(profile.getName());
							profileDetails.setProfile_id(profile.getId().toString());
							profileList.add(profileDetails);
						}
						return new CommonResponse(HttpStatus.OK, new Response(Constant.PROFILE_DETAILS_RESPONSE, profileList),
								"Profile details fetch Successfull");
					} else {
						APIException exception = (APIException) responseHandler;
						return new CommonResponse(HttpStatus.BAD_REQUEST,
								new Response(Constant.PROFILE_DETAILS_RESPONSE, null), exception.getMessage());
					}
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Get profile details", null),
					e.getMessage());

		}
		return null;
	}

	@Override
	public CommonResponse getUserRoles(String appId) {
		try {
			Environment environment = INDataCenter.PRODUCTION;

			AdaptorDetails applicationDetails = adapRepo.findByApplicationId(appId);

			Token token = new OAuthToken.Builder().clientID(applicationDetails.getClientId())
					.clientSecret(applicationDetails.getClientSecret()).refreshToken(applicationDetails.getApiToken())
					.build();
			new Initializer.Builder().environment(environment).token(token).initialize();

			RolesOperations rolesOperations = new RolesOperations();
			APIResponse<com.zoho.crm.api.roles.ResponseHandler> response = rolesOperations.getRoles();
			List<ProfilesAndRoles> roleList = new ArrayList<>();
			if (response != null) {

				if (Arrays.asList(204, 304).contains(response.getStatusCode())) {
					return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.ROLE_DETAILS_RESPONSE, null),
							"Role details fetch unsuccessfull");
				}
				if (response.isExpected()) {
					com.zoho.crm.api.roles.ResponseHandler responseHandler = response.getObject();
					if (responseHandler instanceof com.zoho.crm.api.roles.ResponseWrapper) {
						com.zoho.crm.api.roles.ResponseWrapper responseWrapper = (com.zoho.crm.api.roles.ResponseWrapper) responseHandler;
						List<com.zoho.crm.api.roles.Role> roles = responseWrapper.getRoles();
						for (com.zoho.crm.api.roles.Role role : roles) {
							ProfilesAndRoles roleDetails = new ProfilesAndRoles();
							roleDetails.setRole(role.getName());
							roleDetails.setRole_id(role.getId().toString());
							roleList.add(roleDetails);
						}
						return new CommonResponse(HttpStatus.OK, new Response(Constant.ROLE_DETAILS_RESPONSE, roleList),
								"Role details fetch Successfull");
					} else {
						APIException exception = (APIException) responseHandler;
						return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.ROLE_DETAILS_RESPONSE, null),
								exception.getMessage());
					}
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.ROLE_DETAILS_RESPONSE, null),
					e.getMessage());

		}

		return null;

	}

	@Override
	public CommonResponse getLicenseDetails(String appId) throws DataValidationException, IOException {

		ObjectMapper mapper = new ObjectMapper();
		CommonResponse commonResponse = generateRefreshToken(appId);
		RefreshTokenResponse tokenResponse = mapper.readValue(
				mapper.writeValueAsString(commonResponse.getResponse().getData()), RefreshTokenResponse.class);
		HttpHeaders headers = new HttpHeaders();
		headers.set(Constant.AUTHORIZATION, Constant.BEARER + tokenResponse.getAccess_token().trim());
		HttpEntity<String> requestEntity = new HttpEntity<>(headers);
		RestTemplate restTemplate = new RestTemplate();
		try {
			ResponseEntity<String> response = restTemplate.exchange(ZohoConstant.LICENSE_URL, HttpMethod.GET,
					requestEntity, String.class);

			if (response.getStatusCode().is2xxSuccessful()) {

				return new CommonResponse(HttpStatus.OK,
						new Response(Constant.LICENSE_DETAILS_RESPOSNE, mapToCRMLicenseResponse(response.getBody())),
						"License details fetch successful");
			} else {
				return new CommonResponse(HttpStatus.valueOf(response.getStatusCodeValue()),
						new Response(Constant.LICENSE_DETAILS_RESPOSNE, null), response.getBody());
			}
		} catch (HttpClientErrorException.Unauthorized e) {
			CommonUserResponse error = new Gson().fromJson(e.getResponseBodyAsString(), CommonUserResponse.class);
			return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.LICENSE_DETAILS_RESPOSNE, null),
					error.getMessage());

		} catch (RestClientException ex) {
			ex.printStackTrace();
			return new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR, new Response(Constant.LICENSE_DETAILS_RESPOSNE, null),
					ex.getLocalizedMessage());
		}

	}

	private CRMUserLicenseResponse mapToCRMLicenseResponse(String jsonResponse) throws DataValidationException {

		try {
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode rootNode = objectMapper.readTree(jsonResponse);
			JsonNode userDataNode = rootNode.get("__features").get(0);
			CRMUserLicenseResponse crmLicenseDetails = new CRMUserLicenseResponse();
			crmLicenseDetails
					.setTotalLicensesPurchased(userDataNode.path(Constant.DETAILS).path("limits").path(Constant.TOTAL).asInt());
			crmLicenseDetails
					.setAvailableCount(userDataNode.path(Constant.DETAILS).path("available_count").path(Constant.TOTAL).asInt());
			crmLicenseDetails.setUsedCount(userDataNode.path(Constant.DETAILS).path("used_count").path(Constant.TOTAL).asInt());
			return crmLicenseDetails;

		} catch (Exception e) {
			throw new DataValidationException(HttpStatus.BAD_REQUEST,"Failed to map JSON response", null);
		}

	}

	@Override
	public CommonResponse getUserId(String email, String userType, String appId) throws DataValidationException {
		try {
			CommonResponse commonResponse = getUserFromCRM(appId, userType);
			if (commonResponse.getStatus().is2xxSuccessful()) {

				Object res = commonResponse.getResponse().getData();
				ObjectMapper objectMapper = new ObjectMapper();
				if (res != null) {
					List<CRMUsersResponse> usersResponseList = objectMapper.readValue(
							objectMapper.writeValueAsString(res), new TypeReference<List<CRMUsersResponse>>() {
							});

					for (CRMUsersResponse user : usersResponseList) {
						if (user.getEmail().equals(email.trim())) {
							return new CommonResponse(HttpStatus.OK,
									new Response(Constant.USER_ID_RESPONSE, new UserIdResponse(user.getId().trim())),
									"User ID found successfully");
						}
					}
					return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.USER_ID_RESPONSE, null),
							"User id not found");
				} else {
					return new CommonResponse(HttpStatus.BAD_REQUEST, new Response(Constant.USER_ID_RESPONSE, null),
							"No users found with provided credentials");

				}
			} else {
				return commonResponse;
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new DataValidationException(HttpStatus.BAD_REQUEST,"Failed to map JSON response", null);
		}
	}

	@Override
	public CommonResponse constructURL(String appId) throws IOException, DataValidationException {

		StringBuilder builder = new StringBuilder(ZohoConstant.ZOHOCRM_PLUS_BASE_URL);
		CommonResponse commonResponse = generateRefreshToken(appId);
		ObjectMapper mapper = new ObjectMapper();
		String accessURL = "";
		try {
			Environment environment = INDataCenter.PRODUCTION;
			AdaptorDetails applicationDetails = adapRepo.findByApplicationId(appId);

			Token token = new OAuthToken.Builder().clientID(applicationDetails.getClientId())
					.clientSecret(applicationDetails.getClientSecret()).refreshToken(applicationDetails.getApiToken()).build();

			new Initializer.Builder().environment(environment).token(token).initialize();
			OrgOperations orgOperations = new OrgOperations();
			APIResponse<ResponseHandler> response = orgOperations.getOrganization();

			if (response != null &&  (response.isExpected())) {
					ResponseHandler responseHandler = response.getObject();
					if (responseHandler instanceof com.zoho.crm.api.org.ResponseWrapper) {

						ResponseWrapper responseWrapper = (ResponseWrapper) responseHandler;
						List<Org> orgs = responseWrapper.getOrg();
						for (com.zoho.crm.api.org.Org org : orgs) {

							accessURL = org.getWebsite().substring(org.getWebsite().lastIndexOf("/") + 1);
							builder.append(accessURL + "/index.do/cxapp/crm/");
							commonResponse = getUserId(org.getPrimaryEmail(), ZohoConstant.USER_TYPE.get(1), appId);
							Details userIdresponse = mapper.readValue(
									mapper.writeValueAsString(commonResponse.getResponse().getData()), Details.class);
							builder.append(org.getDomainName() + "/settings/users/" + userIdresponse.getId());
							return new CommonResponse(HttpStatus.OK,
									new Response("Create User Response", builder.toString()),
									"User creation successful successfully");
						}
					} else {
						APIException exception = (APIException) responseHandler;
						return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Create User Response", null),
								exception.getMessage());
					}
				
			}
		} catch (Exception e) {
			e.printStackTrace();

		}
		return commonResponse;

	}

}
