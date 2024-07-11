package com.saaspe.Adaptor.Model;


import java.util.Map;

import lombok.Data;
 
@Data
public class ErrorResponse {
    private String status;
    private String message;
    private String correlationId;
    private Map<String, Object> context;
    private String category;
    private String subCategory;
 
}
