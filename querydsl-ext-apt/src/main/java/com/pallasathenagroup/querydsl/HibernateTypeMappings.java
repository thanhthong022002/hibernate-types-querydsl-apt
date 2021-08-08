package com.pallasathenagroup.querydsl;

import com.pallasathenagroup.querydsl.array.PostgresqlArrayPath;
import com.pallasathenagroup.querydsl.hstore.HstorePath;
import com.pallasathenagroup.querydsl.json.JsonPath;
import com.querydsl.codegen.EntityType;
import com.querydsl.codegen.JavaTypeMappings;
import com.querydsl.codegen.Property;
import com.querydsl.codegen.utils.model.SimpleType;
import com.querydsl.codegen.utils.model.Type;
import com.querydsl.codegen.utils.model.Types;
import com.querydsl.core.util.Annotations;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HibernateTypeMappings extends JavaTypeMappings {

    public static final Map<String, String> hibernateTypeNameMappings;

    static {
        hibernateTypeNameMappings = new HashMap<>();
        hibernateTypeNameMappings.put("string-array", "array");
        hibernateTypeNameMappings.put("int-array", "array");
        hibernateTypeNameMappings.put("list-array", "array");
        hibernateTypeNameMappings.put("jsonb", "jsonb");
        hibernateTypeNameMappings.put("hstore", "hstore");
    }

    public HibernateTypeMappings() {
        super();
    }

    @Override
    public Type getPathType(Type type, EntityType model, Property property, boolean raw) {
        if (property != null) {
            Annotations annotatedElement = (Annotations) property.getAnnotatedElement();

            if (annotatedElement != null) {
                org.hibernate.annotations.Type typeAnn = annotatedElement.getAnnotation(org.hibernate.annotations.Type.class);

                /*System.out.println(
                        "getPathType: " + type.getFullName() + " model: " + model.getFullName() +
                                " type: " + (typeAnn != null ? typeAnn.type() : "empty")
                );*/

                if (typeAnn != null && hibernateTypeNameMappings.containsKey(typeAnn.type())) {
                    String typeValue = typeAnn.type();
                    String targetType = hibernateTypeNameMappings.get(typeValue);

                    if (targetType.equals("hstore")) {
                        SimpleType hstore = new SimpleType(new SimpleType(
                                HstorePath.class.getName()
                        ));
                        return hstore;
                    } else if (targetType.equals("jsonb")) {
                        SimpleType jsonb = new SimpleType(new SimpleType(JsonPath.class.getName()),
                                Collections.singletonList(type));
                        return jsonb;
                    } else if (targetType.equals("array")) {
                        Type param1 = type.getParameters().size() > 0 ?
                                type.getParameters().get(0) : type.getComponentType();

                        SimpleType enumType = new SimpleType(param1.getFullName(), param1.getPackageName(), param1.getSimpleName());
                        Type enumArrayType = enumType.asArrayType();
                        Class arrayPathClass = PostgresqlArrayPath.class;

                        return new SimpleType(
                                arrayPathClass.getName(),
                                arrayPathClass.getPackage().getName(),
                                arrayPathClass.getSimpleName(),
                                enumArrayType, wrap(enumType));
                    }
                }
            }
        }

        return getPathType(type, model, raw, false, false);
    }

    private Type wrap(Type type) {
        if (type.equals(Types.BOOLEAN_P)) {
            return Types.BOOLEAN;
        } else if (type.equals(Types.BYTE_P)) {
            return Types.BYTE;
        } else if (type.equals(Types.CHAR)) {
            return Types.CHARACTER;
        } else if (type.equals(Types.DOUBLE_P)) {
            return Types.DOUBLE;
        } else if (type.equals(Types.FLOAT_P)) {
            return Types.FLOAT;
        } else if (type.equals(Types.INT)) {
            return Types.INTEGER;
        } else if (type.equals(Types.LONG_P)) {
            return Types.LONG;
        } else if (type.equals(Types.SHORT_P)) {
            return Types.SHORT;
        } else {
            return type;
        }
    }
}
