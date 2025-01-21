package com.pallasathenagroup.querydsl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.sql.DataSource;
import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.hibernate.cfg.Environment;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;
import org.hibernate.testing.util.jpa.PersistenceUnitInfoAdapter;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

public abstract class BaseTestContainersTest {

    static final PostgreSQLContainer POSTGRE_SQL_CONTAINER;
    static EntityManagerFactory _emf;

    static {

        POSTGRE_SQL_CONTAINER = new PostgreSQLContainer("postgres:17");
        POSTGRE_SQL_CONTAINER.setCommand("postgres",
                "-c", "fsync=off",
                "-c", "log_statement=all"
        );
        POSTGRE_SQL_CONTAINER
                .withReuse(true)
                .withLabel("reuse.UUID", "querydsl-ext-api")
                .start();

//        properties.put("javax.persistence.schema-generation.database.action","create");
    }

    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] {};
    }

    public Supplier<EntityManagerFactory> buildEmf() {
        return () -> {
            if (_emf == null) {
                Properties properties = new Properties();
                properties.setProperty(Environment.DIALECT, "org.hibernate.dialect.PostgreSQL95Dialect");
                properties.setProperty(Environment.DRIVER, "org.postgresql.Driver");
                properties.setProperty(Environment.HBM2DDL_AUTO, "drop");
                properties.setProperty(Environment.HBM2DDL_DATABASE_ACTION, "create-drop");
                properties.setProperty(Environment.SHOW_SQL, "true");
                properties.setProperty(Environment.URL, POSTGRE_SQL_CONTAINER.getJdbcUrl());
                properties.setProperty(Environment.PASS, POSTGRE_SQL_CONTAINER.getPassword());
                properties.setProperty(Environment.USER, POSTGRE_SQL_CONTAINER.getUsername());
                properties.setProperty("hibernate.metadata_builder_contributor",
                        "com.pallasathenagroup.querydsl.GlobalMetadataBuilderContributor");

                System.out.println("Database Url: " + POSTGRE_SQL_CONTAINER.getJdbcUrl());

                // Configure DataSource
                PGSimpleDataSource originalDataSource = new PGSimpleDataSource();
                originalDataSource.setURL(POSTGRE_SQL_CONTAINER.getJdbcUrl());
                originalDataSource.setUser(POSTGRE_SQL_CONTAINER.getUsername());
                originalDataSource.setPassword(POSTGRE_SQL_CONTAINER.getPassword());

                // Wrap DataSource with ProxyDataSource to enable logging
                DataSource dataSource = ProxyDataSourceBuilder.create(originalDataSource)
                        .name("ProxyDS")
                        .logQueryBySlf4j(SLF4JLogLevel.INFO)
                        .build();

                Class<?>[] classes = this.getAnnotatedClasses();

                PersistenceUnitInfo persistenceUnitInfo = new MyPersistenceUnitInfo(classes, properties);

                Map<String, Object> configuration = new HashMap<>();
                configuration.put("javax.persistence.nonJtaDataSource", dataSource);
                _emf = new EntityManagerFactoryBuilderImpl(
                        new PersistenceUnitInfoDescriptor(persistenceUnitInfo), configuration)
                        .withDataSource(dataSource)
                        .build();
            }

            return _emf;
        };
    }

    public EntityManagerFactory sessionFactory() {
        return this.buildEmf().get();
    }

    public static final class MyPersistenceUnitInfo extends PersistenceUnitInfoAdapter {

        private final Class<?>[] classes;

        public MyPersistenceUnitInfo(Class<?>[] classes, Properties properties) {
            super(properties);
            this.classes = classes;
        }

        @Override
        public List<String> getManagedClassNames() {
            return Arrays.stream(classes).map(c -> c.getName()).collect(Collectors.toList());
        }
    }

}
