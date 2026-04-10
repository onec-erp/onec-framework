package com.onec.numbering;

public interface NumberGenerator {

    String nextNumber(String entityName, int length);

    String nextCode(String entityName, int length);
}
