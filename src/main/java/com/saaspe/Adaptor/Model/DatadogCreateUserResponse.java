package com.saaspe.Adaptor.Model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@lombok.Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DatadogCreateUserResponse {
	 @JsonProperty("data")
	 private Data data;
	 
	 @JsonProperty("errors")
	    private List<String> errors;

	    public static class Data {
	        @JsonProperty("type")
	        private String type;

	        @JsonProperty("id")
	        private String id;

	        @JsonProperty("attributes")
	        private Attributes attributes;

			public String getId() {
				return id;
			}


	    }

	    public static class Attributes {
	        @JsonProperty("name")
	        private String name;

	        @JsonProperty("handle")
	        private String handle;

	        @JsonProperty("email")
	        private String email;

	        
	        @JsonProperty("disabled")
	        private boolean disabled;


	        @JsonProperty("status")
	        private String status;

	       
	    }

	   
	}



