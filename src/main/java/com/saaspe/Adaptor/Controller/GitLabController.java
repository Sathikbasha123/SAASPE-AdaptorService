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

import com.saaspe.Adaptor.Advice.CommonResponse;
import com.saaspe.Adaptor.Advice.Response;
import com.saaspe.Adaptor.Model.GitLabDeleteUserRequest;
import com.saaspe.Adaptor.Model.GitlabUserRequest;
import com.saaspe.Adaptor.Service.GitLabService;

@RestController
@RequestMapping("/v1/gitlab")
public class GitLabController {

	@Autowired
	private GitLabService gitlabService;

	@GetMapping("/getOAuthURL")
	public ResponseEntity<CommonResponse> getAuthURL(@RequestParam String appId) {
		return new ResponseEntity<>(gitlabService.getAuthURL(appId), HttpStatus.OK);
	}

	@GetMapping("/getAccessToken")
	public ResponseEntity<CommonResponse> getAccessToken(@RequestParam String appId) throws IOException {
		CommonResponse response = gitlabService.getAccessToken(appId);
		return new ResponseEntity<>(response, response.getStatus());
	}

	@GetMapping("/getRefreshToken")
	public ResponseEntity<CommonResponse> generateToken(@RequestParam String appId) throws IOException {
		CommonResponse response = gitlabService.generateToken(appId);
		return new ResponseEntity<>(response, response.getStatus());
	}

	@GetMapping("/profile")
	public ResponseEntity<CommonResponse> getUserProfile(@RequestParam String appId) {
		try {
			CommonResponse response = gitlabService.getProfileDetails(appId);
			return new ResponseEntity<>(response, response.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Exception in get User Profile method", null), "Exception in get User profile method"),
					HttpStatus.BAD_REQUEST);

		}
	}

	@PostMapping("/addMemberToGroup")
	public ResponseEntity<CommonResponse> addMemberToGroup(@RequestBody GitlabUserRequest gitLabUserRequest,
			@RequestParam String appId) {
		try {
			CommonResponse response = gitlabService.addMemberToGroup(gitLabUserRequest, appId);
			return new ResponseEntity<>(response, response.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Exception in Add User method", null), "Exception in Add Group User method"),
					HttpStatus.BAD_REQUEST);
		}
	}

	@DeleteMapping("/revokeGroupMember")
	public ResponseEntity<CommonResponse> removeGroupMember(@RequestBody GitLabDeleteUserRequest deleteUserRequest,
			@RequestParam String appId) {
		try {
			CommonResponse response = gitlabService.removeGroupMember(deleteUserRequest, appId);
			return new ResponseEntity<>(response, response.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Exception in Remoke Group User method", null),
					"Exception in Revoke Group User method"), HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping("/getGroups")
	public ResponseEntity<CommonResponse> getUserGroups(@RequestParam String appId) {
		try {
			CommonResponse response = gitlabService.getGroups(appId);
			return new ResponseEntity<>(response, response.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Exception in User group method", null), "Exception in User Groups method"),
					HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping("/getProjects")
	public ResponseEntity<CommonResponse> getProjectsList(@RequestParam String appId) {
		try {
			CommonResponse response = gitlabService.getProjects(appId);
			return new ResponseEntity<>(response, response.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Exception in Project's list method", null), "Exception in project's list method"),
					HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping("/findUsers/{userName}")
	public ResponseEntity<CommonResponse> findUser(@PathVariable String userName, @RequestParam String appId) {
		try {
			CommonResponse response = gitlabService.findUser(userName, appId);
			return new ResponseEntity<>(response, response.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Exception in find user method", null), "Exception in find user method"),
					HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping("/getAccessRoles")
	public ResponseEntity<CommonResponse> getAccessRoleList() {
		try {
			CommonResponse response = gitlabService.getAccessRoles();
			return new ResponseEntity<>(response, response.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Exception in access role list method", null), "Exception in access role list method"),
					HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping("/getGroupSubsInfo")
	public ResponseEntity<CommonResponse> getGroupSubsInfo(@RequestParam String appId) {
		try {
			CommonResponse response = gitlabService.getSubscriptionInfo(appId);
			return new ResponseEntity<>(response, response.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Exception in Group Subscrption Info method", null),
					"Exception in Group Subscrption Info method"), HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping("/getAllUsers")
	public ResponseEntity<CommonResponse> getAllUsers(@RequestParam String appId) {
		try {
			CommonResponse response = gitlabService.getAllUsers(appId);
			return new ResponseEntity<>(response, response.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Get all users", null),
					"Exception in Get all users method"), HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping("/getResourceMembers")
	public ResponseEntity<CommonResponse> getResourceMembers(@RequestParam String appId) {
		try {
			CommonResponse response = gitlabService.getResourceMembers(appId);
			return new ResponseEntity<>(response, response.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Get resource members", null), "Exception in Get resource members method"),
					HttpStatus.BAD_REQUEST);
		}
	}

	@DeleteMapping("/deleteGroup")
	public ResponseEntity<CommonResponse> deleteGroup(@RequestParam String appId) {
		try {
			CommonResponse response = gitlabService.deleteGroup(appId);
			return new ResponseEntity<>(response, response.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Delete Gitlab Group", null), "Exception in delete gitlab group method"),
					HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping("/listGroupInvitations")
	public ResponseEntity<CommonResponse> listInvitations(@RequestParam String appId) {
		try {
			CommonResponse response = gitlabService.getInvitationList(appId);
			return new ResponseEntity<>(response, response.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Group invitation list response", null), "Exception in get invitation list method"),
					HttpStatus.BAD_REQUEST);
		}
	}

	@DeleteMapping("/deleteUserInvitation")
	public ResponseEntity<CommonResponse> deleteUserInvitation(@RequestParam String email, @RequestParam String appId) {
		try {
			CommonResponse response = gitlabService.revokeInvitation(email, appId);
			return new ResponseEntity<>(response, response.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(
					new CommonResponse(HttpStatus.BAD_REQUEST, new Response("Delete user Invitation response", null),
							"Exception in delete user invitation method"),
					HttpStatus.BAD_REQUEST);
		}
	}

}
