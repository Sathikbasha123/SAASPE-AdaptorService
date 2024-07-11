package com.saaspe.Adaptor.Model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Microsoft365AssignLicense {
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AddLicense {
        private String skuId;
    }

    private List<AddLicense> addLicenses;
    private List<String> removeLicenses;
}
