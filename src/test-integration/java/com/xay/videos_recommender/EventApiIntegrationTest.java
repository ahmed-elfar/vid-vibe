package com.xay.videos_recommender;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for Event API payload parsing.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EventApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String VALID_PAYLOAD = """
            {
              "events": [
                {
                  "type": "video_watch",
                  "videoId": "v123",
                  "timestamp": "2025-01-06T12:00:00Z",
                  "data": {
                    "watch_duration_ms": 45000,
                    "watch_percentage": 0.75
                  }
                }
              ],
              "context": {
                "device_type": "mobile"
              }
            }
            """;

    @Test
    @DisplayName("POST /v1/events with valid payload returns 202 Accepted")
    void postEvents_validPayload_returns202() throws Exception {
        mockMvc.perform(post("/v1/events")
                        .header("X-Tenant-ID", 1)
                        .header("X-User-ID", "a4f2e8c1b9d3e7f6")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PAYLOAD))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status", is("accepted")))
                .andExpect(jsonPath("$.eventsCount", is(1)));
    }

    @Test
    @DisplayName("POST /v1/events with invalid type returns 400")
    void postEvents_invalidType_returns400() throws Exception {
        String invalidPayload = """
                {
                  "events": [
                    {
                      "type": "invalid_type",
                      "videoId": "v123",
                      "timestamp": "2025-01-06T12:00:00Z"
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/v1/events")
                        .header("X-Tenant-ID", 1)
                        .header("X-User-ID", "a4f2e8c1b9d3e7f6")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_JSON")));
    }

    @Test
    @DisplayName("POST /v1/events with empty events returns 400")
    void postEvents_emptyEvents_returns400() throws Exception {
        String emptyPayload = """
                {
                  "events": []
                }
                """;

        mockMvc.perform(post("/v1/events")
                        .header("X-Tenant-ID", 1)
                        .header("X-User-ID", "a4f2e8c1b9d3e7f6")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emptyPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }
}

