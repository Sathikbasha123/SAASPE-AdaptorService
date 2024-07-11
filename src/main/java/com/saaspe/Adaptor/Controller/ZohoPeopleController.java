package com.saaspe.Adaptor.Controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.saaspe.Adaptor.Advice.CommonResponse;
import com.saaspe.Adaptor.Advice.Response;
import com.saaspe.Adaptor.Model.ZohoPeopleInviteRequest;
import com.saaspe.Adaptor.Service.ZohoPeopleService;

@RestController
@RequestMapping("/zohoPeople")
public class ZohoPeopleController {

	@Autowired
	ZohoPeopleService zohoPeopleService;

	private static final Logger log = LoggerFactory.getLogger(ZohoPeopleController.class);

	@GetMapping("/getAuthUri")
	public ResponseEntity<CommonResponse> getAuthURI(@RequestParam("appId") String appId) {
		try {
			CommonResponse response = zohoPeopleService.getAuthUri(appId);
			return new ResponseEntity<>(response, response.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Exception in get Auth URI method", null), "Exception in get Auth URI method"),
					HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping("/getAccessToken")
	public ResponseEntity<CommonResponse> getAcessToken(@RequestParam("appId") String appId,
			@RequestParam String code) {
		try {
			CommonResponse response = zohoPeopleService.getAccessToken(appId, code);
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
			CommonResponse response = zohoPeopleService.generateToken(appId);
			return new ResponseEntity<>(response, response.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Exception in generate Token method", null), "Exception in generate token method"),
					HttpStatus.BAD_REQUEST);
		}
	}

	@PostMapping("/user/inviteUser")
	public ResponseEntity<CommonResponse> addUser(@RequestParam String appId,
			@RequestBody ZohoPeopleInviteRequest inviteRequest) {

		try {
			CommonResponse response = zohoPeopleService.inviteUser(appId, inviteRequest);
			return new ResponseEntity<>(response, response.getStatus());

		} catch (Exception e) {
			e.printStackTrace();
			log.info("*** Exception occured in invite user method {}", e);
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Exception in invite user method", null), "Exception in invite user method"),
					HttpStatus.BAD_REQUEST);
		}

	}

	@GetMapping("/user/list")
	public ResponseEntity<CommonResponse> getAllUsers(@RequestParam String appId) {
		try {
			CommonResponse response = zohoPeopleService.getAllUsers(appId);
			return new ResponseEntity<>(response, response.getStatus());

		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Exception in get all users method", null), "Exception in get all users method"),
					HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping("/user/findByEmail")
	public ResponseEntity<CommonResponse> findByEmail(@RequestParam String appId, @RequestParam String email) {
		try {
			CommonResponse response = zohoPeopleService.findUserByEmail(appId, email);
			return new ResponseEntity<>(response, response.getStatus());

		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Exception in find user method", null), "Exception in find user method"),
					HttpStatus.BAD_REQUEST);
		}
	}

	@PutMapping("/user/revokeAccess")
	public ResponseEntity<CommonResponse> revokeAccess(@RequestParam String appId, @RequestParam String userEmail) {

		try {
			CommonResponse response = zohoPeopleService.revokeLicense(appId, userEmail);
			return new ResponseEntity<>(response, response.getStatus());

		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Exception in revoke access method", null), "Exception in revoke access method"),
					HttpStatus.BAD_REQUEST);
		}
	}

}
