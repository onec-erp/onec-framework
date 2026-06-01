package com.onec.ui.divkit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Builds the per-surface DivKit <em>content</em> (catalog/document lists, document
 * detail, register report) from the resolved metadata view + data rows. Returns a
 * bare content div — {@link com.onec.ui.DivKitController} wraps it in the responsive
 * app shell. Composed only from native DivKit primitives so it renders on every
 * official SDK with no custom code, keeping a future Flutter client cheap.
 */
public final class SurfaceDivBuilder {

    private SurfaceDivBuilder() {}

    // ----- catalog list -----

    @SuppressWarnings("unchecked")
    public static Map<String, Object> catalogList(Map<String, Object> meta, List<Map<String, Object>> rows) {
        List<Map<String, Object>> visible = visible(
                (List<Map<String, Object>>) meta.getOrDefault("attributes", List.of()), "visibleInList");

        List<String> headers = new ArrayList<>(List.of("Code", "Description"));
        for (Map<String, Object> a : visible) headers.add(str(a.get("displayName")));

        List<Components.Row> body = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            List<String> cells = new ArrayList<>();
            cells.add(str(row.get("_code")));
            cells.add(str(row.get("_description")));
            for (Map<String, Object> a : visible) cells.add(cell(a, row));
            body.add(new Components.Row(cells, null));
        }

