package com.tvmod.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Pattern;

public class URLValidator {

    private static final Pattern URL_PATTERN = Pattern.compile(
        "^(https?://)?" +
        "([a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}" +
        "(:\\d{1,5})?" +
        "(/[^\\s]*)?" +
        "$",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern LOCALHOST_PATTERN = Pattern.compile(
        "^(https?://)?localhost(:\\d{1,5})?(/[^\\s]*)?$",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern IP_PATTERN = Pattern.compile(
        "^(https?://)?" +
        "((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}" +
        "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)" +
        "(:\\d{1,5})?" +
        "(/[^\\s]*)?$",
        Pattern.CASE_INSENSITIVE
    );

    private static final String GOOGLE_SEARCH_URL = "https://www.google.com/search?q=";
    private static final String DEFAULT_PROTOCOL = "https://";

    public static boolean isValidUrl(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }

        String trimmed = input.trim();

        return URL_PATTERN.matcher(trimmed).matches() ||
               LOCALHOST_PATTERN.matcher(trimmed).matches() ||
               IP_PATTERN.matcher(trimmed).matches();
    }

    public static String normalizeUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return "";
        }

        String trimmed = url.trim();

        if (trimmed.toLowerCase().startsWith("http://") || 
            trimmed.toLowerCase().startsWith("https://")) {
            return trimmed;
        }

        return DEFAULT_PROTOCOL + trimmed;
    }

    public static String toSearchUrl(String query) {
        if (query == null || query.trim().isEmpty()) {
            return GOOGLE_SEARCH_URL;
        }

        try {
            String encoded = URLEncoder.encode(query.trim(), "UTF-8");
            return GOOGLE_SEARCH_URL + encoded;
        } catch (UnsupportedEncodingException e) {
            return GOOGLE_SEARCH_URL + query.trim().replace(" ", "+");
        }
    }

    public static String processInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }

        String trimmed = input.trim();

        if (isValidUrl(trimmed)) {
            return normalizeUrl(trimmed);
        } else {
            return toSearchUrl(trimmed);
        }
    }
}
