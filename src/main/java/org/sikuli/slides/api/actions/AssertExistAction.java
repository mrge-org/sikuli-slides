package org.sikuli.slides.api.actions;

import org.sikuli.script.Pattern;
import org.sikuli.script.Region;
import org.sikuli.slides.api.Context;

import com.google.common.base.Objects;

public class AssertExistAction extends TargetAction {
	
	public AssertExistAction(Pattern pattern) {
		super(pattern);
	}		

	@Override
	public void execute(Context context) throws ActionExecutionException {
		Region screenRegion = context.getScreenRegion();
		try {
			screenRegion.find(getPattern().similar(context.getMinScore()));
		} catch (org.sikuli.script.FindFailed e) {
			throw new ActionExecutionException("Unable to find the target expected to exist", this);
		}
	}
	
	public String toString(){
		return Objects.toStringHelper(this).add("pattern",getPattern()).toString();
	}
}