package com.dev.IbioScience.utils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QnaHtmlImageUtil {

    // img src="..."/ src='...'
    private static final Pattern IMG_SRC = Pattern.compile(
            "<img[^>]+src\\s*=\\s*(['\"])(.*?)\\1",
            Pattern.CASE_INSENSITIVE
    );

    public static Set<String> extractImageSrcUrls(String html) {
        if (html == null || html.isBlank()) return Collections.emptySet();

        Set<String> urls = new HashSet<>();
        Matcher m = IMG_SRC.matcher(html);
        while (m.find()) {
            String url = m.group(2);
            if (url != null && !url.isBlank()) {
                urls.add(url.trim());
            }
        }
        return urls;
    }
}