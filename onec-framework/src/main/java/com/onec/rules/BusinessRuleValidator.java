package com.onec.rules;

import com.onec.annotations.BusinessRule;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Collection;

public class BusinessRuleValidator {

    public void validate(Object target) {
        for (BusinessRule rule : target.getClass().getAnnotationsByType(BusinessRule.class)) {
            if (!evaluate(target, rule.expression())) {
                String message = rule.message().isBlank()
                        ? "Business rule failed: " + rule.name()
                        : rule.message();
                throw new IllegalStateException(message);
            }
        }
    }

    private boolean evaluate(Object target, String expression) {
        String expr = expression.trim();
        if (expr.endsWith(" not empty")) {
            Object value = read(target, expr.substring(0, expr.length() - " not empty".length()).trim());
            if (value instanceof Collection<?> collection) return !collection.isEmpty();
            if (value instanceof String string) return !string.isBlank();
            return value != null;
        }
        if (expr.endsWith(" != null")) {
            return read(target, expr.substring(0, expr.length() - " != null".length()).trim()) != null;
        }
        if (expr.endsWith(" == null")) {
            return read(target, expr.substring(0, expr.length() - " == null".length()).trim()) == null;
        }
        for (String op : new String[]{">=", "<=", ">", "<", "=="}) {
            int idx = expr.indexOf(" " + op + " ");
            if (idx > 0) {
                Object left = read(target, expr.substring(0, idx).trim());
                String rightRaw = expr.substring(idx + op.length() + 2).trim();
                return compare(left, rightRaw, op);
            }
        }
        throw new IllegalArgumentException("Unsupported business rule expression: " + expression);
    }

    private boolean compare(Object left, String rightRaw, String op) {
        if ("==".equals(op)) {
            if (rightRaw.startsWith("'") && rightRaw.endsWith("'")) {
                return String.valueOf(left).equals(rightRaw.substring(1, rightRaw.length() - 1));
            }
            return left != null && String.valueOf(left).equals(rightRaw);
        }
        BigDecimal leftNumber = toBigDecimal(left);
        BigDecimal rightNumber = new BigDecimal(rightRaw);
        int cmp = leftNumber.compareTo(rightNumber);
        return switch (op) {
            case ">=" -> cmp >= 0;
            case "<=" -> cmp <= 0;
            case ">" -> cmp > 0;
            case "<" -> cmp < 0;
            default -> false;
        };
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number n) return new BigDecimal(n.toString());
        return new BigDecimal(value.toString());
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
