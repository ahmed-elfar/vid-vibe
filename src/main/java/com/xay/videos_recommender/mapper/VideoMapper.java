package com.xay.videos_recommender.mapper;

import com.xay.videos_recommender.model.domain.ContentCandidate;
import com.xay.videos_recommender.model.entity.Video;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * Mapper for converting Video entity to ContentCandidate domain object.
 * Only handles field mapping, not scoring calculations.
 */
@Component
public class VideoMapper {

    public ContentCandidate toContentCandidate(Video video,
                                                BigDecimal baseScore,
                                                BigDecimal freshnessScore,
                                                BigDecimal engagementScore) {
        return ContentCandidate.builder()
                .videoId(video.getId())
                .externalId(video.getExternalId())
                .category(video.getCategory())
                .tags(parseTags(video.getTags()))
                .baseScore(baseScore)
                .editorialBoost(video.getEditorialBoost() != null ? video.getEditorialBoost() : BigDecimal.ONE)
                .freshnessScore(freshnessScore)
                .engagementScore(engagementScore)
                .maturityRating(video.getMaturityRating())
                .build();
    }

    private List<String> parseTags(String tags) {
        if (tags == null || tags.isBlank() || tags.equals("[]")) {
            return List.of();
        }
        // Simple parsing for JSON array like ["tag1", "tag2"]
        String cleaned = tags.replaceAll("[\\[\\]\"]", "");
        return Arrays.stream(cleaned.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
