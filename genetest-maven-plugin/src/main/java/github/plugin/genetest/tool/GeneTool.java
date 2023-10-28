package github.plugin.genetest.tool;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import github.plugin.genetest.util.ExprUtils;
import github.plugin.genetest.util.FieldUtils;
import github.plugin.genetest.util.NameUtils;
import github.plugin.genetest.util.StringUtils;
import org.apache.maven.plugin.logging.Log;
import github.plugin.genetest.util.AstUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

public class GeneTool {

    public final static String MOCKITO = "mockito";

    private final Log log;

    private final File srcFile;

    private final File testFile;

    private final String mock;

    private final boolean append;

    private final Map<String, Integer> methodNameTimes = new HashMap<>();

    private boolean privateMethodExists = false;

    private boolean privateFieldExists = false;

    private boolean voidMethodExists = false;

    public GeneTool(String mock, File srcFile, File testFile, boolean append, Log log) {
        this.mock = mock;
        this.srcFile = srcFile;
        this.testFile = testFile;
        this.append = append;
        this.log = log;
    }

    public void generate() {
        CompilationUnit testUnit = createTestUnit();
        outputTestFile(testFile, testUnit.toString());
    }

    private CompilationUnit createTestUnit() {
        // parse src unit
        CompilationUnit srcUnit = AstUtils.unit(srcFile);

        // create test unit
        CompilationUnit testUnit = calcTestUnit();

        // create test class
        ClassOrInterfaceDeclaration testClass = createTestClass(srcUnit, testUnit);

        // create be tested class field
        createBeTestClassField(testClass);

        // create test class inject field
        createInjectedField(srcUnit, testClass);

        // create setUp method
        createSetUpMethod(srcUnit, testClass);

        // create tearDown method
        createTearDownMethod(testClass);

        // create test_xxx_branch_xxx method
        createTestMethod(srcUnit, testClass);

        // create reflectField field
        if (privateFieldExists) {
            createReflectField(testUnit, testClass);
        }

        // create reflectMethod method
        if (privateMethodExists) {
            createReflectMethod(testUnit, testClass);
        }

        // create stackTrace method
        if (voidMethodExists) {
            createStackTrace(testUnit, testClass);
        }

        return testUnit;
    }

    private CompilationUnit calcTestUnit() {
        if (append) {
            return AstUtils.unit(testFile);
        } else {
            return new CompilationUnit();
        }
    }

    private ClassOrInterfaceDeclaration createTestClass(CompilationUnit srcUnit, CompilationUnit testUnit) {
        // create package
        PackageDeclaration pkg = AstUtils.packageDeclaration(srcUnit);
        testUnit.setPackageDeclaration(pkg);
        info("create package: " + pkg.getNameAsString());
        debug(pkg);

        // create class
        String testClassName = calcClassName(testFile);
        ClassOrInterfaceDeclaration clazz = testUnit.addClass(testClassName);

        if (MOCKITO.equals(mock)) {
            // add import
            String importName = "org.mockito.junit.MockitoJUnitRunner";
            addImport(testUnit, importName);

            // add annotation
            String name = "value";
            String value = "MockitoJUnitRunner.class";
            NodeList<MemberValuePair> memberValues = AstUtils.createMemberValues(name, value);
            String annotationName = "org.junit.runner.RunWith";
            AnnotationExpr annotation = AstUtils.createAnnotationExpr(clazz, annotationName, memberValues);
            addAnnotation(clazz, annotation);
        }

        info("create be test class: " + clazz.getNameAsString());
        debug(clazz);
        return clazz;
    }

    private void createBeTestClassField(ClassOrInterfaceDeclaration testClass) {
        // create field
        String className = calcClassName(srcFile);
        String fieldName = NameUtils.toCamelCase(className);
        ClassOrInterfaceType fieldType = new ClassOrInterfaceType(null, className);
        FieldDeclaration field = testClass.addPublicField(fieldType, fieldName);

        if (MOCKITO.equals(mock)) {
            // add annotation
            String annotationName = "org.mockito.InjectMocks";
            addAnnotation(testClass, field, annotationName);
        } else {
            // set default value
            VariableDeclarator variable = AstUtils.createVariableDeclarator(fieldType, fieldName);
            field.setVariable(0, variable);
        }

        info("create be test field: " + fieldName);
        debug(field);
    }

