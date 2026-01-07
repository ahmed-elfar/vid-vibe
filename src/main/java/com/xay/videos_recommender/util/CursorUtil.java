package com.xay.videos_recommender.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Utility for cursor-based pagination.
 * Encodes/decodes offset as base64 for opaque cursor.
 */
public final class CursorUtil {

    private CursorUtil() {}

    public static String encode(int offset) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(String.valueOf(offset).getBytes(StandardCharsets.UTF_8));
    }

    public static int decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return 0;
        }
        try {
            String decoded = new String(
                    Base64.getUrlDecoder().decode(cursor),
                    StandardCharsets.UTF_8
            );
            return Integer.parseInt(decoded);
        } catch (Exception e) {
            return 0;
        }
    }
}
