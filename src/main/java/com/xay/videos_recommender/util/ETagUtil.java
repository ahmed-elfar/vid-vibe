package com.xay.videos_recommender.util;

/**
 * Utility for generating ETag headers.
 * Format: "{candidatesVersion}x{feedVersion}"
 */
public final class ETagUtil {

    private ETagUtil() {}

    public static String generate(int candidatesVersion, int feedVersion) {
        return "\"" + candidatesVersion + "x" + feedVersion + "\"";
    }

    public static boolean matches(String clientETag, int candidatesVersion, int feedVersion) {
        if (clientETag == null || clientETag.isBlank()) {
            return false;
        }
        String expected = generate(candidatesVersion, feedVersion);
        return expected.equals(clientETag);
    }
}
