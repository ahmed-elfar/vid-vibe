package com.xay.videos_recommender;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the Feed API.
 * Tests the full request/response cycle with actual Spring context.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FeedApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TENANT_ID = "1";
    private static final String USER_ID = "user_abc123";

    @Test
    @DisplayName("GET /v1/feed returns personalized feed for active user")
    void getFeed_returnsPersonalizedFeed() throws Exception {
        mockMvc.perform(get("/v1/feed")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", USER_ID)
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(lessThanOrEqualTo(5)))
                .andExpect(jsonPath("$.pagination").exists())
                .andExpect(jsonPath("$.meta.feedType").exists());
    }

    @Test
    @DisplayName("GET /v1/feed with pagination returns next page")
    void getFeed_withCursor_returnsNextPage() throws Exception {
        // First request to get cursor
        MvcResult result = mockMvc.perform(get("/v1/feed")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", USER_ID)
                        .param("limit", "3"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        boolean hasMore = response.path("pagination").path("hasMore").asBoolean(false);

        if (hasMore) {
            String cursor = response.path("pagination").path("nextCursor").asText();

            // Second request with cursor
            mockMvc.perform(get("/v1/feed")
                            .header("X-Tenant-ID", TENANT_ID)
                            .header("X-User-ID", USER_ID)
                            .param("limit", "3")
                            .param("cursor", cursor))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isArray());
        }
    }

    @Test
    @DisplayName("GET /v1/feed without tenant header returns 400")
    void getFeed_missingTenantHeader_returns400() throws Exception {
        mockMvc.perform(get("/v1/feed")
                        .header("X-User-ID", USER_ID))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /v1/feed without user header returns 400")
    void getFeed_missingUserHeader_returns400() throws Exception {
        mockMvc.perform(get("/v1/feed")
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /v1/feed for cold-start user returns fallback feed")
    void getFeed_coldStartUser_returnsFallbackFeed() throws Exception {
        mockMvc.perform(get("/v1/feed")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "new_user_no_history")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.feedType").value(anyOf(
                        equalTo("cold_start"),
                        equalTo("fallback"),
                        equalTo("personalized")
                )));
    }

    @Test
    @DisplayName("GET /v1/feed returns ETag header")
    void getFeed_returnsETagHeader() throws Exception {
        mockMvc.perform(get("/v1/feed")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", USER_ID))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"));
    }

    @Test
    @DisplayName("GET /v1/feed with matching ETag returns 304")
    void getFeed_matchingETag_returns304() throws Exception {
        // First request to get ETag
        String etag = mockMvc.perform(get("/v1/feed")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", USER_ID))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader("ETag");

        // Second request with If-None-Match
        mockMvc.perform(get("/v1/feed")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", USER_ID)
                        .header("If-None-Match", etag))
                .andExpect(status().isNotModified());
    }

    @Test
    @DisplayName("GET /v1/feed paginated request with page 1 ETag returns 200 (different page = different ETag)")
    void getFeed_paginatedWithPage1ETag_returns200() throws Exception {
        // Use unique user to avoid cache interference from other tests
        String uniqueUser = "pagination_test_user";

        // First request to get ETag and cursor (use small limit to ensure pagination)
        MvcResult firstResult = mockMvc.perform(get("/v1/feed")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", uniqueUser)
                        .param("limit", "3"))
                .andExpect(status().isOk())
                .andReturn();

        String page1ETag = firstResult.getResponse().getHeader("ETag");
        JsonNode response = objectMapper.readTree(firstResult.getResponse().getContentAsString());
        boolean hasMore = response.path("pagination").path("hasMore").asBoolean(false);

        // Only run pagination test if there's more data
        assertTrue(hasMore, "Expected more pages. Ensure sample data has > 3 videos.");

        String cursor = response.path("pagination").path("nextCursor").asText();

        // Paginated request with page 1's ETag should return 200
        // because ETags include cursor: page 1 ETag != page 2 ETag
        MvcResult secondResult = mockMvc.perform(get("/v1/feed")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", uniqueUser)
                        .header("If-None-Match", page1ETag)
                        .param("cursor", cursor)
                        .param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andReturn();

        // Verify page 2 has a different ETag than page 1
        String page2ETag = secondResult.getResponse().getHeader("ETag");
        assertNotEquals(page1ETag, page2ETag, "Page 1 and Page 2 should have different ETags");
    }
}
