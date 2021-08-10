package com.pallasathenagroup.querydsl;

import com.querydsl.jpa.impl.JPAProvider;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.spi.MetadataBuilderContributor;
import org.hibernate.boot.spi.MetadataBuilderInitializer;

import java.util.ServiceLoader;

/**
 * Need this because hibernate not pickup MetadataBuilderInitializer automatically
 * And property hibernate.metadata_builder_contributor accept 1 class only
 * We also need to add mapping for ExtendedHQLTemplates so that JpaQuery can use it by default
 */
public class GlobalMetadataBuilderContributor implements MetadataBuilderContributor {

    public GlobalMetadataBuilderContributor() {
        JPAProvider.addMapping("org.hibernate.Session", ExtendedHQLTemplates.DEFAULT);
        JPAProvider.addMapping("org.hibernate.ejb.HibernateEntityManager", ExtendedHQLTemplates.DEFAULT);
        JPAProvider.addMapping("org.hibernate.jpa.HibernateEntityManager", ExtendedHQLTemplates.DEFAULT);
    }

    @Override
    public void contribute(MetadataBuilder metadataBuilder) {
        ServiceLoader<MetadataBuilderInitializer> loader = ServiceLoader.load(MetadataBuilderInitializer.class, MetadataBuilderInitializer.class.getClassLoader());

        for (MetadataBuilderInitializer extension : loader) {
            extension.contribute(metadataBuilder, null);
        }
    }
}
