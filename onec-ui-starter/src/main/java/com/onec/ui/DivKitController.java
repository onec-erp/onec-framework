package com.onec.ui;

import com.onec.metadata.AccumulationRegisterDescriptor;
import com.onec.metadata.CatalogDescriptor;
import com.onec.metadata.DocumentDescriptor;
import com.onec.model.AccumulationType;
import com.onec.ui.divkit.DashboardDivBuilder;
import com.onec.ui.divkit.DivCard;
import com.onec.ui.divkit.Palette;
import com.onec.ui.divkit.ShellLayoutBuilder;
import com.onec.ui.divkit.SurfaceDivBuilder;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Emits the full server-rendered DivKit app: every endpoint returns a complete
 * card (chrome + surface content) for the caller's resolved persona, intersected
 * with RBAC, in the client's theme. The client is a thin DivKit canvas; it passes
 * {@code ?viewport=mobile} and {@code ?theme=dark} so the layout and colors are
 * chosen server-side — the same hooks a Flutter client would use.
 */
@RestController
@RequestMapping("/api/divkit")
public class DivKitController {

    private final UiLayout layout;
    private final UiLayoutResolver layoutResolver;
    private final UiProfileResolver profileResolver;
    private final UiAccessService access;
    private final CurrentUserResolver currentUserResolver;
    private final ResolvedMetadataService resolvedMetadata;
    private final CatalogQueryService catalogQuery;
    private final DocumentQueryService documentQuery;
    private final RegisterQueryService registerQuery;

    public DivKitController(UiLayout layout,
                            UiLayoutResolver layoutResolver,
                            UiProfileResolver profileResolver,
                            UiAccessService access,
                            CurrentUserResolver currentUserResolver,
                            ResolvedMetadataService resolvedMetadata,
                            CatalogQueryService catalogQuery,
                            DocumentQueryService documentQuery,
                            RegisterQueryService registerQuery) {
        this.layout = layout;
        this.layoutResolver = layoutResolver;
        this.profileResolver = profileResolver;
        this.access = access;
        this.currentUserResolver = currentUserResolver;
        this.resolvedMetadata = resolvedMetadata;
        this.catalogQuery = catalogQuery;
        this.documentQuery = documentQuery;
        this.registerQuery = registerQuery;
    }

    @GetMapping("/app")
    public Map<String, Object> app(@RequestParam(required = false) String profile,
                                   @RequestParam(required = false) String viewport,
                                   @RequestParam(required = false) String theme,
                                   Principal principal) {
        boolean mobile = isMobile(viewport);
        Palette p = Palette.of(theme);
        UiLayout.Profile active = activeProfile(principal, profile);
        CurrentUserResolver.CurrentUser user = currentUserResolver.resolve(principal);

        List<DashboardDivBuilder.Widget> widgets = layoutResolver.resolveWidgets(active).stream()
                .filter(w -> access.canRead(principal, w.entityType(), w.entityName()))
                .map(w -> new DashboardDivBuilder.Widget(w.title(), w.widgetType()))
                .toList();
        String title = active.title() == null || active.title().isBlank() ? "Dashboard" : active.title();
        Map<String, Object> content = DashboardDivBuilder.build(
                title, "Welcome back, " + user.displayName(), widgets, mobile ? 1 : 2, p);

        return renderShell(principal, active, user, mobile, p, null, content);
    }

    @GetMapping("/catalogs/{name}")
    public Map<String, Object> catalogList(@PathVariable String name,
                                           @RequestParam(required = false) String profile,
                                           @RequestParam(required = false) String viewport,
                                           @RequestParam(required = false) String theme,
                                           Principal principal) {
        CatalogDescriptor desc = catalogQuery.require(name);
        access.requireRead(principal, desc);
        Palette p = Palette.of(theme);
        Map<String, Object> content = SurfaceDivBuilder.catalogList(
                resolvedMetadata.describeCatalog(desc), catalogQuery.list(desc), p);
        return renderShell(principal, profile, isMobile(viewport), p, "/catalogs/" + name, content);
    }

    @GetMapping("/documents/{name}")
    public Map<String, Object> documentList(@PathVariable String name,
                                            @RequestParam(required = false) String from,
                                            @RequestParam(required = false) String to,
                                            @RequestParam(required = false) String profile,
                                            @RequestParam(required = false) String viewport,
                                            @RequestParam(required = false) String theme,
                                            Principal principal) {
        DocumentDescriptor desc = documentQuery.require(name);
        access.requireRead(principal, desc);
        Palette p = Palette.of(theme);
        Map<String, Object> content = SurfaceDivBuilder.documentList(
                resolvedMetadata.describeDocument(desc), documentQuery.list(desc, from, to), name, p);
        return renderShell(principal, profile, isMobile(viewport), p, "/documents/" + name, content);
    }

