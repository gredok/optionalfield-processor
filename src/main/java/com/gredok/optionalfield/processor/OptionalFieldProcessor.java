package com.gredok.optionalfield.processor;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * Annotation processor that generates {@code *Req} classes for types annotated with
 * {@link OptionalClassReq}. Runs at compile time via the standard
 * {@code javax.annotation.processing} SPI — no manual configuration required.
 *
 * @see OptionalClassReq
 */
@SupportedAnnotationTypes(OptionalFieldProcessor.PACKAGE + ".OptionalClassReq")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@SupportedOptions("verbose")
public class OptionalFieldProcessor extends AbstractProcessor {

    /** Base package used in generated imports. */
    protected static final String PACKAGE = "com.gredok.optionalfield.processor";

    private Set<String> skipAnnotations = Set.of("Schema");

    private Messager messager;
    private Elements elementUtils;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        messager = processingEnv.getMessager();
        elementUtils = processingEnv.getElementUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) {
            return false;
        }

        for (TypeElement annotation : annotations) {
            messager.printMessage(Diagnostic.Kind.NOTE, "Found annotation: " + annotation.getQualifiedName());
        }

        // Collect all classes that contain OptionalField annotations
        Set<TypeElement> mainClasses = new HashSet<>();
        for (Element element : roundEnv.getElementsAnnotatedWith(OptionalClassReq.class)) {
            if (element.getKind() == ElementKind.CLASS) {
                TypeElement typeElement = (TypeElement) element;
                mainClasses.add(typeElement);
            }
        }

        // Generate main class for each class containing OptionalField annotations
        for (TypeElement mainClass : mainClasses) {
            generateMainClass(mainClass);
        }

        return true;
    }

    private void generateMainClass(TypeElement classElement) {
        String className = classElement.getSimpleName() + "Req";
        String packageName = getPackageName(classElement);

        try {
            JavaFileObject sourceFile = processingEnv.getFiler()
                    .createSourceFile(packageName + "." + className);

            Set<String> imports = new HashSet<>();
            imports.add("tools.jackson.databind.annotation.JsonDeserialize");
            imports.add("tools.jackson.databind.annotation.JsonSerialize");
            imports.add(PACKAGE + ".OptionalField");
            imports.add(PACKAGE + ".jackson.AbstractOptionalFieldClassDeserializer");
            imports.add(PACKAGE + ".jackson.AbstractOptionalFieldClassSerializer");
            imports.add("java.util.HashMap");
            imports.add("java.util.Map");

            // Collect imports
            for (Element field : classElement.getEnclosedElements()) {
                if (isProcessableField(field)) {
                    imports.addAll(getAnnotationImports(field));
                }
                imports.addAll(getRequiredImports(field.asType(), classElement.getSimpleName() + "Builder"));
            }

            try (PrintWriter out = new PrintWriter(sourceFile.openWriter())) {
                out.println("package " + packageName + ";");
                out.println();

                // Write imports
                imports
                        .stream()
                        .filter(s -> !s.startsWith("io.swagger.v3"))
                        .forEach(imp -> {
                            out.println("import " + imp + ";");
                        });

                out.println();

                out.println("@JsonDeserialize(using = " + className + ".Deserializer.class)");
                out.println("@JsonSerialize(using = " + className + ".Serializer.class)");
                out.println("public class " + className + " {");

                // Generate fields
                for (Element field : classElement.getEnclosedElements()) {
                    if (isProcessableField(field)) {
                        generateField(out, field);
                    }
                }

                // Generate getters
                generateGetters(out, classElement);

                // Generate setters
                generateSetters(out, classElement);

                // Generate constructors
                generateConstructors(out, className, classElement);

                // Generate Builder
                generateBuilder(out, classElement);

                generateToMap(out, classElement);

                generateDeserializer(out, classElement);

                generateSerializer(out, classElement);

                out.println("}");
            }
        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Failed to generate main class: " + e.getMessage(), classElement);
        }
    }

    private void generateGetters(PrintWriter out, TypeElement classElement) {
        for (Element field : classElement.getEnclosedElements()) {
            if (isProcessableField(field)) {
                String fieldName = field.getSimpleName().toString();
                String simpleType = getSimpleTypeName(field.asType());
                String capitalizedFieldName = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                out.println();
                out.println("    public OptionalField<" + simpleType + "> get" + capitalizedFieldName + "() {");
                out.println("        return this." + fieldName + ";");
                out.println("    }");
            }
        }
    }

    private void generateSetters(PrintWriter out, TypeElement classElement) {
        for (Element field : classElement.getEnclosedElements()) {
            if (isProcessableField(field)) {
                String fieldName = field.getSimpleName().toString();
                String simpleType = getSimpleTypeName(field.asType());
                String capitalizedFieldName = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                out.println();
                out.println("    public void set" + capitalizedFieldName + "(OptionalField<" + simpleType + "> " + fieldName + ") {");
                out.println("        this." + fieldName + " = " + fieldName + " != null ? " + fieldName + " : OptionalField.empty();");
                out.println("    }");
            }
        }
    }

    private void generateConstructors(PrintWriter out, String className, TypeElement classElement) {
        // Default constructor
        out.println();
        out.println("    public " + className + "() {}");
    }

    private void generateBuilder(PrintWriter out, TypeElement classElement) {
        String className = classElement.getSimpleName() + "Req";

        out.println();
        out.println("    public static Builder builder() {");
        out.println("        return new Builder();");
        out.println("    }");
        out.println();
        out.println("    public static class Builder {");

        for (Element field : classElement.getEnclosedElements()) {
            if (isProcessableField(field)) {
                String fieldName = field.getSimpleName().toString();
                String simpleType = getSimpleTypeName(field.asType());
                out.println("        private OptionalField<" + simpleType + "> " + fieldName + " = OptionalField.empty();");
            }
        }

        out.println();

        // Builder methods - also without annotations in parameters
        for (Element field : classElement.getEnclosedElements()) {
            if (isProcessableField(field)) {
                String fieldName = field.getSimpleName().toString();
                String simpleType = getSimpleTypeName(field.asType());

                out.println("        public Builder " + fieldName + "(" + simpleType + " " + fieldName + ") {");
                out.println("            this." + fieldName + " = new OptionalField<" + simpleType + ">(\"" + fieldName + "\", true, " + fieldName + ");");
                out.println("            return this;");
                out.println("        }");
                out.println();
            }
        }

        // Build method
        out.println("        public " + className + " build() {");
        out.println("            " + className + " instance = new " + className + "();");
        for (Element field : classElement.getEnclosedElements()) {
            if (isProcessableField(field)) {
                String fieldName = field.getSimpleName().toString();
                out.println("            instance." + fieldName + " = this." + fieldName + ";");
            }
        }
        out.println("            return instance;");
        out.println("        }");
        out.println("    }");
    }

    private void generateToMap(PrintWriter out, TypeElement classElement) {
        out.println();
        out.println("    public Map<String, Object> toMap() {");
        out.println("        Map<String, Object> map = new HashMap<>();");
        for (Element field : classElement.getEnclosedElements()) {
            if (isProcessableField(field)) {
                String fieldName = field.getSimpleName().toString();
                out.println("        if (this." + fieldName + ".isPresent()) map.put(\"" + fieldName + "\", this." + fieldName + ".getValue());");
            }
        }
        out.println("        return map;");
        out.println("    }");
    }

    private void generateDeserializer(PrintWriter out, TypeElement classElement) {
        Name originalClassName = classElement.getSimpleName();
        String reqClassName = originalClassName + "Req";

        out.println();

        out.println("    public static class Deserializer extends AbstractOptionalFieldClassDeserializer<" + reqClassName + "> {");

        out.println("        @Override protected Class<" + reqClassName + "> rawClass() {");
        out.println("            return " + reqClassName + ".class;");
        out.println("        }");

        out.println("    }");
    }

    private void generateSerializer(PrintWriter out, TypeElement classElement) {
        Name originalClassName = classElement.getSimpleName();
        String reqClassName = originalClassName + "Req";

        out.println();

        out.println("    public static class Serializer extends AbstractOptionalFieldClassSerializer<" + reqClassName + "> {");
        out.println("    }");
    }

    private void generateField(PrintWriter out, Element field) {
        // Get all annotations except OptionalField
        List<? extends AnnotationMirror> annotations = field.getAnnotationMirrors().stream()
                .filter(am -> !am.getAnnotationType().toString().contains("OptionalClassReq"))
                .filter(am -> !skipAnnotations.contains(am.getAnnotationType().toString().substring(am.getAnnotationType().toString().lastIndexOf('.') + 1)))
                .toList();

        List<? extends AnnotationMirror> fliedAnnotations = annotations.stream()
                .filter(this::isFieldAnnotation)
                .toList();
        List<? extends AnnotationMirror> typeAnnotations = annotations.stream()
                .filter(am -> !isFieldAnnotation(am))
                .toList();

        StringBuilder fieldDeclaration = new StringBuilder("    private ");

        fliedAnnotations.forEach(am ->
                fieldDeclaration.append("@")
                        .append(getSimpleAnnotationName(am)).append(" ")
        );

        fieldDeclaration.append("OptionalField<");

        // Add type annotations (e.g. @NotNull)
        typeAnnotations.forEach(annotation ->
                fieldDeclaration
                        .append("@")
                        .append(getSimpleAnnotationName(annotation)).append(" ")
        );

        // Add the type
        TypeMirror fieldType = field.asType();
        fieldDeclaration
                .append(getSimpleType(fieldType))
                .append("> ")
                .append(field.getSimpleName())
                .append(" = new OptionalField<>(\"").append(field.getSimpleName()).append("\", false, null)")
                .append(";");

        out.println(fieldDeclaration);
    }

    private boolean isFieldAnnotation(AnnotationMirror annotation) {
        String fullName = annotation.getAnnotationType().toString();
        String simpleName = fullName.substring(fullName.lastIndexOf('.') + 1);
        return simpleName.equals("JsonProperty") || simpleName.equals("JsonSerialize") || simpleName.equals("JsonDeserialize");
    }

    /**
     * A field is processable (wrapped as a generated {@code OptionalField<T>}) only if it's an
     * instance field. Static fields (constants) on the source class are left out of the generated
     * request class entirely — they're not part of the wire payload.
     */
    private boolean isProcessableField(Element field) {
        return field.getKind() == ElementKind.FIELD && !field.getModifiers().contains(Modifier.STATIC);
    }

    private String getSimpleTypeName(TypeMirror type) {
        if (type.getKind().isPrimitive()) {
            return switch (type.getKind()) {
                case BOOLEAN -> "Boolean";
                case INT -> "Integer";
                case LONG -> "Long";
                case DOUBLE -> "Double";
                case FLOAT -> "Float";
                case SHORT -> "Short";
                case BYTE -> "Byte";
                case CHAR -> "Character";
                default -> type.toString(); // fallback
            };
        }

        // Handle generic types
        if (type instanceof DeclaredType) {
            DeclaredType declaredType = (DeclaredType) type;
            TypeElement typeElement = (TypeElement) declaredType.asElement();

            // For generic types like TreeSet<String>
            if (!declaredType.getTypeArguments().isEmpty()) {
                String mainType = typeElement.getSimpleName().toString();
                List<String> typeArgs = declaredType.getTypeArguments().stream()
                        .map(this::getSimpleTypeName)
                        .toList();
                return mainType + "<" + String.join(", ", typeArgs) + ">";
            }

            // For simple types
            return typeElement.getSimpleName().toString();
        }

        return type.toString();
    }

    private String getSimpleType(TypeMirror type) {
        if (type.getKind().isPrimitive()) {
            return switch (type.getKind()) {
                case BOOLEAN -> "Boolean";
                case INT -> "Integer";
                case LONG -> "Long";
                case DOUBLE -> "Double";
                case FLOAT -> "Float";
                case SHORT -> "Short";
                case BYTE -> "Byte";
                case CHAR -> "Character";
                default -> type.toString(); // fallback
            };
        }

        if (type.getKind() == TypeKind.DECLARED) {
            DeclaredType declaredType = (DeclaredType) type;
            TypeElement typeElement = (TypeElement) declaredType.asElement();

            if (!declaredType.getTypeArguments().isEmpty()) {
                StringBuilder genericType = new StringBuilder();
                genericType.append(typeElement.getSimpleName());
                genericType.append("<");

                List<? extends TypeMirror> typeArgs = declaredType.getTypeArguments();
                for (int i = 0; i < typeArgs.size(); i++) {
                    if (i > 0) {
                        genericType.append(", ");
                    }

                    // Get annotations on the type argument
                    TypeMirror typeArg = typeArgs.get(i);
                    List<? extends AnnotationMirror> annotations = typeArg.getAnnotationMirrors();
                    for (AnnotationMirror annotation : annotations) {
                        genericType.append("@").append(getSimpleAnnotationName(annotation)).append(" ");
                    }

                    genericType.append(getSimpleType(typeArg));
                }

                genericType.append(">");
                return genericType.toString();
            }

            return typeElement.getSimpleName().toString();
        }

        return type.toString();
    }

    private String getSimpleAnnotationName(AnnotationMirror annotation) {
        String fullName = annotation.getAnnotationType().toString();
        String simpleName = fullName.substring(fullName.lastIndexOf('.') + 1);

        Map<? extends ExecutableElement, ? extends AnnotationValue> values =
                annotation.getElementValues();
        if (!values.isEmpty()) {
            StringBuilder sb = new StringBuilder(simpleName).append("(");
            boolean first = true;
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
                    values.entrySet()) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(entry.getKey().getSimpleName()).append("=").append(entry.getValue());
                first = false;
            }
            sb.append(")");
            return sb.toString();
        }
        return simpleName;
    }

    private Set<String> getAnnotationImports(Element field) {
        Set<String> imports = new HashSet<>();
        for (AnnotationMirror annotation : field.getAnnotationMirrors()) {
            if (!annotation.getAnnotationType().toString().contains("OptionalField")) {
                imports.add(annotation.getAnnotationType().toString());
            }
        }
        return imports;
    }

    private String getPackageName(Element element) {
        return elementUtils.getPackageOf(element).getQualifiedName().toString();
    }

    private Set<String> getRequiredImports(TypeMirror type, String skippedPackage) {
        Set<String> imports = new HashSet<>();

        if (type.getKind() == TypeKind.DECLARED) {
            DeclaredType declaredType = (DeclaredType) type;
            TypeElement typeElement = (TypeElement) declaredType.asElement();
            // Add import for the main type
            String fullName = typeElement.getQualifiedName().toString();
            if (!fullName.startsWith("java.lang.") && !fullName.contains(skippedPackage)) { // Skip java.lang packages
                imports.add(fullName);
            }

            // Add imports for annotations on the type
            for (AnnotationMirror annotation : type.getAnnotationMirrors()) {
                imports.add(annotation.getAnnotationType().toString());
            }

            // Add imports for generic parameters
            for (TypeMirror typeArg : declaredType.getTypeArguments()) {
                imports.addAll(getRequiredImports(typeArg, skippedPackage));
                for (AnnotationMirror annotation : typeArg.getAnnotationMirrors()) {
                    imports.add(annotation.getAnnotationType().toString());
                }
            }
        }

        return imports;
    }
}
