package com.saaspe.Adaptor.Model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
public class Microsoft365SubscribedSku {
    private String accountName;
    private String accountId;
    private String appliesTo;
    private String capabilityStatus;
    private int consumedUnits; 
    private String id;
    private String skuId;
    private String skuPartNumber;
    private List<String> subscriptionIds;
    private PrepaidUnits prepaidUnits; 
    private List<ServicePlan> servicePlans;

    @Data
    @NoArgsConstructor
    public static class PrepaidUnits {
        private int enabled;
        private int suspended;
        private int warning;
        private int lockedOut;
    }

    @Data
    @NoArgsConstructor
    public static class ServicePlan {
        private String servicePlanId;
        private String servicePlanName;
        private String provisioningStatus;
        private String appliesTo;
    }
}
