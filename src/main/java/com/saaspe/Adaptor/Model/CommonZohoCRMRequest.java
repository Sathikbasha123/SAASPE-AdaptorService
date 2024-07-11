package com.saaspe.Adaptor.Model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
@JsonInclude(content = Include.NON_NULL)
public class CommonZohoCRMRequest {

	@NonNull
	private List<CommonCRMModel> users;
	
}
