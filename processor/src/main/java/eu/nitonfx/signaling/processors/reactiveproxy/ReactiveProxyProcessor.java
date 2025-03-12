package eu.nitonfx.signaling.processors.reactiveproxy;

import com.google.auto.service.AutoService;
import com.niton.compile.processor.BaseProcessor;
import com.squareup.javapoet.*;
import eu.nitonfx.signaling.api.Context;
import eu.nitonfx.signaling.api.Signal;

import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AutoService(Processor.class)
public class ReactiveProxyProcessor extends BaseProcessor {
    private final List<ReactiveImpl> impls = new ArrayList<>();
        record ReactiveImpl(TypeName impl, TypeName init){}
    @Override
    public boolean performProcessing(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        impls.addAll(roundEnvironment.getElementsAnnotatedWith(Reactive.class).stream().map(this::process).toList());
        if (roundEnvironment.processingOver()) createFactory(impls);
        return true;
    }

    private void createFactory(List<ReactiveImpl> impls) {
        var factory = TypeSpec.classBuilder("ProxyFactory")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        createNullishFactory(impls, factory);
        createInitFactory(impls, factory);

        writeClass(this.getClass().getPackageName(), factory.build());
    }

    private static void createInitFactory(List<ReactiveImpl> impls, TypeSpec.Builder factory) {
        var generic = TypeVariableName.get("T");
        var parameterizedClass = ParameterizedTypeName.get(ClassName.get(Init.class), generic);
        var typeParam = ParameterSpec.builder(parameterizedClass, "init").build();
        var nullishCreate = MethodSpec.methodBuilder("create")
                .addModifiers(Modifier.FINAL, Modifier.PUBLIC, Modifier.STATIC)
                .addTypeVariable(generic)
                .returns(generic)
                .addParameter(Context.class, "cx")
                .addParameter(typeParam);

        impls.forEach(impl -> {
            nullishCreate.beginControlFlow("if ($N instanceof $T typedInit)", typeParam, impl.init)
                    .addStatement("return ($T) new $T($N,$N)", generic, impl.impl, "cx", "typedInit")
                    .endControlFlow();
        });
        nullishCreate.addStatement("throw new $T($N + $S)", IllegalArgumentException.class, typeParam, " has no reactive implementation. Consider using @Reactive");
        factory.addMethod(nullishCreate.build());
    }

    private static void createNullishFactory(List<ReactiveImpl> impls, TypeSpec.Builder factory) {
        var generic = TypeVariableName.get("T");
        var parameterizedClass = ParameterizedTypeName.get(ClassName.get(Class.class), generic);
        var typeParam = ParameterSpec.builder(parameterizedClass, "type").build();
        var nullishCreate = MethodSpec.methodBuilder("create")
                .addModifiers(Modifier.FINAL, Modifier.PUBLIC, Modifier.STATIC)
                .addTypeVariable(generic)
                .returns(generic)
                .addParameter(Context.class, "cx")
                .addParameter(typeParam);

        impls.forEach(impl -> {
            nullishCreate.beginControlFlow("if ($N.isAssignableFrom($T.class))", typeParam, impl.impl)
                    .addStatement("return ($T) new $T($N)", generic, impl.impl, "cx")
                    .endControlFlow();
        });
        nullishCreate.addStatement("throw new $T($N + $S)", IllegalArgumentException.class, typeParam, " has no reactive implementation. Consider using @Reactive");
        factory.addMethod(nullishCreate.build());
    }

    private ReactiveImpl process(Element element) {
        verifier.isInterface(element)
                .failOnViolation();
        TypeElement typeElement = (TypeElement) element;
        var methods = elementUtils().getAllMembers(typeElement).stream()
                .filter(member -> member.getKind() == ElementKind.METHOD)
                .map(member -> (ExecutableElement) member)
                .filter(method -> !method.isDefault())
                .filter(method -> !method.getModifiers().contains(Modifier.STATIC))
                .filter(method -> method.getEnclosingElement().getKind() == ElementKind.INTERFACE)
                .toList();
        var fields = methods.stream().map(method -> {
            var methodName = method.getSimpleName().toString();
            var isGetter = methodName.startsWith("get");
            var isSetter = methodName.startsWith("set");
            if ((isGetter || isSetter) && methodName.length() == 3) {
                logger.fail(method, "get & set without a name are not valid setters/getters");
            }
            if (isSetter) {
                if (method.getReturnType().getKind() != TypeKind.VOID) {
                    logger.fail(method, "setters are not allowed to have return types");
                }
                if (method.getParameters().size() != 1) {
                    logger.fail(method, "setters require exactly one parameter");
                }
                return new Field(method.getParameters().getFirst().asType(), decapitalize(methodName.substring(3)), null, method);
            } else {
                if (method.getReturnType().getKind() == TypeKind.VOID) {
                    logger.fail(method, "getters require a return type");
                }
                if (!method.getParameters().isEmpty()) {
                    logger.fail(method, "getters are not allowed to have parameters (%s)", method);
                }
                return new Field(method.getReturnType(), decapitalize(isGetter ? methodName.substring(3) : methodName), method, null);
            }
        }).collect(Collectors.toMap(it -> it.name, Function.identity(), (f1, f2) -> {
            if (!f1.type.equals(f2.type)) {
                logger.fail(Objects.requireNonNullElse(f2.getter, f2.setter), "type does not match with %s", Objects.requireNonNullElse(f1.getter, f1.setter));
            }
            return new Field(f1.type, f1.name, Objects.requireNonNullElse(f1.getter, f2.getter), f1.setter != null ? f1.setter : f2.setter);
        }));
        return createReactiveImplementation(typeElement, fields);
    }

