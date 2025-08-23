package org.sikuli.slides.api.actions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.sikuli.script.Pattern;
import org.sikuli.slides.api.Context;

import static org.mockito.Mockito.mock;

/**
 * Migrated compile-only tests: validate constructors against SikuliX Pattern API.
 * Execution is UI-dependent and thus ignored.
 */
public class TargetActionTest {

    private Context context;

    @Before
    public void setUp(){
        context = new Context();
    }

    @Ignore("compile-only migration: avoids UI/image dependency")
    @Test
    public void testTargetAction_withChild_compiles() throws ActionExecutionException {
        Pattern p = new Pattern("nonexistent.png");
        Action childAction = mock(Action.class);
        Action action = new TargetAction(p, childAction);
        // Do not execute; just ensure types are correct
        action.toString();
    }

    @Ignore("compile-only migration: avoids UI/image dependency")
    @Test
    public void testTargetAction_withoutChild_compiles() throws ActionExecutionException {
        Pattern p = new Pattern("nonexistent.png");
        Action action = new TargetAction(p);
        action.toString();
    }
}
