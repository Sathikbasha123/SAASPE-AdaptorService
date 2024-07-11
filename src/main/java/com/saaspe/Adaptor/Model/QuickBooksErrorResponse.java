package com.saaspe.Adaptor.Model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.intuit.ipp.data.Error;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuickBooksErrorResponse {

	private Fault fault;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private String time;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Fault {

        private List<Error> error;
        private String type;

    }
}