        return content(List.of(
                Components.pageHeader(str(meta.get("name")), count(rows.size(), "item")),
                Components.table(headers, body)));
    }

    // ----- document list -----

    @SuppressWarnings("unchecked")
    public static Map<String, Object> documentList(Map<String, Object> meta, List<Map<String, Object>> rows,
                                                   String routeName) {
        List<Map<String, Object>> visible = visible(
                (List<Map<String, Object>>) meta.getOrDefault("attributes", List.of()), "visibleInList");

        List<String> headers = new ArrayList<>(List.of("Number", "Date", "Status"));
        for (Map<String, Object> a : visible) headers.add(str(a.get("displayName")));

        List<Components.Row> body = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            List<String> cells = new ArrayList<>();
            cells.add(str(row.get("_number")));
            cells.add(str(row.get("_date")));
            cells.add(Boolean.TRUE.equals(row.get("_posted")) ? "Posted" : "Draft");
            for (Map<String, Object> a : visible) cells.add(cell(a, row));
            String url = "onec://documents/" + routeName + "/" + str(row.get("_id"));
            body.add(new Components.Row(cells, url));
        }

        return content(List.of(
                Components.pageHeader(str(meta.get("name")), count(rows.size(), "document")),
                Components.table(headers, body)));
    }

    // ----- document detail -----

    @SuppressWarnings("unchecked")
    public static Map<String, Object> documentDetail(Map<String, Object> meta, Map<String, Object> row) {
        List<Map<String, Object>> items = new ArrayList<>();

        boolean posted = Boolean.TRUE.equals(row.get("_posted"));
        items.add(detailHeader(str(meta.get("name")) + " " + str(row.get("_number")), posted));

        List<Map<String, Object>> fieldRows = new ArrayList<>();
        fieldRows.add(Components.fieldRow("Date", str(row.get("_date"))));
        for (Map<String, Object> a : visible(
                (List<Map<String, Object>>) meta.getOrDefault("attributes", List.of()), "visibleInDetail")) {
            fieldRows.add(Components.fieldRow(str(a.get("displayName")), cell(a, row)));
        }
        items.add(Components.card(fieldRows));

        for (Map<String, Object> ts : (List<Map<String, Object>>) meta.getOrDefault("tabularSections", List.of())) {
            List<Map<String, Object>> tsAttrs = (List<Map<String, Object>>) ts.getOrDefault("attributes", List.of());
            List<Map<String, Object>> tsRows = (List<Map<String, Object>>) row.getOrDefault(str(ts.get("name")), List.of());

            List<String> headers = new ArrayList<>(List.of("#"));
            for (Map<String, Object> a : tsAttrs) headers.add(str(a.get("displayName")));

            List<Components.Row> body = new ArrayList<>();
            int line = 1;
            for (Map<String, Object> tsRow : tsRows) {
                List<String> cells = new ArrayList<>();
                Object ln = tsRow.get("_line_number");
                cells.add(ln != null ? str(ln) : String.valueOf(line));
                for (Map<String, Object> a : tsAttrs) cells.add(cell(a, tsRow));
                body.add(new Components.Row(cells, null));
                line++;
            }
            items.add(sectionLabel(str(ts.get("name"))));
            items.add(Components.table(headers, body));
        }

        return content(items);
    }

    // ----- register report -----

    @SuppressWarnings("unchecked")
    public static Map<String, Object> registerReport(Map<String, Object> meta,
                                                     List<Map<String, Object>> movements,
                                                     List<Map<String, Object>> balances) {
        String type = str(meta.get("type"));
        List<Map<String, Object>> dimensions = (List<Map<String, Object>>) meta.getOrDefault("dimensions", List.of());
        List<Map<String, Object>> resources = (List<Map<String, Object>>) meta.getOrDefault("resources", List.of());

        List<Map<String, Object>> items = new ArrayList<>();
        items.add(Components.pageHeader(str(meta.get("name")),
                "BALANCE".equals(type) ? "Balance register" : "Turnover register"));

        if ("BALANCE".equals(type) && balances != null) {
            List<String> headers = new ArrayList<>();
            for (Map<String, Object> d : dimensions) headers.add(str(d.get("displayName")));
            for (Map<String, Object> r : resources) headers.add(str(r.get("displayName")));
            List<Components.Row> body = new ArrayList<>();
            for (Map<String, Object> row : balances) {
                List<String> cells = new ArrayList<>();
                for (Map<String, Object> d : dimensions) cells.add(cell(d, row));
                for (Map<String, Object> r : resources) cells.add(cell(r, row));
                body.add(new Components.Row(cells, null));
            }
            items.add(sectionLabel("Balance"));
            items.add(Components.table(headers, body));
        }

        List<String> headers = new ArrayList<>(List.of("Period", "Type"));
        for (Map<String, Object> d : dimensions) headers.add(str(d.get("displayName")));
        for (Map<String, Object> r : resources) headers.add(str(r.get("displayName")));
        List<Components.Row> body = new ArrayList<>();
        for (Map<String, Object> row : movements) {
            List<String> cells = new ArrayList<>();
            cells.add(str(row.get("_period")));
            cells.add(str(row.get("_movement_type")));
            for (Map<String, Object> d : dimensions) cells.add(cell(d, row));
            for (Map<String, Object> r : resources) cells.add(cell(r, row));
            body.add(new Components.Row(cells, null));
        }
        items.add(sectionLabel("Movements"));
        items.add(Components.table(headers, body));

        return content(items);
    }

    // ----- shared helpers -----

    private static Map<String, Object> content(List<Map<String, Object>> items) {
        Map<String, Object> root = Div.vertical(items);
        Div.matchWidth(root);
        Div.gap(root, 4);
        return root;
    }

    private static Map<String, Object> detailHeader(String title, boolean posted) {
        Map<String, Object> heading = Div.color(Div.text(title, 22, "bold"), Palette.TEXT);
        Map<String, Object> spacer = Div.weight(Div.horizontal(List.of()), 1);
        Map<String, Object> badge = Components.statusBadge(posted, posted ? "Posted" : "Draft");
        Map<String, Object> row = Div.horizontal(List.of(heading, spacer, badge));
        Div.matchWidth(row);
        Div.alignV(row, "center");
        Div.margins(row, 0, 0, 16, 0);
        return row;
    }

    private static Map<String, Object> sectionLabel(String text) {
        Map<String, Object> label = Div.color(Div.text(text, 13, "medium"), Palette.MUTED);
        Div.margins(label, 16, 0, 8, 2);
        return label;
    }

    private static String count(int n, String noun) {
        return n + " " + noun + (n == 1 ? "" : "s");
    }

    private static List<Map<String, Object>> visible(List<Map<String, Object>> attrs, String slot) {
        return attrs.stream()
                .filter(a -> Boolean.TRUE.equals(a.get(slot)))
                .sorted(Comparator.comparingInt(a -> a.get("order") == null
                        ? 0 : ((Number) a.get("order")).intValue()))
                .toList();
    }

    private static String cell(Map<String, Object> attr, Map<String, Object> row) {
        String col = str(attr.get("columnName"));
        Object display = row.get(col + "_display");
        Object value = display != null ? display : row.get(col);
        return value == null ? "" : value.toString();
    }

    private static String str(Object value) {
        return value == null ? "" : value.toString();
    }
}
