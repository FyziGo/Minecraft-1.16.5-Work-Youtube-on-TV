package com.tvmod.util;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class URLParser {

    private static final String VIDEO_ID_PATTERN = "([a-zA-Z0-9_-]{11})";

    private static final Pattern YOUTUBE_WATCH = Pattern.compile(
            "(?:https?://)?(?:www\\.)?youtube\\.com/watch\\?.*v=" + VIDEO_ID_PATTERN);

    private static final Pattern YOUTUBE_SHORT = Pattern.compile(
            "(?:https?://)?youtu\\.be/" + VIDEO_ID_PATTERN);

    private static final Pattern YOUTUBE_EMBED = Pattern.compile(
            "(?:https?://)?(?:www\\.)?youtube\\.com/embed/" + VIDEO_ID_PATTERN);

    public static Optional<String> extractYouTubeId(String url) {
        if (url == null || url.trim().isEmpty()) {
            return Optional.empty();
        }

        String trimmedUrl = url.trim();

        Matcher watchMatcher = YOUTUBE_WATCH.matcher(trimmedUrl);
        if (watchMatcher.find()) {
            return Optional.of(watchMatcher.group(1));
        }

        Matcher shortMatcher = YOUTUBE_SHORT.matcher(trimmedUrl);
        if (shortMatcher.find()) {
            return Optional.of(shortMatcher.group(1));
        }

        Matcher embedMatcher = YOUTUBE_EMBED.matcher(trimmedUrl);
        if (embedMatcher.find()) {
            return Optional.of(embedMatcher.group(1));
        }

        return Optional.empty();
    }

    public static boolean isValidUrl(String url) {
        return extractYouTubeId(url).isPresent();
    }

    public static String normalizeUrl(String url) {
        return extractYouTubeId(url)
                .map(URLParser::formatUrl)
                .orElse("");
    }

    public static String formatUrl(String videoId) {
        if (videoId == null || videoId.length() != 11) {
            return "";
        }
        if (!videoId.matches("[a-zA-Z0-9_-]{11}")) {
            return "";
        }
        return "https://www.youtube.com/watch?v=" + videoId;
    }

    public static boolean isValidVideoId(String videoId) {
        return videoId != null && videoId.matches("[a-zA-Z0-9_-]{11}");
    }
}
