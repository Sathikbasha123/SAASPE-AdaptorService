package com.saaspe.Adaptor.Constant;

import java.util.Arrays;
import java.util.List;

public class GitlabConstant {
	
	private GitlabConstant() {
		super();
	}

	public static final List<String> accessRole = Arrays.asList("Guest ", "Reporter", "Developer ", "Maintainer",
			"Owner");
	public static final List<Integer> accessValues = Arrays.asList(10, 20, 30, 40, 50);

	public static final String GITLAB_GROUP = "GROUP";
	public static final String GITLAB_PROJECT = "PROJECT";

}
