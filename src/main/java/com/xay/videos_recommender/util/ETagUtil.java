package com.xay.videos_recommender.util;

public final class ETagUtil {

    private ETagUtil() {
        // Utility class
    }

    public static String generate(int candidatesVersion, int feedVersion) {
        return "\"" + candidatesVersion + "x" + feedVersion + "\"";
    }

    public static int[] parse(String etag) {
        if (etag == null || etag.isBlank()) {
            return new int[]{0, 0};
        }
        try {
            String cleaned = etag.replace("\"", "");
            String[] parts = cleaned.split("x");
            return new int[]{
                    Integer.parseInt(parts[0]),
                    parts.length > 1 ? Integer.parseInt(parts[1]) : 0
            };
        } catch (Exception e) {
            return new int[]{0, 0};
        }
    }

    public static boolean matches(String etag, int candidatesVersion, int feedVersion) {
        int[] parsed = parse(etag);
        return parsed[0] == candidatesVersion && parsed[1] == feedVersion;
    }
}

