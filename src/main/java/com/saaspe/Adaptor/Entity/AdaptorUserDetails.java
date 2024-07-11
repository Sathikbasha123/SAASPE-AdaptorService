package com.saaspe.Adaptor.Entity;

import java.util.List;

import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import com.saaspe.Adaptor.Model.AdaptorFields;

import lombok.Data;

@Data
@Document(collection = "saaspe_adaptor_user_details")
public class AdaptorUserDetails {

	@Transient
	public static final String SEQUENCE_NAME = "adaptoruserdetailssequence";

	private long id;

	private String userEmail;

	private String userId;

	private List<AdaptorFields> fields;
}
