package com.saaspe.Adaptor.Service;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.saaspe.Adaptor.Advice.CommonResponse;
import com.saaspe.Adaptor.Advice.DataValidationException;

public interface DatadogService {

	CommonResponse createUser(String userEmail, String appId) throws JsonParseException, JsonMappingException, IOException;

	CommonResponse getUser(String appId) throws DataValidationException, IOException;

	CommonResponse deleteUser(String appId, String userEmail);




		
}
