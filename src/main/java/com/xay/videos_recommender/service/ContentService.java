package com.xay.videos_recommender.service;

import com.xay.videos_recommender.cache.AppCache;
import com.xay.videos_recommender.mapper.VideoMapper;
import com.xay.videos_recommender.model.domain.ContentCandidate;
import com.xay.videos_recommender.model.entity.Video;
import com.xay.videos_recommender.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class ContentService {

    private final VideoRepository videoRepository;
    private final AppCache appCache;
    private final VideoMapper videoMapper;

    // Version tracking per tenant (in production, this would be in Redis)
    private final Map<Long, Integer> candidateVersions = new ConcurrentHashMap<>();

    public List<ContentCandidate> getContentCandidates(Long tenantId) {
        return appCache.getContentCandidates(tenantId)
                .orElseGet(() -> {
                    List<ContentCandidate> candidates = loadContentCandidates(tenantId);
                    appCache.putContentCandidates(tenantId, candidates);
                    return candidates;
                });
    }

    public void rebuildContentCandidates(Long tenantId) {
        appCache.evictContentCandidates(tenantId);
        candidateVersions.merge(tenantId, 1, Integer::sum);
    }

    public int getContentCandidatesVersion(Long tenantId) {
        return candidateVersions.getOrDefault(tenantId, 1);
    }

    private List<ContentCandidate> loadContentCandidates(Long tenantId) {
        List<Video> videos = videoRepository.findByTenantIdAndStatus(tenantId, "active");

        return videos.stream()
                .map(this::toContentCandidate)
                .sorted((a, b) -> Double.compare(b.baseScore().doubleValue(), a.baseScore().doubleValue()))
                .toList();
    }

    private ContentCandidate toContentCandidate(Video video) {
        BigDecimal freshnessScore = calculateFreshnessScore(video);
        BigDecimal engagementScore = calculateEngagementScore(video);
        BigDecimal baseScore = calculateBaseScore(freshnessScore, engagementScore);

        return videoMapper.toContentCandidate(video, baseScore, freshnessScore, engagementScore);
    }

    private BigDecimal calculateBaseScore(BigDecimal freshnessScore, BigDecimal engagementScore) {
        // Combine engagement and freshness for base score
        double engagement = engagementScore.doubleValue();
        double freshness = freshnessScore.doubleValue();
        return BigDecimal.valueOf(engagement * 0.6 + freshness * 0.4);
    }

    private BigDecimal calculateFreshnessScore(Video video) {
        if (video.getPublishedAt() == null) {
            return BigDecimal.valueOf(0.5);
        }
        // Exponential decay with 1-week half-life
        long hoursAgo = Duration.between(video.getPublishedAt(), Instant.now()).toHours();
        double score = Math.exp(-hoursAgo / 168.0); // 168 hours = 1 week
        return BigDecimal.valueOf(Math.max(0.1, score));
    }

    private BigDecimal calculateEngagementScore(Video video) {
        // Normalize engagement metrics
        long views = video.getViewCount() != null ? video.getViewCount() : 0;
        long likes = video.getLikeCount() != null ? video.getLikeCount() : 0;
        long shares = video.getShareCount() != null ? video.getShareCount() : 0;
        double avgWatch = video.getAvgWatchPercentage() != null
                ? video.getAvgWatchPercentage().doubleValue()
                : 0.5;

        // Simple engagement formula
        double engagement = (likes * 2.0 + shares * 3.0 + avgWatch * 100) / (views + 1);
        return BigDecimal.valueOf(Math.min(1.0, engagement));
    }
}
