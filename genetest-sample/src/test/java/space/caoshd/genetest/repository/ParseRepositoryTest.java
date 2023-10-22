package space.caoshd.genetest.repository;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.Assert;
import java.util.Map;
import java.util.HashMap;

public class ParseRepositoryTest {

    public ParseRepository parseRepository = new ParseRepository();

    @Before()
    public void setUp() {
    }

    @After()
    public void tearDown() {
    }

    @Test()
    public void test_select_by_id() {
        // TODO given
        String str = "";
        // when
        Object actual = parseRepository.selectById(str);
        // TODO then
        Object expect = null;
        Assert.assertEquals(expect, actual);
    }

    @Test()
    public void test_select_by_id2() {
        // TODO given
        int str = 0;
        // when
        Object actual = parseRepository.selectById(str);
        // TODO then
        Object expect = null;
        Assert.assertEquals(expect, actual);
    }

    @Test()
    public void test_select_by_id_branch_if_options_is_empty() {
        // TODO given
        Map<String, Object> options = new HashMap<>();
        // when
        Object actual = parseRepository.selectById(options);
        // TODO then
        Object expect = null;
        Assert.assertEquals(expect, actual);
    }

    @Test()
    public void test_select_by_id_branch_else_options_is_empty() {
        // TODO given
        Map<String, Object> options = new HashMap<>();
        // when
        Object actual = parseRepository.selectById(options);
        // TODO then
        Object expect = null;
        Assert.assertEquals(expect, actual);
    }
}
