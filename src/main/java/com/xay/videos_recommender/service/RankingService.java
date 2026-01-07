package com.xay.videos_recommender.service;

import com.xay.videos_recommender.model.domain.ContentCandidate;
import com.xay.videos_recommender.model.domain.RankedVideo;
import com.xay.videos_recommender.model.domain.UserSignals;
import com.xay.videos_recommender.model.entity.Tenant;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RankingService {

    public List<RankedVideo> rank(List<ContentCandidate> candidates, UserSignals userSignals, Tenant tenant) {
        // TODO: Implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public double calculateScore(ContentCandidate candidate, UserSignals userSignals, Tenant tenant) {
        // TODO: Implement
        throw new UnsupportedOperationException("Not implemented yet");
    }
}

