package com.saaspe.Adaptor.Controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
import com.saaspe.Adaptor.Model.ErrorResponse;
import com.saaspe.Adaptor.Model.Microsoft365getUserlistResponse;
import com.saaspe.Adaptor.Service.Microsoft365Service;

@RestController
@RequestMapping("/microsoft365")
public class Microsoft365Controller {

	@Autowired
	private Microsoft365Service microsoft365Service;

	@GetMapping("/AuthUri")
	public ResponseEntity<CommonResponse> generateAuthUri(@RequestParam String appId,
			@RequestParam String redirectUri) {
		CommonResponse response = new CommonResponse();
		try {
			response= microsoft365Service.generateAuthUri(appId,redirectUri);
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
			CommonResponse tokenResponse = microsoft365Service.getToken(appId, code, uniqueId);
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
		CommonResponse tokenResponse = microsoft365Service.getRefreshToken(appId);
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
			CommonResponse response = microsoft365Service.getUserList(appId);
			return new ResponseEntity<>(response, response.getStatus());
		} catch (Exception e) {
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Exception in get User method", null), "Failed to fetch the list of users"),
					HttpStatus.BAD_REQUEST);
		}
	}


	@PostMapping("/createUser")
	public ResponseEntity<CommonResponse> createUser(
			@RequestParam String userEmail, @RequestParam String appId) {
		try {
			Microsoft365getUserlistResponse response = microsoft365Service.createUser(userEmail,
					appId);
			return new ResponseEntity<>(new CommonResponse(HttpStatus.OK, new Response("Create User", response),
					"User Created successfully"), HttpStatus.OK);
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
	
	
	
	@PatchMapping("/updateUser")
	public ResponseEntity<CommonResponse> updateUser(@RequestParam String userEmail,
			@RequestBody Microsoft365getUserlistResponse microsoft365UpdateUserRequest, @RequestParam String appId) {
		try {
			Microsoft365getUserlistResponse response = microsoft365Service.updateUserByEmail(userEmail,
					microsoft365UpdateUserRequest, appId);
			return new ResponseEntity<>(new CommonResponse(HttpStatus.OK, new Response("Update User", response),
					"User Updated successfully"), HttpStatus.OK);
		} catch (HttpClientErrorException e) {
			if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
				ErrorResponse errorResponse = new ErrorResponse();
				errorResponse.setStatus("NOT_FOUND");
				errorResponse.setMessage("User not found");
				return new ResponseEntity<>(new CommonResponse(HttpStatus.NOT_FOUND,
						new Response("User not found", errorResponse), "Your custom error message"),
						HttpStatus.NOT_FOUND);
			} else {
				return new ResponseEntity<>(
						new CommonResponse(HttpStatus.BAD_REQUEST,
								new Response("Exception in updateUser API", e.getLocalizedMessage()), e.getMessage()),
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
			CommonResponse response = microsoft365Service.deleteUserInMicrosoft365(appId, userEmail);
			return new ResponseEntity<>(new CommonResponse(HttpStatus.OK, new Response("Delete User", response),
					"User deleted successfully"), HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Exception in delete user method", null), "Failed to delete the user"),
					HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping("/getSubscribedSku")
	public ResponseEntity<CommonResponse> getSubscribedSku(@RequestParam String appId) {
	    try {
	        CommonResponse response = microsoft365Service.getSubscribedSku(appId);
	        return new ResponseEntity<>(response, HttpStatus.OK);
	    } catch (Exception e) {
	        return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
	                new Response("Exception in getSubscribedSku method", null), "Failed to get the subscribed skus"),
	                HttpStatus.BAD_REQUEST);
	    }
	}

	
	@GetMapping("/getUserLicenseDetails")
	public  ResponseEntity<CommonResponse> getUserLicenseDetails(@RequestParam String appId,@RequestParam String userEmail) {
		try {
			CommonResponse response= microsoft365Service.getUserLicenseDetails(userEmail,appId);
			return new ResponseEntity<>(new CommonResponse(HttpStatus.OK, new Response("get UserLicenseDetails", response),
					"UserLicenseDetails retrieved successfully"), HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("Exception in getUserLicenseDetails  method", null), "Failed to get the UserLicenseDetails"),
					HttpStatus.BAD_REQUEST);
		}
	}


    
	@PostMapping("/assignLicense")
	public ResponseEntity<CommonResponse> assignLicense(@RequestParam String userEmail,
	        @RequestParam String productName, @RequestParam String appId) {
	    try {
	        Microsoft365getUserlistResponse response = microsoft365Service.assignLicense(userEmail, appId, productName);
	        return new ResponseEntity<>(new CommonResponse(HttpStatus.OK, new Response("Assign license", response),
	                "License Assigned successfully"), HttpStatus.OK);
	    } catch (DataValidationException e) {
	        return new ResponseEntity<>(new CommonResponse(HttpStatus.NOT_FOUND,
	                new Response("There is no licenses available to assign to the users", null), e.getMessage()), HttpStatus.NOT_FOUND);
	    } catch (HttpClientErrorException e) {
	        if (e.getStatusCode() == HttpStatus.CONFLICT) {
	            ErrorResponse errorResponse = new ErrorResponse();
	            errorResponse.setStatus(Constant.CONFLICT);
	            errorResponse.setMessage(Constant.CONFLICT_ERROR_OCCURRED);
	            return new ResponseEntity<>(new CommonResponse(HttpStatus.CONFLICT,
	                    new Response("License already assigned", errorResponse), "Failed to assign license to user"),
	                    HttpStatus.CONFLICT);
	        } else {
	            return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
	                    new Response("Exception in license assign API", e.getLocalizedMessage()), e.getMessage()),
	                    HttpStatus.BAD_REQUEST);
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	}



	@PostMapping("/UnassignLicense")
	public ResponseEntity<CommonResponse> unAssignLicense(@RequestParam String userEmail,
		 @RequestParam String appId, @RequestParam String productName) {
		try {
			Microsoft365getUserlistResponse response = microsoft365Service.unAssignLicense(userEmail, appId, productName);
			return new ResponseEntity<>(new CommonResponse(HttpStatus.OK, new Response("Unassign license", response),
					"License Unassigned successfully"), HttpStatus.OK);
		} catch (HttpClientErrorException e) {
			if (e.getStatusCode() == HttpStatus.CONFLICT) {
				ErrorResponse errorResponse = new ErrorResponse();
				errorResponse.setStatus(Constant.CONFLICT);
				errorResponse.setMessage(Constant.CONFLICT_ERROR_OCCURRED);
				return new ResponseEntity<>(new CommonResponse(HttpStatus.CONFLICT,
						new Response("License already unassigned to user", errorResponse),
						"Failed to unassign license to user"), HttpStatus.CONFLICT);
			} else {
				return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
						new Response("Exception in unassign license  API", e.getLocalizedMessage()), e.getMessage()),
						HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

}
