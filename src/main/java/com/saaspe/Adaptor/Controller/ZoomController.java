package com.saaspe.Adaptor.Controller;

import java.io.IOException;

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
import org.springframework.web.client.HttpClientErrorException;

import com.saaspe.Adaptor.Advice.CommonResponse;
import com.saaspe.Adaptor.Advice.DataValidationException;
import com.saaspe.Adaptor.Advice.Response;
import com.saaspe.Adaptor.Constant.Constant;
import com.saaspe.Adaptor.Model.CreateZoomUserRequest;
import com.saaspe.Adaptor.Model.CreateZoomUserResponse;
import com.saaspe.Adaptor.Model.ErrorResponse;
import com.saaspe.Adaptor.Service.ZoomService;

@RestController
@RequestMapping("/zoom")
public class ZoomController {
	
	@Autowired
	private ZoomService zoomService;
	
	@GetMapping("/AuthUri")
	public ResponseEntity<CommonResponse> generateAuthUri(@RequestParam String appId,@RequestParam String redirectUri) {
		CommonResponse response = new CommonResponse();
		try {
			response= zoomService.generateAuthUri(appId,redirectUri);
		} catch (Exception e) {
			response.setStatus(HttpStatus.BAD_REQUEST);
			response.setMessage("Failed to generate auth URI: " + e.getMessage());
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<>(response, response.getStatus());
	}

	@GetMapping("/getToken")
	public ResponseEntity<CommonResponse> getToken(@RequestParam String appId, @RequestParam String code,
			@RequestParam Long uniqueId) {
		try {
			CommonResponse tokenResponse = zoomService.getToken(appId, code, uniqueId);
			if (tokenResponse.getStatus() == HttpStatus.OK) {
				return new ResponseEntity<>(tokenResponse, HttpStatus.OK);
			} else {
				CommonResponse response = new CommonResponse();
				response.setStatus(HttpStatus.BAD_REQUEST);
				response.setResponse(new Response("getToken", null));
				response.setMessage("Failed to get token: " + tokenResponse.getMessage());
				return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			e.printStackTrace();
			CommonResponse response = new CommonResponse();
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
			response.setResponse(new Response("getToken", null));
			response.setMessage("Failed to get token: " + e.getMessage());
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@GetMapping("/getRefreshToken")
	public ResponseEntity<CommonResponse> getRefreshToken(@RequestParam String appId) throws IOException {
		CommonResponse tokenResponse = zoomService.getRefreshToken(appId);
		if (tokenResponse.getStatus() == HttpStatus.OK) {
			return new ResponseEntity<>(tokenResponse, HttpStatus.OK);
		} else {
			CommonResponse response = new CommonResponse();
			response.setStatus(HttpStatus.BAD_REQUEST);
			response.setResponse(new Response("getRefreshToken", null));
			response.setMessage("Failed to get refresh token: " + tokenResponse.getMessage());
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		}
	}
	
	@GetMapping("/getUserList")
	public ResponseEntity<CommonResponse> getAllUsers(@RequestParam String appId) {
		try {
			CommonResponse response = zoomService.getUserList(appId);
			return new ResponseEntity<>(response, response.getStatus());
		} catch (Exception e) {
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Exception in get User method", null), "Failed to fetch the list of users"),
					HttpStatus.BAD_REQUEST);
		}
	}
	@PostMapping("/createUser")
	public ResponseEntity<CommonResponse> createUser(
	        @RequestBody CreateZoomUserRequest createZoomUserRequest, @RequestParam String appId) throws DataValidationException {
	    try {
	        CreateZoomUserResponse response = zoomService.createUser(createZoomUserRequest, appId);
	        return new ResponseEntity<>(new CommonResponse(HttpStatus.OK, new Response("Create User", response),
	                "User Created successfully"), HttpStatus.OK);
	    } catch (DataValidationException e) {
	        if (e.getMessage() != null && e.getMessage().contains("There is no license available to assign for users")) {
	            return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
	                    new Response("license is not available", null), "There is no license available to assign for users"), HttpStatus.BAD_REQUEST);
	        } else {
	            throw e; 
	        }
	    } catch (HttpClientErrorException e) {
	        if (e.getStatusCode() == HttpStatus.CONFLICT) {
	            ErrorResponse errorResponse = new ErrorResponse();
	            errorResponse.setStatus(Constant.CONFLICT);
	            errorResponse.setMessage(Constant.CONFLICT_ERROR_OCCURRED);
	            return new ResponseEntity<>(new CommonResponse(HttpStatus.CONFLICT,
	                    new Response("user already created", errorResponse), "can't able to create the user twice"),
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
			CommonResponse response = zoomService.deleteUserInZoom(appId, userEmail);
			return new ResponseEntity<>(new CommonResponse(HttpStatus.OK, new Response("Delete User", response),
					"User deleted successfully"), HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Exception in delete user method", null), "Failed to delete the user"),
					HttpStatus.BAD_REQUEST);
		}
	}
	
	@GetMapping("/getLicenseCount")
	public ResponseEntity<CommonResponse> getLicenseCount(@RequestParam String appId) {
		try {
			CommonResponse response = zoomService.getLicenseCount(appId);
			return new ResponseEntity<>(response, response.getStatus());
		} catch (Exception e) {
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Exception in get User method", null), "Failed to fetch the list of users"),
					HttpStatus.BAD_REQUEST);
		}
	}

	
}
