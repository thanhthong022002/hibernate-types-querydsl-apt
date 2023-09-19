package com.pallasathenagroup.querydsl;

import com.google.common.collect.Lists;
import com.pallasathenagroup.querydsl.ArrayEntity.SensorState;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAUpdateClause;
import com.vladmihalcea.hibernate.type.array.ListArrayType;
import com.vladmihalcea.hibernate.type.array.internal.AbstractArrayType;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.hibernate.jpa.TypedParameterValue;
import org.hibernate.usertype.DynamicParameterizedType;
import org.junit.Before;
import org.junit.Test;

import static com.pallasathenagroup.querydsl.HibernateTypesExpressions.createArrayExpression;
import static com.pallasathenagroup.querydsl.QArrayEntity.arrayEntity;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ArrayEntityPathTest extends BaseTestContainersTest {

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] { ArrayEntity.class };
    }

    @Before
    public void setUp() {
        doInJPA(this.buildEmf(), entityManager -> {
            entityManager.createQuery("delete from ArrayEntity j").executeUpdate();

            Date date1 = Date.from(LocalDate.of(1991, 12, 31).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
            Date date2 = Date.from(LocalDate.of(1990, 1, 1).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());

            ArrayEntity arrayEntity = new ArrayEntity();
            arrayEntity.setId(1L);
            arrayEntity.setSensorIds(new UUID[]{UUID.fromString("c65a3bcb-8b36-46d4-bddb-ae96ad016eb1"), UUID.fromString("72e95717-5294-4c15-aa64-a3631cf9a800")});
            arrayEntity.setSensorNames(new String[]{"Temperature", "Pressure"});
            arrayEntity.setSensorNameStr(Lists.newArrayList("Thong", "Nguyen", "Thông", "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum."));
            arrayEntity.setSensorValues(new int[]{12, 756});
            arrayEntity.setSensorLongValues(new long[]{42L, 9223372036854775800L});
            arrayEntity.setSensorDoubleValues(new double[]{0.123, 456.789});
            arrayEntity.setSensorStates(new SensorState[]{SensorState.ONLINE, SensorState.OFFLINE, SensorState.ONLINE, SensorState.UNKNOWN});
            arrayEntity.setDateValues(new Date[]{date1, date2});
            arrayEntity.setTimestampValues(new Date[]{date1, date2});
            entityManager.persist(arrayEntity);
        });
    }

    @Test
    public void testArrayPaths() {
        doInJPA(this.buildEmf(), entityManager -> {
            List<Tuple> fetch = new JPAQuery<>(entityManager)
                    .from(arrayEntity).select(
                            arrayEntity.sensorValues.get(0),
                            arrayEntity.sensorValues.append(5),
                            arrayEntity.sensorValues.prepend(5),
                            arrayEntity.sensorValues.concat(arrayEntity.sensorValues),
                            arrayEntity.sensorValues.contains(12, 756),
                            arrayEntity.sensorValues.contains(123),
                            arrayEntity.sensorValues.isContainedBy(12, 13),
                            arrayEntity.sensorValues.isContainedBy(12, 13, 756),
                            arrayEntity.sensorValues.overlaps(12, 13),
                            arrayEntity.sensorValues.size(),
                            arrayEntity.sensorStates.concat(SensorState.ONLINE, SensorState.UNKNOWN).contains(SensorState.ONLINE),
                            arrayEntity.sensorStates.get(0),

                            // extend
                            arrayEntity.sensorValues.overlaps(Lists.newArrayList(12, 14)),
                            arrayEntity.sensorNameStr.overlaps(Lists.newArrayList("Thong", "Thanh")),
                            arrayEntity.sensorNameStr.contains(Lists.newArrayList("Thong")),
                            arrayEntity.sensorNameStr.asText().contains("Thô"),
                            arrayEntity.sensorNameStr.asText().contains("Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum.")
                            )
                    .where(createArrayExpression(1, 2).contains(createArrayExpression(1,2 )))
                    .fetch();

            Tuple tuple = fetch.get(0);
            assertEquals(12, tuple.get(0, Object.class));
            assertArrayEquals(new int[] {12, 756, 5}, tuple.get(1, int[].class));
            assertArrayEquals(new int[] {5, 12, 756}, tuple.get(2, int[].class));
            assertArrayEquals(new int[] {12, 756, 12, 756}, tuple.get(3, int[].class));
            assertEquals(true, tuple.get(4, Object.class));
            assertEquals(false, tuple.get(5, Object.class));
            assertEquals(false, tuple.get(6, Object.class));
            assertEquals(true, tuple.get(7, Object.class));
            assertEquals(true, tuple.get(8, Object.class));
            assertEquals(2, tuple.get(9, Object.class));
            assertEquals(true, tuple.get(10, Object.class));
            assertEquals(SensorState.ONLINE, tuple.get(11, Object.class));

            // extend
            assertEquals(true, tuple.get(12, Object.class));
            assertEquals(true, tuple.get(13, Object.class));
            assertEquals(true, tuple.get(14, Object.class));
            assertEquals(true, tuple.get(15, Object.class));
            assertEquals(true, tuple.get(16, Object.class));
        });
    }


    @Test
    public void testArrayAgg() {
        doInJPA(this.buildEmf(), entityManager -> {
            new JPAQuery<>(entityManager)
                    .from(arrayEntity)
                    .select(HibernateTypesExpressions.arrayAgg(arrayEntity.sensorStates.get(0)))
                    .fetch();
        });
    }

    @Test
    public void updateByExpression() {
        doInJPA(this::sessionFactory, entityManager -> {
            long result = new JPAUpdateClause(entityManager, arrayEntity)
                    .set(arrayEntity.sensorNames,
                            arrayEntity.sensorNames.concat(arrayEntity.sensorNames)
                    )
                    .execute();

            assertEquals(1, result);

            ArrayEntity entity = new JPAQuery<ArrayEntity>(entityManager)
                    .from(arrayEntity)
                    .fetchOne();
            assertTrue(Arrays.equals(new String[] {"Temperature", "Pressure", "Temperature", "Pressure"}, entity.sensorNames));
        });
    }

    @Test
    public void updateByValue_useListDirectly() {
        doInJPA(this::sessionFactory, entityManager -> {
            long result = new ExtendJpaUpdateClause(entityManager, arrayEntity)
                    .set(arrayEntity.sensorNameStr, List.of("test"))
                    .execute();

            assertEquals(1, result);

            ArrayEntity entity = new JPAQuery<ArrayEntity>(entityManager)
                    .from(arrayEntity)
                    .fetchOne();
            assertTrue(List.of("test").equals(entity.sensorNameStr));
        });
    }

    @Test
    public void updateByValue_useListDirectlyWithWhere() {
        doInJPA(this::sessionFactory, entityManager -> {
            long result = new ExtendJpaUpdateClause(entityManager, arrayEntity)
                    .set(arrayEntity.sensorNameStr, List.of("test"))
                    .where(arrayEntity.id.in(1, 2))
                    .execute();

            assertEquals(1, result);

            ArrayEntity entity = new JPAQuery<ArrayEntity>(entityManager)
                    .from(arrayEntity)
                    .fetchOne();
            assertTrue(List.of("test").equals(entity.sensorNameStr));
        });
    }

    @Test
    public void updateByValue_useTypedParameterValue() {
        doInJPA(this::sessionFactory, entityManager -> {
            var type = new ListArrayType();
            Properties props = new Properties();
            props.setProperty(AbstractArrayType.SQL_ARRAY_TYPE, "text");
            props.setProperty(DynamicParameterizedType.ENTITY, ArrayEntity.class.getName());
            props.setProperty(DynamicParameterizedType.PROPERTY, "sensorNameStr");
            type.setParameterValues(props);

            long result = new ExtendJpaUpdateClause(entityManager, arrayEntity)
                    .setRaw(arrayEntity.sensorNameStr, Expressions.constant(new TypedParameterValue(
                            type, List.of("test"))
                    ))
                    .execute();

            assertEquals(1, result);

            ArrayEntity entity = new JPAQuery<ArrayEntity>(entityManager)
                    .from(arrayEntity)
                    .fetchOne();
            assertTrue(List.of("test").equals(entity.sensorNameStr));
        });
    }
}