    private void createInjectedField(CompilationUnit srcUnit, ClassOrInterfaceDeclaration testClass) {
        // new
        srcUnit.findAll(FieldDeclaration.class)
            .stream().filter(fieldDeclaration -> !fieldDeclaration.isFinal())
            .forEach(srcField -> AstUtils.findClassOrInterfaceType(srcField)
                .filter(field -> (!AstUtils.isInjectType(field) || !MOCKITO.equals(mock)))
                .ifPresent(
                    fieldType -> {
                        // create field
                        String fieldName = AstUtils.name(AstUtils.getVariableDeclarator(srcField));
                        FieldDeclaration field = createInjectedField(srcUnit, testClass, fieldType, fieldName);

                        // set default value
                        VariableDeclarator variable = AstUtils.createVariableDeclarator(fieldType, fieldName);
                        field.setVariable(0, variable);
                        debug(field);
                    }
                )
            );

        // mockito mock
        srcUnit.findAll(FieldDeclaration.class)
            .stream().filter(fieldDeclaration -> !fieldDeclaration.isFinal())
            .forEach(srcField -> AstUtils.findClassOrInterfaceType(srcField)
                .filter(srcFieldType -> AstUtils.isInjectType(srcFieldType) && MOCKITO.equals(mock))
                .ifPresent(
                    fieldType -> {
                        // create field
                        String fieldName = AstUtils.name(AstUtils.getVariableDeclarator(srcField));
                        FieldDeclaration field = createInjectedField(srcUnit, testClass, fieldType, fieldName);

                        // add annotation @Mock
                        String annotationName = "org.mockito.Mock";
                        addAnnotation(testClass, field, annotationName);

                        debug(field);
                    }
                )
            );
    }

    private FieldDeclaration createInjectedField(
        CompilationUnit srcUnit,
        ClassOrInterfaceDeclaration testClass,
        ClassOrInterfaceType fieldType,
        String fieldName
    ) {
        // import package
        AstUtils.findImportDeclarationByClassType(srcUnit, fieldType)
            .ifPresent(declaration -> testClass.findCompilationUnit()
                .ifPresent(unit -> addImport(unit, declaration.getNameAsString())));
        // log
        info("create injected field: " + fieldName);
        // create variable
        return testClass.addPublicField(fieldType, fieldName);
    }

    private void createSetUpMethod(CompilationUnit srcUnit, ClassOrInterfaceDeclaration testClass) {
        // create setUp method
        String methodName = "setUp";
        MethodDeclaration method = createMethod(testClass, methodName);

        // create annotation @Before
        String annotationName = "org.junit.Before";
        addAnnotation(testClass, method, annotationName);

        // create method content
        BlockStmt blockStmt = new BlockStmt();

        // set public field
        srcUnit.findAll(FieldDeclaration.class).stream()
            .filter(fieldDeclaration -> fieldDeclaration.isPublic() && !fieldDeclaration.isFinal())
            .forEach(
                srcField -> {
                    // add setup content
                    String classFieldName = NameUtils.toCamelCase(AstUtils.className(srcUnit));
                    String fieldName = AstUtils.name(AstUtils.getVariableDeclarator(srcField));
                    String field = classFieldName + "." + fieldName + " = " + fieldName + ";";
                    Statement fieldStmt = StaticJavaParser.parseStatement(field);
                    blockStmt.addStatement(fieldStmt);
                }
            );

        // set private file
        srcUnit.findAll(FieldDeclaration.class).stream()
            .filter(fieldDeclaration -> !fieldDeclaration.isPublic())
            .forEach(
                srcField -> {
                    privateFieldExists = true;
                    // add setup content
                    String mockFieldName = AstUtils.name(AstUtils.getVariableDeclarator(srcField));
                    NameExpr fieldName = new NameExpr("\"" + mockFieldName + "\"");
                    NameExpr fieldValue = new NameExpr(mockFieldName);
                    MethodCallExpr methodCallExpr = new MethodCallExpr("reflectField", fieldName, fieldValue);
                    blockStmt.addStatement(methodCallExpr);
                }
            );

        method.setBody(blockStmt);

        debug(method);
    }

