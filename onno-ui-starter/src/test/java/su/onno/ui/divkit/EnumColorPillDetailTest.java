package su.onno.ui.divkit;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The document detail surface renders an enum value declaring {@code @EnumLabel(color = …)} as a
 * coloured status pill: the {@code {col}_color} the read layer stamps on the row drives the badge
 * background, with a readable dark/light text colour derived from it ({@link Components#pillFieldRow}).
 * A value without a colour stays a plain text field row (no badge background).
 */
class EnumColorPillDetailTest {

    private static Map<String, Object> systemColumn(String fieldName, String displayName, String columnName) {
        Map<String, Object> sc = new LinkedHashMap<>();
        sc.put("fieldName", fieldName);
        sc.put("displayName", displayName);
        sc.put("columnName", columnName);
        sc.put("format", "");
        return sc;
    }

    private static Map<String, Object> statusAttr() {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("columnName", "status");
        a.put("displayName", "Статус");
        a.put("visibleInDetail", true);
        a.put("order", 1);
        a.put("hint", "");
        return a;
    }

    private static Map<String, Object> meta() {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("name", "order");
        meta.put("title", "Order");
        meta.put("attributes", List.of(statusAttr()));
        meta.put("systemColumns", List.of(
                systemColumn("number", "Number", "_number"),
                systemColumn("date", "Date", "_date"),
                systemColumn("posted", "Status", "_posted")));
        return meta;
    }

    /** Depth-first search for the (first) div node carrying {@code text == value}. */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> findByText(Object node, String value) {
        if (node instanceof Map<?, ?> map) {
            if (value.equals(map.get("text"))) {
                return (Map<String, Object>) map;
            }
            for (Object v : map.values()) {
                Map<String, Object> hit = findByText(v, value);
                if (hit != null) return hit;
            }
        } else if (node instanceof List<?> list) {
            for (Object v : list) {
                Map<String, Object> hit = findByText(v, value);
                if (hit != null) return hit;
            }
        }
        return null;
    }

    /** The solid background colour set on a div node ({@code background: [{type:solid, color}]}), or null. */
    @SuppressWarnings("unchecked")
    private static String backgroundColor(Map<String, Object> node) {
        Object bg = node == null ? null : node.get("background");
        if (bg instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> solid) {
            return (String) solid.get("color");
        }
        return null;
    }

    private static Map<String, Object> detailFor(String display, String color) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("_number", "O-1");
        row.put("_date", "2026-06-18");
        row.put("status_display", display);
        if (color != null) {
            row.put("status_color", color);
        }
        return SurfaceDivBuilder.documentDetail(meta(), row, List.of(), Palette.of(null));
    }

    @Test
    void colouredEnum_lightBackground_rendersPillWithDarkText() {
        Map<String, Object> pill = findByText(detailFor("Новый", "#F4C7C3"), "Новый");

        assertThat(pill).isNotNull();
        assertThat(backgroundColor(pill)).isEqualTo("#F4C7C3");
        assertThat(pill.get("text_color")).isEqualTo("#1f2937"); // readable dark text on a light pill
    }

    @Test
    void colouredEnum_darkBackground_rendersPillWithWhiteText() {
        Map<String, Object> pill = findByText(detailFor("Скачивается", "#1155CC"), "Скачивается");

        assertThat(pill).isNotNull();
        assertThat(backgroundColor(pill)).isEqualTo("#1155CC");
        assertThat(pill.get("text_color")).isEqualTo("#ffffff");
    }

    @Test
    void uncolouredEnum_staysPlainTextWithoutBadgeBackground() {
        Map<String, Object> node = findByText(detailFor("Отправлен", null), "Отправлен");

        assertThat(node).isNotNull();
        assertThat(backgroundColor(node)).isNull();
    }
}
