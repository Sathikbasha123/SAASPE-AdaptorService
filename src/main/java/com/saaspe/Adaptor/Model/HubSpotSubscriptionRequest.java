package com.saaspe.Adaptor.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HubSpotSubscriptionRequest {
	private String emailAddress;
	private String subscriptionId; 
    private String legalBasis;
	private String legalBasisExplanation;
}
