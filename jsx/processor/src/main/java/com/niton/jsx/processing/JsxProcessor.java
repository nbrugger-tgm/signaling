package com.niton.jsx.processing;

import com.google.auto.service.AutoService;
import com.niton.compile.processor.BaseProcessor;
import com.niton.compile.verify.ProcessingVerification;
import com.niton.jsx.Component;
import com.niton.jsx.parsing.model.*;
import com.niton.jsx.rt.JSX;
import com.niton.jsx.JsxComponent;
import com.niton.jsx.parsing.JsxParser;
import com.niton.parser.ast.AstNode;
import com.niton.parser.exceptions.InterpretationException;
import com.niton.parser.exceptions.ParsingException;
import com.squareup.javapoet.*;
import eu.nitonfx.signaling.api.Context;
import org.jetbrains.annotations.NotNull;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@SupportedAnnotationTypes("com.niton.jsx.JsxComponent")
public class JsxProcessor extends BaseProcessor {
    TypeSpec.Builder componentsClass = TypeSpec.classBuilder("JsxComponents")
            .addFields(jsxEnvironment())
            .addModifiers(Modifier.PUBLIC);

    @Override
    public boolean performProcessing(@NotNull Set<? extends TypeElement> set, @NotNull RoundEnvironment roundEnvironment) {
        componentsClass.addMethods(roundEnvironment.getElementsAnnotatedWith(JsxComponent.class).stream().filter(e -> e.getAnnotation(JsxComponent.class) != null).map(this::toMethod).toList())
                .build();
        if (roundEnvironment.processingOver())
            writeClass("com.niton.jsx", componentsClass.build());
        return false;
    }

    private Iterable<FieldSpec> jsxEnvironment() {
        return List.of(
                FieldSpec.builder(Context.class, "cx", Modifier.STATIC, Modifier.PUBLIC).initializer("Context.create()").build(),
                FieldSpec.builder(HTMLDocument.class, "document", Modifier.STATIC, Modifier.PUBLIC).initializer("HTMLDocument.current()").build(),
                FieldSpec.builder(JSX.class, "jsx", Modifier.STATIC, Modifier.PUBLIC).initializer("new JSX(document, cx)").build()
        );
    }

    Elements elementUtils() {
        return processingEnv.getElementUtils();
    }

    Types typeUtils() {
        return processingEnv.getTypeUtils();
    }

    private MethodSpec toMethod(Element element) {
        verifier.isClass(element).failOnViolation();
        verifier.doesImplement(element, Component.class).failOnViolation();
        var typeElement = (TypeElement) element;
        var fields = elementUtils().getAllMembers(typeElement).stream().filter(
                e -> e.getKind() == ElementKind.FIELD
        ).toList();
        for (Element field : fields) {
            verifyPublicFinal(field);
        }
        var fieldMap = fields.stream().collect(Collectors.toMap(e -> e.getSimpleName().toString(), Function.identity()));
        var parser = new JsxParser();
        var jsxText = typeElement.getAnnotation(JsxComponent.class).value().trim();
        try {
            var jsx = parser.parse(jsxText);
            return transformAST(typeElement, fieldMap, jsx);
        } catch (ParsingException e) {
//            logger.fail(element, e.getFullExceptionTree());
            var prominent = e.getMostProminentDeepException();
//            logger.fail(element,"Most probable error : "+ prominent.getMessage());
            logger.fail(element, "Syntax error!\n"+ AstNode.Location.oneChar(prominent.getLine(), prominent.getColumn()).markInText(
                    jsxText, 2, prominent.getMessage()
            ));
            throw new RuntimeException(prominent);
        } catch (InterpretationException e){
            logger.fail(element, e.getSyntaxErrorMessage(jsxText));
            throw new RuntimeException(e);
        }
    }

    private void verifyPublicFinal(Element field) {
        new ProcessingVerification(
                logger,
                e -> e.getModifiers().contains(Modifier.PUBLIC),
                "Field " + field.getSimpleName() + " must [not] be public",
                field
        ).failOnViolation();
        new ProcessingVerification(
                logger,
                e -> e.getModifiers().contains(Modifier.STATIC),
                "Field " + field.getSimpleName() + " must [not] be static",
                field
        ).not().failOnViolation();
    }

