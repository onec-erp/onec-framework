package com.onec.ui.divkit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Wraps a content div in the full persona app chrome as real DivKit, responsively:
 * a top bar everywhere, a left sidebar on desktop, a bottom tab bar on mobile.
 * The client picks {@code mobile} via a {@code ?viewport} hint, so a Flutter
 * client gets the same server-driven, viewport-appropriate layout for free.
 *
 * <p>Navigation/profile/logout intents are {@code onec://} action URLs the host
 * maps to routes, profile re-fetches, and session calls.</p>
 */
public final class ShellLayoutBuilder {

    private ShellLayoutBuilder() {}

    public record ProfileLink(String id, String title) {}

    public record NavItem(String label, String url, boolean active) {}

    public record NavSection(String title, List<NavItem> items) {}

    public static Map<String, Object> build(String brand,
                                            String userName,
                                            List<ProfileLink> profiles,
                                            String activeProfileId,
                                            List<NavSection> nav,
                                            Map<String, Object> content,
                                            boolean mobile) {
        return mobile
                ? mobileShell(brand, userName, profiles, activeProfileId, nav, content)
                : desktopShell(brand, userName, profiles, activeProfileId, nav, content);
    }

    // ----- desktop: topbar / [sidebar | content] -----

    private static Map<String, Object> desktopShell(String brand, String userName,
                                                    List<ProfileLink> profiles, String activeProfileId,
                                                    List<NavSection> nav, Map<String, Object> content) {
        Map<String, Object> topbar = topbar(brand, userName, profiles, activeProfileId, false);

        Map<String, Object> contentArea = Div.gallery("vertical", List.of(content));
        Div.weight(contentArea, 1);
        Div.matchHeight(contentArea);
        Div.pad(contentArea, 24, 28);

        Map<String, Object> body = Div.horizontal(List.of(sidebar(nav), vDivider(), contentArea));
        Div.weightHeight(body, 1);
        Div.matchWidth(body);

        Map<String, Object> root = Div.vertical(List.of(topbar, hDivider(), body));
        Div.matchHeight(root);
        Div.background(root, Palette.PAGE);
        return root;
    }

    private static Map<String, Object> sidebar(List<NavSection> nav) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (NavSection section : nav) {
            Map<String, Object> heading = Div.text(section.title().toUpperCase(), 11, "medium");
            Div.color(heading, Palette.FAINT);
            Div.margins(heading, 12, 0, 4, 8);
            items.add(heading);
            for (NavItem item : section.items()) {
                items.add(navLink(item));
            }
        }
        Map<String, Object> sidebar = Div.vertical(items);
        Div.width(sidebar, 248);
        Div.matchHeight(sidebar);
        Div.background(sidebar, Palette.SURFACE);
        Div.pad(sidebar, 16, 12);
        return sidebar;
    }

    private static Map<String, Object> navLink(NavItem item) {
        Map<String, Object> link = Div.text(item.label(), 14, item.active() ? "medium" : "regular");
        Div.color(link, item.active() ? Palette.PRIMARY : Palette.TEXT);
        Div.matchWidth(link);
        Div.pad(link, 9, 10);
        Div.corner(link, 8);
        if (item.active()) {
            Div.background(link, Palette.PRIMARY_SOFT);
        }
        Div.action(link, "nav", item.url());
        return link;
    }

    // ----- mobile: topbar / content / bottom tab bar -----

    private static Map<String, Object> mobileShell(String brand, String userName,
                                                   List<ProfileLink> profiles, String activeProfileId,
                                                   List<NavSection> nav, Map<String, Object> content) {
        Map<String, Object> topbar = topbar(brand, userName, profiles, activeProfileId, true);

        Map<String, Object> contentArea = Div.gallery("vertical", List.of(content));
        Div.weightHeight(contentArea, 1);
        Div.pad(contentArea, 16, 16);

        Map<String, Object> root = Div.vertical(List.of(
                topbar, hDivider(), contentArea, hDivider(), bottomNav(nav)));
        Div.matchHeight(root);
        Div.background(root, Palette.PAGE);
        return root;
    }

    private static Map<String, Object> bottomNav(List<NavSection> nav) {
        List<NavItem> flat = new ArrayList<>();
        for (NavSection s : nav) {
            flat.addAll(s.items());
        }
        List<Map<String, Object>> tabs = new ArrayList<>();
        for (NavItem item : flat.subList(0, Math.min(5, flat.size()))) {
            Map<String, Object> label = Div.text(item.label(), 11,
                    item.active() ? "medium" : "regular");
            Div.color(label, item.active() ? Palette.PRIMARY : Palette.MUTED);
            Div.textAlign(label, "center");
            Map<String, Object> tab = Div.vertical(List.of(label));
            Div.weight(tab, 1);
            Div.alignH(tab, "center");
            Div.pad(tab, 10, 4);
            Div.action(tab, "nav", item.url());
            tabs.add(tab);
        }
        Map<String, Object> bar = Div.horizontal(tabs);
        Div.matchWidth(bar);
        Div.background(bar, Palette.SURFACE);
        return bar;
    }

    // ----- shared -----

    private static Map<String, Object> topbar(String brand, String userName,
                                              List<ProfileLink> profiles, String activeProfileId,
                                              boolean mobile) {
        List<Map<String, Object>> row = new ArrayList<>();

        Map<String, Object> brandText = Div.text(brand == null || brand.isBlank() ? "OneC" : brand, 17, "bold");
        Div.color(brandText, Palette.PRIMARY);
        row.add(brandText);

        Map<String, Object> spacer = Div.horizontal(List.of());
        Div.weight(spacer, 1);
        row.add(spacer);

        if (!mobile && profiles != null && profiles.size() > 1) {
            for (ProfileLink p : profiles) {
                row.add(profileChip(p, p.id().equals(activeProfileId)));
            }
        }

        if (userName != null && !userName.isBlank()) {
            Map<String, Object> user = Div.text(userName, 13, "regular");
            Div.color(user, Palette.MUTED);
            Div.margins(user, 0, 12, 0, 8);
            row.add(user);
        }

        Map<String, Object> logout = Div.text("Sign out", 13, "medium");
        Div.color(logout, Palette.PRIMARY);
        Div.background(logout, Palette.PRIMARY_SOFT);
        Div.pad(logout, 7, 12);
        Div.corner(logout, 8);
        Div.action(logout, "logout", "onec://logout");
        row.add(logout);

        Map<String, Object> bar = Div.horizontal(row);
        Div.matchWidth(bar);
        Div.background(bar, Palette.SURFACE);
        Div.pad(bar, 10, 16);
        Div.alignV(bar, "center");
        return bar;
    }

    private static Map<String, Object> profileChip(ProfileLink p, boolean active) {
        Map<String, Object> chip = Div.text(p.title(), 12, active ? "medium" : "regular");
        Div.color(chip, active ? Palette.PRIMARY : Palette.MUTED);
        if (active) {
            Div.background(chip, Palette.PRIMARY_SOFT);
        }
        Div.pad(chip, 6, 10);
        Div.corner(chip, 999);
        Div.margins(chip, 0, 6, 0, 0);
        Div.action(chip, "switch-" + p.id(), "onec://app?profile=" + p.id());
        return chip;
    }

    private static Map<String, Object> hDivider() {
        return Div.separator(Palette.BORDER);
    }

    private static Map<String, Object> vDivider() {
        Map<String, Object> divider = Div.vertical(List.of());
        Div.width(divider, 1);
        Div.matchHeight(divider);
        Div.background(divider, Palette.BORDER);
        return divider;
    }
}