    private void createTearDownMethod(ClassOrInterfaceDeclaration testClass) {
        // create method tearDown
        String methodName = "tearDown";
        MethodDeclaration method = createMethod(testClass, methodName);

        // create annotation @After
        String annotationName = "org.junit.After";
        addAnnotation(testClass, method, annotationName);

        debug(method);
    }

    private void createTestMethod(CompilationUnit srcUnit, ClassOrInterfaceDeclaration testClass) {
        for (MethodDeclaration srcMethod : srcUnit.findAll(MethodDeclaration.class)) {
            srcMethod.getBody().ifPresent((blockStmt) -> {
                List<Node> childNodes = blockStmt.getChildNodes();
                if (childNodes.isEmpty()) {
                    createTestMethod(testClass, srcMethod, "");
                    return;
                }
                if (
                    childNodes.size() == 1
                        && (!(childNodes.get(0) instanceof IfStmt) && !(childNodes.get(0) instanceof TryStmt))
                ) {
                    createTestMethod(testClass, srcMethod, "");
                    return;
                }
                for (Node childNode : childNodes) {
                    createTestMethod(testClass, srcMethod, "_branch", childNode, 0);
                }
            });
        }
    }

    private int createTestMethod(
        ClassOrInterfaceDeclaration testClass,
        MethodDeclaration srcMethod,
        String methodNameSuffix,
        Node node,
        int branchCnt
    ) {
        // if else
        if (node instanceof IfStmt) {
            branchCnt++;
            // if
            Expression condition = ((IfStmt) node).getCondition();
            String expression = ExprUtils.expression(condition.toString());
            String methodNameSuffixIf = methodNameSuffix + "_if_" + expression;
            String methodNameSuffixIfFinal = ExprUtils.replaceDoubleUnderLine(methodNameSuffixIf);
            createTestMethod(testClass, srcMethod, methodNameSuffixIfFinal);
            String methodNameSuffixIfThen = methodNameSuffixIf + "_";
            Statement thenStmt = ((IfStmt) node).getThenStmt();
            for (Node thenNode : thenStmt.getChildNodes()) {
                if (thenNode instanceof IfStmt) {
                    branchCnt = createTestMethod(testClass, srcMethod, methodNameSuffixIfThen, thenNode, branchCnt);
                }
                if (thenNode instanceof TryStmt) {
                    branchCnt = createTestMethod(testClass, srcMethod, methodNameSuffixIfThen, thenNode, branchCnt);
                }
                if (thenNode instanceof BlockStmt) {
                    branchCnt = createTestMethod(testClass, srcMethod, methodNameSuffixIfThen, thenNode, branchCnt);
                }
            }

            // else
            Optional<Statement> elseStmtOptional = ((IfStmt) node).getElseStmt();
            boolean hasElse = elseStmtOptional.isPresent();
            if (hasElse) {
                branchCnt++;
                String methodNameSuffixElse = methodNameSuffix + "_else_" + expression;
                String methodNameSuffixElseFinal = ExprUtils.expression(methodNameSuffixElse);
                createTestMethod(testClass, srcMethod, methodNameSuffixElseFinal);
                String methodNameSuffixElseThen = methodNameSuffixElse + "_";
                Statement elseStmt = elseStmtOptional.get();
                if (elseStmt instanceof IfStmt) {
                    branchCnt = createTestMethod(testClass, srcMethod, methodNameSuffixElseThen, elseStmt, branchCnt);
                }
                if (elseStmt instanceof TryStmt) {
                    branchCnt = createTestMethod(testClass, srcMethod, methodNameSuffixElseThen, elseStmt, branchCnt);
                }
                if (elseStmt instanceof BlockStmt) {
                    branchCnt = createTestMethod(testClass, srcMethod, methodNameSuffixElseThen, elseStmt, branchCnt);
                }
            }
        }

        // try catch
        if (node instanceof TryStmt) {
            branchCnt++;
            // try
            String methodNameSuffixTry = methodNameSuffix + "_try_";
            String methodNameSuffixTryFinal = ExprUtils.expression(methodNameSuffixTry);
            createTestMethod(testClass, srcMethod, methodNameSuffixTryFinal);
            for (Node childNode : ((TryStmt) node).getTryBlock().getChildNodes()) {
                if (childNode instanceof IfStmt) {
                    branchCnt = createTestMethod(testClass, srcMethod, methodNameSuffixTry, childNode, branchCnt);
                }
                if (childNode instanceof TryStmt) {
                    branchCnt = createTestMethod(testClass, srcMethod, methodNameSuffixTry, childNode, branchCnt);
                }
                if (childNode instanceof BlockStmt) {
                    branchCnt = createTestMethod(testClass, srcMethod, methodNameSuffixTry, childNode, branchCnt);
                }
            }

            // catch
            for (CatchClause catchClause : ((TryStmt) node).getCatchClauses()) {
                for (Node childNode : catchClause.getChildNodes()) {
                    if (childNode instanceof IfStmt) {
                        branchCnt = createTestMethod(testClass, srcMethod, methodNameSuffixTry, childNode, branchCnt);
                    }
                    if (childNode instanceof TryStmt) {
                        branchCnt = createTestMethod(testClass, srcMethod, methodNameSuffixTry, childNode, branchCnt);
                    }
                    if (childNode instanceof BlockStmt) {
                        branchCnt++;
                        Parameter parameter = (Parameter) catchClause.getChildNodes().get(0);
                        String methodNameSuffixCatch = methodNameSuffix + "_catch_" + parameter.getTypeAsString();
                        String methodNameSuffixCatchFinal = ExprUtils.expression(methodNameSuffixCatch);
                        createTestMethod(testClass, srcMethod, methodNameSuffixCatchFinal);
                        branchCnt = createTestMethod(testClass, srcMethod, methodNameSuffixTry, childNode, branchCnt);
                    }
                }
            }
        }

        // block
        if (node instanceof BlockStmt) {
            for (Node childNode : node.getChildNodes()) {
                branchCnt = createTestMethod(testClass, srcMethod, methodNameSuffix, childNode, branchCnt);
            }
        }

        return branchCnt;
    }

