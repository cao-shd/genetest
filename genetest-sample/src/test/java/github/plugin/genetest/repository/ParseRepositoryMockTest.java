package github.plugin.genetest.repository;

import org.mockito.junit.MockitoJUnitRunner;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.Assert;
import java.util.Map;
import java.util.HashMap;
import jdk.nashorn.internal.runtime.arrays.ArrayIndex;
import github.plugin.genetest.App;
import java.util.List;
import java.util.ArrayList;
import java.text.Format;

@RunWith(value = MockitoJUnitRunner.class)
public class ParseRepositoryMockTest {

    @InjectMocks()
    public ParseRepository parseRepository;

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
        Map<String, Map<ArrayIndex, App>> options = new HashMap<>();
        // when
        Object actual = parseRepository.selectById(options);
        // TODO then
        Object expect = null;
        Assert.assertEquals(expect, actual);
    }

    @Test()
    public void test_select_by_id_branch_else_options_is_empty() {
        // TODO given
        Map<String, Map<ArrayIndex, App>> options = new HashMap<>();
        // when
        Object actual = parseRepository.selectById(options);
        // TODO then
        Object expect = null;
        Assert.assertEquals(expect, actual);
    }

    @Test()
    public void test_select_by_id_branch_if_options_is_empty2() {
        // TODO given
        List<Format> options = new ArrayList<>();
        // when
        Object actual = parseRepository.selectById(options);
        // TODO then
        Object expect = null;
        Assert.assertEquals(expect, actual);
    }

    @Test()
    public void test_select_by_id_branch_else_options_is_empty2() {
        // TODO given
        List<Format> options = new ArrayList<>();
        // when
        Object actual = parseRepository.selectById(options);
        // TODO then
        Object expect = null;
        Assert.assertEquals(expect, actual);
    }
}
