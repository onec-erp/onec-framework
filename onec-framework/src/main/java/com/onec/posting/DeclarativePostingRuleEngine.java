package com.onec.posting;

import com.onec.annotations.PostingRule;
import com.onec.model.AccumulationRecord;
import com.onec.model.DocumentObject;
import com.onec.repository.RegisterRepository;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;

public class DeclarativePostingRuleEngine {

    public void apply(DocumentObject document, PostingContext context) {
        for (PostingRule rule : document.getClass().getAnnotationsByType(PostingRule.class)) {
            applyRule(document, context, rule);
        }
    }

    private <T extends AccumulationRecord> void applyRule(
            DocumentObject document, PostingContext context, PostingRule rule) {
        @SuppressWarnings("unchecked")
        Class<T> registerClass = (Class<T>) rule.register();
        RegisterRepository<T> movements = context.movements(registerClass);

        if (rule.forEach().isBlank()) {
            createMovement(document, null, movements, rule);
            return;
        }

        Object rows = read(document, rule.forEach());
        if (!(rows instanceof List<?> list)) {
            throw new IllegalStateException("Posting rule forEach field is not a List: " + rule.forEach());
        }
        for (Object item : list) {
            createMovement(document, item, movements, rule);
        }
    }

    private <T extends AccumulationRecord> void createMovement(
            DocumentObject document, Object item, RegisterRepository<T> movements, PostingRule rule) {
        T record = switch (rule.movement()) {
            case RECEIPT -> movements.addReceipt();
            case EXPENSE -> movements.addExpense();
        };

        for (String mapping : rule.map()) {
            int eq = mapping.indexOf('=');
            if (eq < 1) {
                throw new IllegalArgumentException("Posting rule mapping must be target = source: " + mapping);
            }
            String target = mapping.substring(0, eq).trim();
            String source = mapping.substring(eq + 1).trim();
            write(record, target, resolve(document, item, source));
        }
    }

    private Object resolve(DocumentObject document, Object item, String source) {
        if (source.startsWith("document.")) {
            return read(document, source.substring("document.".length()));
        }
        if (source.startsWith("item.")) {
            if (item == null) {
                throw new IllegalStateException("Posting rule source uses item but forEach is empty: " + source);
            }
            return read(item, source.substring("item.".length()));
        }
        if (source.startsWith("'") && source.endsWith("'") && source.length() >= 2) {
            return source.substring(1, source.length() - 1);
        }
        if (source.matches("-?\\d+(\\.\\d+)?")) {
            return new BigDecimal(source);
        }
        return read(document, source);
    }

    private Object read(Object target, String fieldName) {
        try {
            Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException(
                    "Cannot read field " + fieldName + " from " + target.getClass().getName(), e);
        }
    }

    private void write(Object target, String fieldName, Object value) {
        try {
            Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException(
                    "Cannot write field " + fieldName + " on " + target.getClass().getName(), e);
        }
    }

    private Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }
}
