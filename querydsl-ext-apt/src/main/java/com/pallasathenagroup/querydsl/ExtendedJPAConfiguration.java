package com.pallasathenagroup.querydsl;

import com.querydsl.apt.*;
import com.querydsl.codegen.CodegenModule;
import com.querydsl.codegen.Keywords;
import com.querydsl.codegen.NameClassSerializer;
import com.querydsl.core.annotations.*;
import com.querydsl.core.util.Annotations;
import org.hibernate.annotations.Type;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.persistence.*;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ExtendedJPAConfiguration extends DefaultConfiguration {

    private final List<Class<? extends Annotation>> annotations;

    private final Types types;
    private final CodegenModule codegenModule;

    public ExtendedJPAConfiguration(RoundEnvironment roundEnv,
                                    ProcessingEnvironment processingEnv,
                                    Class<? extends Annotation> entityAnn,
                                    Class<? extends Annotation> superTypeAnn,
                                    Class<? extends Annotation> embeddableAnn,
                                    Class<? extends Annotation> embeddedAnn,
                                    Class<? extends Annotation> skipAnn,
                                    CodegenModule codegenModule) {
        super(processingEnv, roundEnv, processingEnv.getOptions(), Keywords.JPA, QueryEntities.class, entityAnn, superTypeAnn,
                embeddableAnn, embeddedAnn, skipAnn, codegenModule);
        this.codegenModule = codegenModule;
        this.annotations = getAnnotations();
        this.types = processingEnv.getTypeUtils();

        this.codegenModule.bind(DefaultNameClassSerializer.class, DefaultNameClassSerializer.class);
        setStrictMode(true);
    }

    @Override
    public NameClassSerializer getNameClassSerializer() {
        return codegenModule.get(DefaultNameClassSerializer.class);
    }

    @SuppressWarnings("unchecked")
    protected List<Class<? extends Annotation>> getAnnotations() {
        return Collections.unmodifiableList(Arrays.asList(
                Access.class, Basic.class, Column.class, ElementCollection.class,
                Embedded.class, EmbeddedId.class, Enumerated.class, GeneratedValue.class, Id.class,
                JoinColumn.class, ManyToOne.class, ManyToMany.class, MapKeyEnumerated.class,
                OneToOne.class, OneToMany.class, PrimaryKeyJoinColumn.class, QueryType.class, QueryInit.class,
                QueryTransient.class, Temporal.class, Transient.class, Version.class));
    }

    @Override
    public VisitorConfig getConfig(TypeElement e, List<? extends Element> elements) {
        Access access = e.getAnnotation(Access.class);
        if (access != null) {
            if (access.value() == AccessType.FIELD) {
                return VisitorConfig.FIELDS_ONLY;
            } else {
                return VisitorConfig.METHODS_ONLY;
            }
        }
        boolean fields = false, methods = false;
        for (Element element : elements) {
            if (hasRelevantAnnotation(element)) {
                fields |= element.getKind().equals(ElementKind.FIELD);
                methods |= element.getKind().equals(ElementKind.METHOD);
            }
        }
        return VisitorConfig.get(fields, methods, VisitorConfig.ALL);
    }

    @Override
    public TypeMirror getRealType(ExecutableElement method) {
        return getRealElementType(method);
    }

    @Override
    public TypeMirror getRealType(VariableElement field) {
        return getRealElementType(field);
    }

    private TypeMirror getRealElementType(Element element) {
        AnnotationMirror mirror = TypeUtils.getAnnotationMirrorOfType(element, ManyToOne.class);
        if (mirror == null) {
            mirror = TypeUtils.getAnnotationMirrorOfType(element, OneToOne.class);
        }
        if (mirror != null) {
            return TypeUtils.getAnnotationValueAsTypeMirror(mirror, "targetEntity");
        }

        mirror = TypeUtils.getAnnotationMirrorOfType(element, OneToMany.class);
        if (mirror == null) {
            mirror = TypeUtils.getAnnotationMirrorOfType(element, ManyToMany.class);
        }
        if (mirror != null) {
            TypeMirror typeArg = TypeUtils.getAnnotationValueAsTypeMirror(mirror, "targetEntity");
            TypeMirror erasure = types.erasure(element.asType());
            TypeElement typeElement = (TypeElement) types.asElement(erasure);
            if (typeElement != null && typeArg != null) {
                if (typeElement.getTypeParameters().size() == 1) {
                    return types.getDeclaredType(typeElement, typeArg);
                } else if (typeElement.getTypeParameters().size() == 2) {
                    if (element.asType() instanceof DeclaredType) {
                        TypeMirror first = ((DeclaredType) element.asType()).getTypeArguments().get(0);
                        return types.getDeclaredType(typeElement, first, typeArg);
                    }
                }
            }
        }

        return null;
    }

    @Override
    public void inspect(Element element, Annotations annotations) {
        Temporal temporal = element.getAnnotation(Temporal.class);
        if (temporal != null && element.getAnnotation(ElementCollection.class) == null) {
            PropertyType propertyType = null;
            switch (temporal.value()) {
                case DATE: propertyType = PropertyType.DATE; break;
                case TIME: propertyType = PropertyType.TIME; break;
                case TIMESTAMP: propertyType = PropertyType.DATETIME;
            }
            annotations.addAnnotation(new QueryTypeImpl(propertyType));
        }

        Map<String, String> hibernateTypeMappings = HibernateTypeMappings.hibernateTypeNameMappings;
        Type type = element.getAnnotation(Type.class);
        if (hibernateTypeMappings != null && type != null && type.type() != null &&
                hibernateTypeMappings.containsKey(type.type())) {
            annotations.addAnnotation(type);
            annotations.addAnnotation(new QueryTypeImpl(PropertyType.CUSTOM));
        }

    }

    private boolean hasRelevantAnnotation(Element element) {
        for (Class<? extends Annotation> annotation : annotations) {
            if (element.getAnnotation(annotation) != null) {
                return true;
            }
        }
        return false;
    }

}