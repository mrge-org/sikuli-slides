package org.sikuli.slides.api.actions;

import java.awt.Robot;

import org.sikuli.script.Location;
import org.sikuli.script.Pattern;
import org.sikuli.script.Region;
import org.sikuli.script.Match;
import org.sikuli.slides.api.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;

public class TargetAction extends ChainedAction {
    private static final Logger LOG = LoggerFactory.getLogger(TargetAction.class);
	
	private Pattern pattern;
	
	public TargetAction(Pattern pattern){
		this.setPattern(pattern);
	}
	
	public TargetAction(Pattern pattern, Action targetAction){
		this.setPattern(pattern);
		setChild(targetAction);
	}
	
	@Override
	public void execute(Context context) throws ActionExecutionException {
        Pattern searchPattern = getPattern().similar(context.getMinScore());
        Region screenRegion = context.getScreenRegion();
        // park mouse away from region to avoid cursor affecting match
        parkMouse(screenRegion);
        Match targetMatch = null;
        try {
            targetMatch = screenRegion.find(searchPattern);
        } catch (org.sikuli.script.FindFailed e) {
            // target not found
        }
        // if not found and it's the first executed slide, try multi-scale fallbacks
        if (targetMatch == null && Boolean.TRUE.equals(context.getParameters().get("firstExecutedSlide"))) {
            float[] scales = new float[] {2.0f, 0.5f, 1.5f, 0.75f};
            for (float s : scales) {
                try {
                    Pattern scaled = new Pattern(getPattern().getImage()).resize(s).similar(context.getMinScore());
                    LOG.info("retry match with scaled pattern factor=" + s);
                    try {
                        targetMatch = screenRegion.find(scaled);
                    } catch (org.sikuli.script.FindFailed e) {
                        // continue
                    }
                    if (targetMatch != null) {
                        LOG.info("scaled match succeeded with factor=" + s + " score=" + targetMatch.getScore());
                        break;
                    }
                } catch (Throwable t) {
                    LOG.debug("scaled pattern failed to build for factor=" + s + ": " + t.getMessage());
                }
            }
        }
        if (targetMatch != null){
            Location c = targetMatch.getTarget();
            LOG.info("match found at (" + c.getX() + ", " + c.getY() + ") size " + targetMatch.getW() + "x" + targetMatch.getH() + " score=" + targetMatch.getScore());
            Context childConext = new Context(context, targetMatch);
            Action child = getChild();
            if (child != null){
                child.execute(childConext);            
            }
        }else{
            LOG.info("no match in region " + screenRegion + " with min_score=" + context.getMinScore());
            throw new ActionExecutionException("Unable to locate the target on the screen", this);
        }
    }

    private void parkMouse(Region r){
        try {
            int parkX = Math.max(0, r.getX() - 30);
            int parkY = Math.max(0, r.getY() - 30);
            Robot robot = new Robot();
            robot.mouseMove(parkX, parkY);
        } catch (Throwable t) {
            // ignore
        }
    }

	public String toString(){
		return Objects.toStringHelper(this)				
				.add("pattern", getPattern()).toString();
	}

	public Pattern getPattern() {
		return pattern;
	}

	public void setPattern(Pattern pattern) {
		this.pattern = pattern;
	}

}