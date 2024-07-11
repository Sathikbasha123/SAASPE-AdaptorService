package com.saaspe.Adaptor.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.saaspe.Adaptor.Advice.CommonResponse;
import com.saaspe.Adaptor.Advice.DataValidationException;
import com.saaspe.Adaptor.Model.JiraCreateUserRequest;
import com.saaspe.Adaptor.Service.JiraService;

@RestController
@RequestMapping("/jira")
public class JiraController {

	@Autowired
	private JiraService jiraService;
	
	@PostMapping("/createUser")
	public ResponseEntity<CommonResponse> createUser(@RequestBody JiraCreateUserRequest jiraCreateUserRequest,@RequestParam String appId) {
		CommonResponse response = jiraService.createUser(jiraCreateUserRequest,appId);
		return new ResponseEntity<>(response, response.getStatus());
	}
	
	@PostMapping("/addUserToGroup")
	public ResponseEntity<CommonResponse> addUserToGroup(@RequestParam String productName,
			@RequestBody String accountId,@RequestParam String appId) {
		CommonResponse response = jiraService.addUserToGroup(productName, accountId,appId);
		return new ResponseEntity<>(response, response.getStatus());
	}

	@GetMapping("/getAllUsers")
	public ResponseEntity<CommonResponse> getAllUsers(@RequestParam String appId) throws DataValidationException {
		CommonResponse response = jiraService.getAllUsers(appId);
		return new ResponseEntity<>(response, response.getStatus());
	}
	
	@DeleteMapping("/removeUserFromGroup")
	public ResponseEntity<CommonResponse> removeUserFromGroup(@RequestParam String accountId,@RequestParam String appId) {
		CommonResponse response = jiraService.removeUserFromGroup(accountId,appId);
		return new ResponseEntity<>(response, response.getStatus());
	}

	@GetMapping("/getAuditRecords")
	public ResponseEntity<CommonResponse> getAuditRecords(@RequestParam String appId) {
		CommonResponse response = jiraService.getAuditRecords(appId);
		return new ResponseEntity<>(response, response.getStatus());
	}

	@GetMapping("/getInstanceLicense")
	public ResponseEntity<CommonResponse> getInstanceLicense(@RequestParam String appId) {
		CommonResponse response = jiraService.getInstanceLicense(appId);
		return new ResponseEntity<>(response, response.getStatus());
	}



	@DeleteMapping("/deleteUser")
	public ResponseEntity<CommonResponse> deleteUser(@RequestParam String accountId,@RequestParam String appId) {
		CommonResponse response = jiraService.deleteUser(accountId,appId);
		return new ResponseEntity<>(response, response.getStatus());
	}

	@GetMapping("/getAllLicenseDetails")
	public ResponseEntity<CommonResponse> getAllLicenseDetails(@RequestParam String appId) {
		CommonResponse response = jiraService.getAllLicenseDetails(appId);
		return new ResponseEntity<>(response, response.getStatus());
	}

	@GetMapping("/getLicenseDetails/{key}")
	public ResponseEntity<CommonResponse> getLicenseDetails(@PathVariable String key,@RequestParam String appId) {
		CommonResponse response = jiraService.getLicenseDetails(key,appId);
		return new ResponseEntity<>(response, response.getStatus());
	}

	@GetMapping("/getAllAppDetail")
	public ResponseEntity<CommonResponse> getAllAppDetail() {
		CommonResponse response = jiraService.getAllAppDetail();
		return new ResponseEntity<>(response, response.getStatus());
	}

	@GetMapping("/getAppDetail/{addonKey}")
	public ResponseEntity<CommonResponse> getAppDetail(@PathVariable String addonKey) {
		CommonResponse response = jiraService.getAppDetail(addonKey);
		return new ResponseEntity<>(response, response.getStatus());
	}

	@GetMapping("/getUserEmail")
	public ResponseEntity<CommonResponse> getUserEmail(@RequestParam String accountId,@RequestParam String appId) {
		CommonResponse response = jiraService.getUserEmail(accountId,appId);
		return new ResponseEntity<>(response, response.getStatus());
	}

	@GetMapping("/getUserGroups")
	public ResponseEntity<CommonResponse> getUserGroups(@RequestParam String accountId,@RequestParam String appId) {
		CommonResponse response = jiraService.getUserGroups(accountId,appId);
		return new ResponseEntity<>(response, response.getStatus());
	}

	@GetMapping("/getAllGroups")
	public ResponseEntity<CommonResponse> getAllGroups(@RequestParam String appId) {
		CommonResponse response = jiraService.getAllGroups(appId);
		return new ResponseEntity<>(response, response.getStatus());
	}

	@GetMapping("/getGroupMembers")
	public ResponseEntity<CommonResponse> getGroupMembers(@RequestParam String groupName,@RequestParam String appId) {
		CommonResponse response = jiraService.getGroupMembers(groupName,appId);
		return new ResponseEntity<>(response, response.getStatus());
	}


	@GetMapping("/getUserByAccountId")
	public ResponseEntity<CommonResponse> getUserByAccountId(@RequestParam String accountId,@RequestParam String appId) {
		CommonResponse response = jiraService.getUserByAccountId(accountId,appId);
		return new ResponseEntity<>(response, response.getStatus());
	}
}