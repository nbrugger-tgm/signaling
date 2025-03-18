package eu.nitonfx.signaling.processors.elementbuilder;

import com.google.auto.service.AutoService;
import com.niton.compile.processor.BaseProcessor;
import com.squareup.javapoet.*;
import eu.nitonfx.signaling.api.Context;

import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import java.lang.constant.Constable;
import java.time.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AutoService(Processor.class)
public class ElementBuilderProcessor extends BaseProcessor {

    private static AnnotationValue getAnnotationValue(AnnotationMirror annotationMirror, String key) {
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotationMirror.getElementValues().entrySet()) {
            if (entry.getKey().getSimpleName().toString().equals(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static String getReactiveBuilderName(TypeElement baseClass) {
        return "Reactive" + baseClass.getSimpleName() + "Builder";
    }

    @Override
    public boolean performProcessing(Set<? extends TypeElement> __, RoundEnvironment roundEnvironment) {
        var annotated = roundEnvironment.getElementsAnnotatedWith(ReactiveBuilder.class);
        annotated.forEach(this::generate);
        return true;
    }

    private void generate(javax.lang.model.element.Element element) {
        verifier.isA(element, ElementKind.PACKAGE)
                .failOnViolation();
        var pack = ((PackageElement) element).getQualifiedName();
        var config = getAnnotationMirror(element, ReactiveBuilder.class);
        var annotationValues = (List<AnnotationValue>) getAnnotationValue(config, "value").getValue();
        final var classesToReactify = annotationValues.stream()
                .map(cls -> (TypeMirror) cls.getValue())
                .map(cls -> (TypeElement) typeUtils().asElement(cls))
                .toList();
        var annotationExtensions = (List<AnnotationValue>) getAnnotationValue(config, "extensions").getValue();
        final var extensions = annotationExtensions.stream()
                .map(cls -> (TypeMirror) cls.getValue())
                .map(cls -> (TypeElement) typeUtils().asElement(cls))
                .toList();
        var builderClasses = classesToReactify.stream()
                .collect(Collectors.toMap(
                        it -> it.getSimpleName().toString(),
                        it -> generateReactiveBuilder(pack.toString(), it, classesToReactify, extensions)
                ));
        var aggregatorClassname = (String) getAnnotationValue(config, "aggregatorClassname").getValue();
        if (!aggregatorClassname.equals(ReactiveBuilder.NO_AGGREGATOR)) {
            generateAggregator(pack, aggregatorClassname, builderClasses);
        }
    }

    private void generateAggregator(Name pack, String aggregatorClassname, Map<String, TypeSpec> builderClasses) {
        var aggregatorClass = TypeSpec.classBuilder(aggregatorClassname).addModifiers(Modifier.PUBLIC, Modifier.FINAL);
        var cx = FieldSpec.builder(Context.class, "cx", Modifier.PRIVATE, Modifier.FINAL).build();
        aggregatorClass.addField(cx);
        var ctorParam = ParameterSpec.builder(cx.type, cx.name).build();
        aggregatorClass.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ctorParam)
                .addStatement("this.$N = $N", cx, ctorParam)
                .build()
        );
        builderClasses.forEach((k, v) -> {
            v.methodSpecs.stream().filter(MethodSpec::isConstructor).forEach(ctor -> {
                var specificParams = ctor.parameters.subList(1, ctor.parameters.size());
                var rawType = ClassName.get(pack.toString(), v.name);
                var returnType = v.typeVariables.isEmpty() ? rawType : ParameterizedTypeName.get(rawType, v.typeVariables.toArray(new TypeVariableName[0]));
                aggregatorClass.addMethod(MethodSpec.methodBuilder(k)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(returnType)
                        .addParameters(specificParams)
                        .addTypeVariables(v.typeVariables)
                        .addStatement("return new $T($L)", returnType, Stream.concat(
                                Stream.of(cx.name),
                                specificParams.stream().map(it -> it.name)
                        ).collect(Collectors.joining(", ")))
                        .build());
            });
        });
        writeClass(pack.toString(), aggregatorClass.build());
    }

    private TypeSpec generateReactiveBuilder(String pack, TypeElement baseClass, List<TypeElement> toSubstitute, List<TypeElement> extensions) {
        var generics = baseClass.getTypeParameters().stream().map(TypeVariableName::get).toList();
        var baseClassName = generics.isEmpty() ? ClassName.get(baseClass) : ParameterizedTypeName.get(ClassName.get(baseClass), generics.toArray(TypeVariableName[]::new));
        var builderClass = TypeSpec.classBuilder(getReactiveBuilderName(baseClass))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addSuperinterface(ParameterizedTypeName.get(ClassName.get(Element.class), ClassName.get(baseClass)))
                .addTypeVariables(generics);
        var cx = FieldSpec.builder(ClassName.get(Context.class), "cx", Modifier.PRIVATE, Modifier.FINAL).build();
        builderClass.addField(cx);
        var element = FieldSpec.builder(baseClassName, "element", Modifier.PRIVATE, Modifier.FINAL).build();
        builderClass.addField(element);

        var parameterless = ElementFilter.constructorsIn(baseClass.getEnclosedElements()).stream()
                .anyMatch(e -> e.getParameters().isEmpty());
        if (parameterless) {
            builderClass.addMethod(MethodSpec.constructorBuilder()
                    .addParameter(ClassName.get(Context.class), "cx")
                    .addCode("this.element = new $T();", baseClass)
                    .addCode("this.cx = cx;")
                    .build());
        }
        builderClass.addMethod(MethodSpec.constructorBuilder()
                .addParameter(ClassName.get(Context.class), "cx")
                .addParameter(baseClassName, "element")
                .addStatement("this.element = element")
                .addStatement("this.cx = cx")
                .build());

        var methods = getAllMethods(baseClass)
                .filter(e -> e.getModifiers().contains(Modifier.PUBLIC))
                .filter(e -> !((TypeElement) e.getEnclosingElement()).getQualifiedName().contentEquals(Object.class.getCanonicalName()))
                .toList();

        record MethodConflict(String name, int params) {
        }

        var conflictingMethods = methods.stream()
                .collect(Collectors.groupingBy(e -> new MethodConflict(e.getSimpleName().toString(), e.getParameters().size())))
                .entrySet()
                .stream()
                .filter(e -> e.getValue().size() > 1)
                .flatMap(it -> it.getValue().stream())
                .toList();
        methods.stream()
                .flatMap(method -> reactiveMethods(cx, element, method, conflictingMethods.contains(method), toSubstitute, extensions))
                .forEach(builderClass::addMethod);

        builderClass.addMethod(MethodSpec.methodBuilder("get")
                .addModifiers(Modifier.PUBLIC)
                .returns(baseClassName)
                .addStatement("return $N", element)
                .build());

        var built = builderClass.build();
        writeClass(pack, built);
        return built;
    }

    private Stream<ExecutableElement> getAllMethods(TypeElement baseClass) {
        return ElementFilter.methodsIn(processingEnv.getElementUtils().getAllMembers(baseClass)).stream();
    }

    private Stream<MethodSpec> reactiveMethods(FieldSpec cx, FieldSpec element, ExecutableElement method, boolean distinguish, List<TypeElement> toSubstitute, List<TypeElement> extensions) {
        var complexParams = method.getParameters().stream()
                .filter(Predicate.not(e -> isPrimitive(e.asType())))
                .toList();

        Optional<ExecutableElement> relevantExtension = findMatchingExtension(extensions, method);
        var methods = new ArrayList<MethodSpec>(3);
        var isSetter = method.getSimpleName().toString().startsWith("set");
        forwardingMethods(cx, element, method, !complexParams.isEmpty(), isSetter, toSubstitute, relevantExtension).forEach(methods::add);
        if (isSetter) {
            methods.add(reactiveMethod(cx, element, method, distinguish, relevantExtension.isPresent()));
        }
        if (!complexParams.isEmpty()) {
            //special complex handling
        }

        return methods.stream();
    }

    private Optional<ExecutableElement> findMatchingExtension(List<TypeElement> extensions, ExecutableElement method) {
        final var additionalParamCount = 2;//context, element
        return extensions.stream().flatMap(it -> ElementFilter.methodsIn(it.getEnclosedElements()).stream())
                .filter(it -> it.getSimpleName().contentEquals(method.getSimpleName()))
                .filter(it -> typeUtils().isAssignable(method.getEnclosingElement().asType(), typeUtils().erasure(it.getParameters().get(1).asType())))
                .filter(it -> it.getParameters().size() - additionalParamCount == method.getParameters().size())
                .filter(it -> {
                    for (var i = additionalParamCount; i < it.getParameters().size(); i++) {
                        var inExtension = it.getParameters().get(i);
                        var actual = method.getParameters().get(i - additionalParamCount);
                        var extensionType = typeUtils().erasure(inExtension.asType());
                        var actualType = typeUtils().erasure(actual.asType());
                        var matching = typeUtils().isSameType(extensionType, actualType);
                        if (!matching) return false;
                    }
                    return true;
                })
                .findFirst();
    }

    private MethodSpec reactiveMethod(FieldSpec cx, FieldSpec element, ExecutableElement method, boolean distinguish, boolean hasExtension) {
        String qualifier = distinguish ? method.getParameters().stream()
                .map(it -> {
                    var type = it.asType();
                    return type.getKind().isPrimitive() ?
                            type.toString() :
                            typeUtils().asElement(type).getSimpleName().toString();
                })
                .map(this::capitalize)
                .collect(Collectors.joining("")) : "";
        var forwardMethod = MethodSpec.methodBuilder(method.getSimpleName().toString() + qualifier);
        forwardMethod.addModifiers(Modifier.PUBLIC);
        forwardMethod.returns(TypeName.VOID);
        forwardMethod.addExceptions(method.getThrownTypes().stream().map(TypeName::get).toList());
        forwardMethod.addTypeVariables(method.getTypeParameters().stream().map(TypeVariableName::get).toList());
        var params = method.getParameters().stream().map(param -> {
            var supplier = ParameterizedTypeName.get(ClassName.get(Supplier.class), TypeName.get(getBoxedType(param.asType())));
            return ParameterSpec.builder(supplier, param.getSimpleName().toString()).build();
        }).toList();
        forwardMethod.addParameters(params);

        final var parameterString = params.stream().map(it -> it.name + ".get()").collect(Collectors.joining(", "));
        if (hasExtension) {
            forwardMethod.addStatement("$N.createEffect(() -> $N($L))", cx, method.getSimpleName(), parameterString);
        } else {
            forwardMethod.addStatement("$N.createEffect(() -> $N.$N($L))", cx, element, method.getSimpleName(), parameterString);
        }

        return forwardMethod.build();
    }

    private Types typeUtils() {
        return processingEnv.getTypeUtils();
    }

    private String capitalize(String it) {
        return Character.toUpperCase(it.charAt(0)) + it.substring(1);
    }

    public TypeMirror getBoxedType(TypeMirror t) {
        var typeUtil = typeUtils();
        return t.getKind().isPrimitive() ?
                typeUtil.boxedClass(typeUtil.getPrimitiveType(t.getKind())).asType() : t;
    }


    private Stream<MethodSpec> forwardingMethods(FieldSpec cx, FieldSpec element, ExecutableElement method, boolean hasComplexParams, boolean isSetter, List<TypeElement> toSubstitute, Optional<ExecutableElement> relevantExtension) {
        var params = method.getParameters().stream().map(ParameterSpec::get).toList();
        var forwardMethod = MethodSpec.methodBuilder(method.getSimpleName().toString());
        forwardMethod.addModifiers(Modifier.PUBLIC);
        if (isSetter || relevantExtension.isPresent()) forwardMethod.returns(TypeName.VOID);
        else forwardMethod.returns(TypeName.get(method.getReturnType()));
        forwardMethod.addExceptions(method.getThrownTypes().stream().map(TypeName::get).toList());
        forwardMethod.addTypeVariables(method.getTypeParameters().stream().map(TypeVariableName::get).toList());

        var requiresSubstitution = method.getParameters().stream().filter(p -> {
            return toSubstitute.stream().anyMatch(subst -> typeUtils().isAssignable(subst.asType(), p.asType()));
        }).map(it -> it.getSimpleName().toString()).toList();
        var substitutedVariant = !requiresSubstitution.isEmpty() ? forwardMethod.build().toBuilder() : null;
        if (!requiresSubstitution.isEmpty()) {
            substitutedVariant.addParameters(method.getParameters().stream().map(param -> {
                if (!requiresSubstitution.contains(param.getSimpleName().toString())) return ParameterSpec.get(param);
                var wildcardType = WildcardTypeName.subtypeOf(TypeName.get(param.asType()));
                var reactiveSubst = ParameterizedTypeName.get(ClassName.get(Element.class), wildcardType);
                return ParameterSpec.builder(reactiveSubst, param.getSimpleName().toString()).build();
            }).toList());

            if (hasComplexParams && isSetter) {
                substitutedVariant.addCode(CodeBlock.builder()
                        .addStatement("$N($L)", method.getSimpleName(), params.stream().map(it -> requiresSubstitution.contains(it.name) ? it.name + ".get()" : it.name).collect(Collectors.joining(", ")))
                        .build()
                );
            } else {
                var hasReturn = !isSetter && method.getReturnType().getKind() != TypeKind.VOID && relevantExtension.isEmpty();
                substitutedVariant.addCode(CodeBlock.builder()
                        .addStatement("$L$N($L)", hasReturn ? "return " : "", method.getSimpleName(), params.stream().map(it -> requiresSubstitution.contains(it.name) ? it.name + ".get()" : it.name).collect(Collectors.joining(", ")))
                        .build()
                );
            }
        }


        forwardMethod.addParameters(params);
        final var parameters = params.stream().map(it -> it.name).collect(Collectors.joining(", "));
        var hasReturn = !isSetter && method.getReturnType().getKind() != TypeKind.VOID;
        var extensionInvocation = relevantExtension.map(extension -> CodeBlock.builder()
                .addStatement("$T.$N($N, $N, $L)",
                        extension.getEnclosingElement().asType(), extension.getSimpleName(), cx, element, parameters)
                .build()
        );
        var directInvocation = CodeBlock.builder().addStatement("$L$N.$N($L)",
                hasReturn ? "return " : "",
                element,
                method.getSimpleName(),
                parameters
        ).build();
        if(extensionInvocation.isPresent()) {
            forwardMethod.addCode(extensionInvocation.get());
        } else if (hasComplexParams && isSetter) {
            final var createEffectInvocation = CodeBlock.builder()
                    .add("$N.createEffect(", cx)
                    .beginControlFlow("()->")
                        .add(directInvocation)
                    .endControlFlow()
                    .add(");")
                    .build();
            forwardMethod.addCode(createEffectInvocation);
        } else {
            forwardMethod.addCode(directInvocation);
        }
        return Stream.of(forwardMethod, substitutedVariant).filter(Objects::nonNull).map(MethodSpec.Builder::build);
    }

    public boolean isPrimitive(
            TypeMirror typeMirror//,
//            Collection<TypeElement> additionalPrimitives
    ) {
        if (typeMirror.getKind().isPrimitive() || typeMirror.getKind() == TypeKind.ARRAY)
            return true;
        var type = typeUtils().asElement(typeMirror);
        var stringType = type.toString();
        Class<?> classType;
        try {
            classType = Class.forName(stringType);
        } catch (ClassNotFoundException e) {
            classType = null;
        }
        Set<Class<?>> primitives = Set.of(
                LocalDate.class,
                LocalDateTime.class,
                Instant.class,
                ZonedDateTime.class,
                OffsetDateTime.class,
                UUID.class
        );
        var isBasePrimitive = primitives.stream().anyMatch(e -> stringType.equals(e.getName()));
//        var isAdditionalPrimitive = additionalPrimitives.stream()
//                .map(TypeElement::getQualifiedName)
//                .map(Name::toString)
//                .anyMatch(stringType::equals);
        return isBasePrimitive
//                || isAdditionalPrimitive
                || type.getKind() == ElementKind.ENUM
                || (classType != null && (
                classType.isPrimitive() || Constable.class.isAssignableFrom(classType)
        ));
    }


    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(ReactiveBuilder.class.getCanonicalName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_21;
    }

    private AnnotationMirror getAnnotationMirror(javax.lang.model.element.Element typeElement, Class<?> clazz) {
        String clazzName = clazz.getName();
        for (AnnotationMirror m : typeElement.getAnnotationMirrors()) {
            if (m.getAnnotationType().toString().equals(clazzName)) {
                return m;
            }
        }
        logger.fail(typeElement, "Annotation %s not found", typeElement);
        throw new NoSuchElementException("Annotation " + clazzName + " not found");
    }
}
