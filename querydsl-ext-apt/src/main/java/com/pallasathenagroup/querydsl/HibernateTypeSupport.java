package com.pallasathenagroup.querydsl;

import com.google.common.collect.Lists;
import com.querydsl.apt.TypeUtils;
import com.querydsl.codegen.AbstractModule;
import com.querydsl.codegen.Extension;
import com.vladmihalcea.hibernate.type.array.*;
import com.vladmihalcea.hibernate.type.basic.PostgreSQLHStoreType;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import org.hibernate.annotations.TypeDefs;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Collect name of hibernate types, to dynamic convert field with that tye
 * mainly for array, jsonb. The others follow normal process of type resolving.
 */
public class HibernateTypeSupport implements Extension {

    @Override
    public void addSupport(AbstractModule module) {
        RoundEnvironment roundEnvironment = module.get(RoundEnvironment.class);
        Map<String, String> mapping = HibernateTypeMappings.hibernateTypeNameMappings;

        final List<String> jsonClasses = Lists.newArrayList(
                JsonBinaryType.class.getName()
        );
        final List<String> hstoreClasses = Lists.newArrayList(
                PostgreSQLHStoreType.class.getName()
        );
        final List<String> arrayClasses = Lists.newArrayList(
                BooleanArrayType.class.getName(),
                DateArrayType.class.getName(),
                DecimalArrayType.class.getName(),
                DoubleArrayType.class.getName(),
                EnumArrayType.class.getName(),
                IntArrayType.class.getName(),
                ListArrayType.class.getName(),
                LongArrayType.class.getName(),
                StringArrayType.class.getName(),
                TimestampArrayType.class.getName(),
                UUIDArrayType.class.getName()
        );

        System.out.println("Detect typdefs");
        for (Element element : roundEnvironment.getElementsAnnotatedWith(TypeDefs.class)) {
            System.out.println("Found element with TypeDefs: " + element.getSimpleName());
            AnnotationMirror typeDefsMirror = TypeUtils.getAnnotationMirrorOfType(element, TypeDefs.class);
            for (AnnotationValue value : typeDefsMirror.getElementValues().values()) {
                for (AnnotationMirror typeDefMirror : ((List<AnnotationMirror>) value.getValue())) {
                    String name = (String) getAnnotationValue(typeDefMirror, "name");
                    String typeClass = getAnnotationValue(typeDefMirror, "typeClass").toString();

                    if (name != null && typeClass != null) {
                        if (jsonClasses.contains(typeClass)) {
                            mapping.put(name, "jsonb");
                        }
                        else if (arrayClasses.contains(typeClass)) {
                            mapping.put(name, "array");
                        }
                        else if (hstoreClasses.contains(typeClass)) {
                            mapping.put(name, "hstore");
                        }
                    }
                }
            }
        }

        System.out.println("Detected mapping: " + HibernateTypeMappings.hibernateTypeNameMappings
                .entrySet()
                .stream().map(entry -> entry.getKey() + "->" + entry.getValue()).collect(Collectors.joining(", ")));
    }

    private static Object getAnnotationValue(AnnotationMirror annotationMirror, String key) {
        for(Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotationMirror.getElementValues().entrySet() ) {
            if(entry.getKey().getSimpleName().toString().equals(key)) {
                return entry.getValue().getValue();
            }
        }
        return null;
    }
}