    @GetMapping("/documents/{name}/{id}")
    public Map<String, Object> documentDetail(@PathVariable String name, @PathVariable UUID id,
                                              @RequestParam(required = false) String profile,
                                              @RequestParam(required = false) String viewport,
                                              @RequestParam(required = false) String theme,
                                              Principal principal) {
        DocumentDescriptor desc = documentQuery.require(name);
        access.requireRead(principal, desc);
        Palette p = Palette.of(theme);
        Map<String, Object> content = SurfaceDivBuilder.documentDetail(
                resolvedMetadata.describeDocument(desc), documentQuery.get(desc, id), p);
        return renderShell(principal, profile, isMobile(viewport), p, "/documents/" + name, content);
    }

    @GetMapping("/registers/{name}")
    public Map<String, Object> registerReport(@PathVariable String name,
                                              @RequestParam(required = false) String from,
                                              @RequestParam(required = false) String to,
                                              @RequestParam(required = false) String profile,
                                              @RequestParam(required = false) String viewport,
                                              @RequestParam(required = false) String theme,
                                              Principal principal) {
        AccumulationRegisterDescriptor desc = registerQuery.require(name);
        access.requireRead(principal, desc);
        Palette p = Palette.of(theme);
        List<Map<String, Object>> balances = desc.accumulationType() == AccumulationType.BALANCE
                ? registerQuery.balance(desc, Map.of())
                : null;
        Map<String, Object> content = SurfaceDivBuilder.registerReport(
                resolvedMetadata.describeRegister(desc), registerQuery.movements(desc, from, to), balances, p);
        return renderShell(principal, profile, isMobile(viewport), p, "/registers/" + name, content);
    }

    // ----- shell assembly -----

    private Map<String, Object> renderShell(Principal principal, String profileParam, boolean mobile,
                                            Palette p, String activePath, Map<String, Object> content) {
        UiLayout.Profile active = activeProfile(principal, profileParam);
        return renderShell(principal, active, currentUserResolver.resolve(principal), mobile, p, activePath, content);
    }

    private Map<String, Object> renderShell(Principal principal, UiLayout.Profile active,
                                            CurrentUserResolver.CurrentUser user, boolean mobile,
                                            Palette p, String activePath, Map<String, Object> content) {
        List<ShellLayoutBuilder.NavSection> nav = new ArrayList<>();
        for (UiLayout.ResolvedSection section : layoutResolver.resolve(active)) {
            List<ShellLayoutBuilder.NavItem> items = section.items().stream()
                    .filter(item -> access.canRead(principal, item.type(), item.name()))
                    .map(item -> new ShellLayoutBuilder.NavItem(
                            item.name(), "onec:/" + item.href(), item.href().equals(activePath)))
                    .toList();
            if (!items.isEmpty()) {
                nav.add(new ShellLayoutBuilder.NavSection(section.name(), items));
            }
        }

        List<ShellLayoutBuilder.ProfileLink> profileLinks = profileResolver.switchable(layout, access.roles(principal))
                .stream()
                .map(pl -> new ShellLayoutBuilder.ProfileLink(pl.id(),
                        pl.title() == null || pl.title().isBlank() ? pl.id() : pl.title()))
                .toList();

        String brand = active.title() == null || active.title().isBlank() ? "OneC" : active.title();
        Map<String, Object> root = ShellLayoutBuilder.build(
                brand, user.displayName(), profileLinks, active.id(), nav, content, mobile, p);

        List<Map<String, Object>> variables = active.theme() == null || active.theme().isBlank()
                ? List.of()
                : List.of(DivCard.stringVar("theme", active.theme()));
        return DivCard.of("onec-app", root, variables);
    }

    private UiLayout.Profile activeProfile(Principal principal, String profileParam) {
        Set<String> roles = access.roles(principal);
        UiProfileResolver.Resolution resolution = profileResolver.resolve(layout, roles);
        Set<String> switchableIds = resolution.switchable().stream()
                .map(UiLayout.Profile::id)
                .collect(Collectors.toSet());
        return profileParam != null && switchableIds.contains(profileParam)
                ? profileResolver.byId(layout, profileParam)
                : resolution.profile();
    }

    private static boolean isMobile(String viewport) {
        return "mobile".equalsIgnoreCase(viewport);
    }
}
