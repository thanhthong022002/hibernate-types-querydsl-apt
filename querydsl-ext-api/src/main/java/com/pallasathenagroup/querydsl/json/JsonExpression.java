package com.pallasathenagroup.querydsl.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.pallasathenagroup.querydsl.CommonOps;
import com.querydsl.core.types.ConstantImpl;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.Visitor;
import com.querydsl.core.types.dsl.*;
import com.vladmihalcea.hibernate.type.array.StringArrayType;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import org.hibernate.jpa.TypedParameterValue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class JsonExpression<T> extends SimpleExpression<T> {

    private transient volatile BooleanExpression isnull, isnotnull;

    public JsonExpression(Expression<T> mixin) {
        super(mixin);
    }

    @Override
    public <R, C> @org.jetbrains.annotations.Nullable R accept(Visitor<R, C> v, @org.jetbrains.annotations.Nullable C context) {
        return mixin.accept(v, context);
    }

    public BooleanExpression containsKey(Expression<String> key) {
        return Expressions.booleanOperation(JsonOps.CONTAINS_KEY, mixin, key);
    }

    public BooleanExpression containsKey(String key) {
        return containsKey(Expressions.constant(key));
    }

    public BooleanExpression contains(JsonExpression<?> object) {
        return Expressions.booleanOperation(JsonOps.CONTAINS, mixin, object);
    }

    public BooleanExpression contains(Object object) {
        return contains(jsonbConstant(object));
    }

    public JsonOperation<JsonNode> get(Expression<?> key) {
        return new JsonOperation<>(Expressions.operation(JsonNode.class, JsonOps.GET, mixin, key)) {
            @Override
            public StringExpression asText() {
                return Expressions.stringOperation(JsonOps.GET_TEXT, JsonExpression.this.mixin, key);
            }
        };
    }

    public JsonExpression<JsonNode> get(Integer index) {
        return get(Expressions.constant(index));
    }

    public JsonExpression<JsonNode> get(String... key) {
        String[] keys = Arrays.stream(Arrays.stream(key)
                        .collect(Collectors.joining("."))
                        .split("\\."))
                .toArray(String[]::new);
        return get(arrayConstant(keys));
    }

    public StringExpression asText() {
        throw new UnsupportedOperationException();
    }

    public <A extends Number & Comparable<? super A>> NumberExpression<A> asNumber(Class<A> type) {
        return Expressions.numberOperation(type, Ops.NUMCAST, mixin, ConstantImpl.create(type));
    }

    public NumberExpression<Integer> asInteger() {
        return asNumber(Integer.class);
    }

    public BooleanExpression asBoolean() {
        return Expressions.predicate(CommonOps.CAST, mixin, ConstantImpl.create("boolean"));
    }

    public NumberExpression<Long> asLong() {
        return asNumber(Long.class);
    }

    public JsonOperation<ArrayNode> concat(JsonExpression<?> other) {
        return new JsonOperation<>(Expressions.operation(ArrayNode.class, JsonOps.CONCAT, mixin, other));
    }

    public <A> JsonOperation<ArrayNode> concat(A... other) {
        // convert to list, since array will cause
        // IllegalArgumentException: Encountered array-valued parameter binding, but was expecting
        return concat(jsonbConstant(List.of(other)));
    }

    public <A> JsonOperation<ArrayNode> concat(List<A> other) {
        return concat(jsonbConstant(other));
    }

    public final NumberExpression<Integer> size() {
        return Expressions.numberOperation(Integer.class, JsonOps.MAP_SIZE, mixin);
    }

    public final StringExpression keys() {
        return Expressions.stringOperation(JsonOps.KEYS, mixin);
    }

    public final JsonOperation<JsonNode> elements() {
        return new JsonOperation<>(Expressions.operation(JsonNode.class, JsonOps.ELEMENTS, mixin));
    }

    @Override
    public BooleanExpression isNull() {
        if (isnull == null) {
            isnull = Expressions.anyOf(
                super.isNull(),
                eq((T) NullNode.instance)
            );
        }
        return isnull;
    }

    @Override
    public BooleanExpression isNotNull() {
        if (isnotnull == null) {
            isnotnull = Expressions.allOf(
                super.isNotNull(),
                ne((T) NullNode.instance)
            );
        }
        return isnotnull;
    }

    public JsonExpression<?> jsonbConstant(Object object) {
        return new JsonExpression<>(
                Expressions.constant(
                    new TypedParameterValue(
                            JsonBinaryType.INSTANCE,
                            object
                    )
                )
        );
    }

    public Expression<TypedParameterValue> arrayConstant(String... keys) {
        return Expressions.constant(
                new TypedParameterValue(
                        StringArrayType.INSTANCE,
                        keys
                )
        );
    }
    public JsonOperation<JsonNode> deleteByKey(Expression<String> key){
        return new JsonOperation<>(Expressions.operation(JsonNode.class, JsonOps.DELETE_KEY, mixin, key));
    }

    public JsonOperation<JsonNode> deleteByKey(String key){
        return deleteByKey(Expressions.constant(key));
    }
    public JsonOperation<ArrayNode> deleteByIndex(Expression<Integer> index){
        return new JsonOperation<>(Expressions.operation(ArrayNode.class, JsonOps.DELETE_INDEX, mixin, index));
    }
    public JsonOperation<ArrayNode> deleteByIndex(int index){
        return deleteByIndex(Expressions.constant(index));
    }


    public BooleanExpression isEmptyArray() {
        return Expressions.booleanOperation(JsonOps.IS_EMPTY_ARRAY, mixin);
    }
    public JsonExpression<JsonNode> deleteByPath(String... path) {
        String[] paths = Arrays.stream(
                        Arrays.stream(path)
                                .collect(Collectors.joining("."))
                                .split("\\."))
                .toArray(String[]::new);
        return deleteByPath(arrayConstant(paths));
    }
    public JsonOperation<JsonNode> deleteByPath(Expression<?> path) {
        return new JsonOperation<>(Expressions.operation(JsonNode.class, JsonOps.DELETE_PATH, mixin, path));
    }
}
