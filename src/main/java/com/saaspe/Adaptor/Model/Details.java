package com.saaspe.Adaptor.Model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(content = Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Details {

	@JsonProperty("id")
	 private String id;
	@JsonProperty("expected_data_type")	
	private String expected_data_type;
	@JsonProperty("api_name")
	private String api_name;
	@JsonProperty("json_path")
	private String json_path;
}
