package com.saaspe.Adaptor.Model;
 
import lombok.Data;
 
@Data
public class CreateHubSpotUserRequest {
 
	private boolean sendWelcomeEmail;
	private String email;
 
}