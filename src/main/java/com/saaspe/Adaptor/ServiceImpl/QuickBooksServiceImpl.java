package com.saaspe.Adaptor.ServiceImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intuit.ipp.core.Context;
import com.intuit.ipp.core.ServiceType;
import com.intuit.ipp.data.CompanyInfo;
import com.intuit.ipp.data.EmailAddress;
import com.intuit.ipp.data.Employee;
import com.intuit.ipp.data.Error;
import com.intuit.ipp.data.PhysicalAddress;
import com.intuit.ipp.data.TelephoneNumber;
import com.intuit.ipp.exception.FMSException;
import com.intuit.ipp.security.OAuth2Authorizer;
import com.intuit.ipp.services.DataService;
import com.intuit.ipp.services.QueryResult;
import com.intuit.ipp.util.Config;
import com.intuit.oauth2.client.OAuth2PlatformClient;
import com.intuit.oauth2.config.Environment;
import com.intuit.oauth2.config.OAuth2Config;
import com.intuit.oauth2.config.Scope;
import com.intuit.oauth2.data.BearerTokenResponse;
import com.intuit.oauth2.exception.InvalidRequestException;
import com.intuit.oauth2.exception.OAuthException;
import com.saaspe.Adaptor.Advice.CommonResponse;
import com.saaspe.Adaptor.Advice.Response;
import com.saaspe.Adaptor.Constant.Constant;
import com.saaspe.Adaptor.Entity.AdaptorDetails;
import com.saaspe.Adaptor.Model.QuickBooksToken;
import com.saaspe.Adaptor.Model.QuickBooksUserRequest;
import com.saaspe.Adaptor.Model.QuickBooksUsers;
import com.saaspe.Adaptor.Repository.AdaptorDetailsRepository;
import com.saaspe.Adaptor.Service.QuickBooksService;

@Service
public class QuickBooksServiceImpl implements QuickBooksService {

	@Autowired
	private AdaptorDetailsRepository adaptorDetailsRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@Override
	public CommonResponse getAuthoriztionCode(String appId, String redirectUri) throws InvalidRequestException {
		CommonResponse response = new CommonResponse();
		AdaptorDetails adaptorDetails = adaptorDetailsRepository.findByApplicationId(appId);
		OAuth2Config oauth2Config = new OAuth2Config.OAuth2ConfigBuilder(adaptorDetails.getClientId(),
				adaptorDetails.getClientSecret()).callDiscoveryAPI(Environment.SANDBOX).buildConfig();
		String csrf = oauth2Config.generateCSRFToken();
		List<Scope> scopes = new ArrayList<>();
		scopes.add(Scope.Accounting);
		String url = oauth2Config.prepareUrl(scopes, redirectUri, csrf);
		response.setMessage("Authorization code URL");
		response.setResponse(new Response(null, url));
		response.setStatus(HttpStatus.OK);
		return response;
	}

	@Override
	public CommonResponse getAccessToken(String appId, String authCode, String realmId, Long uniqueId)
			throws OAuthException {
		CommonResponse response = new CommonResponse();
		AdaptorDetails adaptorDetails = adaptorDetailsRepository.findBySequenceId(uniqueId);
		OAuth2Config oauth2Config = new OAuth2Config.OAuth2ConfigBuilder(adaptorDetails.getClientId(),
				adaptorDetails.getClientSecret()).callDiscoveryAPI(Environment.SANDBOX).buildConfig();
		OAuth2PlatformClient client = new OAuth2PlatformClient(oauth2Config);
		BearerTokenResponse bearerTokenResponse = client.retrieveBearerTokens(authCode,
				adaptorDetails.getRedirectUrl());
		QuickBooksToken token = new QuickBooksToken();
		token.setAccessToken(bearerTokenResponse.getAccessToken());
		token.setRefreshToken(bearerTokenResponse.getRefreshToken());
		token.setExpiresIn(bearerTokenResponse.getExpiresIn());
		token.setXRefreshTokenExpiresIn(bearerTokenResponse.getXRefreshTokenExpiresIn());
		adaptorDetails.setApiToken(token.getRefreshToken());
		adaptorDetails.setRealmId(realmId);
		adaptorDetails.setRefreshtokenCreatedOn(new Date());
		adaptorDetailsRepository.save(adaptorDetails);
		response.setMessage("Access token retrieved");
		response.setResponse(new Response("Token details", token));
		response.setStatus(HttpStatus.OK);
		return response;
	}

