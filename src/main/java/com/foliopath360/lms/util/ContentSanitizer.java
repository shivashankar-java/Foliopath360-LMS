package com.foliopath360.lms.util;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

/**
 * Sanitizes rich text lesson content before persisting it.
 *
 * Allows doc-style formatting tags (bold, italic, underline, lists,
 * headings, paragraphs) and strips everything else — including scripts,
 * event handlers and unknown attributes — so the HTML stored in the
 * database is always safe to render for students.
 */
public final class ContentSanitizer {

    private ContentSanitizer() {
    }

    private static final Safelist RICH_TEXT_SAFELIST = Safelist.basic()
            .addTags("h1", "h2", "h3", "h4", "h5", "h6");

    public static String sanitize(String html) {
        if (html == null || html.isBlank()) {
            return html;
        }
        return Jsoup.clean(html, "", RICH_TEXT_SAFELIST);
    }
}
