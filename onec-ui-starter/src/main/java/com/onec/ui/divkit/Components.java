package com.onec.ui.divkit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Shared, styled DivKit building blocks (cards, headers, badges, tables). */
final class Components {

    private Components() {}

    static Map<String, Object> pageHeader(String title, String subtitle) {
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(Div.color(Div.text(title, 22, "bold"), Palette.TEXT));
        if (subtitle != null && !subtitle.isBlank()) {
            Map<String, Object> sub = Div.color(Div.text(subtitle, 13, "regular"), Palette.MUTED);
            Div.margins(sub, 2, 0, 0, 0);
            items.add(sub);
        }
        Map<String, Object> header = Div.vertical(items);
        Div.margins(header, 0, 0, 16, 0);
        return header;
    }

    static Map<String, Object> card(List<Map<String, Object>> items) {
        Map<String, Object> card = Div.vertical(items);
        Div.matchWidth(card);
        Div.background(card, Palette.SURFACE);
        Div.pad(card, 16, 16);
        Div.corner(card, 12);
        Div.stroke(card, Palette.BORDER, 1);
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

    static Map<String, Object> statusBadge(boolean positive, String text) {
        return positive
                ? badge(text, Palette.SUCCESS, Palette.SUCCESS_SOFT)
                : badge(text, Palette.MUTED, Palette.PAGE);
    }

    /** A bordered card containing a header row + data rows, columns evenly weighted. */
    static Map<String, Object> table(List<String> headers, List<Row> rows) {
        List<Map<String, Object>> stack = new ArrayList<>();

        List<Map<String, Object>> headerCells = new ArrayList<>();
        for (String h : headers) {
            headerCells.add(Div.weight(Div.color(Div.text(h, 12, "medium"), Palette.FAINT), 1));
        }
        Map<String, Object> headerRow = Div.horizontal(headerCells);
        Div.pad(headerRow, 10, 14);
        stack.add(headerRow);
        stack.add(Div.separator(Palette.BORDER));

        if (rows.isEmpty()) {
            Map<String, Object> empty = Div.color(Div.text("No records", 13, "regular"), Palette.FAINT);
            Div.pad(empty, 16, 14);
            stack.add(empty);
        }
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            List<Map<String, Object>> cells = new ArrayList<>();
            for (String value : row.cells()) {
                cells.add(Div.weight(Div.color(Div.text(value, 14, "regular"), Palette.TEXT), 1));
            }
            Map<String, Object> rowNode = Div.horizontal(cells);
            Div.pad(rowNode, 11, 14);
            if (i % 2 == 1) {
                Div.background(rowNode, Palette.ROW_ALT);
            }
            if (row.actionUrl() != null) {
                Div.action(rowNode, "open", row.actionUrl());
            }
            stack.add(rowNode);
        }

        Map<String, Object> table = Div.vertical(stack);
        Div.matchWidth(table);
        Div.background(table, Palette.SURFACE);
        Div.corner(table, 12);
        Div.stroke(table, Palette.BORDER, 1);
        return table;
    }

    static Map<String, Object> fieldRow(String label, String value) {
        Map<String, Object> row = Div.horizontal(List.of(
                Div.weight(Div.color(Div.text(label, 13, "regular"), Palette.MUTED), 2),
                Div.weight(Div.color(Div.text(value, 14, "regular"), Palette.TEXT), 3)));
        Div.pad(row, 7, 0);
        return row;
    }

    record Row(List<String> cells, String actionUrl) {}
}
