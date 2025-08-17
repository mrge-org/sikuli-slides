package org.sikuli.slides.api.actions;

import org.sikuli.script.Region;
import org.sikuli.slides.api.Context;

import com.google.common.base.Objects;

public class LeftClickAction extends RobotAction {
	
	@Override
	protected void doExecute(Context context) {
		Region screenRegion = context.getScreenRegion();
		screenRegion.click();
	}
	
	public String toString(){
		return Objects.toStringHelper(this).toString();
	}
}
