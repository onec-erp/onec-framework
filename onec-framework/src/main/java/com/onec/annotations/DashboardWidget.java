package com.onec.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Dashboard widget bound to a catalog, document, or register.
 *
 * @deprecated Declare dashboard widgets in a {@code Page} bean instead, e.g.
 * {@snippet :
 *   class DashboardPage implements Page {
 *       public String route() { return "/"; }
 *       public void compose(PageBuilder page) {
 *           page.widget("Recent invoices")
 *               .type("list").order(0).width("1/2")
 *               .document(Invoice.class)
 *               .maxItems(8);
 *       }
 *   }
 * }
 * As of the resolver fix shipped with the page/layout migration, both
 * {@code /api/ui/metadata/dashboard} and {@code /api/ui/metadata/manifest}
 * read from authored pages; this annotation is no longer consulted by either
 * endpoint when the configurer declares any widgets. The annotation will be
 * removed in the next release.
 */
@Deprecated(since = "next", forRemoval = true)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(DashboardWidgets.class)
public @interface DashboardWidget {
    String title();
    String type();
    int order() default 0;
    String width() default "1/3";
    int maxItems() default 10;
    String dateField() default "";
    String titleField() default "";
    String[] extraConfig() default {};
}
