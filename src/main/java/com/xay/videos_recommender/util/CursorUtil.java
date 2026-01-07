package com.xay.videos_recommender.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class CursorUtil {

    private CursorUtil() {
        // Utility class
    }

    public static String encode(int offset) {
        String json = "{\"offset\":" + offset + "}";
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    public static int decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return 0;
        }
        try {
            String json = new String(Base64.getDecoder().decode(cursor), StandardCharsets.UTF_8);
            // Simple parsing - extract offset value
            int start = json.indexOf(":") + 1;
            int end = json.indexOf("}");
            return Integer.parseInt(json.substring(start, end).trim());
        } catch (Exception e) {
            return 0;
        }
    }
}

