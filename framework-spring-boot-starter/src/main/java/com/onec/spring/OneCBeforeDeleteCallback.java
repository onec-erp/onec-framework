package com.onec.spring;

import com.onec.lifecycle.BeforeDeleteHandler;

import org.springframework.data.relational.core.mapping.event.BeforeDeleteCallback;
import org.springframework.data.relational.core.conversion.MutableAggregateChange;

public class OneCBeforeDeleteCallback implements BeforeDeleteCallback<Object> {

    @Override
    public Object onBeforeDelete(Object aggregate, MutableAggregateChange<Object> aggregateChange) {
        if (aggregate instanceof BeforeDeleteHandler handler) {
            handler.beforeDelete();
        }
        return aggregate;
    }
}
