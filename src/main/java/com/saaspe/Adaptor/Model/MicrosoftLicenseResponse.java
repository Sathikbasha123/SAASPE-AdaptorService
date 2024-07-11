package com.saaspe.Adaptor.Model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
@Data
public class MicrosoftLicenseResponse {
    @JsonProperty("userDetails")
    private Microsoft365getUserlistResponse userDetails;
    
    @JsonProperty("accountEnabled")
    private boolean accountEnabled;
    
    @JsonProperty("assignedLicenses")
    private List<AssignedLicense> assignedLicenses;
    
    @JsonProperty("assignedPlans")
    private List<AssignedPlan> assignedPlans;
    
    @JsonProperty("businessPhones")
    private List<String> businessPhones;
    
    @JsonProperty("city")
    private String city;
    
    @JsonProperty("companyName")
    private String companyName;
    
    @JsonProperty("assignedLicenseIds")
    private List<String> assignedLicenseIds; 

    @JsonProperty("removedLicenseIds")
    private List<String> removedLicenseIds; 

    @Data
    static class AssignedLicense {
        
        @JsonProperty("skuId")
        private String skuId;
    }

    @Data
    static class AssignedPlan {
        @JsonProperty("assignedDateTime")
        private String assignedDateTime;
        
        @JsonProperty("capabilityStatus")
        private String capabilityStatus;
        
        @JsonProperty("service")
        private String service;
        
        @JsonProperty("servicePlanId")
        private String servicePlanId;

        @JsonProperty("servicePlanName") 
        private String servicePlanName;

        @JsonProperty("provisioningStatus")
        private String provisioningStatus;
    }
}