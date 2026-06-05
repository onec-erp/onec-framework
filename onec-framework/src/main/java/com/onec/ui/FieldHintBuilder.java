package com.onec.ui;

/**
 * Per-field hint builder. Obtained from {@link EntityConfigBuilder#field(String)}
 * inside a lambda passed to {@code SectionBuilder.catalog/document/register}.
 *
 * <p>Chain field-level setters; call {@link #field(String)} to switch to another
 * field on the same entity. Anything not set falls through to {@code @UiHint}
 * (deprecated) or the scanner default.</p>
 */
public class FieldHintBuilder {

    private final EntityConfigBuilder parent;
    private final String fieldName;

    private Boolean visibleInList;
    private Boolean visibleInForm;
    private Boolean visibleInDetail;
    private Integer order;
    private String group;
    private String width;
    private String widget;
    private String placeholder;
    private String format;

    FieldHintBuilder(EntityConfigBuilder parent, String fieldName) {
        this.parent = parent;
        this.fieldName = fieldName;
    }

    public FieldHintBuilder order(int order) {
        this.order = order;
        return this;
    }

    public FieldHintBuilder group(String group) {
        this.group = group;
        return this;
    }

    public FieldHintBuilder width(String width) {
        this.width = width;
        return this;
    }

    public FieldHintBuilder widget(String widget) {
        this.widget = widget;
        return this;
    }

    /** Placeholder text shown in this field's empty input on the edit form. */
    public FieldHintBuilder placeholder(String placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    /**
     * How this field's value is displayed in tables (list) and the detail surface. The hint is
     * interpreted by value type:
     * <ul>
     *   <li><b>Dates / date-times</b> — a date pattern, e.g. {@code "dd-MM-yy"},
     *       {@code "dd/MM/yyyy HH:mm"} (uppercase {@code D}/{@code Y} are accepted as day/year).</li>
     *   <li><b>Numbers</b> — {@code "integer"}, {@code "decimal"}, {@code "percent"},
     *       {@code "currency"} (or {@code "currency:EUR"}), or a decimal pattern like
     *       {@code "#,##0.00"}.</li>
     * </ul>
     * It does not affect the edit form's input control (use {@link #widget(String)} for that).
     */
    public FieldHintBuilder format(String format) {
        this.format = format;
        return this;
    }

    public FieldHintBuilder hideInList() {
        this.visibleInList = false;
        return this;
    }

    public FieldHintBuilder hideInForm() {
        this.visibleInForm = false;
        return this;
    }

    public FieldHintBuilder hideInDetail() {
        this.visibleInDetail = false;
        return this;
    }

    public FieldHintBuilder visibleInList(boolean v) {
        this.visibleInList = v;
        return this;
    }

    public FieldHintBuilder visibleInForm(boolean v) {
        this.visibleInForm = v;
        return this;
    }

    public FieldHintBuilder visibleInDetail(boolean v) {
        this.visibleInDetail = v;
        return this;
    }

    /** Switch to configuring another field on the same entity. */
    public FieldHintBuilder field(String name) {
        return parent.field(name);
    }

    String fieldName() {
        return fieldName;
    }

    FieldHint build() {
        return new FieldHint(
                visibleInList, visibleInForm, visibleInDetail,
                order, group, width, widget, placeholder, format);
    }
}
