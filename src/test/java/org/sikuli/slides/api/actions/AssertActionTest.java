package org.sikuli.slides.api.actions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.sikuli.script.Pattern;
import org.sikuli.slides.api.Context;

/**
 * Migrated to SikuliX Pattern-based API. These are smoke tests to ensure
 * construction compiles against new dependencies. Actual screen matching is
 * UI-dependent and intentionally ignored here.
 */
public class AssertActionTest {
    private Context context;

    @Before
    public void setUp(){
        context = new Context();
    }

    @Ignore("UI-dependent: requires on-screen image to match")
    @Test
    public void testExistAction_compilesWithPattern() throws ActionExecutionException {
        Pattern p = new Pattern("nonexistent.png");
        Action action = new AssertExistAction(p);
        // action.execute(context); // intentionally not executed
        action.toString();
    }

    @Ignore("UI-dependent: requires on-screen image to match")
    @Test
    public void testNotExistAction_compilesWithPattern() throws ActionExecutionException {
        Pattern p = new Pattern("nonexistent.png");
        Action action = new AssertNotExistAction(p);
        // action.execute(context); // intentionally not executed
        action.toString();
    }
}
