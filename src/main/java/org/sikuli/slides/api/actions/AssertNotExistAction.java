package org.sikuli.slides.api.actions;

import org.sikuli.script.Pattern;
import org.sikuli.script.Region;
import org.sikuli.slides.api.Context;

import com.google.common.base.Objects;

public class AssertNotExistAction extends TargetAction {
	
	public AssertNotExistAction(Pattern pattern) {
		super(pattern); 
	}
	
	@Override
	public void execute(Context context) throws ActionExecutionException{
		Region screenRegion = context.getScreenRegion();
		try {
			screenRegion.find(getPattern().similar(context.getMinScore()));
			throw new ActionExecutionException("Found a target that is not expected to exist", this);
		} catch (org.sikuli.script.FindFailed e) {
			// target not found, which is expected
		}
	}	
	
	public String toString(){
		return Objects.toStringHelper(this).add("pattern",getPattern()).toString();
	}


}