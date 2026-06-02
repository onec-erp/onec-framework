package com.onec.ui;

import com.onec.fixtures.TestProduct;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UiLayoutBuilderTest {

    @Test
    void catalogWithoutConfigurer_hasEmptyFieldHints() {
        UiLayoutBuilder layout = new UiLayoutBuilder();
        layout.section("Reference").catalog(TestProduct.class);

        List<UiLayout.Section> sections = layout.build();
        assertThat(sections).hasSize(1);

        UiLayoutBuilder.EntityRef ref = sections.get(0).entityRefs().get(0);
        assertThat(ref.type()).isEqualTo("catalog");
        assertThat(ref.javaClass()).isEqualTo(TestProduct.class);
        assertThat(ref.fieldHints()).isEmpty();
    }

    @Test
    void catalogWithConfigurer_capturesFieldHints() {
        UiLayoutBuilder layout = new UiLayoutBuilder();
        layout.section("Reference").catalog(TestProduct.class, c -> c
                .field("name").order(0).widget("textarea").hideInForm()
                .field("code").order(1).group("identity").width("1/4"));

        UiLayoutBuilder.EntityRef ref = layout.build().get(0).entityRefs().get(0);
        assertThat(ref.fieldHints()).containsOnlyKeys("name", "code");

        FieldHint name = ref.fieldHints().get("name");
        assertThat(name.order()).isEqualTo(0);
        assertThat(name.widget()).isEqualTo("textarea");
        assertThat(name.visibleInForm()).isFalse();
        // unset hints stay null so they can fall through to scanner defaults
        assertThat(name.visibleInList()).isNull();
        assertThat(name.group()).isNull();
        assertThat(name.width()).isNull();

        FieldHint code = ref.fieldHints().get("code");
        assertThat(code.order()).isEqualTo(1);
        assertThat(code.group()).isEqualTo("identity");
        assertThat(code.width()).isEqualTo("1/4");
        assertThat(code.widget()).isNull();
    }

    @Test
    void documentAndRegisterOverloads_acceptConfigurer() {
        UiLayoutBuilder layout = new UiLayoutBuilder();
        layout.section("Sales")
                .document(TestProduct.class, d -> d.field("total").hideInList())
                .register(TestProduct.class, r -> r.field("quantity").order(5));

        List<UiLayoutBuilder.EntityRef> refs = layout.build().get(0).entityRefs();
        assertThat(refs).extracting(UiLayoutBuilder.EntityRef::type)
                .containsExactly("document", "register");
        assertThat(refs.get(0).fieldHints().get("total").visibleInList()).isFalse();
        assertThat(refs.get(1).fieldHints().get("quantity").order()).isEqualTo(5);
    }
}
