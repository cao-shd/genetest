package github.plugin.genetest.service;

import github.plugin.genetest.repository.ParseRepository;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.Assert;
import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ParseServiceTest {

    public ParseService parseService = new ParseService();

    public String name = "";

    public ParseRepository parseRepository = new ParseRepository();

    public Integer age = 0;

    @Before()
    public void setUp() {
        parseService.name = name;
        parseService.age = age;
        reflectField("parseRepository", parseRepository);
    }

    @After()
    public void tearDown() {
    }

    @Test()
    public void test_select_user_name() {
        // TODO given
        String username = "";
        // when
        parseService.selectUserName(username);
        try {
            // TODO then assert inner method run times
            throw new RuntimeException();
        } catch (Exception e) {
            String stack = stackTrace(e);
            Assert.fail("Should not run here.\n\t" + stack);
        }
    }

    @Test()
    public void test_condition1_branch_if_name_equals_null() {
        // TODO given
        String name = "";
        int age = 0;
        List<String> children = new ArrayList<>();
        Class<?>[] types = { String.class, Integer.class, List.class };
        Object[] params = { name, age, children };
        // when
        Object actual = reflectMethod("condition1", types, params);
        // TODO then
        Object expect = null;
        Assert.assertEquals(expect, actual);
    }

    @Test()
    public void test_condition1_branch_if_name_equals_null_if_str_equals_hello() {
        // TODO given
        String name = "";
        int age = 0;
        List<String> children = new ArrayList<>();
        Class<?>[] types = { String.class, Integer.class, List.class };
        Object[] params = { name, age, children };
        // when
        Object actual = reflectMethod("condition1", types, params);
        // TODO then
        Object expect = null;
        Assert.assertEquals(expect, actual);
    }

    @Test()
    public void test_condition1_branch_if_name_equals_null_else_str_equals_hello() {
        // TODO given
        String name = "";
        int age = 0;
        List<String> children = new ArrayList<>();
        Class<?>[] types = { String.class, Integer.class, List.class };
        Object[] params = { name, age, children };
        // when
        Object actual = reflectMethod("condition1", types, params);
        // TODO then
        Object expect = null;
        Assert.assertEquals(expect, actual);
    }

    @Test()
    public void test_condition1_branch_else_name_equals_null() {
        // TODO given
        String name = "";
        int age = 0;
        List<String> children = new ArrayList<>();
        Class<?>[] types = { String.class, Integer.class, List.class };
        Object[] params = { name, age, children };
        // when
        Object actual = reflectMethod("condition1", types, params);
        // TODO then
        Object expect = null;
        Assert.assertEquals(expect, actual);
    }

    @Test()
    public void test_condition1_branch_else_name_equals_null_if_name_is_empty() {
        // TODO given
        String name = "";
        int age = 0;
        List<String> children = new ArrayList<>();
        Class<?>[] types = { String.class, Integer.class, List.class };
        Object[] params = { name, age, children };
        // when
        Object actual = reflectMethod("condition1", types, params);
        // TODO then
        Object expect = null;
        Assert.assertEquals(expect, actual);
    }

    @Test()
    public void test_condition1_branch_else_name_equals_null_else_name_is_empty() {
        // TODO given
        String name = "";
        int age = 0;
        List<String> children = new ArrayList<>();
        Class<?>[] types = { String.class, Integer.class, List.class };
        Object[] params = { name, age, children };
        // when
        Object actual = reflectMethod("condition1", types, params);
        // TODO then
        Object expect = null;
        Assert.assertEquals(expect, actual);
    }

    @Test()
    public void test_condition1_branch_else_name_equals_null_else_name_is_empty_if_name_equals_hello() {
        // TODO given
        String name = "";
        int age = 0;
        List<String> children = new ArrayList<>();
        Class<?>[] types = { String.class, Integer.class, List.class };
        Object[] params = { name, age, children };
        // when
        Object actual = reflectMethod("condition1", types, params);
        // TODO then
        Object expect = null;
        Assert.assertEquals(expect, actual);
    }

    @Test()
    public void test_condition1_branch_else_name_equals_null_else_name_is_empty_else_name_equals_hello() {
        // TODO given
        String name = "";
        int age = 0;
        List<String> children = new ArrayList<>();
        Class<?>[] types = { String.class, Integer.class, List.class };
        Object[] params = { name, age, children };
        // when
        Object actual = reflectMethod("condition1", types, params);
        // TODO then
        Object expect = null;
        Assert.assertEquals(expect, actual);
    }

    @Test()
    public void test_condition2() {
        // TODO given
        int name = 0;
        // when
        Object actual = parseService.condition2(name);
        // TODO then
        Object expect = null;
        Assert.assertEquals(expect, actual);
    }

    @Test()
    public void test_condition3_branch_try() {
        // TODO given
        int name = 0;
        // when
        Object actual = parseService.condition3(name);
        // TODO then
        Object expect = null;
        Assert.assertEquals(expect, actual);
    }

    @Test()
    public void test_condition3_branch_try_if_str_equals_hello() {
        // TODO given
        int name = 0;
        // when
        Object actual = parseService.condition3(name);
        // TODO then
        Object expect = null;
        Assert.assertEquals(expect, actual);
    }

    @Test()
    public void test_condition3_branch_try_else_str_equals_hello() {
        // TODO given
        int name = 0;
        // when
        Object actual = parseService.condition3(name);
        // TODO then
        Object expect = null;
        Assert.assertEquals(expect, actual);
    }

    @Test()
    public void test_condition3_branch_catch_exception() {
        // TODO given
        int name = 0;
        // when
        Object actual = parseService.condition3(name);
        // TODO then
        Object expect = null;
        Assert.assertEquals(expect, actual);
    }

    private void reflectField(String fieldName, Object fieldValue) {
        try {
            Field field = ParseService.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(parseService, fieldValue);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object reflectMethod(String methodName, Class<?>[] types, Object[] params) {
        try {
            Method method = ParseService.class.getDeclaredMethod(methodName, types);
            method.setAccessible(true);
            return method.invoke(parseService, params);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String stackTrace(Exception e) {
        return Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString).collect(Collectors.joining("\n\t"));
    }
}
