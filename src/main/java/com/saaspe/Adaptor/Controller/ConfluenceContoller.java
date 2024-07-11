package com.saaspe.Adaptor.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.DeleteMapping;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.saaspe.Adaptor.Advice.CommonResponse;
import com.saaspe.Adaptor.Model.ConfluenceCreateUser;
import com.saaspe.Adaptor.Service.ConfluenceService;

@RestController
@RequestMapping("/confluence")
public class ConfluenceContoller {

	@Autowired
	private ConfluenceService confluenceService;
	
	@PostMapping("/createUser")
	public ResponseEntity<CommonResponse> createUser(@RequestBody ConfluenceCreateUser confluenceCreateUser,@RequestParam String appId) {
		CommonResponse response = new CommonResponse();
		try {
	    response = confluenceService.createUser(confluenceCreateUser,appId);
		return new ResponseEntity<>(response, response.getStatus());
		}
		catch(Exception e){
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}	
	}

	@GetMapping("/getUserList")
	public ResponseEntity<CommonResponse> getallUser(@RequestParam String appId){
	CommonResponse response = new CommonResponse();
	try {
		response=confluenceService.getUserList(appId);
		return new ResponseEntity<>(response, response.getStatus());
	}
	catch(Exception e){
		e.printStackTrace();
		return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
	}
	}

	@DeleteMapping("/deleteUser")
	public ResponseEntity<CommonResponse> deleteUsers(@RequestParam String accountId,@RequestParam String appId){
	CommonResponse response = new CommonResponse();
	try {	
		response = confluenceService.deleteUser(accountId,appId);
		return new ResponseEntity<>(response, response.getStatus());
	}
	catch(Exception e) {
		e.printStackTrace();
		return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);	
	}
}

	
}


