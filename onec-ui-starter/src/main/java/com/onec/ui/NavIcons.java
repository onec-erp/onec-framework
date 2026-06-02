package com.onec.ui;

/**
 * Picks a distinct glyph for a navigation entry from its entity name, so the nav
 * (sidebar, bottom bar, mobile menu) doesn't repeat one section icon down every row.
 * A heuristic default only — a layout that sets an explicit icon should win, and the
 * section icon is the fallback when nothing matches.
 *
 * <p>Icon names map to the bundled monochrome SVGs under {@code public/icons}.</p>
 */
final class NavIcons {

    private NavIcons() {}

    /** A per-item icon derived from {@code name}, falling back to {@code sectionIcon}. */
    static String forItem(String name, String type, String sectionIcon) {
        String n = name == null ? "" : name.toLowerCase();

        if (has(n, "propert", "apartment", "room", "unit")) return "building";
        if (has(n, "bed", "occupan")) return "bed";
        if (has(n, "book", "reservation", "stay")) return "calendar";
        if (has(n, "client", "customer", "guest", "tenant", "contact")) return "users";
        if (has(n, "employee", "staff", "user", "people", "person")) return "user";
        if (has(n, "bill", "invoice")) return "receipt";
        if (has(n, "payment", "transaction")) return "wallet";
        if (has(n, "bank", "account", "balance", "cash")) return "banknote";
        if (has(n, "receivable", "payable", "ledger")) return "file-text";
        if (has(n, "revenue", "sales", "report", "income", "stat")) return "bar-chart";
        if (has(n, "countr", "region", "location", "address", "place")) return "map-pin";
        if (has(n, "product", "item", "good", "stock", "inventor", "warehouse")) return "package";
        if (has(n, "dashboard", "home", "overview")) return "home";

        // Registers are reporting surfaces; a chart reads better than a blank section icon.
        if ("register".equals(type)) return "bar-chart";

        return sectionIcon == null || sectionIcon.isBlank() ? "circle" : sectionIcon;
    }

    private static boolean has(String haystack, String... needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) return true;
        }
        return false;
    }
}
