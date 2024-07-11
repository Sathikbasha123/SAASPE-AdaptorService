package com.saaspe.Adaptor.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.saaspe.Adaptor.Advice.CommonResponse;
import com.saaspe.Adaptor.Advice.Response;
import com.saaspe.Adaptor.Service.SalesforceService;

@RestController
@RequestMapping("/salesforce")
public class SalesforceController {

	@Autowired
	SalesforceService salaesforceService;

	@PostMapping("/generateToken")
	public ResponseEntity<CommonResponse> generateOauthToken(@RequestParam String appId) {
		try {
			CommonResponse response = salaesforceService.generateToken(appId);
			return new ResponseEntity<>(response, response.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Exception in generate Token method", null), "Exception in generate token method"),
					HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping("/user/list")
	public ResponseEntity<CommonResponse> getUsersList(@RequestParam String appId) {
		try {
			CommonResponse response = salaesforceService.getUserList(appId);
			return new ResponseEntity<>(response, response.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Exception in user list method", null), "Exception in user list method"),
					HttpStatus.BAD_REQUEST);
		}
	}

	@PostMapping("/user/create")
	public ResponseEntity<CommonResponse> createUser(@RequestParam String appId, @RequestParam String userEmail,
			@RequestParam String userName,@RequestParam String firstName) {
		try {
			CommonResponse response = salaesforceService.createUser(appId, userEmail, userName,firstName);
			return new ResponseEntity<>(response, response.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Exception in create salesforce user method", null),
					"Exception in create user method"), HttpStatus.BAD_REQUEST);
		}
	}

	@PutMapping("/user/remove")
	public ResponseEntity<CommonResponse> removeUser(@RequestParam String appId, @RequestParam String userEmail,
			@RequestParam String userName) {

		try {
			CommonResponse response = salaesforceService.removeUser(appId, userEmail, userName);
			return new ResponseEntity<>(response, response.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Exception in remove salesforce user method", null),
					"Exception in remove user method"), HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping("/licenseDetails")
	public ResponseEntity<CommonResponse> licenseDetails(@RequestParam String appId) {

		try {
			CommonResponse response = salaesforceService.getLicenseDetails(appId);
			return new ResponseEntity<>(response, response.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Exception in get license details method", null),
					"Exception in get license details method"), HttpStatus.BAD_REQUEST);
		}
	}
}
