package com.saaspe.Adaptor.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.saaspe.Adaptor.Advice.CommonResponse;
import com.saaspe.Adaptor.Advice.Response;
import com.saaspe.Adaptor.Service.ZohoAnalyticsService;

@RestController
@RequestMapping("/zohoanalytics")
public class ZohoAnalyticsController {

	@Autowired
	ZohoAnalyticsService zohoAnalyticsService;

	@GetMapping("/getAccessToken")
	public ResponseEntity<CommonResponse> getAcessToken(@RequestParam("appId") String appId,
			@RequestParam String code) {
		try {
			CommonResponse response = zohoAnalyticsService.getAccessToken(appId, code);
			return new ResponseEntity<>(response, response.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Exception in get Access Token method", null), "Exception in get access token method"),
					HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping("/generateToken")
	public ResponseEntity<CommonResponse> generateToken(@RequestParam("appId") String appId) {
		try {
			CommonResponse response = zohoAnalyticsService.generateToken(appId);
			return new ResponseEntity<>(response, response.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Exception in generate Token method", null), "Exception in generate token method"),
					HttpStatus.BAD_REQUEST);
		}
	}

	@PostMapping("/saveOrganizationDetails")
	public ResponseEntity<CommonResponse> saveOrganizationDetails(@RequestParam("appId") String appId) {
		try {
			CommonResponse response = zohoAnalyticsService.saveOrgDetails(appId);
			return new ResponseEntity<>(response, response.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Exception in save organization details method", null),
					"Exception in save organization details method"), HttpStatus.BAD_REQUEST);
		}
	}

	@PostMapping("/user/inviteUser")
	public ResponseEntity<CommonResponse> inviteUser(@RequestParam("appId") String appId,
			@RequestParam String userEmail) {
		try {
			CommonResponse response = zohoAnalyticsService.inviteUser(appId, userEmail);
			return new ResponseEntity<>(response, response.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Exception in invite user method", null), "Exception in invite user  method"),
					HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping("/subscritpion/details")
	public ResponseEntity<CommonResponse> getSubscriptionDetails(@RequestParam("appId") String appId) {
		try {
			CommonResponse response = zohoAnalyticsService.getSubscriptionDetails(appId);
			return new ResponseEntity<>(response, response.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Exception in getSubscriptionDetails method", null),
					"Exception in getSubscriptionDetails method"), HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping("/user/list")
	public ResponseEntity<CommonResponse> getUsersList(@RequestParam("appId") String appId) {

		try {
			CommonResponse response = zohoAnalyticsService.getUsersList(appId);
			return new ResponseEntity<>(response, response.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Exception in get User List method", null), "Exception in get list of users method"),
					HttpStatus.BAD_REQUEST);
		}
	}

	@DeleteMapping("/user/revokeAccess")
	public ResponseEntity<CommonResponse> revokeAccess(@RequestParam("appId") String appId,
			@RequestParam String userEmail) {
		try {
			CommonResponse response = zohoAnalyticsService.revokeAccess(appId, userEmail);
			return new ResponseEntity<>(response, response.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Exception in invite user method", null), "Exception in invite user  method"),
					HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping("/organization/list")
	public ResponseEntity<CommonResponse> getOrganizationList(@RequestParam("appId") String appId) {
		try {
			CommonResponse response = zohoAnalyticsService.getOrganizationList(appId);
			return new ResponseEntity<>(response, response.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Exception in get organization list method", null),
					"Exception in get organization list method"), HttpStatus.BAD_REQUEST);
		}
	}
}