	@Override
	public CommonResponse getUsers(String appId) throws FMSException {
		CommonResponse response = new CommonResponse();
		AdaptorDetails adaptorDetails = adaptorDetailsRepository.findByApplicationId(appId);
		String url = Constant.QUICKBOOKS_URL;
		Config.setProperty(Config.BASE_URL_QBO, url);
		String accessToken = getToken(appId);
		OAuth2Authorizer oauth = new OAuth2Authorizer(accessToken);
		Context context = new Context(oauth, ServiceType.QBO, adaptorDetails.getRealmId());
		DataService service = new DataService(context);
		Employee user = new Employee();
		List<Employee> users = service.findAll(user);
		List<QuickBooksUsers> userDetails = new ArrayList<>();
		for (Employee userdetail : users) {
			QuickBooksUsers userDetail = new QuickBooksUsers();
			userDetail.setUserId(userdetail.getId());
			userDetail.setUserName(userdetail.getDisplayName());
			if (userdetail.getPrimaryEmailAddr().getAddress() != null) {
				userDetail.setUserEmail(userdetail.getPrimaryEmailAddr().getAddress());
			}
			userDetails.add(userDetail);
		}
		Set<QuickBooksUsers> userinfo = new HashSet<>(userDetails);
		response.setMessage("Employee Details retrieved successfully");
		response.setResponse(new Response("Employee details", userinfo));
		response.setStatus(HttpStatus.OK);
		return response;
	}

	@Override
	public CommonResponse getInfo(String appId) throws FMSException {
		CommonResponse response = new CommonResponse();
		AdaptorDetails adaptorDetails = adaptorDetailsRepository.findByApplicationId(appId);
		String url = Constant.QUICKBOOKS_URL;
		Config.setProperty(Config.BASE_URL_QBO, url);
		String accessToken = getToken(appId);
		OAuth2Authorizer oauth = new OAuth2Authorizer(accessToken);
		Context context = new Context(oauth, ServiceType.QBO, adaptorDetails.getRealmId());
		DataService service = new DataService(context);
		String sql = "select * from companyinfo";
		QueryResult queryResult = service.executeQuery(sql);
		CompanyInfo companyInfo = new CompanyInfo();
		if (!queryResult.getEntities().isEmpty()) {
			companyInfo = (CompanyInfo) queryResult.getEntities().get(0);
		}
		response.setMessage("Company Details retrieved successfully");
		response.setResponse(new Response("Company details", companyInfo));
		response.setStatus(HttpStatus.OK);
		return response;
	}

	@Override
	public CommonResponse addUsers(String appId, QuickBooksUserRequest userRequest) throws FMSException {
		AdaptorDetails adaptorDetails = adaptorDetailsRepository.findByApplicationId(appId);
		CommonResponse response = new CommonResponse();
		String url = Constant.QUICKBOOKS_URL;
		Config.setProperty(Config.BASE_URL_QBO, url);
		String accessToken = getToken(appId);
		OAuth2Authorizer oauth = new OAuth2Authorizer(accessToken);
		Context context = new Context(oauth, ServiceType.QBO, adaptorDetails.getRealmId());
		DataService service = new DataService(context);
		String sql;
		try {
			sql = "select * from Employee where PrimaryEmailAddr = '" + userRequest.getPrimaryEmailAddr().getAddress()
					+ "'";

		} catch (NullPointerException e) {
			Error error = new Error();
			error.setMessage("Email field is empty");
			error.setDetail("User email is missing");
			List<Error> list = new ArrayList<>();
			list.add(error);
			throw new FMSException(list);
		}
		QueryResult queryResult = service.executeQuery(sql);
		if (queryResult.getMaxResults() != null) {
			Error error = new Error();
			error.setMessage("Duplicate user email");
			error.setDetail("User with email " + userRequest.getPrimaryEmailAddr().getAddress() + " already exist");
			List<Error> list = new ArrayList<>();
			list.add(error);
			throw new FMSException(list);
		} else {
			Employee employee = getEmployeeWithMandatoryFields(userRequest);
			Employee savedEmployee = service.add(employee);
			response.setMessage("User added successfully");
			response.setResponse(new Response("User added", savedEmployee));
			response.setStatus(HttpStatus.OK);
			return response;
		}

	}

