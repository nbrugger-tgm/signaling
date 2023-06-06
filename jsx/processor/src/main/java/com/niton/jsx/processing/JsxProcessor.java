package com.niton.jsx.processing;

import com.google.auto.service.AutoService;
import com.niton.compile.processor.BaseProcessor;
import com.niton.compile.verify.ProcessingVerification;
import com.niton.jsx.Component;
import com.niton.jsx.rt.JSX;
import com.niton.jsx.JsxComponent;
import com.niton.jsx.parsing.JsxParser;
import com.niton.parser.ast.AstNode;
import com.niton.parser.ast.LocatableReducedNode;
import com.niton.parser.ast.ReducedNode;
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
        /*
    public static Context cx = Context.create();
    public static HTMLDocument document = HTMLDocument.current();
    public static JSX jsx = new JSX(document, cx);
         */
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
        var parser = JsxParser.get();
        var jsx = typeElement.getAnnotation(JsxComponent.class).value().trim();
        try {
            var jsxAst = parser.parse(jsx).reduce("jsx");
            return transformAST(typeElement, fieldMap, jsxAst.orElseThrow());
        } catch (ParsingException e) {
//            logger.fail(element, e.getFullExceptionTree());
            var prominent = e.getMostProminentDeepException();
//            logger.fail(element,"Most probable error : "+ prominent.getMessage());
            logger.fail(element, "Syntax error!\n"+ AstNode.Location.oneChar(prominent.getLine(), prominent.getColumn()).markInText(
                    jsx, 2, prominent.getMessage()
            ));
            throw new RuntimeException(prominent);
        } catch (InterpretationException e){
            logger.fail(element, e.getSyntaxErrorMessage(jsx));
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

    private MethodSpec transformAST(TypeElement typeElement, Map<String, ? extends Element> fieldMap, LocatableReducedNode jsxAst) {
        try {
            return MethodSpec.methodBuilder(typeElement.getSimpleName().toString())
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
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

    private Optional<CodeBlock> substituteProps(LocatableReducedNode jsxAst, Map<String, ? extends Element> fieldMap) {
        return transformNode(jsxAst).map(CodeBlock::toString).map(s -> {
            for (var entry : fieldMap.entrySet()) {
                s = s.replaceAll(entry.getKey()+"(?=\\.)", "component." + entry.getKey());
                s = s.replaceAll(entry.getKey()+"(?!\\.)", "component." + entry.getKey()+".get()");
            }
            return s;
        }).map(CodeBlock::of);
    }

    private Optional<CodeBlock> transformNode(LocatableReducedNode jsxAst) {
        var nodeType = jsxAst.getSubNode("type").orElseThrow().getValue();
        var nodeValue = jsxAst.getSubNode("value").orElseThrow();
        if (nodeType == null) throw new RuntimeException("Node type is null : \n" + jsxAst.format());
        return switch (JsxParser.NodeType.valueOf(nodeType)) {
            case TEXT_NODE -> transformTextNode(nodeValue);
            case EXPRESSION_NODE -> Optional.of(transformExpressionNode(nodeValue));
            case CONTAINER_TAG, SELF_CLOSING_TAG -> Optional.of(transformTagNode(nodeValue));
        };
    }

    private CodeBlock transformTagNode(LocatableReducedNode nodeValue) {
        var parameters = nodeValue.getSubNode("parameters").orElseThrow().getChildren();
        var tagName = nodeValue.getSubNode("name").orElseThrow().join();
        nodeValue.getSubNode("closingName").ifPresent(closingName -> {
            if (!Objects.equals(closingName.join(), tagName))
                throw new InterpretationException("Closing tag name does not match opening tag name (%s)".formatted(tagName), closingName, 5);
        });
        var staticParameters = new HashMap<String, String>(parameters.size());
        var dynamicParameters = new HashMap<String, String>(parameters.size());
        var eventListeners = new HashMap<String, String>(parameters.size());
        transformParameters(parameters, staticParameters, dynamicParameters, eventListeners);
        var staticMap = staticParameters.entrySet().stream().flatMap(quotedValuePairs(true)).collect(Collectors.joining(", "));
        var dynamicMap = dynamicParameters.entrySet().stream().flatMap(quotedValuePairs(false)).collect(Collectors.joining(", "));
        var eventMap = eventListeners.entrySet().stream().flatMap(quotedValuePairs(false)).collect(Collectors.joining(", "));

        if(Character.isUpperCase(tagName.charAt(0))){
            return transformComponentNode(tagName, staticMap, dynamicMap, eventMap);
        }
        var children = nodeValue.getSubNode("children");
        return children.map(childrenNode -> {
            var childList = childrenNode.getChildren().stream().map(this::transformNode).flatMap(Optional::stream).map(CodeBlock::toString).collect(Collectors.joining(",\n\t"));
            return CodeBlock.builder()
                    .add("jsx.generic($S, $T.of($L), $T.of($L), $T.of($L), $T.of(\n\t$L\n))", tagName,Map.class, staticMap,Map.class, dynamicMap, Map.class,eventMap,List.class, childList)
                    .build();
        }).orElseGet(
                () -> CodeBlock.builder()
                        .add("jsx.generic($S, $T.of($L), $T.of($L), $T.of($L))", tagName,Map.class, staticMap,Map.class, dynamicMap,Map.class, eventMap)
                        .build()
        );
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

    private void transformParameters(
            List<LocatableReducedNode> parameters,
            Map<String, String> staticParamers,
            Map<String, String> dynamicParameters,
            Map<String, String> eventListeners
    ) {
        for (LocatableReducedNode parameter : parameters) {
            var name = parameter.getSubNode("name").orElseThrow().getValue();
            var value = parameter.getSubNode("value").orElseThrow();
            var valueType = JsxParser.ParameterType.valueOf(value.getSubNode("type").orElseThrow().getValue());
            value = value.getSubNode("value").orElseThrow();
            switch (valueType) {
                case STATIC_VALUE -> staticParamers.put(name, value.getSubNode("content").orElseThrow().getValue());
                case EXPRESSION_VALUE -> {
                    var expression = value.getValue();
                    if (name.startsWith("on")) {
                        var withoutOn = name.substring(2);
                        withoutOn = Character.toLowerCase(withoutOn.charAt(0)) + withoutOn.substring(1);
                        eventListeners.put(withoutOn, expression);
                    } else {
                        dynamicParameters.put(name, "()->"+expression);
                    }
                }
            }
        }
    }

    private CodeBlock transformExpressionNode(ReducedNode nodeValue) {
        return CodeBlock.builder()
                .add("jsx.text(()->$L)", nodeValue.getValue())
                .build();
    }

    private Optional<CodeBlock> transformTextNode(ReducedNode jsxAst) {
        var value = jsxAst.getValue().trim().replace("\n", "").replace("\r", "");
        if(value.isEmpty()) return Optional.empty();
        return Optional.of(CodeBlock.builder()
                .indent()
                .add("jsx.text($S)", value)
                .build());
    }
}
