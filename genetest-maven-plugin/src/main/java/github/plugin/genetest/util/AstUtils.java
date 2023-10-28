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
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
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

    public static CompilationUnit unit(File file) {
        try {
            return StaticJavaParser.parse(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static CompilationUnit unit(ClassOrInterfaceDeclaration classDeclaration) {
        return classDeclaration.findCompilationUnit().orElseThrow(() -> new RuntimeException("test class unit not exist."));
    }

    public static CompilationUnit unit(MethodDeclaration methodDeclaration) {
        return methodDeclaration.findCompilationUnit().orElseThrow(() -> new RuntimeException("src method unit not exist."));
    }

    public static void addImport(CompilationUnit unit, String importName) {
        unit.addImport(importName);
    }

    public static void addAnnotation(NodeWithAnnotations<? extends Node> node, AnnotationExpr annotation) {
        node.addAnnotation(annotation);
    }

    public static PackageDeclaration packageDeclaration(CompilationUnit unit) {
        return unit.getPackageDeclaration().orElseThrow(() -> new RuntimeException("package not exists."));
    }

    public static String className(CompilationUnit unit) {
        return unit.getPrimaryTypeName().orElseThrow(() -> new RuntimeException("class name not exists."));
    }

    public static NodeList<MemberValuePair> createMemberValues(List<MemberValuePair> memberValuePairs) {
        NodeList<MemberValuePair> result = new NodeList<>();
        result.addAll(memberValuePairs);
        return result;
    }

    public static NodeList<MemberValuePair> createMemberValues(Map<String, String> nameValuePairs) {
        List<MemberValuePair> memberValuePairs = nameValuePairs.entrySet().stream().map((e) -> new MemberValuePair(e.getKey(), new NameExpr(e.getValue()))).collect(Collectors.toList());
        return createMemberValues(memberValuePairs);
    }

    public static NodeList<MemberValuePair> createMemberValues(String name, String value) {
        Map<String, String> nameValuePairs = new HashMap<>();
        nameValuePairs.put(name, value);
        return createMemberValues(nameValuePairs);
    }

    public static AnnotationExpr createAnnotationExpr(ClassOrInterfaceDeclaration classDeclaration, String className, NodeList<MemberValuePair> memberValuePairs) {
        AstUtils.unit(classDeclaration).addImport(className);
        return new NormalAnnotationExpr(new Name(StringUtils.splitLast(className, "\\.")), memberValuePairs);
    }

    public static AnnotationExpr createAnnotationExpr(ClassOrInterfaceDeclaration classDeclaration, String className) {
        AstUtils.unit(classDeclaration).addImport(className);
        return new NormalAnnotationExpr(new Name(StringUtils.splitLast(className, "\\.")), new NodeList<>());
    }

    public static MethodDeclaration createMethodDeclaration(ClassOrInterfaceDeclaration classDeclaration, String methodName) {
        return classDeclaration.addMethod(methodName, Modifier.Keyword.PUBLIC);
    }

    public static String name(NodeWithName<? extends Node> type) {
        return type.getName().asString();
    }

    public static String name(NodeWithSimpleName<? extends Node> type) {
        return type.getName().asString();
    }

    public static Optional<ImportDeclaration> findImportDeclarationByClassType(CompilationUnit srcUnit, ClassOrInterfaceType classType) {
        return srcUnit.getImports().stream().filter(importDeclaration -> name(classType).equals(StringUtils.splitLast(name(importDeclaration), "\\."))).findFirst();
    }

    public static VariableDeclarator getVariableDeclarator(FieldDeclaration fieldDeclaration) {
        return fieldDeclaration.findAll(VariableDeclarator.class).get(0);
    }

    public static VariableDeclarator createVariableDeclarator(ClassOrInterfaceType fieldType, String fieldName) {
        VariableDeclarator variableDeclarator = new VariableDeclarator();
        variableDeclarator.setType(fieldType);
        variableDeclarator.setName(fieldName);
        variableDeclarator.setInitializer(FieldUtils.defaultValue(fieldType.asString(), false));
        return variableDeclarator;
    }

    public static Optional<ClassOrInterfaceType> findClassOrInterfaceType(FieldDeclaration srcFieldDeclaration) {
        if (srcFieldDeclaration.getElementType().isClassOrInterfaceType()) {
            return Optional.of(srcFieldDeclaration.getElementType().asClassOrInterfaceType());
        }

        if (srcFieldDeclaration.getElementType().isPrimitiveType()) {
            return Optional.of(srcFieldDeclaration.getElementType().asPrimitiveType().toBoxedType());
        }

        return Optional.empty();
    }

    public static String type(Type type) {
        return StringUtils.splitFirst(type.toString(), "<");
    }

    public static void handleClassTypeField(
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

    public static boolean fieldExists(ClassOrInterfaceDeclaration testClass, String fieldName) {
        return testClass.getFieldByName(fieldName).isPresent();
    }

    public static boolean methodExists(ClassOrInterfaceDeclaration testClass, String methodName) {
        return !testClass.getMethodsByName(methodName).isEmpty();
    }

}
