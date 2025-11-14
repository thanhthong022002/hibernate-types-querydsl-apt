package com.pallasathenagroup.querydsl;

import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.MetadataBuilderInitializer;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.type.DateType;
import org.hibernate.type.StringType;
import org.hibernate.type.TimestampType;

public class CommonFunctionInitializer implements MetadataBuilderInitializer {

    @Override
    public void contribute(MetadataBuilder metadataBuilder, StandardServiceRegistry standardServiceRegistry) {
        metadataBuilder.applySqlFunction("AS_TEXT", new SQLFunctionTemplate(StringType.INSTANCE, "?1::TEXT"));
        metadataBuilder.applySqlFunction("date", new SQLFunctionTemplate(DateType.INSTANCE, "date(?1)"));
        metadataBuilder.applySqlFunction("AT_TIME_ZONE", new SQLFunctionTemplate(TimestampType.INSTANCE, "?1 AT TIME ZONE ?2"));
    }

}
