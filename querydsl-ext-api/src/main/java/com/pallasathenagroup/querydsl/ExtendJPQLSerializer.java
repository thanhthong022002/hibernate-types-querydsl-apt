package com.pallasathenagroup.querydsl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.NullNode;
import com.querydsl.core.QueryMetadata;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Path;
import com.querydsl.jpa.JPQLSerializer;
import com.querydsl.jpa.JPQLTemplates;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import com.vladmihalcea.hibernate.type.util.Configuration;
import java.util.Map;
import java.util.Set;
import javax.persistence.EntityManager;
import org.hibernate.jpa.TypedParameterValue;

/**
 * Support serialize update a field in jsonb type
 */
public class ExtendJPQLSerializer extends JPQLSerializer {

    private static final String UPDATE = "update ";
    private static final String SET = "\nset ";
    private static final String WHERE = "\nwhere ";

    public ExtendJPQLSerializer(JPQLTemplates templates) {
        super(templates);
    }

    public ExtendJPQLSerializer(JPQLTemplates templates, EntityManager em) {
        super(templates, em);
    }

    public void serializeForUpdate(QueryMetadata md, Map<Path<?>, Expression<?>> updates,
                                   Set<ExtendJpaUpdateClause.JsonUpdatePair> jsonUpdate) {

        append(UPDATE);
        handleJoinTarget(md.getJoins().get(0));
        append(SET);
        boolean first = true;
        for (Map.Entry<Path<?>, Expression<?>> entry : updates.entrySet()) {
            if (!first) {
                append(", ");
            }
            handle(entry.getKey());
            append(" = ");
            handle(entry.getValue());
            first = false;
        }
        for (var entry : jsonUpdate) {
            if (!first) {
                append(", ");
            }

            var path = HibernateTypesExpressions.createArrayExpression(entry.path);
            var rawValue = entry.value;
            if (rawValue instanceof String) {
                var wrapper = Configuration.INSTANCE.getObjectMapperWrapper();
                try {
                    rawValue = wrapper.getObjectMapper().writeValueAsString(rawValue);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
            var value = rawValue instanceof Expression ? rawValue :
                    new TypedParameterValue(
                        JsonBinaryType.INSTANCE,
                        rawValue == null ? NullNode.instance : rawValue
                    );

            handle(entry.field);
            append(" = ");
            append(" jsonb_set(");
            handle(entry.field);
            append(",");
            handle(path);
            append(",");
            handle(value);
            append(")");
            first = false;
        }
        if (md.getWhere() != null) {
            append(WHERE).handle(md.getWhere());
        }
    }
}
