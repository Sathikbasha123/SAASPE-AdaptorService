package com.saaspe.Adaptor.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.saaspe.Adaptor.Advice.CommonResponse;
import com.saaspe.Adaptor.Advice.Response;
import com.saaspe.Adaptor.Model.GitHubInviteRequestBody;
import com.saaspe.Adaptor.Model.RemoveUserRequest;
import com.saaspe.Adaptor.Model.UserUpdateRequest;
import com.saaspe.Adaptor.Service.GitHubService;

@RestController
@RequestMapping("/GitHub")
public class GitHubController {

    @Autowired
    private GitHubService gitHubService;

    @GetMapping("/AuthUri")
    public ResponseEntity<CommonResponse> getAuthUri(@RequestParam String appId) {
        CommonResponse response = new CommonResponse();
        try {
            CommonResponse authUri = gitHubService.getAuthUri(appId);
            response.setStatus(HttpStatus.OK);
            response.setResponse(new Response("generateAuthUri", authUri));
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            response.setStatus(HttpStatus.BAD_REQUEST);
            response.setMessage("Failed to generate auth URI: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/getToken")
    public ResponseEntity<CommonResponse> getToken(@RequestParam String appId)  {
        CommonResponse tokenResponse = gitHubService.getToken(appId);

        if (tokenResponse.getStatus() == HttpStatus.OK) {
            return new ResponseEntity<>(tokenResponse, HttpStatus.OK);
        } else {
            CommonResponse response = new CommonResponse();
            response.setStatus(HttpStatus.BAD_REQUEST);
            response.setResponse(new Response("getToken", null));
            response.setMessage("Failed to get token: " + tokenResponse.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }
    
    @GetMapping("/getUser")
    public ResponseEntity<CommonResponse> getUser(@RequestParam String appId) {
        try {
            return new ResponseEntity<>(gitHubService.getUserDetails(appId), HttpStatus.OK);
       
        } catch (Exception e) {
            return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
                    new Response("Exception in getUser method", e.getMessage()), "Failed to fetch GitHub user details"),
                    HttpStatus.BAD_REQUEST);
        }
    }
    
    @PostMapping("/inviteUser")
    public ResponseEntity<CommonResponse> inviteUserToOrganization(@RequestParam String appId,@RequestBody GitHubInviteRequestBody inviteRequestBody) {
        try {
            CommonResponse inviteResponse = gitHubService.inviteUserToOrganization(appId, inviteRequestBody);
            return new ResponseEntity<>(inviteResponse, inviteResponse.getStatus());
        } catch (Exception e) {
            return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
                    new Response("Exception in inviteUserToOrganization method", e.getMessage()),
                    "Failed to invite user to GitHub organization"), HttpStatus.BAD_REQUEST);
        }
    }
    
    @GetMapping("/getMembers")
    public ResponseEntity<CommonResponse> getOrganizationMembers(@RequestParam String appId) {
        try {
            CommonResponse membersResponse = gitHubService.getOrganizationMembers(appId);
            return new ResponseEntity<>(membersResponse, membersResponse.getStatus());
        } catch (Exception e) {
            return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
                    new Response("Exception in getOrganizationMembers method", e.getMessage()),
                    "Failed to fetch GitHub organization members"), HttpStatus.BAD_REQUEST);
        }
    }
    
    @GetMapping("/billing/actions")
    public ResponseEntity<CommonResponse> getActionsBilling(@RequestParam String appId) {
        try {
            CommonResponse billingActionsResponse = gitHubService.getActionsBilling(appId);
            return new ResponseEntity<>(billingActionsResponse, billingActionsResponse.getStatus());
        } catch (Exception e) {
            return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
                    new Response("Exception in getBillingActions method", e.getMessage()),
                    "Failed to fetch GitHub organization billing actions"), HttpStatus.BAD_REQUEST);
        }
    }
    
    @GetMapping("/billing/packages")
    public ResponseEntity<CommonResponse> getPackagesBilling(@RequestParam String appId) {
        try {
            CommonResponse billingActionsResponse = gitHubService.getPackagesBilling(appId);
            return new ResponseEntity<>(billingActionsResponse, billingActionsResponse.getStatus());
        } catch (Exception e) {
            return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
                    new Response("Exception in getPackagesBilling method", e.getMessage()),
                    "Failed to fetch GitHub organization packages billings"), HttpStatus.BAD_REQUEST);
        }
    }
    
    @GetMapping("/billing/shared-storage")
    public ResponseEntity<CommonResponse> getSharedStorageBilling( @RequestParam String appId){
        try {
            CommonResponse billingSharedStorageResponse = gitHubService.getSharedStorageBilling(appId);
            return new ResponseEntity<>(billingSharedStorageResponse, billingSharedStorageResponse.getStatus());
        } catch (Exception e) {
            return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
                    new Response("Exception in getSharedStorageBilling method", e.getMessage()),
                    "Failed to fetch GitHub organization shared storage billings"), HttpStatus.BAD_REQUEST);
        }
    }
    
    @DeleteMapping("/remove/organizationMember")
    public ResponseEntity<CommonResponse> removeOrganizationMember(@RequestParam String appId, @RequestBody RemoveUserRequest removeUserRequest) {
        try {
            CommonResponse removeMemberResponse = gitHubService.removeOrganizationMember(appId, removeUserRequest);
            return new ResponseEntity<>(removeMemberResponse, removeMemberResponse.getStatus());
        } catch (Exception e) {
            return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
                    new Response("Exception in removeOrganizationMember method", e.getMessage()),
                    "Failed to remove GitHub organization member"), HttpStatus.BAD_REQUEST);
        }
    }
    
    @PutMapping("/update/memberRole")
    public ResponseEntity<CommonResponse> updateMembership(@RequestParam String appId, @RequestBody UserUpdateRequest userUpdateRequest ) {
        try {
            CommonResponse updateMembershipResponse = gitHubService.updateMembership(appId, userUpdateRequest);
            return new ResponseEntity<>(updateMembershipResponse, updateMembershipResponse.getStatus());
        } catch (Exception e) {
            return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
                    new Response("Exception in updateMembership method", e.getMessage()),
                    "Failed to update GitHub organization membership"), HttpStatus.BAD_REQUEST);
        }
    }
    
    
    @GetMapping("/org/details")
    public ResponseEntity<CommonResponse> getOrgDetails(@RequestParam String appId) {
        try {
            CommonResponse orgDetailsResponse = gitHubService.getOrgDetails(appId);
            return new ResponseEntity<>(orgDetailsResponse, orgDetailsResponse.getStatus());
        } catch (Exception e) {
            return new ResponseEntity<>(new CommonResponse(HttpStatus.BAD_REQUEST,
                    new Response("Exception in getOrgDetails method", e.getMessage()),
                    "Failed to fetch GitHub org details"), HttpStatus.BAD_REQUEST);
        }
    }
}
