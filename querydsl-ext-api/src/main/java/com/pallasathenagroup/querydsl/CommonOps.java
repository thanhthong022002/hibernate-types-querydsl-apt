package com.pallasathenagroup.querydsl;

import com.querydsl.core.types.Operator;

public enum CommonOps implements Operator {
    CAST(Object.class);

    private final Class<?> type;

    private CommonOps(Class<?> type) {
        this.type = type;
    }

    @Override
    public Class<?> getType() {
        return type;
    }
}
