package com.saaspe.Adaptor.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.saaspe.Adaptor.Entity.AtlassianGroups;

public interface AtlassianGroupRepository extends JpaRepository<AtlassianGroups, Long> {

	AtlassianGroups findByGroupIdAndAccountId(String id, String accountId);

	void deleteByGroupIdAndAccountId(String groupId, String accountId);
}
