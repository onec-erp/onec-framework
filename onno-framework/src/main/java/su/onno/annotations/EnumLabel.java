package su.onno.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Human-facing display label for a single {@code @Enumeration} constant, distinct from the Java
 * constant name. Put it on the constant to localize or pretty-print the value
 * (e.g. {@code @EnumLabel("Новый") NEW}) <em>without</em> renaming the constant — the name stays
 * part of the data/API contract (it derives the value's stable UUID and is mirrored by importers
 * and list filters), while the label is what users see.
 *
 * <p>Surfaces wherever an enum value is displayed: {@code {column}_display} in the read API, the
 * {@code label} of each entry in an attribute's {@code enumValues} metadata, and the form dropdown.
 * When absent (or empty), the display falls back to the constant name.
 *
 * <p>An optional {@link #color()} paints the value as a colored pill — a status chip the colour of
 * the spreadsheet cell it replaces — in list cells, the form dropdown, and the detail view. It rides
 * the read API as {@code {column}_color} (next to {@code {column}_display}) and the {@code color} of
 * each {@code enumValues} entry; the client derives a readable (dark/light) text colour from it. When
 * absent the value renders as plain text, exactly as before.
 *
 * <pre>{@code
 * @Enumeration(name = "OrderStatuses", title = "Статусы заказов")
 * public enum OrderStatus {
 *     @EnumLabel(value = "Новый", color = "#F4C7C3") NEW,
 *     @EnumLabel(value = "Отгружен", color = "#0B8043") SHIPPED
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface EnumLabel {

    String value();

    /**
     * Optional badge colour for this value, as a CSS hex string ({@code "#RRGGBB"} or {@code "#RGB"}).
     * Drives a colored status pill wherever the value is shown. Empty (the default) renders plain text.
     */
    String color() default "";
}
