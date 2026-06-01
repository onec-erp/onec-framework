package com.onec.ui.divkit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Shared, theme-aware DivKit building blocks (cards, headers, badges, tables). */
final class Components {

    private Components() {}

    static Map<String, Object> pageHeader(String title, String subtitle, Palette p) {
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(Div.color(Div.text(title, 22, "bold"), p.text()));
        if (subtitle != null && !subtitle.isBlank()) {
            Map<String, Object> sub = Div.color(Div.text(subtitle, 13, "regular"), p.muted());
            Div.margins(sub, 2, 0, 0, 0);
            items.add(sub);
        }
        Map<String, Object> header = Div.vertical(items);
        Div.margins(header, 0, 0, 16, 0);
        return header;
    }

    static Map<String, Object> card(List<Map<String, Object>> items, Palette p) {
        Map<String, Object> card = Div.vertical(items);
        Div.matchWidth(card);
        Div.background(card, p.surface());
        Div.pad(card, 16, 16);
        Div.corner(card, 12);
        Div.stroke(card, p.border(), 1);
        Div.gap(card, 8);
        return card;
    }

    static Map<String, Object> badge(String text, String fg, String bg) {
        Map<String, Object> badge = Div.text(text, 12, "medium");
        Div.color(badge, fg);
        Div.background(badge, bg);
        Div.pad(badge, 3, 9);
        Div.corner(badge, 999);
        return badge;
    }

    static Map<String, Object> statusBadge(boolean positive, String text, Palette p) {
        return positive
                ? badge(text, p.success(), p.successSoft())
                : badge(text, p.muted(), p.rowAlt());
    }

    /** A bordered card containing a header row + data rows, columns evenly weighted. */
    static Map<String, Object> table(List<String> headers, List<Row> rows, Palette p) {
        List<Map<String, Object>> stack = new ArrayList<>();

        List<Map<String, Object>> headerCells = new ArrayList<>();
        for (String h : headers) {
            headerCells.add(Div.weight(Div.color(Div.text(h, 12, "medium"), p.faint()), 1));
        }
        Map<String, Object> headerRow = Div.horizontal(headerCells);
        Div.pad(headerRow, 10, 14);
        stack.add(headerRow);
        stack.add(Div.separator(p.border()));

        if (rows.isEmpty()) {
            Map<String, Object> empty = Div.color(Div.text("No records", 13, "regular"), p.faint());
            Div.pad(empty, 16, 14);
            stack.add(empty);
        }
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            List<Map<String, Object>> cells = new ArrayList<>();
            for (String value : row.cells()) {
                cells.add(Div.weight(Div.color(Div.text(value, 14, "regular"), p.text()), 1));
            }
            Map<String, Object> rowNode = Div.horizontal(cells);
            Div.pad(rowNode, 11, 14);
            if (i % 2 == 1) {
                Div.background(rowNode, p.rowAlt());
            }
            if (row.actionUrl() != null) {
                Div.action(rowNode, "open", row.actionUrl());
            }
            stack.add(rowNode);
        }

        Map<String, Object> table = Div.vertical(stack);
        Div.matchWidth(table);
        Div.background(table, p.surface());
        Div.corner(table, 12);
        Div.stroke(table, p.border(), 1);
        return table;
    }

    static Map<String, Object> fieldRow(String label, String value, Palette p) {
        Map<String, Object> row = Div.horizontal(List.of(
                Div.weight(Div.color(Div.text(label, 13, "regular"), p.muted()), 2),
                Div.weight(Div.color(Div.text(value, 14, "regular"), p.text()), 3)));
        Div.pad(row, 7, 0);
        return row;
    }

    record Row(List<String> cells, String actionUrl) {}
}
