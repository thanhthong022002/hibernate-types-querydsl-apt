package com.pallasathenagroup.querydsl;

import com.querydsl.codegen.AnnotationHelper;
import com.querydsl.codegen.utils.model.TypeCategory;
import org.hibernate.annotations.Type;

import java.lang.annotation.Annotation;

public class HibernateTypeAnnotationHelper implements AnnotationHelper {

    @Override
    public boolean isSupported(Class<? extends Annotation> annotationClass) {
        System.out.println("Support class: " + annotationClass);
        return Type.class.isAssignableFrom(annotationClass);
    }

    @Override
    public Object getCustomKey(Annotation annotation) {
        System.out.println("Custom key: " + annotation);
        return annotation;
    }

    @Override
    public TypeCategory getTypeByAnnotation(Class<?> cl, Annotation annotation) {
        System.out.println("Type by annotation: " + cl + " ann: " + annotation);
        return TypeCategory.CUSTOM;
    }
}
