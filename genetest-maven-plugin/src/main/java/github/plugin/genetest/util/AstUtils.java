package github.plugin.genetest.util;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithStaticModifier;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class AstUtils {

    public static CompilationUnit getUnit(File file) {
        try {
            return StaticJavaParser.parse(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static CompilationUnit getUnit(ClassOrInterfaceDeclaration clazz) {
        return clazz.findCompilationUnit()
            .orElseThrow(() -> new RuntimeException("test class unit not exist."));
    }

    public static CompilationUnit getUnit(MethodDeclaration method) {
        return method.findCompilationUnit()
            .orElseThrow(() -> new RuntimeException("src method unit not exist."));
    }

    public static PackageDeclaration getPackageDeclaration(CompilationUnit unit) {
        return unit.getPackageDeclaration()
            .orElseThrow(() -> new RuntimeException("package not exists."));
    }

    public static String getPackageName(CompilationUnit unit) {
        return getName(getPackageDeclaration(unit));
    }

    public static VariableDeclarator getVariableDeclarator(FieldDeclaration field) {
        return field.findAll(VariableDeclarator.class).get(0);
    }

    public static String getClassName(CompilationUnit unit) {
        return unit.getPrimaryTypeName()
            .orElseThrow(() -> new RuntimeException("class name not exists."));
    }

    public static String getName(NodeWithName<? extends Node> type) {
        return type.getName().asString();
    }

    public static String getName(NodeWithSimpleName<? extends Node> type) {
        return type.getName().asString();
    }

    public static String getType(Type type) {
        return StringUtils.splitFirst(type.toString(), "<");
    }

    public static ClassOrInterfaceType getClassOrInterfaceType(Type type) {
        if (type.isPrimitiveType()) {
            return ((PrimitiveType) type).toBoxedType();
        } else {
            return (ClassOrInterfaceType) type;
        }
    }

    public static Optional<ImportDeclaration> findImportDeclaration(
        CompilationUnit unit,
        String classTypeName
    ) {
        return unit.getImports()
            .stream()
            .filter(importDeclaration -> {
                String importPackageName = getName(importDeclaration);
                String importClassTypeName = StringUtils.splitLast(importPackageName, "\\.");
                return classTypeName.equals(importClassTypeName);
            })
            .findFirst();
    }

    private static String getDefaultValue(Type type) {
        if (type.isArrayType()) {
            return "new " + ((ArrayType) type).getComponentType().asString() + "[]{}";
        } else {
            ClassOrInterfaceType classType = getClassOrInterfaceType(type);
            return getDefaultValue(classType);
        }
    }

    public static String getDefaultValue(ClassOrInterfaceType fieldType) {
        String fieldTypeName = getName(fieldType);
        boolean genericExists = fieldType.getTypeArguments().isPresent();
        return FieldUtils.createDefaultValue(fieldTypeName, genericExists);
    }

    public static Type toUnboxedType(Type type) {
        if (type.isClassOrInterfaceType()) {
            ClassOrInterfaceType classType = (ClassOrInterfaceType) type;
            if (classType.isBoxedType()) {
                return classType.toUnboxedType();
            }
        }
        return type;
    }

    public static Optional<ClassOrInterfaceType> findClassOrInterfaceType(FieldDeclaration field) {
        if (field.getElementType().isClassOrInterfaceType()) {
            return Optional.of(field.getElementType().asClassOrInterfaceType());
        }

        if (field.getElementType().isPrimitiveType()) {
            return Optional.of(field.getElementType().asPrimitiveType().toBoxedType());
        }

        return Optional.empty();
    }

    public static void addImport(CompilationUnit unit, String importName) {
        unit.addImport(importName);
    }

    public static void addAnnotation(NodeWithAnnotations<? extends Node> node, AnnotationExpr annotation) {
        node.addAnnotation(annotation);
    }

    public static NodeList<MemberValuePair> createMemberValues(List<MemberValuePair> memberValuePairs) {
        NodeList<MemberValuePair> result = new NodeList<>();
        result.addAll(memberValuePairs);
        return result;
    }

    public static NodeList<MemberValuePair> createMemberValues(Map<String, String> nameValuePairs) {
        List<MemberValuePair> memberValuePairs = nameValuePairs.entrySet()
            .stream()
            .map((entry) -> {
                String name = entry.getKey();
                String value = entry.getValue();
                return new MemberValuePair(name, new NameExpr(value));
            })
            .collect(Collectors.toList());
        return createMemberValues(memberValuePairs);
    }

    public static NodeList<MemberValuePair> createMemberValues(String name, String value) {
        Map<String, String> nameValuePairs = new HashMap<>();
        nameValuePairs.put(name, value);
        return createMemberValues(nameValuePairs);
    }

    public static AnnotationExpr createAnnotationExpr(
        ClassOrInterfaceDeclaration classDeclaration,
        String annotationFullClassName,
        NodeList<MemberValuePair> memberValuePairs
    ) {
        CompilationUnit unit = AstUtils.getUnit(classDeclaration);
        unit.addImport(annotationFullClassName);

        String annotationName = StringUtils.splitLast(annotationFullClassName, "\\.");
        return new NormalAnnotationExpr(new Name(annotationName), memberValuePairs);
    }

    public static AnnotationExpr createAnnotationExpr(
        ClassOrInterfaceDeclaration clazz,
        String annotationFullClassName
    ) {
        return createAnnotationExpr(clazz, annotationFullClassName, new NodeList<>());
    }

    public static MethodDeclaration createMethodDeclaration(
        ClassOrInterfaceDeclaration classDeclaration,
        String methodName
    ) {
        return classDeclaration.addMethod(methodName, Modifier.Keyword.PUBLIC);
    }

    public static VariableDeclarator createVariableDeclarator(
        Type fieldType,
        String fieldName,
        String defaultValue
    ) {
        VariableDeclarator variableDeclarator = new VariableDeclarator();
        variableDeclarator.setName(fieldName);
        variableDeclarator.setType(fieldType);
        variableDeclarator.setInitializer(defaultValue);
        return variableDeclarator;
    }

    public static ExpressionStmt createExpressionStmt(VariableDeclarationExpr variableExpr) {
        return new ExpressionStmt(variableExpr);
    }

    public static VariableDeclarationExpr createVariableDeclarationExpr(
        Type type,
        String fieldName
    ) {
        VariableDeclarationExpr variableDeclarationExpr = new VariableDeclarationExpr();
        NodeList<VariableDeclarator> variableDeclarators = new NodeList<>();
        String defaultValue = AstUtils.getDefaultValue(type);
        Type unboxedType = toUnboxedType(type);
        VariableDeclarator variableDeclarator = createVariableDeclarator(unboxedType, fieldName, defaultValue);
        variableDeclarators.add(variableDeclarator);
        variableDeclarationExpr.setVariables(variableDeclarators);
        return variableDeclarationExpr;
    }

    public static void consumeField(
        CompilationUnit unit,
        BiConsumer<FieldDeclaration, ClassOrInterfaceType> consumer
    ) {
        unit.findAll(FieldDeclaration.class).stream()
            .filter(declaration -> !declaration.isFinal())
            .forEach(
                declaration -> AstUtils.findClassOrInterfaceType(declaration)
                    .ifPresent(fieldType -> consumer.accept(declaration, fieldType))
            );
    }

    public static boolean checkFieldExists(ClassOrInterfaceDeclaration testClass, String fieldName) {
        return testClass.getFieldByName(fieldName).isPresent();
    }

    public static boolean checkMethodExists(ClassOrInterfaceDeclaration testClass, String methodName) {
        return !testClass.getMethodsByName(methodName).isEmpty();
    }

    public static boolean checkVoidReturn(MethodDeclaration method) {
        return "void".equals(method.getType().toString());
    }

    public static boolean checkUtilClass(CompilationUnit srcUnit) {
        return srcUnit.findAll(MethodDeclaration.class).stream().anyMatch(NodeWithStaticModifier::isStatic);
    }

}
