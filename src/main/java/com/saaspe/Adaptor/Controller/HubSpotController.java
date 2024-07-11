package com.saaspe.Adaptor.Controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import com.saaspe.Adaptor.Advice.CommonResponse;
import com.saaspe.Adaptor.Advice.DataValidationException;
import com.saaspe.Adaptor.Advice.Response;
import com.saaspe.Adaptor.Constant.Constant;
import com.saaspe.Adaptor.Model.CreateHubSpotUserRequest;
import com.saaspe.Adaptor.Model.CreateHubSpotUserResponse;
import com.saaspe.Adaptor.Model.ErrorResponse;
import com.saaspe.Adaptor.Model.HubSpotSubscriptionRequest;
import com.saaspe.Adaptor.Service.HubSpotService;

@RestController
@RequestMapping("/HubSpot")
public class HubSpotController {
	@Autowired
	private HubSpotService hubSpotService;

	@GetMapping("/AuthUri")
	public ResponseEntity<CommonResponse> generateAuthUri(@RequestParam String appId) {
		CommonResponse response = new CommonResponse();
		try {
			CommonResponse authUri = hubSpotService.generateAuthUri(appId);
			response.setStatus(HttpStatus.OK);
			response.setResponse(new Response("generateAuthUri", authUri));
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (Exception e) {
			response.setStatus(HttpStatus.BAD_REQUEST);
			response.setMessage("Failed to generate auth URI: " + e.getMessage());
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping("/getToken")
	public ResponseEntity<CommonResponse> getToken(@RequestParam String appId)
			throws IOException {
		CommonResponse tokenResponse = hubSpotService.getToken(appId);
		if (tokenResponse.getStatus() == HttpStatus.OK) {
			return new ResponseEntity<>(tokenResponse, HttpStatus.OK);
		} else {
			CommonResponse response = new CommonResponse();
			response.setStatus(HttpStatus.BAD_REQUEST);
			response.setResponse(new Response("getToken", null));
			response.setMessage("Failed to get token: " + tokenResponse.getMessage());
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping("/getRefreshToken")
	public ResponseEntity<CommonResponse> getRefreshToken(@RequestParam String appId)
			throws IOException {
		CommonResponse tokenResponse = hubSpotService.getRefreshToken(appId);
		if (tokenResponse.getStatus() == HttpStatus.OK) {
			return new ResponseEntity<>(tokenResponse, HttpStatus.OK);
		} else {
			CommonResponse response = new CommonResponse();
			response.setStatus(HttpStatus.BAD_REQUEST);
			response.setResponse(new Response("getToken", null));
			response.setMessage("Failed to get refresh token: " + tokenResponse.getMessage());
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping("/getUser")
	public ResponseEntity<CommonResponse> getAllUsers(@RequestParam String appId) {
		try {
			CommonResponse response = hubSpotService.getAllUsersFromHubSpot(appId);
			return new ResponseEntity<>(response, response.getStatus());
		} catch (Exception e) {
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Exception in get User method", null), "Failed to fetch the list of users"),
					HttpStatus.BAD_REQUEST);
		}
	}

	@PostMapping("/createUser")
	public ResponseEntity<CommonResponse> createUser(@RequestBody CreateHubSpotUserRequest hubSpotUserRequest,
			@RequestParam String appId) {
		try {
			CreateHubSpotUserResponse response = hubSpotService.createUser(hubSpotUserRequest, appId);
			return new ResponseEntity<>(new CommonResponse(HttpStatus.OK, new Response("Create User", response),
					"User Created successfully"), HttpStatus.OK);
		} catch (HttpClientErrorException e) {
			if (e.getStatusCode() == HttpStatus.CONFLICT) {
				ErrorResponse errorResponse = new ErrorResponse();
				errorResponse.setStatus(Constant.CONFLICT);
				errorResponse.setMessage(Constant.CONFLICT_ERROR_OCCURRED);
				return new ResponseEntity<>(new CommonResponse(HttpStatus.CONFLICT,
						new Response("user already created", errorResponse), e.getMessage()),
						HttpStatus.CONFLICT);
			} else {
				return new ResponseEntity<>(
						new CommonResponse(HttpStatus.BAD_REQUEST,
								new Response("Exception in createUser API", e.getLocalizedMessage()), e.getMessage()),
						HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@DeleteMapping("/deleteUser")
	public ResponseEntity<CommonResponse> deleteUser(@RequestParam String appId, @RequestParam String userEmail) {
		try {
			CommonResponse response = hubSpotService.deleteUserInHubSpot(appId, userEmail);
			return new ResponseEntity<>(new CommonResponse(HttpStatus.OK, new Response("Delete User", response),
					"User deleted successfully"), HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Exception in delete user method", null), "Failed to delete the user"),
					HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping("/getLicenseCount")
	public ResponseEntity<CommonResponse> getUserCount(@RequestParam String appId) {
		try {
			CommonResponse userCount = hubSpotService.getcountUsersFromHubSpot(appId);
			Response response = new Response("Get License Count", userCount);
			return new ResponseEntity<>(
					new CommonResponse(HttpStatus.OK, response, "License count fetched successfully"), HttpStatus.OK);
		} catch (IOException e) {
			e.printStackTrace();
			Response errorResponse = new Response("Exception in get User count method", null);
			return new ResponseEntity<>(
					new CommonResponse(HttpStatus.BAD_REQUEST, errorResponse, "Failed to fetch the user count"),
					HttpStatus.BAD_REQUEST);
		} catch (DataValidationException e) {
			e.printStackTrace();
			return new ResponseEntity<>(
					new CommonResponse(HttpStatus.BAD_REQUEST, new Response(e.getMessage(), null), "Failed to fetch the user count"),
					HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping("/getLoginAuditLogs")
	public ResponseEntity<CommonResponse> getLoginAuditLogs(@RequestParam String appId,
			@RequestParam String userEmail) {
		try {
			return new ResponseEntity<>(hubSpotService.getLoginActivityHubSpot(appId, userEmail), HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Exception in get Login Audit Logs method", null), "Failed to fetch Login audit logs"),
					HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping("/getSecurityAuditLogs")
	public ResponseEntity<CommonResponse> getSecurityAuditLogs(@RequestParam String appId,
			@RequestParam String userEmail) {
		try {
			return new ResponseEntity<>(hubSpotService.getSecurityActivityHubSpot(appId, userEmail), HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Exception in get Security Audit Logs method", null),
					"Failed to fetch Security audit logs"), HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping("/getAccountInfoAuditLogs")
	public ResponseEntity<CommonResponse> getAccountInfoAuditLogs(@RequestParam String appId) {
		try {
			return new ResponseEntity<>(hubSpotService.getAccountInfoHubSpot(appId), HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Exception in get Account Info Audit Logs method", null),
					"Failed to fetch Account Info audit logs"), HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping("/{emailId}")
	public ResponseEntity<CommonResponse> getUserSubDetails(@PathVariable String emailId, @RequestParam String appId) {
		try {
			return new ResponseEntity<>(hubSpotService.getUserSubDetailsHubSpot(appId, emailId), HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Exception in get user subscription details method", null),
					"Failed to fetch user subscription details"), HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping("/Subscription")
	public ResponseEntity<CommonResponse> getAllSubDefinition(@RequestParam String appId) {
		try {
			return new ResponseEntity<>(hubSpotService.getAllSubDefinitionHubSpot(appId), HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Exception in get all subscription definition method", null),
					"Failed to fetch all the subscription definition"), HttpStatus.BAD_REQUEST);
		}
	}

	@PostMapping("/createSubscription")
	public ResponseEntity<CommonResponse> createSubscription(
			@RequestBody HubSpotSubscriptionRequest hubSpotSubscriptionRequest, @RequestParam String appId) {
		try {
			CommonResponse response = hubSpotService.createSubscription(hubSpotSubscriptionRequest, appId);
			return new ResponseEntity<>(
					new CommonResponse(HttpStatus.OK, new Response("Assign Subscription to user", response),
							"Subscription assigned to user successfully"),
					HttpStatus.OK);
		} catch (HttpClientErrorException e) {
			if (e.getStatusCode() == HttpStatus.CONFLICT) {
				ErrorResponse errorResponse = new ErrorResponse();
				errorResponse.setStatus(Constant.CONFLICT);
				errorResponse.setMessage(Constant.CONFLICT_ERROR_OCCURRED);
				return new ResponseEntity<>(new CommonResponse(HttpStatus.CONFLICT,
						new Response("Subscription already assigned to user", errorResponse),
						e.getMessage()), HttpStatus.CONFLICT);
			} else {
				return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
						new Response("Exception in createSubscription API", e.getLocalizedMessage()), e.getMessage()),
						HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/createUnSubscription")
	public ResponseEntity<CommonResponse> createUnSubscription(
			@RequestBody HubSpotSubscriptionRequest hubSpotSubscriptionRequest, @RequestParam String appId) {
		try {
			CommonResponse response = hubSpotService.createUnSubscription(hubSpotSubscriptionRequest, appId);
			return new ResponseEntity<>(new CommonResponse(HttpStatus.OK,
					new Response("De-Assign Subscription to user", response), "UnSubscribed to user successfully"),
					HttpStatus.OK);
		} catch (HttpClientErrorException e) {
			if (e.getStatusCode() == HttpStatus.CONFLICT) {
				ErrorResponse errorResponse = new ErrorResponse();
				errorResponse.setStatus(Constant.CONFLICT);
				errorResponse.setMessage(Constant.CONFLICT_ERROR_OCCURRED);
				return new ResponseEntity<>(new CommonResponse(HttpStatus.CONFLICT,
						new Response("This Subscription is already de-assigned to user", errorResponse),
						e.getMessage()), HttpStatus.CONFLICT);
			} else {
				return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
						new Response("Exception in createUnSubscription API", e.getLocalizedMessage()), e.getMessage()),
						HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}