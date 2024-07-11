package com.saaspe.Adaptor.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DatadogGetUserResponse {
	    private String type;
	    private String id;
	    private String name;
	    private String email;
	    private boolean verified;
	    private boolean disabled;
	    private String status;

	    
}
