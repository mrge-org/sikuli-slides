package org.sikuli.slides.api.actions;

import org.sikuli.script.Region;
import org.sikuli.slides.api.Context;

import com.google.common.base.Objects;

public class DropAction extends RobotAction {
	
	@Override
	protected void doExecute(Context context) {
		logger.debug("executing {}", this);
		Region screenRegion = context.getScreenRegion();
		logger.info("performing drop action (approximate click) at region: " + screenRegion);
		screenRegion.click();
	}
	
	public String toString(){
		return Objects.toStringHelper(this).toString();
	}
}
