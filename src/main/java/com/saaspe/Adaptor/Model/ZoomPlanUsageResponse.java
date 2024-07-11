package com.saaspe.Adaptor.Model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ZoomPlanUsageResponse {
    @JsonProperty("plan_base")
    private PlanDetail planBase;

    @Data
    public static class PlanDetail {
        private String type;
        private int hosts;
        private int usage;
        private int pending;
    }
}
