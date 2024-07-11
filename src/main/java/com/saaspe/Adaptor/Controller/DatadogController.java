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
import com.saaspe.Adaptor.Service.DatadogService;

@RestController
@RequestMapping("/Datadog")
public class DatadogController {
	
	@Autowired 
	DatadogService datadogService;
	
	
	@PostMapping("/createUser")
	public ResponseEntity<CommonResponse> createUser(@RequestParam String userEmail,@RequestParam String appId){
		CommonResponse response = new CommonResponse();
		try {
			response = datadogService.createUser(userEmail,appId);
		    return new ResponseEntity<>(response,response.getStatus());
		}
		catch(Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);	
		}
	}
	
	@GetMapping("/getUser")
	public ResponseEntity<CommonResponse> getUser(@RequestParam String appId){
		CommonResponse response = new CommonResponse();
		try {
			response = datadogService.getUser(appId);
			return new ResponseEntity<>(response, response.getStatus());
		}
		catch(Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@DeleteMapping("/disableUser")
	public ResponseEntity<CommonResponse> deleteUser(@RequestParam String appId,@RequestParam String userEmail){
		CommonResponse response = new CommonResponse();
		try {
			response=datadogService.deleteUser(appId,userEmail);
			return new ResponseEntity<>(response, response.getStatus());
		}
		catch(Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	

}
