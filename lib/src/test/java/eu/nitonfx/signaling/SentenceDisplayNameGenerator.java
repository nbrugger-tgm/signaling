package eu.nitonfx.signaling;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGenerator;

import java.lang.reflect.Method;

public class SentenceDisplayNameGenerator extends DisplayNameGenerator.Standard {
    @Override
    public String generateDisplayNameForClass(Class<?> testClass) {
        var name = super.generateDisplayNameForClass(testClass);
        if (name.endsWith("Test"))
            return name.substring(0, name.length() - 4);
        return name;
    }

    @Override
    public String generateDisplayNameForNestedClass(Class<?> nestedClass) {
        var annotation = nestedClass.getAnnotation(DisplayName.class);
        if (annotation != null)
            return annotation.value();
        var simpleName = nestedClass.getSimpleName();
        return decapitalize(simpleName) + "()";
    }

    private String decapitalize(String simpleName) {
        if(simpleName.isEmpty()) return "";
        return Character.toLowerCase(simpleName.charAt(0))+simpleName.substring(1);
    }

    @Override
    public String generateDisplayNameForMethod(Class<?> testClass, Method testMethod) {
        var annotation = testMethod.getAnnotation(DisplayName.class);
        if (annotation != null) return annotation.value();
        if (testMethod.getName().startsWith("should")) {
            var name = testMethod.getName();
            name = name.replace("_", ", ");
            return separateCase(name, " ").toLowerCase();
        }
        return super.generateDisplayNameForMethod(testClass, testMethod);
    }

    private String separateCase(String string, String separator){
        StringBuilder result = new StringBuilder();
        boolean prevUpper = false;
        for (int i = 0; i < string.length(); i++) {
            var letter = string.charAt(i);
            if(Character.isUpperCase(letter)) {
                if(!prevUpper) {
                    result.append(separator);
                    result.append(Character.toLowerCase(letter));
                }
                else result.append(letter);
                prevUpper = true;
            } else {
                result.append(letter);
                prevUpper = false;
            }
        }
        return result.toString();
    }
}