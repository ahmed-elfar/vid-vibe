package com.xay.videos_recommender.config;

import com.xay.videos_recommender.repository.TenantRepository;
import com.xay.videos_recommender.repository.UserProfileRepository;
import com.xay.videos_recommender.repository.VideoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class DataInitializerConfig {

    @Bean
    public CommandLineRunner initData(
            TenantRepository tenantRepository,
            VideoRepository videoRepository,
            UserProfileRepository userProfileRepository
    ) {
        return args -> {
            // TODO: Initialize sample data
            log.info("DataInitializerConfig: Sample data initialization (not implemented yet)");
        };
    }
}
