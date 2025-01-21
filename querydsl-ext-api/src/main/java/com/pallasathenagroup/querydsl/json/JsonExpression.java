package com.pallasathenagroup.querydsl.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.pallasathenagroup.querydsl.CommonOps;
import com.querydsl.core.types.ConstantImpl;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.Visitor;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.SimpleExpression;
import com.querydsl.core.types.dsl.StringExpression;
import com.vladmihalcea.hibernate.type.util.Configuration;
import java.util.List;

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
        return contains(JsonExpressions.jsonbConstant(object));
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

    /**
     * Retrieves JSON elements based on their paths.
     *
     * @param paths An array of paths to the JSON elements to be retrieved.
     *              Each path can be either a simple key or a key path separated by dots.
     * @return A JsonExpression object representing the result of the retrieval operation.
     * @throws IllegalArgumentException If the provided paths are null, empty, or if they contain invalid patterns.
     */
    public JsonExpression<JsonNode> get(String... paths) {
        String[] keys = this.validatePaths(paths);
        return get(JsonExpressions.arrayConstant(keys));
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
        return concat(JsonExpressions.jsonbConstant(List.of(other)));
    }

    public <A> JsonOperation<ArrayNode> concat(List<A> other) {
        return concat(JsonExpressions.jsonbConstant(other));
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

    public JsonOperation<JsonNode> deleteByKey(Expression<String> key) {
        return new JsonOperation<>(Expressions.operation(JsonNode.class, JsonOps.JSON_DELETE_KEY, mixin, key));
    }

    public JsonOperation<JsonNode> deleteByKey(String key) {
        return deleteByKey(Expressions.constant(key));
    }

    public JsonOperation<ArrayNode> deleteByIndex(Expression<Integer> index) {
        return new JsonOperation<>(Expressions.operation(ArrayNode.class, JsonOps.JSON_DELETE_INDEX, mixin, index));
    }

    public JsonOperation<ArrayNode> deleteByIndex(int index) {
        return deleteByIndex(Expressions.constant(index));
    }

    /**
     * Deletes JSON elements based on their paths.
     *
     * @param paths An array of paths to the JSON elements to be deleted.
     *              Each path can be either a simple key or a key path separated by dots.
     * @return A JsonExpression object representing the result of the deletion operation.
     * @throws IllegalArgumentException If the provided paths are null, empty, or if they contain invalid patterns.
     */
    public JsonExpression<JsonNode> deleteByPath(String... paths) {
        String[] keys = this.validatePaths(paths);
        return deleteByPath(JsonExpressions.arrayConstant(keys));
    }

    public JsonOperation<JsonNode> deleteByPath(Expression<?> path) {
        return new JsonOperation<>(Expressions.operation(JsonNode.class, JsonOps.JSON_DELETE_PATH, mixin, path));
    }

    public JsonOperation<JsonNode> set(String path, Object value) {
        String[] keys = this.validatePaths(path);
        var afValue = Configuration.INSTANCE.getObjectMapperWrapper().getObjectMapper().valueToTree(value);
        return set(JsonExpressions.arrayConstant(keys), JsonExpressions.jsonbConstant(afValue));
    }

    public JsonOperation<JsonNode> set(Expression<?> path, Expression<?> value) {
        return new JsonOperation<>(Expressions.operation(JsonNode.class, JsonOps.SET, mixin, path, value));
    }

    public JsonOperation<JsonNode> coalesce(Expression<?> expr) {
        return new JsonOperation<>(Expressions.operation(JsonNode.class, Ops.COALESCE, Expressions.list(mixin, expr)));
    }

    public BooleanExpression isEmptyArray() {
        return Expressions.anyOf(
                this.isNull(),
                Expressions.allOf(
                        this.isArray(),
                        this.size().eq(0)
                ));
    }

    public BooleanExpression isArray() {
        return Expressions.allOf(
                this.isNotNull(),
                Expressions.stringOperation(JsonOps.JSON_TYPEOF, mixin).eq("array")
        );
    }

    private String[] validatePaths(String... paths) {
        // Check if paths are null or empty; throw IllegalArgumentException if they are.
        if (paths == null || paths.length == 0) {
            throw new IllegalArgumentException("Path cannot be null or empty.");
        }
        boolean containsDot = false;
        String[] keys;

        // Iterate through the list of paths to check for the presence of dots.
        for (String path : paths) {
            if (path.isEmpty()){
                throw new IllegalArgumentException("The provided path is invalid.");
            }
            if (path.contains(".")) {
                containsDot = true;
                break;
            }
        }

        // Handle based on context:
        if (containsDot) {
            // If there is a dot and more than one path, throw an IllegalArgumentException.
            if (paths.length > 1) {
                throw new IllegalArgumentException("The provided path is invalid.");
            } else {
                // If there is only one path with a dot, split it into keys.
                keys = paths[0].split("\\.");
            }
        } else {
            // If there are no dots in the paths, use them as keys.
            keys = paths;
        }
        return keys;
    }

}