	public static Employee getEmployeeWithMandatoryFields(QuickBooksUserRequest userRequest) {
		Employee employee = new Employee();
		employee.setGivenName(userRequest.getGivenName());
		employee.setFamilyName(userRequest.getFamilyName());
		employee.setDisplayName(userRequest.getGivenName() + " " + userRequest.getFamilyName());
		if (userRequest.getPrimaryEmailAddr() != null) {
			EmailAddress mail = new EmailAddress();
			mail.setAddress(userRequest.getPrimaryEmailAddr().getAddress());
			employee.setPrimaryEmailAddr(mail);
		}
		if (userRequest.getPrimaryAddress() != null) {
			PhysicalAddress address = new PhysicalAddress();
			address.setId(userRequest.getPrimaryAddress().getId());
			address.setLine1(userRequest.getPrimaryAddress().getLine1());
			address.setCity(userRequest.getPrimaryAddress().getCity());
			address.setCountrySubDivisionCode(userRequest.getPrimaryAddress().getCountrySubDivisionCode());
			address.setPostalCode(userRequest.getPrimaryAddress().getPostalCode());
			employee.setPrimaryAddr(address);
		}
		if (userRequest.getPrimaryPhone() != null) {
			TelephoneNumber phone = new TelephoneNumber();
			phone.setFreeFormNumber(userRequest.getPrimaryPhone().getFreeFormNumber());
			employee.setPrimaryPhone(phone);
		}
		return employee;
	}

	@Override
	public CommonResponse getLicenseCount(String appId) throws FMSException {
		CommonResponse response = new CommonResponse();
		AdaptorDetails adaptorDetails = adaptorDetailsRepository.findByApplicationId(appId);
		String url = Constant.QUICKBOOKS_URL;
		Config.setProperty(Config.BASE_URL_QBO, url);
		String accessToken = getToken(appId);
		OAuth2Authorizer oauth = new OAuth2Authorizer(accessToken);
		Context context = new Context(oauth, ServiceType.QBO, adaptorDetails.getRealmId());
		DataService service = new DataService(context);
		Employee user = new Employee();
		List<Employee> users = service.findAll(user);
		int count = users.size();
		response.setMessage("License count retrieved successfully");
		response.setResponse(new Response("License Count", count));
		response.setStatus(HttpStatus.OK);
		return response;
	}

	@Override
	public CommonResponse deleteUser(String appId, String id) throws FMSException {
		AdaptorDetails adaptorDetails = adaptorDetailsRepository.findByApplicationId(appId);
		CommonResponse response = new CommonResponse();
		String url = Constant.QUICKBOOKS_URL;
		Config.setProperty(Config.BASE_URL_QBO, url);
		String accessToken = getToken(appId);
		OAuth2Authorizer oauth = new OAuth2Authorizer(accessToken);
		Context context = new Context(oauth, ServiceType.QBO, adaptorDetails.getRealmId());
		DataService service = new DataService(context);
		String sql = "select * from employee where id = '" + id + "'";
		QueryResult queryResult = service.executeQuery(sql);
		if (queryResult.getMaxResults() != null) {
			Employee employee = (Employee) queryResult.getEntities().get(0);
			employee.setActive(false);
			Employee delete = service.add(employee);
			response.setMessage("User deleted successfully");
			response.setResponse(new Response("User deleted", delete));
			response.setStatus(HttpStatus.OK);
		} else {
			Error error = new Error();
			error.setMessage("Invalid user ID");
			error.setDetail("User with ID " + id + " does not exist/offboarded");
			List<Error> list = new ArrayList<>();
			list.add(error);
			throw new FMSException(list);
		}
		return response;

	}

