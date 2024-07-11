package com.saaspe.Adaptor.Controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intuit.ipp.data.Error;
import com.intuit.ipp.data.Fault;
import com.intuit.ipp.exception.FMSException;
import com.intuit.oauth2.exception.InvalidRequestException;
import com.intuit.oauth2.exception.OAuthException;
import com.saaspe.Adaptor.Advice.CommonResponse;
import com.saaspe.Adaptor.Advice.Response;
import com.saaspe.Adaptor.Model.QuickBooksUserRequest;
import com.saaspe.Adaptor.Model.QuickBooksUsers;
import com.saaspe.Adaptor.Service.QuickBooksService;

@Controller
@RequestMapping("/v1/quickBooks/company")
public class QuickBooksController {

	@Autowired
	private QuickBooksService service;

	@GetMapping("/authCodeUrl")
	public ResponseEntity<CommonResponse> getUrl(@RequestParam String appId, @RequestParam String redirectUri) {
		CommonResponse response = new CommonResponse();
		try {
			response = service.getAuthoriztionCode(appId, redirectUri);
		} catch (InvalidRequestException e) {
			e.printStackTrace();
			response.setMessage("Exception ocurred in url creation");
			response.setResponse(new Response(e.getErrorMessage(), null));
			response.setStatus(HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<>(response, response.getStatus());
	}

	@GetMapping("/accessToken")
	public ResponseEntity<CommonResponse> getAccessToken(@RequestParam String appId, @RequestParam String authCode,
			@RequestParam String realmId,@RequestParam Long uniqueId) {
		CommonResponse response = new CommonResponse();
		try {
			response = service.getAccessToken(appId, authCode, realmId,uniqueId);
		} catch (OAuthException e) {
			e.printStackTrace();
			response.setMessage("Exception ocurred in fetching Access Token");
			response.setResponse(new Response(e.getErrorMessage(), null));
			response.setStatus(HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<>(response, response.getStatus());
	}

	@GetMapping("/getUsers")
	public ResponseEntity<CommonResponse> getUsers(@RequestParam String appId) {
		CommonResponse response = new CommonResponse();
		try {
			response = service.getUsers(appId);
		} catch (FMSException e) {
			e.printStackTrace();
			List<Error> list = e.getErrorList();
			Fault fault = new Fault();
			fault.setError(list);
			response.setMessage("Exception ocurred in fetching users details");
			response.setResponse(new Response(null, list));
			response.setStatus(HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<>(response, response.getStatus());
	}

	@GetMapping("/getInfo")
	public ResponseEntity<CommonResponse> getInfo(@RequestParam String appId) {
		CommonResponse response = new CommonResponse();
		try {
			response = service.getInfo(appId);
		} catch (FMSException e) {
			e.printStackTrace();
			List<Error> list = e.getErrorList();
			Fault fault = new Fault();
			fault.setError(list);
			response.setMessage("Exception ocurred in fetching company details");
			response.setResponse(new Response(null, list));
			response.setStatus(HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<>(response, response.getStatus());
	}

	@PostMapping("/addUser")
	public ResponseEntity<CommonResponse> addUser(@RequestParam String appId,
			@RequestBody QuickBooksUserRequest userRequest) {
		CommonResponse response = new CommonResponse();
		try {
			response = service.addUsers(appId, userRequest);
		} catch (FMSException e) {
			e.printStackTrace();
			List<Error> list = e.getErrorList();
			Fault fault = new Fault();
			fault.setError(list);
			response.setMessage("Exception ocurred in adding user");
			response.setResponse(new Response(null, fault));
			response.setStatus(HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<>(response, response.getStatus());
	}

	@GetMapping("/licenseCount")
	public ResponseEntity<CommonResponse> getLicenseCount(@RequestParam String appId) {
		CommonResponse response = new CommonResponse();
		try {
			response = service.getLicenseCount(appId);
		} catch (FMSException e) {
			e.printStackTrace();
			List<Error> list = e.getErrorList();
			Fault fault = new Fault();
			fault.setError(list);
			response.setMessage("Exception ocurred in fetching license count");
			response.setResponse(new Response(null, list));
			response.setStatus(HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<>(response, response.getStatus());
	}

	@PostMapping("/deleteUser")
	public ResponseEntity<CommonResponse> deleteUser(@RequestParam String appId, @RequestParam String userEmail)
			throws IOException {
		CommonResponse response = new CommonResponse();
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			CommonResponse quickbooksUsersResponse = service.getUsers(appId);
			List<QuickBooksUsers> quickbookUsers = null;
			quickbookUsers = objectMapper.readValue(
					objectMapper.writeValueAsString(quickbooksUsersResponse.getResponse().getData()),
					new TypeReference<List<QuickBooksUsers>>() {
					});
			for (QuickBooksUsers quickbookUser : quickbookUsers) {
				if (quickbookUser.getUserEmail().equals(userEmail)) {
					response = service.deleteUser(appId, quickbookUser.getUserId());
				}
			}
		} catch (FMSException e) {
			e.printStackTrace();
			List<Error> list = e.getErrorList();
			Fault fault = new Fault();
			fault.setError(list);
			response.setMessage("Exception ocurred in deleting the user");
			response.setResponse(new Response(null, list));
			response.setStatus(HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<>(response, response.getStatus());
	}

	@GetMapping("/userDetail")
	public ResponseEntity<CommonResponse> userDetailsByEmail(@RequestParam String appId,
			@RequestParam String userEmail) {
		CommonResponse response = new CommonResponse();
		try {
			response = service.userDetailsByEmail(appId, userEmail);
		} catch (FMSException e) {
			e.printStackTrace();
			List<Error> list = e.getErrorList();
			Fault fault = new Fault();
			fault.setError(list);
			response.setMessage("Exception ocurred in fetching user details");
			response.setResponse(new Response(null, list));
			response.setStatus(HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<>(response, response.getStatus());
	}

	@GetMapping("/refreshToken")
	public ResponseEntity<CommonResponse> generateRefreshToken(@RequestParam String appId) {
		CommonResponse response = new CommonResponse();
		try {
			response = service.generateRefreshToken(appId);
		} catch (OAuthException e) {
			e.printStackTrace();
			Error error = new Error();
			error.setMessage("message=" + e.getErrorMessage() + "; errorCode=" + e.getErrorCode() + "; statusCode="
					+ e.getStatusCode());
			List<Error> list = new ArrayList<>();
			list.add(error);
			Fault fault = new Fault();
			fault.setError(list);
			response.setMessage("Exception ocurred in refresh token");
			response.setResponse(new Response(null, list));
			response.setStatus(HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<>(response, response.getStatus());
	}

}
