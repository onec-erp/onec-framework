package su.onno.fixtures;

import su.onno.annotations.EnumLabel;
import su.onno.annotations.Enumeration;

/**
 * Fixture exercising {@code @Enumeration.title} and per-constant {@code @EnumLabel}: a labelled value
 * with a badge colour ({@code NEW}), a labelled value without one ({@code IN_PROGRESS}), and a
 * deliberately unlabelled constant ({@code COMPLETED}) to cover the name-fallback / no-colour paths.
 */
@Enumeration(name = "LabeledStatuses", title = "Статусы заказов")
public enum TestLabeledStatus {
    @EnumLabel(value = "Новый", color = "#F4C7C3") NEW,
    @EnumLabel("В работе") IN_PROGRESS,
    COMPLETED
}
