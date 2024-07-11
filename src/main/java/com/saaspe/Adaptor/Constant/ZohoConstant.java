package com.saaspe.Adaptor.Constant;

import java.util.Arrays;
import java.util.List;

public class ZohoConstant {
	
	private ZohoConstant() {
		super();
	}

	public static final String ZOHOCRM_PLUS_BASE_URL="https://crmplus.zoho.in/";
	public static final String USER_URL= "https://www.zohoapis.in/crm/v5/users";
	public static final String LICENSE_URL= "https://www.zohoapis.in/crm/v6/__features/user_licenses";
	public static final String GRANT_TOKEN_URL="https://accounts.zoho.in/oauth/v2/auth";
	public static final String OAUTH_TOKEN_URL="https://accounts.zoho.in/oauth/v2/token";
    public static final List<String> ROLES=Arrays.asList("588984000000031151","588984000000031154");
    public static final List<String> PROFILES=Arrays.asList("588984000000031157","588984000000031160");
    public static final List<String> USER_TYPE=Arrays.asList("AllUsers","ActiveUsers","DeactiveUsers","DeletedUsers","AdminUsers","CurrentUser");
}
