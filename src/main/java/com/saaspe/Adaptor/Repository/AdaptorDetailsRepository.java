package com.saaspe.Adaptor.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.saaspe.Adaptor.Entity.AdaptorDetails;

@Repository
public interface AdaptorDetailsRepository extends JpaRepository<AdaptorDetails, Long> {

	AdaptorDetails findByApplicationId(String appId);

	@Query("SELECT a FROM AdaptorDetails a WHERE a.id = :appId")
	AdaptorDetails findBySequenceId(Long appId);
}
