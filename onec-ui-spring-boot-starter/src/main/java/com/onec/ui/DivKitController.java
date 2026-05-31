package com.onec.ui;

import com.onec.metadata.DashboardWidgetDescriptor;
import com.onec.ui.divkit.AppShellBuilder;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Emits server-rendered DivKit cards. {@code GET /app} is the bootstrap a generic
 * client fetches on login: it returns the persona-tailored app shell for the
 * caller's resolved profile, intersected with RBAC. Profiles only curate what is
 * shown; access is still enforced per data endpoint elsewhere.
 */
@RestController
@RequestMapping("/api/ui/divkit")
public class DivKitController {

    private final UiLayout layout;
    private final UiLayoutResolver layoutResolver;
    private final UiProfileResolver profileResolver;
    private final UiAccessService access;
    private final CurrentUserResolver currentUserResolver;

    public DivKitController(UiLayout layout,
                            UiLayoutResolver layoutResolver,
                            UiProfileResolver profileResolver,
                            UiAccessService access,
                            CurrentUserResolver currentUserResolver) {
        this.layout = layout;
        this.layoutResolver = layoutResolver;
        this.profileResolver = profileResolver;
        this.access = access;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping("/app")
    public Map<String, Object> app(@RequestParam(required = false) String profile, Principal principal) {
        Set<String> roles = access.roles(principal);
        UiProfileResolver.Resolution resolution = profileResolver.resolve(layout, roles);

        List<UiLayout.Profile> switchable = resolution.switchable();
        Set<String> switchableIds = switchable.stream()
                .map(UiLayout.Profile::id)
                .collect(Collectors.toSet());
        // Honor a client-requested profile only if the user is eligible for it.
        UiLayout.Profile active = profile != null && switchableIds.contains(profile)
                ? profileResolver.byId(layout, profile)
                : resolution.profile();

        List<AppShellBuilder.NavSection> nav = new ArrayList<>();
        for (UiLayout.ResolvedSection section : layoutResolver.resolve(active)) {
            List<AppShellBuilder.NavItem> items = section.items().stream()
                    .filter(item -> access.canRead(principal, item.type(), item.name()))
                    .map(item -> new AppShellBuilder.NavItem(item.name(), "onec:/" + item.href()))
                    .toList();
            if (!items.isEmpty()) {
                nav.add(new AppShellBuilder.NavSection(section.name(), items));
            }
        }

        List<String> home = layoutResolver.resolveWidgets(active).stream()
                .filter(w -> access.canRead(principal, w.entityType(), w.entityName()))
                .map(DashboardWidgetDescriptor::title)
                .toList();

        List<AppShellBuilder.ProfileLink> profileLinks = switchable.stream()
                .map(p -> new AppShellBuilder.ProfileLink(p.id(),
                        p.title() == null || p.title().isBlank() ? p.id() : p.title()))
                .toList();

        CurrentUserResolver.CurrentUser user = currentUserResolver.resolve(principal);
        String title = active.title() == null || active.title().isBlank() ? "Home" : active.title();
        String greeting = "Hi, " + user.displayName();

        return AppShellBuilder.build(title, active.theme(), greeting, active.id(),
                profileLinks, nav, home);
    }
}