    private void createTestMethod(
        ClassOrInterfaceDeclaration testClass,
        MethodDeclaration srcMethod,
        String methodNameSuffix
    ) {
        // create method
        String methodName = createTestMethodName(srcMethod.getNameAsString(), methodNameSuffix);
        MethodDeclaration method = createMethod(testClass, methodName);

        // create annotation @Test
        String annotationName = "org.junit.Test";
        addAnnotation(testClass, method, annotationName);

        debug(method);

        // create test method content
        BlockStmt testContent = new BlockStmt();

        // create given
        createGivenBlock(testClass, srcMethod, testContent);

        // create when block
        createWhenBlock(srcMethod, testContent);

        // create mock block
        // createMockBlock(srcMethod, testContent);
        boolean isVoidReturn = isVoidReturn(srcMethod);
        if (isVoidReturn) {
            createAssertTimesBlock(testClass, testContent);
        } else {
            // create assert block
            createAssertResultBlock(testClass, testContent);
        }

        debug(testContent);

        method.setBody(testContent);
    }

    private String createTestMethodName(String srcMethodName, String methodNameSuffix) {
        String base = "test_" + NameUtils.toUnderscoreCase(srcMethodName) + methodNameSuffix;
        if (!methodNameTimes.containsKey(base)) {
            methodNameTimes.put(base, 1);
            return base;
        } else {
            Integer times = methodNameTimes.get(base) + 1;
            methodNameTimes.put(base, times);
            return base + times;
        }
    }

    private boolean isVoidReturn(MethodDeclaration srcMethod) {
        return "void".equals(srcMethod.getType().toString());
    }

    private void createAssertTimesBlock(ClassOrInterfaceDeclaration testClass, BlockStmt testContent) {
        voidMethodExists = true;

        CompilationUnit unit = AstUtils.unit(testClass);
        String importName = "org.junit.Assert";
        addImport(unit, importName);

        TryStmt tryStmt = new TryStmt();
        BlockStmt tryBlock = new BlockStmt();
        tryStmt.setTryBlock(tryBlock);
        NodeList<CatchClause> catchClauses = new NodeList<>();
        tryStmt.setCatchClauses(catchClauses);

        Statement throwStmt = StaticJavaParser.parseStatement("throw new RuntimeException();");
        throwStmt.setLineComment(" TODO then assert inner method run times");
        tryBlock.addStatement(throwStmt);

        BlockStmt catchStmt = new BlockStmt();
        Parameter parameter = new Parameter(new TypeParameter("Exception"), "e");
        CatchClause catchClause = new CatchClause(parameter, catchStmt);

        // stack content
        String stackStr = "String stack = stackTrace(e);";
        Statement stackStmt = StaticJavaParser.parseStatement(stackStr);
        catchStmt.addStatement(stackStmt);
        // assert content
        String assertStr = " Assert.fail(\"Should not run here.\\n\\t\" + stack);";
        Statement assertStmt = StaticJavaParser.parseStatement(assertStr);
        catchStmt.addStatement(assertStmt);

        catchClause.setBody(catchStmt);
        catchClauses.add(catchClause);

        testContent.addStatement(tryStmt);
    }

