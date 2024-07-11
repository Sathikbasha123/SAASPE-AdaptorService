package com.saaspe.Adaptor.Repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.saaspe.Adaptor.Entity.AdaptorUserDetails;

public interface AdaptorUserDetailsRepository extends MongoRepository<AdaptorUserDetails, Long> {

	AdaptorUserDetails findByUserEmail(String userEmail);

	boolean existsByUserEmail(String userEmail);
}
