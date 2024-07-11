package com.saaspe.Adaptor.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuickBooksUserRequest {
	
		private String givenName;
		private String familyName;
		private Email primaryEmailAddr; 
		private Address primaryAddress;
		private PhoneNumber primaryPhone;
	
		@Data
		@AllArgsConstructor
		@NoArgsConstructor
		public class Email{
			private String address;
		}
		
		@Data
		@AllArgsConstructor
		@NoArgsConstructor
		public class Address {
			
			private String id;
			private String line1;
			private String countrySubDivisionCode;
			private String city;
			private String postalCode;

		}
		
		@Data
		@AllArgsConstructor
		@NoArgsConstructor
		public class PhoneNumber {
			
			private String freeFormNumber;

		}
}
