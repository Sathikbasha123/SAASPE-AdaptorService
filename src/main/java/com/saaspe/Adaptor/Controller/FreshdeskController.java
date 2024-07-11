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
import com.saaspe.Adaptor.Service.FreshdeskService;

@RestController
@RequestMapping("/freshdesk")
public class FreshdeskController {

	@Autowired
	private FreshdeskService freshservice;

	@GetMapping("/account")
	public ResponseEntity<CommonResponse> getAccountDetails(@RequestParam String appId) {
		try {
			CommonResponse response = freshservice.getAccountDetails(appId);
			return new ResponseEntity<>(response, response.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Get Account details response", null), "Exception in get account details method"),
					HttpStatus.BAD_REQUEST);
		}
	}

	@PostMapping("/user/create")
	public ResponseEntity<CommonResponse> createUser(@RequestParam String appId, @RequestParam String userEmail,
			@RequestParam String userName) {
		try {
			CommonResponse response = freshservice.inviteUser(appId, userEmail, userName);
			return new ResponseEntity<>(response, response.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Create user response", null), "Exception in create user method"),
					HttpStatus.BAD_REQUEST);
		}
	}

	@DeleteMapping("/user/revokeAccess")
	public ResponseEntity<CommonResponse> revokeUserAccess(@RequestParam String appId, @RequestParam String userEmail) {
		try {
			CommonResponse response = freshservice.revokeUserAccess(appId, userEmail);
			return new ResponseEntity<>(response, response.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Revoke user access response", null), "Exception in revoke user access method"),
					HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping("/user/search")
	public ResponseEntity<CommonResponse> searchUser(@RequestParam String appId, @RequestParam String userEmail) {
		try {
			CommonResponse response = freshservice.searchUser(appId, userEmail);
			return new ResponseEntity<>(response, response.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Search user response", null), "Exception in search user method"),
					HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping("/user/list")
	public ResponseEntity<CommonResponse> userList(@RequestParam String appId) {
		try {
			CommonResponse response = freshservice.getUsersList(appId);
			return new ResponseEntity<>(response, response.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("User list response", null), "Exception in user list method"), HttpStatus.BAD_REQUEST);
		}
	}

	

}
