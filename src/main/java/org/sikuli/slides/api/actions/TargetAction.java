package org.sikuli.slides.api.actions;

import org.sikuli.script.Pattern;
import org.sikuli.script.Region;
import org.sikuli.script.Match;
import org.sikuli.slides.api.Context;

import com.google.common.base.Objects;

public class TargetAction extends ChainedAction {
	
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
		Match targetMatch = null;
		try {
			targetMatch = screenRegion.find(searchPattern);
		} catch (org.sikuli.script.FindFailed e) {
			// target not found
		}
		if (targetMatch != null){			
			Context childConext = new Context(context, targetMatch);
			Action child = getChild();
			if (child != null){
				child.execute(childConext);			
			}
		}else{
			throw new ActionExecutionException("Unable to locate the target on the screen", this);
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