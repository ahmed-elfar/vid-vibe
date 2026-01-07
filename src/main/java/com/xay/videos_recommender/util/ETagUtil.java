package com.xay.videos_recommender.util;

/**
 * Utility for generating ETag headers.
 * Format: "{candidatesVersion}x{feedVersion}x{cursor}"
 * 
 * Including cursor ensures each page has a unique ETag,
 * enabling proper 304 handling for all requests.
 */
public final class ETagUtil {

    private ETagUtil() {}

    public static String generate(int candidatesVersion, int feedVersion, int cursor) {
        return candidatesVersion + "x" + feedVersion + "x" + cursor;
    }

    public static boolean matches(String clientETag, int candidatesVersion, int feedVersion, int cursor) {
        if (clientETag == null || clientETag.isBlank()) {
            return false;
        }
        // Handle quoted ETags (HTTP spec requires quotes)
        String normalizedETag = clientETag.replace("\"", "");
        String expected = generate(candidatesVersion, feedVersion, cursor);
        return expected.equals(normalizedETag);
    }
}
