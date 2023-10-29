package github.plugin.genetest.tool;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
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
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import github.plugin.genetest.util.AstUtils;
import github.plugin.genetest.util.ExprUtils;
import github.plugin.genetest.util.FileUtils;
import github.plugin.genetest.util.NameUtils;
import github.plugin.genetest.util.StringUtils;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
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
        FileUtils.output(testFile, testUnit.toString());
    }

    private CompilationUnit createTestUnit() {
        // parse src unit
        CompilationUnit srcUnit = AstUtils.getUnit(srcFile);

        // create test unit if not exists
        CompilationUnit testUnit = createIfNotExistsTestUnit();

        // create test class if not exists
        ClassOrInterfaceDeclaration testClass = createIfNotExistsTestClass(srcUnit, testUnit);

        // create test class field if not exists
        createIfNotExistsTestClassField(srcUnit, testClass);

        // create setUp method if not exists
        createIfNotExistsSetUpMethod(srcUnit, testClass);

        // create tearDown method if not exists
        createIfNotExistsTearDownMethod(testClass);

        // create test_xxx_branch_xxx method if not exists
        createIfNotExistsTestMethod(srcUnit, testClass);

        // create reflectField field
        if (privateFieldExists) {
            String className = AstUtils.getClassName(srcUnit);
            createIfNotExistsReflectField(testClass, className);
        }

        // create reflectMethod method
        if (privateMethodExists) {
            String className = AstUtils.getClassName(srcUnit);
            createIfNotExistsReflectMethod(testClass, className);
        }

        // create stackTrace method
        if (voidMethodExists) {
            createIfNotExistsGetStackTrace(testClass);
        }

        return testUnit;
    }

    private CompilationUnit createIfNotExistsTestUnit() {
        if (append) {
            return AstUtils.getUnit(testFile);
        } else {
            return new CompilationUnit();
        }
    }

    private ClassOrInterfaceDeclaration createIfNotExistsTestClass(
        CompilationUnit srcUnit,
        CompilationUnit testUnit
    ) {
        String testClassName = StringUtils.splitFirst(testFile.getName(), "\\.");
        Optional<ClassOrInterfaceDeclaration> optional = testUnit.getClassByName(testClassName);
        return optional.orElseGet(() -> createTestClass(srcUnit, testUnit));
    }

    private ClassOrInterfaceDeclaration createTestClass(
        CompilationUnit srcUnit,
        CompilationUnit testUnit
    ) {
        // create package
        String packageName = AstUtils.getPackageName(srcUnit);
        createPackage(testUnit, packageName);

        // create class
        String className = StringUtils.splitFirst(testFile.getName(), "\\.");
        ClassOrInterfaceDeclaration clazz = testUnit.addClass(className);

        // create class annotation
        if (MOCKITO.equals(mock)) {
            // add import
            String importName = "org.mockito.junit.MockitoJUnitRunner";
            createImport(testUnit, importName);

            // add annotation
            String name = "value";
            String value = "MockitoJUnitRunner.class";
            NodeList<MemberValuePair> memberValues = AstUtils.createMemberValues(name, value);
            String annotationName = "org.junit.runner.RunWith";
            AnnotationExpr annotation = AstUtils.createAnnotationExpr(clazz, annotationName, memberValues);
            AstUtils.addAnnotation(clazz, annotation);
            info("create method annotation: " + annotationName);
        }

        // print log
        info("create be test class: " + clazz.getNameAsString());
        debug(clazz);

        return clazz;
    }

    private void createIfNotExistsTestClassField(
        CompilationUnit srcUnit,
        ClassOrInterfaceDeclaration testClass
    ) {
        // create test class be tested field if not exists
        String className = AstUtils.getClassName(srcUnit);
        String beTestFieldName = NameUtils.toCamelCase(className);
        if (!AstUtils.checkFieldExists(testClass, beTestFieldName)) {
            if (!AstUtils.checkUtilClass(srcUnit)) {
                ClassOrInterfaceType beTestFieldType = new ClassOrInterfaceType(null, className);
                if (!MOCKITO.equals(mock)) {
                    createField(srcUnit, testClass, beTestFieldType, beTestFieldName);
                } else {
                    String annotationName = "org.mockito.InjectMocks";
                    createMockedField(srcUnit, testClass, beTestFieldType, beTestFieldName, annotationName);
                }
            }
        }

        // create test class inject field if not exists
        AstUtils.consumeField(srcUnit, (injectField, injectFieldType) -> {
            String injectFieldName = AstUtils.getName(AstUtils.getVariableDeclarator(injectField));
            if (!AstUtils.checkFieldExists(testClass, injectFieldName)) {
                if (!MOCKITO.equals(mock)) {
                    createField(srcUnit, testClass, injectFieldType, injectFieldName);
                } else {
                    String annotationName = "org.mockito.Mock";
                    createMockedField(srcUnit, testClass, injectFieldType, injectFieldName, annotationName);
                }
            }
        });
    }

    private void createIfNotExistsSetUpMethod(
        CompilationUnit srcUnit,
        ClassOrInterfaceDeclaration testClass
    ) {
        // check method setUp
        String methodName = "setUp";
        if (!AstUtils.checkMethodExists(testClass, methodName)) {
            // create setUp method
            String annotationName = "org.junit.Before";
            MethodDeclaration method = createMethod(testClass, methodName, annotationName);

            // create setUp method content
            BlockStmt blockStmt = createSetUpMethodContent(srcUnit);
            method.setBody(blockStmt);

            // debug method
            debug(method);
        }
    }

    private BlockStmt createSetUpMethodContent(CompilationUnit srcUnit) {
        BlockStmt blockStmt = new BlockStmt();
        AstUtils.consumeField(srcUnit, ((declaration, type) -> {
            if (declaration.isPublic()) {
                // add setup content
                String classFieldName = NameUtils.toCamelCase(AstUtils.getClassName(srcUnit));
                String fieldName = AstUtils.getName(AstUtils.getVariableDeclarator(declaration));
                String field = classFieldName + "." + fieldName + " = " + fieldName + ";";
                Statement fieldStmt = StaticJavaParser.parseStatement(field);
                blockStmt.addStatement(fieldStmt);
            } else {
                privateFieldExists = true;
                // add setup content
                String mockFieldName = AstUtils.getName(AstUtils.getVariableDeclarator(declaration));
                NameExpr fieldName = new NameExpr("\"" + mockFieldName + "\"");
                NameExpr fieldValue = new NameExpr(mockFieldName);
                MethodCallExpr methodCallExpr = new MethodCallExpr("reflectField", fieldName, fieldValue);
                blockStmt.addStatement(methodCallExpr);
            }
        }));
        return blockStmt;
    }

    private void createIfNotExistsTearDownMethod(ClassOrInterfaceDeclaration testClass) {
        // check method tearDown
        String methodName = "tearDown";
        if (!AstUtils.checkMethodExists(testClass, methodName)) {
            // create method tearDown
            String annotationName = "org.junit.After";
            createMethod(testClass, methodName, annotationName);
        }
    }

    private void createIfNotExistsTestMethod(
        CompilationUnit srcUnit,
        ClassOrInterfaceDeclaration testClass
    ) {
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
        // calc test method name
        String methodName = calcTestMethodName(srcMethod.getNameAsString(), methodNameSuffix);
        if (AstUtils.checkMethodExists(testClass, methodName)) {
            return;
        }

        // create test method
        String annotationName = "org.junit.Test";
        MethodDeclaration method = createMethod(testClass, methodName, annotationName);

        // create test method content
        BlockStmt testContent = new BlockStmt();

        // create given block
        createGivenBlock(testClass, srcMethod, testContent);

        // create when block
        createWhenBlock(srcMethod, testContent);

        // create assert block
        if (AstUtils.checkVoidReturn(srcMethod)) {
            // assert times
            createAssertTimesBlock(testClass, testContent);
        } else {
            // assert result
            createAssertResultBlock(testClass, testContent);
        }

        // add content
        method.setBody(testContent);

        // print log
        debug(testContent);
    }

    private String calcTestMethodName(String srcMethodName, String methodNameSuffix) {
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

    private void createAssertTimesBlock(ClassOrInterfaceDeclaration testClass, BlockStmt testContent) {
        voidMethodExists = true;

        CompilationUnit unit = AstUtils.getUnit(testClass);
        String importName = "org.junit.Assert";
        createImport(unit, importName);

        TryStmt tryStmt = new TryStmt();
        BlockStmt tryBlock = new BlockStmt();
        tryStmt.setTryBlock(tryBlock);
        NodeList<CatchClause> catchClauses = new NodeList<>();
        tryStmt.setCatchClauses(catchClauses);

        Statement throwStmt = StaticJavaParser.parseStatement("throw new RuntimeException();");
        throwStmt.setLineComment(" TODO then assert inner method run times");
        tryBlock.addStatement(throwStmt);

        BlockStmt catchStmt = new BlockStmt();
        Parameter parameter = new Parameter(new TypeParameter("Exception"), "exception");
        CatchClause catchClause = new CatchClause(parameter, catchStmt);

        // stack content
        String stackStr = "String stackTrace = getStackTrace(exception);";
        Statement stackStmt = StaticJavaParser.parseStatement(stackStr);
        catchStmt.addStatement(stackStmt);
        // assert content
        String assertStr = " Assert.fail(\"Should not run here.\\n\\t\" + stackTrace);";
        Statement assertStmt = StaticJavaParser.parseStatement(assertStr);
        catchStmt.addStatement(assertStmt);

        catchClause.setBody(catchStmt);
        catchClauses.add(catchClause);

        testContent.addStatement(tryStmt);
    }

    private void createAssertResultBlock(ClassOrInterfaceDeclaration testClass, BlockStmt testContent) {
        // import package
        CompilationUnit unit = AstUtils.getUnit(testClass);
        String importName = "org.junit.Assert";
        createImport(unit, importName);

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
        CompilationUnit testUnit = AstUtils.getUnit(testClass);
        CompilationUnit srcUnit = AstUtils.getUnit(srcMethod);
        NodeList<Parameter> parameters = srcMethod.getParameters();
        for (int i = 0, parametersSize = parameters.size(); i < parametersSize; i++) {
            Parameter parameter = parameters.get(i);
            Type fieldType = parameter.getType();

            // create import
            List<String> importNames = AstUtils.getImportNames(srcUnit, fieldType);
            for (String importName : importNames) {
                createImport(testUnit, importName);
            }

            // create given statement
            String fieldName = AstUtils.getName(parameter);
            VariableDeclarationExpr variableExpr = AstUtils.createVariableDeclarationExpr(fieldType, fieldName);
            ExpressionStmt expressionStmt = AstUtils.createExpressionStmt(variableExpr);

            if (i == 0) {
                // first line add comment
                String comment = " TODO given";
                variableExpr.setLineComment(comment);
            }

            testContent.addStatement(expressionStmt);
        }
    }

    private void createWhenBlock(MethodDeclaration srcMethod, BlockStmt testContent) {
        Statement whenStmt;
        if (srcMethod.isPublic()) {
            String whenField = createCallWhenMethod(srcMethod);
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

            String whenField = createCallReflectWhenMethod(srcMethod);
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

    private String createCallReflectWhenMethod(MethodDeclaration method) {
        String methodName = method.getNameAsString();
        String nameMethod = "\"" + methodName + "\"";
        String whenStr;
        if (AstUtils.checkVoidReturn(method)) {
            whenStr = "reflectMethod(" + nameMethod + ", " + "types" + ",  " + "params" + ");";
        } else {
            whenStr = "Object actual = reflectMethod(" + nameMethod + ", " + "types" + ",  " + "params" + ");";
        }
        log.debug(whenStr);
        return whenStr;
    }

    private String createCallWhenMethod(MethodDeclaration srcMethod) {

        StringBuilder builder = new StringBuilder();
        if (!AstUtils.checkVoidReturn(srcMethod)) {
            builder.append("Object");
            builder.append(" actual = ");
        }

        CompilationUnit srcUnit = AstUtils.getUnit(srcMethod);
        String classType = AstUtils.getClassName(srcUnit);
        String classFieldName = NameUtils.toCamelCase(classType);
        if (srcMethod.isStatic()) {
            builder.append(AstUtils.getClassName(srcUnit));
        } else {
            builder.append(classFieldName);
        }

        builder.append(".");
        String methodName = srcMethod.getNameAsString();
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
            joiner.add(AstUtils.getTypeName(param.getType()) + ".class");
        }
        return joiner.toString();
    }

    private void createIfNotExistsReflectField(
        ClassOrInterfaceDeclaration testClass,
        String srcClassName
    ) {
        // check method exists
        String methodName = "reflectField";
        if (!AstUtils.checkMethodExists(testClass, methodName)) {
            String importName = "java.lang.reflect.Field";
            String methodContent = getReflectField(srcClassName);
            createMethod(testClass, methodName, methodContent, importName);
        }
    }

    private void createIfNotExistsReflectMethod(
        ClassOrInterfaceDeclaration clazz,
        String srcClassName
    ) {
        // check method exists
        String methodName = "reflectMethod";
        if (!AstUtils.checkMethodExists(clazz, methodName)) {
            String importName = "java.lang.reflect.Method";
            String methodContent = getReflectMethod(srcClassName);
            createMethod(clazz, methodName, methodContent, importName);
        }
    }

    private void createIfNotExistsGetStackTrace(ClassOrInterfaceDeclaration clazz) {
        // check method exists
        String methodName = "getStackTrace";
        if (!AstUtils.checkMethodExists(clazz, methodName)) {
            String importName1 = "java.util.Arrays";
            String importName2 = "java.util.stream.Collectors";
            String methodContent = getGetStackTrace();
            createMethod(clazz, methodName, methodContent, importName1, importName2);
        }
    }

    private static String getReflectField(String className) {
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

    private static String getReflectMethod(String className) {
        // create reflect method body
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

    private static String getGetStackTrace() {
        return "private String getStackTrace(Exception e) {"
            + "    return Arrays.stream(e.getStackTrace())"
            + "        .map(StackTraceElement::toString)"
            + "        .collect(Collectors.joining(\"\\n\\t\"));"
            + "}";
    }

    private void createPackage(CompilationUnit unit, String packageName) {
        // create package
        unit.setPackageDeclaration(packageName);
        // print log
        info("create package: " + packageName);
        debug("import package " + packageName + ";");
    }

    private void createImport(CompilationUnit unit, String... importNames) {
        for (String importName : importNames) {
            AstUtils.addImport(unit, importName);
            info("import package: " + importName);
            debug("import " + importName + ";");
        }
    }

    private void createField(
        CompilationUnit srcUnit,
        ClassOrInterfaceDeclaration testClass,
        ClassOrInterfaceType fieldType,
        String fieldName
    ) {
        // import package
        AstUtils.findImportDeclaration(srcUnit, AstUtils.getName(fieldType))
            .ifPresent(declaration -> testClass.findCompilationUnit()
                .ifPresent(unit -> createImport(unit, declaration.getNameAsString())));

        // create field
        FieldDeclaration field = testClass.addPublicField(fieldType, fieldName);

        // set default value
        String defaultValue = AstUtils.getDefaultValue(fieldType);
        VariableDeclarator variable = AstUtils.createVariableDeclarator(fieldType, fieldName, defaultValue);
        field.setVariable(0, variable);

        // print log
        info("create field: " + fieldName);
        debug(field);
    }

    private void createMockedField(
        CompilationUnit srcUnit,
        ClassOrInterfaceDeclaration testClass,
        ClassOrInterfaceType fieldType,
        String fieldName,
        String annotationName
    ) {
        // import package
        AstUtils.findImportDeclaration(srcUnit, AstUtils.getName(fieldType))
            .ifPresent(declaration -> testClass.findCompilationUnit()
                .ifPresent(unit -> createImport(unit, declaration.getNameAsString())));

        // create field
        FieldDeclaration field = testClass.addPublicField(fieldType, fieldName);

        // add mock annotation
        AnnotationExpr annotation = AstUtils.createAnnotationExpr(testClass, annotationName);
        AstUtils.addAnnotation(field, annotation);

        // print log
        info("create field: " + fieldName);
        debug(field);
    }

    private MethodDeclaration createMethod(
        ClassOrInterfaceDeclaration clazz,
        String methodName,
        String annotationName
    ) {
        // create method
        MethodDeclaration method = AstUtils.createMethodDeclaration(clazz, methodName);

        // create annotation
        AnnotationExpr annotation = AstUtils.createAnnotationExpr(clazz, annotationName);
        AstUtils.addAnnotation(method, annotation);
        info("create method annotation: " + annotationName);

        // print log
        info("create method: " + methodName);
        debug(method);

        return method;
    }

    private void createMethod(
        ClassOrInterfaceDeclaration clazz,
        String methodName,
        String methodContent,
        String... importNames
    ) {
        // import package
        CompilationUnit unit = AstUtils.getUnit(clazz);
        createImport(unit, importNames);

        // create method
        MethodDeclaration method = StaticJavaParser.parseMethodDeclaration(methodContent);

        // add method
        clazz.addMember(method);

        // print log
        info("create method: " + methodName);
        debug(method);
    }

    private void info(String info) {
        log.info(info);
    }

    private void debug(Object object) {
        log.debug(object.toString().replaceAll("\r\n", " ").replaceAll("\n", " "));
    }

}
