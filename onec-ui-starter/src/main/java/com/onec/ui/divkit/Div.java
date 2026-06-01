package com.onec.ui.divkit;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Factories + fluent styling helpers for real DivKit div nodes, emitted as plain
 * maps so Jackson serializes them to the DivKit JSON schema ({@code "type":
 * "container" | "text" | "gallery" | "image" | "separator" | ...}). Every styling
 * helper mutates and returns the node so calls chain.
 */
public final class Div {

    private Div() {}

    // ----- node factories -----

    public static Map<String, Object> text(String text) {
        Map<String, Object> node = node("text");
        node.put("text", text == null ? "" : text);
        return node;
    }

    public static Map<String, Object> text(String text, int fontSize, String fontWeight) {
        Map<String, Object> node = text(text);
        node.put("font_size", fontSize);
        if (fontWeight != null) {
            node.put("font_weight", fontWeight);
        }
        return node;
    }

    public static Map<String, Object> maxLines(Map<String, Object> node, int lines) {
        node.put("max_lines", lines);
        return node;
    }

    public static Map<String, Object> container(String orientation, List<Map<String, Object>> items) {
        Map<String, Object> node = node("container");
        node.put("orientation", orientation);
        node.put("items", items);
        return node;
    }

    public static Map<String, Object> vertical(List<Map<String, Object>> items) {
        return container("vertical", items);
    }

    public static Map<String, Object> horizontal(List<Map<String, Object>> items) {
        return container("horizontal", items);
    }

    /** Scrollable list along its orientation. */
    public static Map<String, Object> gallery(String orientation, List<Map<String, Object>> items) {
        Map<String, Object> node = node("gallery");
        node.put("orientation", orientation);
        node.put("items", items);
        return node;
    }

    public static Map<String, Object> grid(int columnCount, List<Map<String, Object>> items) {
        Map<String, Object> node = node("grid");
        node.put("column_count", columnCount);
        node.put("items", items);
        return node;
    }

    public static Map<String, Object> image(String url) {
        Map<String, Object> node = node("image");
        node.put("image_url", url);
        return node;
    }

    public static Map<String, Object> separator() {
        Map<String, Object> node = node("separator");
        return node;
    }

    public static Map<String, Object> separator(String color) {
        Map<String, Object> node = separator();
        Map<String, Object> delimiter = new LinkedHashMap<>();
        delimiter.put("color", color);
        node.put("delimiter_style", delimiter);
        return node;
    }

    /** Custom extension node — clients render it via the registered {@code custom_type}. */
    public static Map<String, Object> custom(String customType, Map<String, Object> payload) {
        Map<String, Object> node = node("custom");
        node.put("custom_type", customType);
        if (payload != null) {
            node.putAll(payload);
        }
        return node;
    }

    // ----- styling helpers (mutate + return) -----

    public static Map<String, Object> action(Map<String, Object> node, String logId, String url) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("log_id", logId);
        action.put("url", url);
        node.put("action", action);
        return node;
    }

    public static Map<String, Object> color(Map<String, Object> node, String color) {
        node.put("text_color", color);
        return node;
    }

    public static Map<String, Object> background(Map<String, Object> node, String color) {
        Map<String, Object> solid = new LinkedHashMap<>();
        solid.put("type", "solid");
        solid.put("color", color);
        node.put("background", List.of(solid));
        return node;
    }

    public static Map<String, Object> corner(Map<String, Object> node, int radius) {
        border(node).put("corner_radius", radius);
        return node;
    }

    public static Map<String, Object> stroke(Map<String, Object> node, String color, int width) {
        Map<String, Object> stroke = new LinkedHashMap<>();
        stroke.put("color", color);
        stroke.put("width", width);
        border(node).put("stroke", stroke);
        return node;
    }

    public static Map<String, Object> pad(Map<String, Object> node, int v, int h) {
        return pad(node, v, h, v, h);
    }

    public static Map<String, Object> pad(Map<String, Object> node, int top, int right, int bottom, int left) {
        node.put("paddings", edges(top, right, bottom, left));
        return node;
    }

    public static Map<String, Object> margins(Map<String, Object> node, int top, int right, int bottom, int left) {
        node.put("margins", edges(top, right, bottom, left));
        return node;
    }

    public static Map<String, Object> gap(Map<String, Object> node, int dp) {
        node.put("item_spacing", dp);
        return node;
    }

    public static Map<String, Object> width(Map<String, Object> node, int dp) {
        node.put("width", fixed(dp));
        return node;
    }

    public static Map<String, Object> height(Map<String, Object> node, int dp) {
        node.put("height", fixed(dp));
        return node;
    }

    public static Map<String, Object> matchWidth(Map<String, Object> node) {
        node.put("width", Map.of("type", "match_parent"));
        return node;
    }

    /** Size to content (not stretched to the parent) so children keep natural width. */
    public static Map<String, Object> wrapWidth(Map<String, Object> node) {
        node.put("width", Map.of("type", "wrap_content"));
        return node;
    }

    public static Map<String, Object> matchHeight(Map<String, Object> node) {
        node.put("height", Map.of("type", "match_parent"));
        return node;
    }

    /** Share main-axis space with siblings (match_parent + weight). */
    public static Map<String, Object> weight(Map<String, Object> node, double weight) {
        Map<String, Object> width = new LinkedHashMap<>();
        width.put("type", "match_parent");
        width.put("weight", weight);
        node.put("width", width);
        return node;
    }

    public static Map<String, Object> weightHeight(Map<String, Object> node, double weight) {
        Map<String, Object> height = new LinkedHashMap<>();
        height.put("type", "match_parent");
        height.put("weight", weight);
        node.put("height", height);
        return node;
    }

    public static Map<String, Object> alignH(Map<String, Object> node, String alignment) {
        node.put("content_alignment_horizontal", alignment);
        return node;
    }

    public static Map<String, Object> alignV(Map<String, Object> node, String alignment) {
        node.put("content_alignment_vertical", alignment);
        return node;
    }

    public static Map<String, Object> textAlign(Map<String, Object> node, String alignment) {
        node.put("text_alignment_horizontal", alignment);
        return node;
    }

    // ----- internals -----

    @SuppressWarnings("unchecked")
    private static Map<String, Object> border(Map<String, Object> node) {
        return (Map<String, Object>) node.computeIfAbsent("border", k -> new LinkedHashMap<String, Object>());
    }

    private static Map<String, Object> edges(int top, int right, int bottom, int left) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("top", top);
        e.put("right", right);
        e.put("bottom", bottom);
        e.put("left", left);
        return e;
    }

    private static Map<String, Object> fixed(int dp) {
        Map<String, Object> size = new LinkedHashMap<>();
        size.put("type", "fixed");
        size.put("value", dp);
        return size;
    }

    private static Map<String, Object> node(String type) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("type", type);
        return node;
    }
}
