package com.xay.videos_recommender.repository;

import com.xay.videos_recommender.model.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {
    
    List<Video> findByTenantIdAndStatus(Long tenantId, String status);
}
