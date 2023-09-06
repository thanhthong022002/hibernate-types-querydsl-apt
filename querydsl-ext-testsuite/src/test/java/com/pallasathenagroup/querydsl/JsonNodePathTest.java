package com.pallasathenagroup.querydsl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.pallasathenagroup.querydsl.json.JsonExpressions;
import com.pallasathenagroup.querydsl.json.JsonPath;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;

import static com.pallasathenagroup.querydsl.QJsonNodeEntity.jsonNodeEntity;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class JsonNodePathTest extends BaseTestContainersTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private JsonNodeEntity entity;

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] { JsonNodeEntity.class };
    }

    @Before
    public void setUp() {
        doInJPA(this::sessionFactory, entityManager -> {
            entityManager.createQuery("delete from JsonNodeEntity j").executeUpdate();

            entity = new JsonNodeEntity();
            entity.jsonNode = objectMapper.valueToTree(ImmutableMap.of("a", 123));
            entity.listInt = List.of(1, 2, 3, 4);
            entity.intNumber = 1;
            entity.uuid = UUID.randomUUID();

            JsonNodeEntity.Embed1 e1 = new JsonNodeEntity.Embed1();
            e1.embed1_attr1 = "embed1_attr1";
            e1.embed1_intList = List.of(1, 2, 3);
            e1.embed1_boolean = true;
            e1.embed1_int = 1;
            e1.uuidText = entity.uuid.toString();
            entity.embed1 = e1;

            JsonNodeEntity.Embed2 e2 = new JsonNodeEntity.Embed2();
            e2.embed2_attr1 = "embed2_attr1";
            e1.embed1_attr2 = e2;

            // embed1 list
            JsonNodeEntity.Embed1 e1_1 = new JsonNodeEntity.Embed1();
            e1_1.embed1_attr1 = "embed1_attr1";
            e1_1.embed1_intList = List.of(1, 2, 3);
            e1_1.embed1_boolean = true;
            e1_1.embed1_int = 1;
            JsonNodeEntity.Embed1 e1_2 = new JsonNodeEntity.Embed1();
            e1_2.embed1_attr1 = "embed1_attr1";
            e1_2.embed1_intList = List.of(1, 2, 3);
            e1_2.embed1_boolean = true;
            e1_2.embed1_int = 1;
            entity.embed1List = Lists.newArrayList(e1_1, e1_2);

            // null value
            // to check missing property
            entity.null_1 = objectMapper.valueToTree(ImmutableMap.of());
            // to check property is null
            Map<String, Object> nullValue = new HashMap<>();
            nullValue.put("test", null);
            entity.null_2 = objectMapper.valueToTree(nullValue);

            // not null value
            entity.not_null = objectMapper.valueToTree(Map.of("test", 1));

            entityManager.persist(entity);
        });
    }

    @Test
    public void getFieldAsText() {
        doInJPA(this::sessionFactory, entityManager -> {
            String result = new JPAQuery<JsonNodeEntity>(entityManager)
                    .from(jsonNodeEntity)
                    .select(
                            jsonNodeEntity.jsonNode.get("a").asText()
                    )
                    .fetchOne();

            assertEquals("123", result);
        });
    }

    @Test
    public void cast() {
        doInJPA(this::sessionFactory, entityManager -> {
            Tuple result = new JPAQuery<JsonNodeEntity>(entityManager)
                    .from(jsonNodeEntity)
                    .select(
                            jsonNodeEntity.id,
                            jsonNodeEntity.embed1.get("embed1_int")
                                    .asNumber(Integer.class)
                                    .in(1, 2, 4),
                            jsonNodeEntity.intNumber.eq(
                                    jsonNodeEntity.embed1
                                            .get("embed1_int").asInteger()
                            )
                    )
                    .fetchOne();

            assertEquals(true, result.get(1, Object.class));
            assertEquals(true, result.get(2, Object.class));
        });
    }

    @Test
    public void size() {
        doInJPA(this::sessionFactory, entityManager -> {
            Tuple result = new JPAQuery<JsonNodeEntity>(entityManager)
                    .from(jsonNodeEntity)
                    .select(
                            jsonNodeEntity.id,
                            jsonNodeEntity.embed1.get("embed1_intList").size(),
                            jsonNodeEntity.listInt.size()
                    )
                    .fetchOne();

            assertEquals(3, result.get(1, Object.class));
            assertEquals(4, result.get(2, Object.class));
        });
    }

    @Test
    public void getFieldAsNode() {
        doInJPA(this::sessionFactory, entityManager -> {
            Tuple result = new JPAQuery<JsonNode>(entityManager)
                    .from(jsonNodeEntity)
                    .select(
                            jsonNodeEntity.jsonNode.get("a"),
                            jsonNodeEntity.embed1.get("embed1_boolean"),
                            jsonNodeEntity.embed1.get("embed1_attr2", "embed2_attr1"),
                            jsonNodeEntity.embed1.get("embed1_attr2", "embed2_attr1")
                                    .asText().eq("embed2_attr1"),
                            jsonNodeEntity.embed1.get("embed1_attr2.embed2_attr1")
                                    .asText().eq("embed2_attr1"),
                            jsonNodeEntity.embed1.get(NEmbed1.embed1.embed1_attr2.embed2_attr1)
                                    .asText().eq("embed2_attr1"),
                            jsonNodeEntity.embed1.get("uuidText").asText()
                                    .contains(Expressions.stringPath(jsonNodeEntity.uuid.getMetadata())),

                            jsonNodeEntity.embed1.get("embed1_boolean").asBoolean()
                    )
                    .fetchOne();

            assertEquals(IntNode.valueOf(123), result.get(0, Object.class));
            assertEquals(BooleanNode.TRUE, result.get(1, Object.class));
            assertEquals(TextNode.valueOf("embed2_attr1"), result.get(2, Object.class));
            assertEquals(true, result.get(3, Object.class));
            assertEquals(true, result.get(4, Object.class));
            assertEquals(true, result.get(5, Object.class));
            assertEquals(true, result.get(6, Object.class));
        });
    }

    @Test
    public void getKeys() {
        doInJPA(this::sessionFactory, entityManager -> {
            List<String> result = new JPAQuery<JsonNodeEntity>(entityManager)
                    .from(jsonNodeEntity)
                    .select(
                            jsonNodeEntity.jsonNode.keys()
                    )
                    .fetch();

            assertEquals(ImmutableList.of("a"), result);
        });
    }

    @Test
    public void contains() {
        doInJPA(this::sessionFactory, entityManager -> {
            JsonPath<JsonNodeEntity.Embed1> embed1 = jsonNodeEntity.embed1;

            JsonNodeEntity.Embed1 e1_1 = new JsonNodeEntity.Embed1();
            e1_1.embed1_attr1 = "embed1_attr1";
            e1_1.embed1_intList = List.of(1, 2, 3);
            e1_1.embed1_boolean = true;
            e1_1.embed1_int = 1;

            List<Tuple> result = new JPAQuery<JsonNodeEntity>(entityManager)
                    .from(jsonNodeEntity)
                    .select(
                            embed1.containsKey(NEmbed1.embed1.embed1_attr1),
                            embed1.contains(Map.of(NEmbed1.embed1.embed1_attr1, "embed1_attr1")),
                            embed1.get(NEmbed1.embed1.embed1_attr2.path())
                                    .contains(Map.of(NEmbed2.embed2.embed2_attr1, "embed2_attr1")),
                            embed1.contains(
                                    Map.of(
                                        NEmbed1.embed1.embed1_intList,
                                        List.of(1)
                                    )
                            ),
                            jsonNodeEntity.listInt.contains(1),
                            jsonNodeEntity.embed1List.contains(Lists.newArrayList(e1_1))
                    )
                    .fetch();

            Tuple tuple = result.get(0);
            assertEquals(true, tuple.get(0, Object.class));
            assertEquals(true, tuple.get(1, Object.class));
            assertEquals(true, tuple.get(2, Object.class));
            assertEquals(true, tuple.get(3, Object.class));
            assertEquals(true, tuple.get(4, Object.class));
            assertEquals(true, tuple.get(5, Object.class));
        });
    }

    @Test
    public void concat() {
        doInJPA(this::sessionFactory, entityManager -> {
            JsonPath<JsonNodeEntity.Embed1> embed1 = jsonNodeEntity.embed1;

            List<Tuple> result = new JPAQuery<JsonNodeEntity>(entityManager)
                    .from(jsonNodeEntity)
                    .select(
                            jsonNodeEntity.id,
                            jsonNodeEntity.listInt.concat(List.of(5, 6, 7)),
                            embed1.get("embed1_intList").concat(4, 5, 6),
                            jsonNodeEntity.listInt.concat(embed1.get("embed1_intList"))
                    )
                    .fetch();

            Tuple tuple = result.get(0);
            assertEquals(List.of(1, 2, 3, 4, 5, 6, 7),
                    objectMapper.convertValue(
                            tuple.get(1, ArrayNode.class),
                            new TypeReference<List<Integer>>() {})
            );
            assertEquals(List.of(1, 2, 3, 4, 5, 6),
                    objectMapper.convertValue(
                            tuple.get(2, ArrayNode.class),
                            new TypeReference<List<Integer>>() {})
            );
            assertEquals(List.of(1, 2, 3, 4, 1, 2, 3),
                    objectMapper.convertValue(
                            tuple.get(3, ArrayNode.class),
                            new TypeReference<List<Integer>>() {})
            );
        });
    }

    @Test
    public void checkNull() {
        doInJPA(this::sessionFactory, entityManager -> {
            List<Integer> result = new JPAQuery<JsonNodeEntity>(entityManager)
                    .from(jsonNodeEntity)
                    .select(Expressions.ONE)
                    .where(
                            jsonNodeEntity.null_1.get("test").isNull()
                            .and(jsonNodeEntity.null_2.get("test").isNull())
                    )
                    .fetch();

            assertEquals(Integer.valueOf(1), result.get(0));
        });
    }

    @Test
    public void checkNotNull() {
        doInJPA(this::sessionFactory, entityManager -> {
            List<Integer> result = new JPAQuery<JsonNodeEntity>(entityManager)
                    .from(jsonNodeEntity)
                    .select(Expressions.ONE)
                    .where(
                            jsonNodeEntity.not_null.get("test").isNotNull()
                            .and(jsonNodeEntity.not_null.get("test").isNull().not())
                    )
                    .fetch();

            assertEquals(Integer.valueOf(1), result.get(0));
        });
    }

    @Test
    public void buildJsonObject() {
        doInJPA(this::sessionFactory, entityManager -> {
            ObjectNode result = new JPAQuery<JsonNodeEntity>(entityManager)
                    .from(jsonNodeEntity)
                    .select(
                            JsonExpressions.buildJsonObject(jsonNodeEntity.id.as("id"), jsonNodeEntity.id.as("id2"))
                    )
                    .fetchOne();

            try {
                assertEquals(ImmutableMap.of("id", entity.id.intValue(), "id2", entity.id.intValue()), objectMapper.treeToValue(result, Map.class));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void buildJsonObject2() {
        doInJPA(this::sessionFactory, entityManager -> {
            ObjectNode result = new JPAQuery<JsonNodeEntity>(entityManager)
                    .from(jsonNodeEntity)
                    .select(
                            JsonExpressions.buildJsonObject(ImmutableMap.of("id", jsonNodeEntity.id, "id2", jsonNodeEntity.id))
                    )
                    .fetchOne();

            try {
                assertEquals(ImmutableMap.of("id", entity.id.intValue(), "id2", entity.id.intValue()), objectMapper.treeToValue(result, Map.class));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void update() {
        doInJPA(this::sessionFactory, entityManager -> {
            long result = new ExtendJpaUpdateClause(entityManager, jsonNodeEntity)
//                    .set(jsonNodeEntity.embed1, "embed1_intList", List.of(1,2,3,4,5))
//                    .set(jsonNodeEntity.embed1, "embed1_int", 100)
//                    .set(jsonNodeEntity.embed1, "embed1_boolean", null)
//                    .set(jsonNodeEntity.embed1, "embed1_attr1", "value_via_update")
//                    .set(jsonNodeEntity.embed1, "embed1_attr2.embed2_attr1", "value_via_update")
                    .set(jsonNodeEntity.embed1, "embed1_intList", jsonNodeEntity.listInt.concat(5))
                    .where(jsonNodeEntity.id.in(1, 2, 3))
                    .execute();

            assertEquals(1, result);

            JsonNodeEntity entity = new JPAQuery<JsonNodeEntity>(entityManager)
                    .from(QJsonNodeEntity.jsonNodeEntity)
                    .fetchOne();
//            assertEquals(Lists.newArrayList(1, 2, 3, 4, 5), entity.embed1.embed1_intList);
//            assertEquals(100, entity.embed1.embed1_int.longValue());
//            assertEquals(null, entity.embed1.embed1_boolean);
//            assertEquals("value_via_update", entity.embed1.embed1_attr1);
//            assertEquals("value_via_update", entity.embed1.embed1_attr2.embed2_attr1);
            assertEquals(Lists.newArrayList(1, 2, 3, 4, 5), entity.embed1.embed1_intList);

            // TODO update multiple field of json field
        });
    }
}