    private void createAssertResultBlock(ClassOrInterfaceDeclaration testClass, BlockStmt testContent) {
        // import package
        CompilationUnit unit = AstUtils.unit(testClass);
        String importName = "org.junit.Assert";
        addImport(unit, importName);

        // create expect result
        String expectContentStr = "Object expect = null;";
        Statement expectStmt = StaticJavaParser.parseStatement(expectContentStr);
        // create modify comment
        expectStmt.setLineComment(" TODO then");
        testContent.addStatement(expectStmt);

        // create assert content
        String assertContentStr = "Assert.assertEquals(expect, actual);";
        Statement assertStmt = StaticJavaParser.parseStatement(assertContentStr);
        testContent.addStatement(assertStmt);
    }

    private void createGivenBlock(
        ClassOrInterfaceDeclaration testClass,
        MethodDeclaration srcMethod,
        BlockStmt testContent
    ) {
        CompilationUnit testUnit = AstUtils.unit(testClass);
        NodeList<Parameter> parameters = srcMethod.getParameters();
        for (int i = 0, parametersSize = parameters.size(); i < parametersSize; i++) {
            Parameter parameter = parameters.get(i);
            Type parameterType = parameter.getType();
            SimpleName parameterName = parameter.getName();

            ExpressionStmt expressionStmt;
            if (parameterType.isArrayType()) {
                expressionStmt = createArrayExpressionStmt((ArrayType) parameterType, parameterName);
            } else {
                ClassOrInterfaceType classOrInterfaceType;
                if (parameterType.isPrimitiveType()) {
                    classOrInterfaceType = ((PrimitiveType) parameterType).toBoxedType();
                } else {
                    classOrInterfaceType = (ClassOrInterfaceType) parameterType;
                }

                String parameterTypeName = classOrInterfaceType.getName().asString();
                if ("List".equals(parameterTypeName)) {
                    String importName1 = "java.util.List";
                    addImport(testUnit, importName1);

                    String importName2 = "java.util.ArrayList";
                    addImport(testUnit, importName2);
                }
                if ("Map".equals(parameterTypeName)) {
                    String importName1 = "java.util.Map";
                    addImport(testUnit, importName1);

                    String importName2 = "java.util.HashMap";
                    addImport(testUnit, importName2);
                }
                expressionStmt = createClassExpressionStmt(classOrInterfaceType, parameterName);
            }
            if (i == 0) {
                expressionStmt.setLineComment(" TODO given");
            }
            testContent.addStatement(expressionStmt);
        }
    }

    private void createWhenBlock(MethodDeclaration srcMethod, BlockStmt testContent) {
        CompilationUnit srcUnit = AstUtils.unit(srcMethod);
        String classType = AstUtils.className(srcUnit);
        String classFieldName = NameUtils.toCamelCase(classType);
        String methodName = srcMethod.getNameAsString();
        Statement whenStmt;
        if (srcMethod.isPublic()) {

            String whenField = createCallWhenMethod(srcMethod, classFieldName, methodName);
            whenStmt = StaticJavaParser.parseStatement(whenField);
        } else {
            privateMethodExists = true;
            NodeList<Parameter> parameters = srcMethod.getParameters();
            String whenFieldTypes = createCallReflectWhenMethodTypes(parameters);
            Statement whenFieldTypesStmt = StaticJavaParser.parseStatement(whenFieldTypes);
            testContent.addStatement(whenFieldTypesStmt);

            String whenFieldParams = createCallReflectWhenMethodParams(parameters);
            Statement whenFieldParamsStmt = StaticJavaParser.parseStatement(whenFieldParams);
            testContent.addStatement(whenFieldParamsStmt);

            String whenField = createCallReflectWhenMethod(methodName);
            whenStmt = StaticJavaParser.parseStatement(whenField);
        }
        whenStmt.setLineComment(" when");
        testContent.addStatement(whenStmt);
    }

