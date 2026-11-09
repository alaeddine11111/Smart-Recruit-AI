package tn.iset.smartrecruit.offers.external;

import com.fasterxml.jackson.databind.JsonNode;

final class ExternalText {
    private ExternalText() {}

    /** Removes HTML tags/entities and shortens the text. */
    static String clean(String html, int max) {
        if (html == null) return null;
        String t = html.replaceAll("<[^>]*>", " ")
                .replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<")
                .replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'")
                .replaceAll("\\s+", " ").trim();
        return t.length() > max ? t.substring(0, max - 3) + "..." : t;
    }

    static String blankToNull(String s) { return s == null || s.isBlank() ? null : s; }

    static String join(JsonNode array) {
        if (array == null || !array.isArray()) return "";
        StringBuilder sb = new StringBuilder();
        for (JsonNode n : array) sb.append(n.asText("")).append(' ');
        return sb.toString().trim();
    }
}
