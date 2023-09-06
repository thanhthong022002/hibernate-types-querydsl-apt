package com.pallasathenagroup.querydsl;

import com.vladmihalcea.hibernate.type.interval.PostgreSQLIntervalType;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.MetadataBuilderInitializer;
import org.hibernate.dialect.function.SQLFunctionTemplate;

public class DurationFunctionInitializer implements MetadataBuilderInitializer {

    @Override
    public void contribute(MetadataBuilder metadataBuilder, StandardServiceRegistry standardServiceRegistry) {
        metadataBuilder.applySqlFunction("DURATION_ADD", new SQLFunctionTemplate(PostgreSQLIntervalType.INSTANCE, "?1 + ?2"));
        metadataBuilder.applySqlFunction("DURATION_SUBTRACT", new SQLFunctionTemplate(PostgreSQLIntervalType.INSTANCE, "?1 - ?2"));
        metadataBuilder.applySqlFunction("DURATION_DIVIDE", new SQLFunctionTemplate(PostgreSQLIntervalType.INSTANCE, "?1 / ?2"));
        metadataBuilder.applySqlFunction("DURATION_MULTIPLY", new SQLFunctionTemplate(PostgreSQLIntervalType.INSTANCE, "?1 * ?2"));
        metadataBuilder.applySqlFunction("DURATION_BETWEEN", new SQLFunctionTemplate(PostgreSQLIntervalType.INSTANCE, "?1::timestamp - ?2::timestamp"));
        metadataBuilder.applySqlFunction("DURATION_AVG", new SQLFunctionTemplate(PostgreSQLIntervalType.INSTANCE, "AVG(?1)"));
        metadataBuilder.applySqlFunction("DURATION_MAX", new SQLFunctionTemplate(PostgreSQLIntervalType.INSTANCE, "MAX(?1)"));
        metadataBuilder.applySqlFunction("DURATION_MIN", new SQLFunctionTemplate(PostgreSQLIntervalType.INSTANCE, "MIN(?1)"));
        metadataBuilder.applySqlFunction("DURATION_SUM", new SQLFunctionTemplate(PostgreSQLIntervalType.INSTANCE, "SUM(?1)"));

    }

}
