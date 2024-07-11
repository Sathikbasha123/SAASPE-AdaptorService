package com.saaspe.Adaptor.Service;

import com.saaspe.Adaptor.Advice.CommonResponse;
import com.saaspe.Adaptor.Advice.DataValidationException;
import com.saaspe.Adaptor.Model.ConfluenceCreateUser;

public interface ConfluenceService {

	CommonResponse createUser(ConfluenceCreateUser confluenceCreateUser, String appId);

	CommonResponse getUserList(String appId) throws DataValidationException;

	CommonResponse deleteUser(String emailAddress, String appId);


}
