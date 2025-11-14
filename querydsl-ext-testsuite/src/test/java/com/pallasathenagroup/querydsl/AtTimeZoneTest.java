package com.pallasathenagroup.querydsl;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static com.pallasathenagroup.querydsl.QTimeZoneEntity.timeZoneEntity;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

public class AtTimeZoneTest extends BaseTestContainersTest {

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] { TimeZoneEntity.class };
    }

    @Before
    public void setUp() {
        doInJPA(this::sessionFactory, entityManager -> {
            entityManager.createQuery("delete from TimeZoneEntity").executeUpdate();
            
            TimeZoneEntity entity = new TimeZoneEntity();
            entity.localTimestamp = LocalDateTime.of(2020, 1, 1, 12, 0, 0);
            entity.zonedTimestamp = ZonedDateTime.of(2020, 1, 1, 12, 0, 0, 0, ZoneId.of("UTC"));
            entity.offsetTimestamp = OffsetDateTime.of(2020, 1, 1, 12, 0, 0, 0, ZoneId.of("UTC").getRules().getOffset(LocalDateTime.now()));
            entityManager.persist(entity);
        });
    }

    @Test
    public void testAtTimeZoneWithString() {
        doInJPA(this::sessionFactory, entityManager -> {
            List<Tuple> fetch = new JPAQuery<TimeZoneEntity>(entityManager)
                .from(timeZoneEntity)
                .select(
                    timeZoneEntity.localTimestamp,
                    HibernateTypesExpressions.atTimeZone(
                        Expressions.asDateTime(timeZoneEntity.localTimestamp),
                        "UTC"
                    ),
                    HibernateTypesExpressions.atTimeZone(
                        Expressions.asDateTime(timeZoneEntity.localTimestamp),
                        "America/New_York"
                    )
                )
                .fetch();

            Assert.assertFalse(fetch.isEmpty());
            Tuple tuple = fetch.get(0);
            Assert.assertNotNull(tuple.get(0, Object.class)); // original timestamp
            Assert.assertNotNull(tuple.get(1, Object.class)); // UTC conversion
            Assert.assertNotNull(tuple.get(2, Object.class)); // America/New_York conversion
        });
    }

    @Test
    public void testAtTimeZoneWithExpression() {
        doInJPA(this::sessionFactory, entityManager -> {
            // Test with a constant expression for time zone
            var fetch = new JPAQuery<TimeZoneEntity>(entityManager)
                .from(timeZoneEntity)
                .select(
                    HibernateTypesExpressions.atTimeZone(
                        Expressions.asDateTime(timeZoneEntity.localTimestamp),
                        "UTC"
                    )
                )
                .fetch();

            Assert.assertFalse(fetch.isEmpty());
            var tuple = fetch.get(0);
            Assert.assertNotNull(tuple);
        });
    }

    @Test
    public void testAtTimeZoneInWhereClause() {
        doInJPA(this::sessionFactory, entityManager -> {
            List<TimeZoneEntity> fetch = new JPAQuery<TimeZoneEntity>(entityManager)
                .from(timeZoneEntity)
                .where(
                    HibernateTypesExpressions.atTimeZone(
                        Expressions.asDateTime(timeZoneEntity.localTimestamp),
                        "UTC"
                    ).isNotNull()
                )
                .fetch();

            Assert.assertFalse(fetch.isEmpty());
        });
    }

    @Test
    public void testAtTimeZoneWithZonedTimestamp() {
        doInJPA(this::sessionFactory, entityManager -> {
            List<Tuple> fetch = new JPAQuery<TimeZoneEntity>(entityManager)
                .from(timeZoneEntity)
                .select(
                    timeZoneEntity.zonedTimestamp,
                    HibernateTypesExpressions.atTimeZone(
                        timeZoneEntity.zonedTimestamp,
                        "Asia/Tokyo"
                    )
                )
                .fetch();

            Assert.assertFalse(fetch.isEmpty());
            Tuple tuple = fetch.get(0);
            Assert.assertNotNull(tuple.get(0, Object.class));
            Assert.assertNotNull(tuple.get(1, Object.class));
        });
    }

    @Test
    public void testAtTimeZoneWithOffsetTimestamp() {
        doInJPA(this::sessionFactory, entityManager -> {
            List<Tuple> fetch = new JPAQuery<TimeZoneEntity>(entityManager)
                .from(timeZoneEntity)
                .select(
                    timeZoneEntity.offsetTimestamp,
                    HibernateTypesExpressions.atTimeZone(
                        Expressions.asDateTime(timeZoneEntity.offsetTimestamp),
                        "Europe/Paris"
                    )
                )
                .fetch();

            Assert.assertFalse(fetch.isEmpty());
            Tuple tuple = fetch.get(0);
            Assert.assertNotNull(tuple.get(0, Object.class));
            Assert.assertNotNull(tuple.get(1, Object.class));
        });
    }

}

