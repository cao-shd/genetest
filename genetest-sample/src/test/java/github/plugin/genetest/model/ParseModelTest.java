package github.plugin.genetest.model;

import java.util.Date;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.Assert;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ParseModelTest {

    public ParseModel parseModel = new ParseModel();

    public String username = "";

    public Integer int1 = 0;

    public Integer int2 = 0;

    public Date date = new Date();

    public Boolean bool = false;

    public Boolean bool2 = false;

    public ParseModel parse = new ParseModel();

    @Before()
    public void setUp() {
        reflectField("username", username);
        reflectField("int1", int1);
        reflectField("int2", int2);
        reflectField("date", date);
        reflectField("bool", bool);
        reflectField("bool2", bool2);
        reflectField("parse", parse);
    }

    @After()
    public void tearDown() {
    }

    @Test()
    public void test_get_username() {
        // when
        Object actual = parseModel.getUsername();
        // TODO then
        Object expect = null;
        Assert.assertEquals(expect, actual);
    }

    @Test()
    public void test_set_username() {
        // TODO given
        String username = "";
        // when
        parseModel.setUsername(username);
        try {
            // TODO then assert inner method run times
            throw new RuntimeException();
        } catch (Exception exception) {
            String stackTrace = getStackTrace(exception);
            Assert.fail("Should not run here.\n\t" + stackTrace);
        }
    }

    @Test()
    public void test_get_int1() {
        // when
        Object actual = parseModel.getInt1();
        // TODO then
        Object expect = null;
        Assert.assertEquals(expect, actual);
    }

    @Test()
    public void test_set_int1() {
        // TODO given
        int int1 = 0;
        // when
        parseModel.setInt1(int1);
        try {
            // TODO then assert inner method run times
            throw new RuntimeException();
        } catch (Exception exception) {
            String stackTrace = getStackTrace(exception);
            Assert.fail("Should not run here.\n\t" + stackTrace);
        }
    }

    @Test()
    public void test_get_int2() {
        // when
        Object actual = parseModel.getInt2();
        // TODO then
        Object expect = null;
        Assert.assertEquals(expect, actual);
    }

    @Test()
    public void test_set_int2() {
        // TODO given
        int int2 = 0;
        // when
        parseModel.setInt2(int2);
        try {
            // TODO then assert inner method run times
            throw new RuntimeException();
        } catch (Exception exception) {
            String stackTrace = getStackTrace(exception);
            Assert.fail("Should not run here.\n\t" + stackTrace);
        }
    }

    @Test()
    public void test_get_date() {
        // when
        Object actual = parseModel.getDate();
        // TODO then
        Object expect = null;
        Assert.assertEquals(expect, actual);
    }

    @Test()
    public void test_set_date() {
        // TODO given
        Date date = new Date();
        // when
        parseModel.setDate(date);
        try {
            // TODO then assert inner method run times
            throw new RuntimeException();
        } catch (Exception exception) {
            String stackTrace = getStackTrace(exception);
            Assert.fail("Should not run here.\n\t" + stackTrace);
        }
    }

    @Test()
    public void test_get_bool() {
        // when
        Object actual = parseModel.getBool();
        // TODO then
        Object expect = null;
        Assert.assertEquals(expect, actual);
    }

    @Test()
    public void test_set_bool() {
        // TODO given
        boolean bool = false;
        // when
        parseModel.setBool(bool);
        try {
            // TODO then assert inner method run times
            throw new RuntimeException();
        } catch (Exception exception) {
            String stackTrace = getStackTrace(exception);
            Assert.fail("Should not run here.\n\t" + stackTrace);
        }
    }

    @Test()
    public void test_is_bool2() {
        // when
        Object actual = parseModel.isBool2();
        // TODO then
        Object expect = null;
        Assert.assertEquals(expect, actual);
    }

    @Test()
    public void test_set_bool2() {
        // TODO given
        boolean bool2 = false;
        // when
        parseModel.setBool2(bool2);
        try {
            // TODO then assert inner method run times
            throw new RuntimeException();
        } catch (Exception exception) {
            String stackTrace = getStackTrace(exception);
            Assert.fail("Should not run here.\n\t" + stackTrace);
        }
    }

    @Test()
    public void test_get_parse() {
        // when
        Object actual = parseModel.getParse();
        // TODO then
        Object expect = null;
        Assert.assertEquals(expect, actual);
    }

    @Test()
    public void test_set_parse() {
        // TODO given
        ParseModel parse = new ParseModel();
        // when
        parseModel.setParse(parse);
        try {
            // TODO then assert inner method run times
            throw new RuntimeException();
        } catch (Exception exception) {
            String stackTrace = getStackTrace(exception);
            Assert.fail("Should not run here.\n\t" + stackTrace);
        }
    }

    private void reflectField(String fieldName, Object fieldValue) {
        try {
            Field field = ParseModel.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(parseModel, fieldValue);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getStackTrace(Exception e) {
        return Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString).collect(Collectors.joining("\n\t"));
    }
}
