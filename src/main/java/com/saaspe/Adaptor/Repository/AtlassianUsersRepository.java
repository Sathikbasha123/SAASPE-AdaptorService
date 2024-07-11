package com.saaspe.Adaptor.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.saaspe.Adaptor.Entity.AtlassianUsers;

public interface AtlassianUsersRepository extends JpaRepository<AtlassianUsers, String> {

	List<AtlassianUsers> findAll();
}