    private String decapitalize(String substring) {
        return Character.toLowerCase(substring.charAt(0)) + substring.substring(1);
    }

    private ReactiveImpl createReactiveImplementation(TypeElement typeElement, Map<String, Field> fields) {
        var implementation = TypeSpec.classBuilder("Reactive" + typeElement.getSimpleName().toString());
        implementation.addModifiers(Modifier.PUBLIC);
        implementation.addSuperinterface(typeElement.asType());


        fields.forEach((__, field) -> {
            var signalType = ParameterizedTypeName.get(ClassName.get(Signal.class), TypeName.get(getBoxedType(field.type)));
            implementation.addField(signalType, field.name, Modifier.FINAL, Modifier.PRIVATE);

            if (field.getter == null) {
                logger.fail(field.setter, "Write only fields are not allowed, please add a getter");
                return;
            }
            implementation.addMethod(MethodSpec.methodBuilder(field.getter.getSimpleName().toString())
                    .addModifiers(Modifier.FINAL, Modifier.PUBLIC)
                    .returns(TypeName.get(field.type))
                    .addStatement("return this.$N.get()", field.name)
                    .build()
            );
            if (field.setter != null) {
                var param = ParameterSpec.get(field.setter.getParameters().getFirst());
                implementation.addMethod(MethodSpec.methodBuilder(field.setter.getSimpleName().toString())
                        .addModifiers(Modifier.FINAL, Modifier.PUBLIC)
                        .addParameter(param)
                        .addStatement("this.$N.set($N)", field.name, param.name)
                        .build()
                );
            }
        });

        var contextCtor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(Context.class), "cx");
        fields.forEach((__, field) -> {
            contextCtor.addParameter(TypeName.get(field.type), field.name);
            contextCtor.addStatement("this.$N = $N.$N($N)", field.name, "cx", "createSignal", field.name);
        });
        implementation.addMethod(contextCtor.build());

        var nullCtor = MethodSpec.constructorBuilder()
                .addParameter(Context.class, "cx")
                .addModifiers(Modifier.PUBLIC);
        fields.forEach((__, field) -> {
            nullCtor.addStatement("this.$N = $N.$N()", field.name, "cx", "createNullSignal");
        });
        implementation.addMethod(nullCtor.build());


        final var pck = elementUtils().getPackageOf(typeElement).getQualifiedName().toString();
        var initRecordName = createInitRecord(typeElement, fields, pck);


        final var initCtor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(Context.class), "cx")
                .addParameter(initRecordName, "init");
        fields.forEach((__, field) -> {
            initCtor.addStatement("this.$N = $N.$N($N.$N)", field.name, "cx", "createSignal", "init", field.name);
        });
        implementation.addMethod(initCtor.build());


        final var implementationClass = implementation.build();
        writeClass(pck, implementationClass);
        var implName = ClassName.get(pck, implementationClass.name);
        return new ReactiveImpl(implName, initRecordName);
    }

    private ClassName createInitRecord(TypeElement typeElement, Map<String, Field> fields, String pck) {
        var initRecord = TypeSpec.classBuilder(typeElement.getSimpleName().toString() + "$Init")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addSuperinterface(typeUtils().getDeclaredType(elementUtils().getTypeElement(Init.class.getCanonicalName()), typeElement.asType()));
        var initRecordCtor = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
        fields.forEach((__, field) -> {
            initRecord.addField(TypeName.get(field.type), field.name);
            initRecordCtor.addParameter(TypeName.get(field.type), field.name);
            initRecordCtor.addStatement("this.$N = $N", field.name, field.name);
        });
        initRecord.addMethod(initRecordCtor.build());

        var initRecordClass = initRecord.build();
        var initRecordName = ClassName.get(pck, initRecordClass.name);
        writeClass(pck, initRecordClass);
        return initRecordName;
    }

    private Types typeUtils() {
        return processingEnv.getTypeUtils();
    }

    private Elements elementUtils() {
        return processingEnv.getElementUtils();
    }

    public TypeMirror getBoxedType(TypeMirror t) {
        return t.getKind().isPrimitive() ?
                typeUtils().boxedClass(typeUtils().getPrimitiveType(t.getKind())).asType() : t;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(Reactive.class.getCanonicalName());
    }

    record Field(TypeMirror type, String name, ExecutableElement getter, ExecutableElement setter) {
    }
}
