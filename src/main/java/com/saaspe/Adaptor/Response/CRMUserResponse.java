package com.saaspe.Adaptor.Response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CRMUserResponse {

	private List<CommonUserResponse> users;
}
