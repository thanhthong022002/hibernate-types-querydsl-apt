package com.pallasathenagroup.querydsl;

import com.pallasathenagroup.querydsl.json.JsonPath;
import com.querydsl.core.JoinType;
import com.querydsl.core.dml.UpdateClause;
import com.querydsl.core.support.QueryMixin;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAQueryMixin;
import com.querydsl.jpa.JPQLSerializer;
import com.querydsl.jpa.JPQLTemplates;
import com.querydsl.jpa.impl.JPAProvider;
import com.querydsl.jpa.impl.JPAUtil;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import org.hibernate.query.internal.QueryImpl;

public class ExtendJpaUpdateClause implements UpdateClause<ExtendJpaUpdateClause> {

    private final QueryMixin<?> queryMixin = new JPAQueryMixin<Void>();

    private final Map<Path<?>, Expression<?>> updates = new LinkedHashMap<>();
    private final Set<JsonUpdatePair> jsonUpdates = new LinkedHashSet<>();

    private final EntityManager entityManager;

    private final JPQLTemplates templates;

    private LockModeType lockMode;

    public ExtendJpaUpdateClause(EntityManager em, EntityPath<?> entity) {
        this(em, entity, JPAProvider.getTemplates(em));
    }

    public ExtendJpaUpdateClause(EntityManager em, EntityPath<?> entity, JPQLTemplates templates) {
        this.entityManager = em;
        this.templates = templates;
        queryMixin.addJoin(JoinType.DEFAULT, entity);
    }

    @Override
    public long execute() {
        ExtendJPQLSerializer serializer = new ExtendJPQLSerializer(templates, entityManager);
        serializer.serializeForUpdate(queryMixin.getMetadata(), updates, jsonUpdates);

        // use custom query impl to allow set List as a value, instead of `list of values`
        Query query = new ExtendQueryImpl<>(
                (QueryImpl) entityManager.createQuery(serializer.toString()));
        if (lockMode != null) {
            query.setLockMode(lockMode);
        }
        JPAUtil.setConstants(query, serializer.getConstants(), queryMixin.getMetadata().getParams());
        return query.executeUpdate();
    }

    @Override
    public <T> ExtendJpaUpdateClause set(Path<T> path, T value) {
        if (value != null) {
            updates.put(path, Expressions.constant(value));
        } else {
            setNull(path);
        }
        return this;
    }

    @Override
    public <T> ExtendJpaUpdateClause set(Path<T> path, Expression<? extends T> expression) {
        if (expression != null) {
            updates.put(path, expression);
        } else {
            setNull(path);
        }
        return this;
    }

    /**
     * A tricky way to use when set value of type {@link org.hibernate.jpa.TypedParameterValue}
     * @param path
     * @param expression
     * @param <T>
     * @return
     */
    public <T> ExtendJpaUpdateClause setRaw(Path<T> path, Expression<?> expression) {
        if (expression != null) {
            updates.put(path, expression);
        } else {
            setNull(path);
        }
        return this;
    }

    public ExtendJpaUpdateClause set(JsonPath<?> path, Expression<?> expression) {
        if (expression != null) {
            updates.put(path, expression);
        } else {
            setNull(path);
        }
        return this;
    }

    public ExtendJpaUpdateClause set(JsonPath<?> path, String jsonPath, Object expression) {
        var innerPath = jsonPath.split("\\.");
        jsonUpdates.add(new JsonUpdatePair(path, innerPath, expression));
        return this;
    }

    @Override
    public <T> ExtendJpaUpdateClause setNull(Path<T> path) {
        updates.put(path, Expressions.nullExpression(path));
        return this;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public ExtendJpaUpdateClause set(List<? extends Path<?>> paths, List<?> values) {
        for (int i = 0; i < paths.size(); i++) {
            if (values.get(i) != null) {
                updates.put(paths.get(i), Expressions.constant(values.get(i)));
            } else {
                updates.put(paths.get(i), Expressions.nullExpression(paths.get(i)));
            }
        }
        return this;
    }

    @Override
    public ExtendJpaUpdateClause where(Predicate... o) {
        for (Predicate p : o) {
            queryMixin.where(p);
        }
        return this;
    }

    public ExtendJpaUpdateClause setLockMode(LockModeType lockMode) {
        this.lockMode = lockMode;
        return this;
    }

    @Override
    public String toString() {
        JPQLSerializer serializer = new JPQLSerializer(templates, entityManager);
        serializer.serializeForUpdate(queryMixin.getMetadata(), updates);
        return serializer.toString();
    }

    @Override
    public boolean isEmpty() {
        return updates.isEmpty();
    }

    public static final class JsonUpdatePair {
        public JsonPath field;
        public String[] path;
        public Object value;

        public JsonUpdatePair(JsonPath field, String[] path, Object value) {
            this.field = field;
            this.path = path;
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            JsonUpdatePair that = (JsonUpdatePair) o;
            return field.equals(that.field) && Arrays.equals(path, that.path);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(field);
            result = 31 * result + Arrays.hashCode(path);
            return result;
        }
    }
}
