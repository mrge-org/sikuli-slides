package org.sikuli.slides.api.listeners;

import org.sikuli.script.Region;
import org.sikuli.slides.api.ExecutionEvent;
import org.sikuli.slides.api.ExecutionListener;
import org.sikuli.slides.api.actions.Action;
import org.sikuli.slides.api.actions.DoubleClickAction;
import org.sikuli.slides.api.actions.AssertExistAction;
import org.sikuli.slides.api.actions.LeftClickAction;
import org.sikuli.slides.api.actions.RightClickAction;
import org.sikuli.slides.api.actions.TypeAction;

public class ExecutionVisualizer implements ExecutionListener {

    boolean accept(Action action){
        return action instanceof LeftClickAction ||
                action instanceof RightClickAction ||
                action instanceof DoubleClickAction ||
                action instanceof AssertExistAction ||
                action instanceof TypeAction;
    }
    
    @Override
    public void beforeExecution(ExecutionEvent event) {
        // Minimal visual cue: highlight region briefly for actionable events
        if (accept(event.getAction())){
            Region region = event.getContext().getScreenRegion();
            if (region != null) {
                region.highlight(0.5);
            }
        }
    }

    @Override
    public void afterExecution(ExecutionEvent event) {
        // No-op: highlight auto-clears after the given duration
    }
}
