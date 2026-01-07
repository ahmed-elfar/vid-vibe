package com.xay.videos_recommender.repository;

import com.xay.videos_recommender.model.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    Optional<UserProfile> findByTenantIdAndHashedUserId(Long tenantId, String hashedUserId);

    List<UserProfile> findByTenantIdOrderByLastActiveAtDesc(Long tenantId);
}

