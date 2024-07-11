package com.saaspe.Adaptor.Response;

import org.springframework.lang.NonNull;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CRMUserLicenseResponse {

	@NonNull
	private int totalLicensesPurchased;
	@NonNull
	private int availableCount;
	@NonNull
	private int usedCount;
}
