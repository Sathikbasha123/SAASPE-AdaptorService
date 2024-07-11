package com.saaspe.Adaptor.Model;
 
import lombok.Data;
 
@Data
public class CreateHubSpotUserResponse {
	    private String id;
	    private String email;
	    private boolean sendWelcomeEmail;
	    private boolean superAdmin;
		
}