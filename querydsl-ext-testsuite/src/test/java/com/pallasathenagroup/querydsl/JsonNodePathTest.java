package com.pallasathenagroup.querydsl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.pallasathenagroup.querydsl.json.JsonExpressions;
import com.pallasathenagroup.querydsl.json.JsonPath;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQuery;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static com.pallasathenagroup.querydsl.QJsonNodeEntity.jsonNodeEntity;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

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

            JsonNodeEntity.Embed1 e1 = new JsonNodeEntity.Embed1();
            e1.embed1_attr1 = "embed1_attr1";
            e1.embed1_intList = List.of(1, 2, 3);
            e1.embed1_boolean = true;
            e1.embed1_int = 1;
            entity.embed1 = e1;

            JsonNodeEntity.Embed2 e2 = new JsonNodeEntity.Embed2();
            e2.embed2_attr1 = "embed2_attr1";
            e1.embed1_attr2 = e2;

            entityManager.persist(entity);
        });
    }

    @Test
    public void getFieldAsText() {
        doInJPA(this::sessionFactory, entityManager -> {
            String result = new JPAQuery<JsonNodeEntity>(entityManager, ExtendedHQLTemplates.DEFAULT)
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
            Tuple result = new JPAQuery<JsonNodeEntity>(entityManager, ExtendedHQLTemplates.DEFAULT)
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
            Tuple result = new JPAQuery<JsonNodeEntity>(entityManager, ExtendedHQLTemplates.DEFAULT)
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
            Tuple result = new JPAQuery<JsonNode>(entityManager, ExtendedHQLTemplates.DEFAULT)
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
                                    .asText().eq("embed2_attr1")
                    )
                    .fetchOne();

            assertEquals(IntNode.valueOf(123), result.get(0, Object.class));
            assertEquals(BooleanNode.TRUE, result.get(1, Object.class));
            assertEquals(TextNode.valueOf("embed2_attr1"), result.get(2, Object.class));
            assertEquals(true, result.get(3, Object.class));
            assertEquals(true, result.get(4, Object.class));
            assertEquals(true, result.get(5, Object.class));
        });
    }

    @Test
    public void getKeys() {
        doInJPA(this::sessionFactory, entityManager -> {
            List<String> result = new JPAQuery<JsonNodeEntity>(entityManager, ExtendedHQLTemplates.DEFAULT)
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

            List<Tuple> result = new JPAQuery<JsonNodeEntity>(entityManager, ExtendedHQLTemplates.DEFAULT)
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
                            )
                    )
                    .fetch();

            Tuple tuple = result.get(0);
            assertEquals(true, tuple.get(0, Object.class));
            assertEquals(true, tuple.get(1, Object.class));
            assertEquals(true, tuple.get(2, Object.class));
            assertEquals(true, tuple.get(3, Object.class));
        });
    }

    @Test
    public void concat() {
        doInJPA(this::sessionFactory, entityManager -> {
            JsonPath<JsonNodeEntity.Embed1> embed1 = jsonNodeEntity.embed1;

            List<Tuple> result = new JPAQuery<JsonNodeEntity>(entityManager, ExtendedHQLTemplates.DEFAULT)
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
    public void buildJsonObject() {
        doInJPA(this::sessionFactory, entityManager -> {
            ObjectNode result = new JPAQuery<JsonNodeEntity>(entityManager, ExtendedHQLTemplates.DEFAULT)
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
            ObjectNode result = new JPAQuery<JsonNodeEntity>(entityManager, ExtendedHQLTemplates.DEFAULT)
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
}
