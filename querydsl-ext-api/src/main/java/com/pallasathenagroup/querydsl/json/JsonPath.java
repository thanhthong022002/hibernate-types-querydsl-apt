package com.pallasathenagroup.querydsl.json;

import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.EntityPathBase;
import java.lang.reflect.AnnotatedElement;

public class JsonPath<T> extends JsonExpression<T> implements Path<T> {

    public JsonPath(Path<T> mixin) {
        super(mixin);
    }

    public JsonPath(PathMetadata pathMetadata) {
        this(ExpressionUtils.path(getTypeFromMetadata(pathMetadata), pathMetadata));
    }

    @SuppressWarnings("unchecked")
    private static Class getTypeFromMetadata(PathMetadata metadata) {
        try {
            return ((EntityPathBase) metadata.getParent()).getType().getDeclaredField(metadata.getName()).getType();
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    @Override
    public PathMetadata getMetadata() {
        return ((Path<T>) mixin).getMetadata();
    }

    @Override
    public Path<?> getRoot() {
        return ((Path<T>) mixin).getRoot();
    }

    @Override
    public AnnotatedElement getAnnotatedElement() {
        return ((Path<T>) mixin).getAnnotatedElement();
    }

}
