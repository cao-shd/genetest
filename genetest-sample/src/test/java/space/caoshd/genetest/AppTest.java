package space.caoshd.genetest;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.Assert;
import java.util.Arrays;
import java.util.stream.Collectors;

public class AppTest {

    public App app = new App();

    @Before()
    public void setUp() {
    }

    @After()
    public void tearDown() {
    }

    @Test()
    public void test_main() {
        // TODO given
        String[] args = new String[]{};
        // when
        App.main(args);
        try {
            // TODO then assert inner method run times
            throw new RuntimeException();
        } catch (Exception e) {
            String stack = stackTrace(e);
            Assert.fail("Should not run here.\n\t" + stack);
        }
    }

    private String stackTrace(Exception e) {
        return Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString).collect(Collectors.joining("\n\t"));
    }
}