	@Override
	public CommonResponse userDetailsByEmail(String appId, String email) throws FMSException {
		AdaptorDetails adaptorDetails = adaptorDetailsRepository.findByApplicationId(appId);
		CommonResponse response = new CommonResponse();
		String url = Constant.QUICKBOOKS_URL;
		Config.setProperty(Config.BASE_URL_QBO, url);
		String accessToken = getToken(appId);
		OAuth2Authorizer oauth = new OAuth2Authorizer(accessToken);
		Context context = new Context(oauth, ServiceType.QBO, adaptorDetails.getRealmId());
		DataService service = new DataService(context);
		String sql = "select * from Employee where primaryEmailAddr = '" + email + "'";
		QueryResult queryResult = service.executeQuery(sql);
		if (queryResult.getMaxResults() != null) {
			Employee employee = (Employee) queryResult.getEntities().get(0);
			response.setMessage("User details retrieved successfully");
			response.setResponse(new Response("User Details", employee));
			response.setStatus(HttpStatus.OK);
		} else {
			Error error = new Error();
			error.setMessage("Invalid user email");
			error.setDetail("User with email " + email + " does not exist/offboarded");
			List<Error> list = new ArrayList<>();
			list.add(error);
			throw new FMSException(list);
		}

		return response;

	}

	@Override
	public CommonResponse generateRefreshToken(String appId) throws OAuthException {
		CommonResponse response = new CommonResponse();
		AdaptorDetails adaptorDetails = adaptorDetailsRepository.findByApplicationId(appId);
		OAuth2Config oauth2Config = new OAuth2Config.OAuth2ConfigBuilder(adaptorDetails.getClientId(),
				adaptorDetails.getClientSecret()).callDiscoveryAPI(Environment.SANDBOX).buildConfig();
		OAuth2PlatformClient client = new OAuth2PlatformClient(oauth2Config);
		BearerTokenResponse bearerTokenResponse = client.refreshToken(adaptorDetails.getApiToken());
		QuickBooksToken token = new QuickBooksToken();
		token.setAccessToken(bearerTokenResponse.getAccessToken());
		token.setRefreshToken(bearerTokenResponse.getRefreshToken());
		token.setExpiresIn(bearerTokenResponse.getExpiresIn());
		token.setXRefreshTokenExpiresIn(bearerTokenResponse.getXRefreshTokenExpiresIn());
		adaptorDetails.setApiToken(token.getRefreshToken());
		adaptorDetails.setRefreshtokenCreatedOn(new Date());
		adaptorDetailsRepository.save(adaptorDetails);
		response.setMessage("Refreshed access token successfully");
		response.setResponse(new Response("Refresh token details", token));
		response.setStatus(HttpStatus.OK);
		return response;
	}

	public String getToken(String appId) throws FMSException {
		AdaptorDetails adaptorDetails = adaptorDetailsRepository.findByApplicationId(appId);
		if (adaptorDetails != null) {
			try {
				CommonResponse response = generateRefreshToken(appId);
				QuickBooksToken token = objectMapper.readValue(objectMapper.writeValueAsString(response.getResponse().getData()),
						QuickBooksToken.class);  
				adaptorDetails.setApiToken(token.getRefreshToken());
				adaptorDetailsRepository.save(adaptorDetails);
				return token.getAccessToken();
			} catch (OAuthException | IOException e) {
				e.printStackTrace();
				Error error = new Error();
				error.setMessage("Exception Occurred in Token response");
				error.setDetail("Invalid/Expired Token");
				List<Error> list = new ArrayList<>();
				list.add(error);
				throw new FMSException(list);
			}
		} else {
			Error error = new Error();
			error.setMessage("Invalid application Id");
			error.setDetail("Application details does not exist ");
			List<Error> list = new ArrayList<>();
			list.add(error);
			throw new FMSException(list);
		}
	}

}