    private String createCallReflectWhenMethodTypes(NodeList<Parameter> parameters) {
        String types = typeJoinStr(parameters);
        String callWhenMethodTypes = "Class<?>[] types = { " + types + "};";
        log.debug(callWhenMethodTypes);
        return callWhenMethodTypes;
    }

    private String createCallReflectWhenMethodParams(NodeList<Parameter> parameters) {
        String params = paramJoinStr(parameters);
        String callWhenMethodParams = "Object[] params = { " + params + "};";
        log.debug(callWhenMethodParams);
        return callWhenMethodParams;
    }

    private String createCallReflectWhenMethod(String methodName) {
        String nameMethod = "\"" + methodName + "\"";
        String whenStr = "Object actual = reflectMethod(" + nameMethod + ", " + "types" + ",  " + "params" + ");";
        log.debug(whenStr);
        return whenStr;
    }

    private String createCallWhenMethod(MethodDeclaration srcMethod, String fieldName, String methodName) {
        StringBuilder builder = new StringBuilder();
        if (!isVoidReturn(srcMethod)) {
            builder.append("Object");
            builder.append(" actual = ");
        }

        if (srcMethod.isStatic()) {
            builder.append(calcClassName(srcFile));
        } else {
            builder.append(fieldName);
        }

        builder.append(".");
        builder.append(methodName);
        builder.append("(");
        String params = paramJoinStr(srcMethod.getParameters());
        builder.append(params);
        builder.append(");");
        String callWhenMethod = builder.toString();
        log.debug(callWhenMethod);
        return callWhenMethod;
    }

    private String paramJoinStr(NodeList<Parameter> parameters) {
        StringJoiner joiner = new StringJoiner(", ");
        for (Parameter param : parameters) {
            joiner.add(param.getName().toString());
        }
        return joiner.toString();
    }

    private String typeJoinStr(NodeList<Parameter> parameters) {
        StringJoiner joiner = new StringJoiner(", ");
        for (Parameter param : parameters) {
            joiner.add(AstUtils.type(param.getType()) + ".class");
        }
        return joiner.toString();
    }

    private ExpressionStmt createClassExpressionStmt(ClassOrInterfaceType srcType, SimpleName parameterName) {
        ExpressionStmt expressionStmt = new ExpressionStmt();
        VariableDeclarationExpr variableDeclarationExpr = new VariableDeclarationExpr();
        VariableDeclarator variableDeclarator = new VariableDeclarator();

        String typeName = srcType.getNameAsString();
        boolean genericExists = srcType.getTypeArguments().isPresent();

        if (srcType.isBoxedType()) {
            variableDeclarator.setType(srcType.toUnboxedType());
        } else {
            variableDeclarator.setType(srcType);
        }
        variableDeclarator.setName(parameterName);
        String VariableDefaultValue = FieldUtils.defaultValue(typeName, genericExists);
        variableDeclarator.setInitializer(VariableDefaultValue);

        NodeList<VariableDeclarator> variableDeclarators = new NodeList<>();
        variableDeclarators.add(variableDeclarator);
        variableDeclarationExpr.setVariables(variableDeclarators);
        expressionStmt.setExpression(variableDeclarationExpr);
        return expressionStmt;
    }

    private ExpressionStmt createArrayExpressionStmt(ArrayType parameterType, SimpleName parameterName) {
        Type componentType = parameterType.getComponentType();

        ExpressionStmt expressionStmt = new ExpressionStmt();
        VariableDeclarationExpr variableDeclarationExpr = new VariableDeclarationExpr();
        VariableDeclarator variableDeclarator = new VariableDeclarator();
        variableDeclarator.setName(parameterName);
        variableDeclarator.setType(parameterType);
        variableDeclarator.setInitializer("new " + componentType.asString() + "[]{}");

        NodeList<VariableDeclarator> variableDeclarators = new NodeList<>();
        variableDeclarators.add(variableDeclarator);
        variableDeclarationExpr.setVariables(variableDeclarators);
        expressionStmt.setExpression(variableDeclarationExpr);
        return expressionStmt;
    }