    private MethodSpec transformAST(TypeElement typeElement, Map<String, ? extends Element> fieldMap, JsxElement jsxAst) {
        try {
            return MethodSpec.methodBuilder(typeElement.getSimpleName().toString())
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addTypeVariables(typeElement.getTypeParameters().stream().map(TypeVariableName::get).collect(Collectors.toList()))
                    .returns(HTMLElement.class)
                    .addCode(CodeBlock.builder()
                            .addStatement("var component = new $T()", typeElement)
                            .addStatement("component.initialize(cx)")
                            .addStatement("return $L", substituteProps(jsxAst, fieldMap).orElseThrow(() -> new RuntimeException("No root element found!")))
                            .build())
                    .build();
        }catch (IllegalArgumentException e){
            logger.fail(typeElement,e.getMessage());
            throw e;
        }
    }

    private Optional<CodeBlock> substituteProps(JsxElement jsxAst, Map<String, ? extends Element> fieldMap) {
        return transformNode(jsxAst).map(CodeBlock::toString).map(s -> {
            for (var entry : fieldMap.entrySet()) {
                s = s.replaceAll(entry.getKey()+"(?=\\.)", "component." + entry.getKey());
                s = s.replaceAll(entry.getKey()+"(?!\\.)", "component." + entry.getKey()+".get()");
            }
            return s;
        }).map(CodeBlock::of);
    }

    private Optional<CodeBlock> transformNode(JsxElement jsxAst) {
        return switch (jsxAst) {
            case JsxTextNode textNode -> transformTextNode(textNode);
            case JsxExpressionNode expressionNode -> Optional.of(transformExpressionNode(expressionNode));
            case JsxTag tag -> Optional.of(transformTagNode(tag));
        };
    }

    private CodeBlock transformTagNode(JsxTag tag) {
        var staticMap = tag.staticAttributes().entrySet().stream().flatMap(quotedValuePairs(true)).collect(Collectors.joining(", "));
        var dynamicMap = tag.dynamicAttributes().entrySet().stream().flatMap(quotedValuePairs(false)).collect(Collectors.joining(", "));
        var eventMap = tag instanceof JsxHtmlTag htmlTag ? htmlTag.eventHandlers().entrySet().stream().flatMap(quotedValuePairs(false)).collect(Collectors.joining(", ")) : "";

        return switch (tag) {
            case JsxComponentTag __ -> transformComponentNode(tag.name(), staticMap, dynamicMap, eventMap);
            case JsxHtmlTag html -> {
                if(html.selfClosing()){
                   yield CodeBlock.builder()
                            .add("jsx.generic($S, $T.of($L), $T.of($L), $T.of($L))", tag.name(),Map.class, staticMap,Map.class, dynamicMap,Map.class, eventMap)
                            .build();
                }else {
                    var childList = Arrays.stream(html.children()).map(this::transformNode).flatMap(Optional::stream).map(CodeBlock::toString).collect(Collectors.joining(",\n\t"));
                    yield  CodeBlock.builder()
                            .add("jsx.generic($S, $T.of($L), $T.of($L), $T.of($L), $T.of(\n\t$L\n))", tag.name(),Map.class, staticMap,Map.class, dynamicMap, Map.class,eventMap,List.class, childList)
                            .build();
                }
            }
        };

    }

    private CodeBlock transformComponentNode(String nodeValue, String staticMap, String dynamicMap, String eventMap) {
        return CodeBlock.builder()
                .add("$T.$L()", ClassName.get("com.niton.jsx","JsxComponents"),nodeValue)
                .build();
    }

    @NotNull
    private static Function<Map.Entry<String, String>, Stream<? extends String>> quotedValuePairs(boolean quoteValue) {
        return e -> Stream.of("\"" + e.getKey() + "\"", quoteValue ? "\"" + e.getValue() + "\"" : e.getValue());
    }

    private CodeBlock transformExpressionNode(JsxExpressionNode nodeValue) {
        return CodeBlock.builder()
                .add("jsx.text(()->$L)", nodeValue.expression())
                .build();
    }

    private Optional<CodeBlock> transformTextNode(JsxTextNode jsxAst) {
        var value = jsxAst.text().trim().replace("\n", "").replace("\r", "");
        if(value.isEmpty()) return Optional.empty();
        return Optional.of(CodeBlock.builder()
                .indent()
                .add("jsx.text($S)", value)
                .build());
    }
}
