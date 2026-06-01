package com.onec.ui.divkit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds the home/dashboard content div: a greeting header and a grid of widget
 * cards. Widget bodies are placeholders for now (title + type); richer widgets
 * (charts/kanban) arrive as {@code div-custom} extensions.
 */
public final class DashboardDivBuilder {

    private DashboardDivBuilder() {}

    public record Widget(String title, String type) {}

    public static Map<String, Object> build(String title, String greeting,
                                            List<Widget> widgets, int columns) {
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(Components.pageHeader(title, greeting));

        if (widgets.isEmpty()) {
            items.add(Components.card(List.of(
                    Div.color(Div.text("Nothing here yet", 14, "regular"), Palette.MUTED))));
            return Div.vertical(items);
        }

        List<Map<String, Object>> cards = new ArrayList<>();
        for (Widget w : widgets) {
            cards.add(widgetCard(w));
        }
        items.add(columns > 1 ? Div.grid(columns, cards) : Div.vertical(cards));
        return Div.vertical(items);
    }

    private static Map<String, Object> widgetCard(Widget w) {
        Map<String, Object> label = Div.color(Div.text(w.type().toUpperCase(), 11, "medium"), Palette.FAINT);
        Map<String, Object> title = Div.color(Div.text(w.title(), 16, "medium"), Palette.TEXT);
        Div.margins(title, 4, 0, 0, 0);
        Map<String, Object> card = Components.card(List.of(label, title));
        Div.margins(card, 0, 6, 12, 6);
        return card;
    }
}