    private void outputTestFile(File testFile, String testUnit) {
        File testDirectory = testFile.getParentFile();
        if (!testDirectory.exists()) {
            if (testDirectory.mkdirs()) {
                System.out.println("create directory: " + testDirectory);
            }
        }

        try (FileOutputStream fos = new FileOutputStream(testFile)) {
            fos.write(testUnit.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String calcClassName(File file) {
        return StringUtils.splitFirst(file.getName(), "\\.");
    }

    private void addImport(CompilationUnit unit, String importName) {
        AstUtils.addImport(unit, importName);
        info("import package: " + importName);
        debug("import " + importName + ";");
    }

    private void addAnnotation(
        ClassOrInterfaceDeclaration clazz,
        NodeWithAnnotations<? extends Node> node,
        String annotationName
    ) {
        AnnotationExpr annotation = AstUtils.createAnnotationExpr(clazz, annotationName);
        addAnnotation(node, annotation);
    }

    private void addAnnotation(NodeWithAnnotations<? extends Node> node, AnnotationExpr annotation) {
        AstUtils.addAnnotation(node, annotation);
        info("create method annotation: " + annotation.getName().toString());
    }

    private MethodDeclaration createMethod(ClassOrInterfaceDeclaration clazz, String methodName) {
        MethodDeclaration method = AstUtils.createMethodDeclaration(clazz, methodName);
        info("create method: " + methodName);
        return method;
    }

    private void createReflectField(CompilationUnit unit, ClassOrInterfaceDeclaration clazz) {
        // import package
        String importName = "java.lang.reflect.Field";
        addImport(unit, importName);

        // create reflect field body
        String srcClassName = calcClassName(srcFile);
        String reflectField = createReflectField(srcClassName);
        MethodDeclaration method = StaticJavaParser.parseMethodDeclaration(reflectField);
        debug(method);

        // add method
        clazz.addMember(method);
    }

    private String createReflectField(String className) {
        String fieldName = NameUtils.toCamelCase(className);
        return "private void reflectField(String fieldName, Object fieldValue) {"
            + "    try {"
            + "        Field field = " + className + ".class.getDeclaredField(fieldName);"
            + "        field.setAccessible(true);"
            + "        field.set(" + fieldName + ", fieldValue);"
            + "    } catch (Exception e) {"
            + "        throw new RuntimeException(e);"
            + "    }"
            + "}";
    }

    private void createReflectMethod(CompilationUnit unit, ClassOrInterfaceDeclaration clazz) {
        // import package
        String importName = "java.lang.reflect.Method";
        addImport(unit, importName);

        // create reflect method body
        String srcClassName = calcClassName(srcFile);
        String reflectMethod = createReflectMethod(srcClassName);
        MethodDeclaration method = StaticJavaParser.parseMethodDeclaration(reflectMethod);
        debug(method);

        // add method
        clazz.addMember(method);
    }

    private String createReflectMethod(String className) {
        String fieldName = NameUtils.toCamelCase(className);
        return "private Object reflectMethod(String methodName, Class<?>[] types, Object[] params) {"
            + "    try {"
            + "        Method method = " + className + ".class.getDeclaredMethod(methodName, types);"
            + "        method.setAccessible(true);"
            + "        return method.invoke(" + fieldName + ", params);"
            + "    } catch (Exception e) {"
            + "        throw new RuntimeException(e);"
            + "    }"
            + "}";
    }

    private void createStackTrace(CompilationUnit unit, ClassOrInterfaceDeclaration clazz) {
        // import package
        String importName1 = "java.util.Arrays";
        addImport(unit, importName1);

        String importName2 = "java.util.stream.Collectors";
        addImport(unit, importName2);

        String reflectMethod = createStackTrace();
        MethodDeclaration method = StaticJavaParser.parseMethodDeclaration(reflectMethod);
        debug(method);

        // add method
        clazz.addMember(method);
    }

    private String createStackTrace() {
        return "private String stackTrace(Exception e) {"
            + "    return Arrays.stream(e.getStackTrace())"
            + "        .map(StackTraceElement::toString)"
            + "        .collect(Collectors.joining(\"\\n\\t\"));"
            + "}";
    }

    private void info(String info) {
        log.info(info);
    }

    private void debug(Object object) {
        log.debug(object.toString().replaceAll("\r\n", " ").replaceAll("\n", " "));
    }

}
