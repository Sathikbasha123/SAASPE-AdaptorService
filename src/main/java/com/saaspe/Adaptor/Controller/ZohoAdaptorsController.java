package com.saaspe.Adaptor.Controller;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.saaspe.Adaptor.Advice.CommonResponse;
import com.saaspe.Adaptor.Advice.DataValidationException;
import com.saaspe.Adaptor.Advice.Response;
import com.saaspe.Adaptor.Model.CommonZohoCRMRequest;
import com.saaspe.Adaptor.Service.ZohoAdaptorsService;

@RestController
@RequestMapping("/zohocrm")
public class ZohoAdaptorsController {

	@Autowired
	private ZohoAdaptorsService adaptorsService;

	@GetMapping("/oauth/getGrantToken")
	public void getGrantToken(HttpServletResponse response, @RequestParam("appId") String applicationId)
			throws DataValidationException {
		try {
			adaptorsService.getGrantToken(response, applicationId);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@PostMapping("/oauth/getAccessToken")
	public ResponseEntity<CommonResponse> getaccessToken(@RequestParam("appId") String appId,
			@RequestParam("code") String code) throws JsonProcessingException {

		CommonResponse commonResponse = new CommonResponse();
		try {
			commonResponse = adaptorsService.getaccessToken(appId, code);
		} catch (Exception e) {
			e.printStackTrace();
			commonResponse.setStatus(HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<>(commonResponse, commonResponse.getStatus());
	}

	@PostMapping("/oauth/getRefreshToken")
	public ResponseEntity<CommonResponse> generateToken(@RequestParam("appId") String appId)
			throws JsonProcessingException {
		CommonResponse commonResponse = new CommonResponse();
		try {
			commonResponse = adaptorsService.generateRefreshToken(appId);
		} catch (Exception e) {
			e.printStackTrace();
			commonResponse.setStatus(HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<>(commonResponse, commonResponse.getStatus());
	}

	@PostMapping("/user/addUser")
	public ResponseEntity<CommonResponse> addUser(@RequestParam("appId") String appId,
			@RequestBody CommonZohoCRMRequest request) {
		CommonResponse commonResponse = new CommonResponse();
		Response r = new Response();
		try {
			commonResponse = adaptorsService.addUserToCRM(appId, request);
		} catch (DataValidationException e) {
			commonResponse.setStatus(HttpStatus.BAD_REQUEST);
			r.setData(e.getMessage());
			commonResponse.setResponse(r);
		} catch (Exception e) {
			commonResponse.setStatus(HttpStatus.BAD_REQUEST);
			r.setData(e.getMessage());
			commonResponse.setResponse(r);
			e.printStackTrace();
		}
		return new ResponseEntity<>(commonResponse, commonResponse.getStatus());

	}

	@GetMapping("/user/getUser/{userId}")
	public ResponseEntity<CommonResponse> getUserById(@RequestParam("appId") String appId,
			@PathVariable String userId) {
		CommonResponse commonResponse = new CommonResponse();
		Response response = new Response();
		try {
			commonResponse = adaptorsService.getUserFromCRMById(appId, userId);
		} catch (Exception e) {
			commonResponse.setStatus(HttpStatus.BAD_REQUEST);
			response.setAction("Exception in get profiles method");
			commonResponse.setMessage("Exception in get profiles method");
			commonResponse.setResponse(response);
			e.printStackTrace();
		}
		return new ResponseEntity<>(commonResponse, commonResponse.getStatus());

	}

	@GetMapping("/user/getOrganizationUser")
	public ResponseEntity<CommonResponse> getUser(@RequestParam("appId") String appId,
			@RequestParam("type") String userType) {
		CommonResponse commonResponse = new CommonResponse();
		Response response = new Response();
		try {
			commonResponse = adaptorsService.getUserFromCRM(appId, userType);
		} catch (Exception e) {
			commonResponse.setStatus(HttpStatus.BAD_REQUEST);
			response.setAction("Exception in get user method");
			commonResponse.setMessage("Exception in get user method");
			commonResponse.setResponse(response);
			e.printStackTrace();
		}
		return new ResponseEntity<>(commonResponse, commonResponse.getStatus());

	}

	@PutMapping("/user/updateUser")
	public ResponseEntity<CommonResponse> updateUser(@RequestParam("appId") String appId,
			@RequestBody CommonZohoCRMRequest request) {
		CommonResponse commonResponse = new CommonResponse();
		Response response = new Response();
		try {
			commonResponse = adaptorsService.updateUserInCRM(appId, request);
		} catch (Exception e) {
			commonResponse.setStatus(HttpStatus.BAD_REQUEST);
			response.setAction("Exception in user update method");
			commonResponse.setMessage("Exception in user update method");
			commonResponse.setResponse(response);
			e.printStackTrace();
		}
		return new ResponseEntity<>(commonResponse, commonResponse.getStatus());

	}

	@DeleteMapping("/user/deleteUser/{userId}")
	public ResponseEntity<CommonResponse> updateUser(@RequestParam("appId") String appId, @PathVariable String userId) {
		CommonResponse commonResponse = new CommonResponse();
		Response response = new Response();
		try {
			commonResponse = adaptorsService.deleteUserInCRM(appId, userId);
		} catch (Exception e) {
			commonResponse.setStatus(HttpStatus.BAD_REQUEST);
			response.setAction("Exception in delete user method");
			commonResponse.setMessage("Exception in delete user method");
			commonResponse.setResponse(response);
			e.printStackTrace();
		}
		return new ResponseEntity<>(commonResponse, commonResponse.getStatus());

	}

	@GetMapping("/organization/getOrganization")
	public ResponseEntity<CommonResponse> getOrganization(@RequestParam("appId") String appId) {
		CommonResponse commonResponse = new CommonResponse();
		Response response = new Response();
		try {
			commonResponse = adaptorsService.getOrganizationInCRM(appId);
		} catch (Exception e) {

			commonResponse.setStatus(HttpStatus.BAD_REQUEST);
			response.setAction("Exception in get organization details method");
			commonResponse.setMessage("Exception in get organization details method");
			commonResponse.setResponse(response);
			e.printStackTrace();
		}
		return new ResponseEntity<>(commonResponse, commonResponse.getStatus());
	}

	@GetMapping("/user/profiles")
	public ResponseEntity<CommonResponse> getProfiles(@RequestParam("appId") String appId) {
		CommonResponse commonResponse = new CommonResponse();
		Response response = new Response();
		try {
			commonResponse = adaptorsService.getUserProfiles(appId);
		} catch (Exception e) {
			commonResponse.setStatus(HttpStatus.BAD_REQUEST);
			response.setAction("Exception in get user profiles method");
			commonResponse.setMessage("Exception in get user profiles method");
			commonResponse.setResponse(response);
			e.printStackTrace();
		}
		return new ResponseEntity<>(commonResponse, commonResponse.getStatus());
	}

	@GetMapping("/user/roles")
	public ResponseEntity<CommonResponse> getRoles(@RequestParam("appId") String appId) {
		CommonResponse commonResponse = new CommonResponse();
		Response response = new Response();
		try {
			commonResponse = adaptorsService.getUserRoles(appId);
		} catch (Exception e) {
			commonResponse.setStatus(HttpStatus.BAD_REQUEST);
			response.setAction("Exception in get roles method");
			commonResponse.setMessage("Exception in get roles method");
			commonResponse.setResponse(response);
			e.printStackTrace();
		}
		return new ResponseEntity<>(commonResponse, commonResponse.getStatus());
	}

	@GetMapping("/organization/getLicenseDetails")
	public ResponseEntity<CommonResponse> getLicense(@RequestParam("appId") String appId) {
		CommonResponse commonResponse = new CommonResponse();
		try {
			commonResponse = adaptorsService.getLicenseDetails(appId);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Exception in getLicense method", null), "Exception in getLicense method"),
					HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<>(commonResponse, commonResponse.getStatus());
	}

	@GetMapping("/user/getUserId")
	public ResponseEntity<CommonResponse> getUserId(@RequestParam("email") String email,
			@RequestParam("userType") String userType, @RequestParam("appId") String appId) {
		CommonResponse commonResponse = new CommonResponse();
		try {
			commonResponse = adaptorsService.getUserId(email, userType, appId);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Exception in getUserId method", null), "Exception in getUserId method"),
					HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<>(commonResponse, commonResponse.getStatus());

	}

	@GetMapping("/user/createUser")
	public ResponseEntity<CommonResponse> constructURL(@RequestParam("appId") String appId) throws IOException {
		CommonResponse commonResponse = new CommonResponse();
		try {
			commonResponse = adaptorsService.constructURL(appId);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Exception in createUser method", null), "Exception in createUser method"),
					HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<>(commonResponse, commonResponse.getStatus());

	}
}