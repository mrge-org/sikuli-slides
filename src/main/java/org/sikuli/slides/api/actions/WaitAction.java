package org.sikuli.slides.api.actions;

import org.sikuli.script.Pattern;
import org.sikuli.script.Region;
import org.sikuli.slides.api.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;

public class WaitAction extends TargetAction {
	
	private long duration = 10000;
	private Pattern pattern;
	private Action retry;
	
	public WaitAction(Pattern pattern){
		super(pattern);
	}		

	@Override
	public void execute(Context context) throws ActionExecutionException {			
//		Action action = new Action(){
//
//			@Override
//			public void execute(Context context) throws ActionExecutionException {
//				ScreenRegion screenRegion = context.getScreenRegion();
//				ScreenRegion ret = screenRegion.find(getTarget());
//				if (ret == null){
//					throw new ActionExecutionException("", this);
//				}
//			}
//
//			@Override
//			public void stop() {				
//			}
//			
//		};
		retry = new RetryAction(new TargetAction(pattern, new EmptyAction()), duration, 1000);		
		retry.execute(context);
	}
	
	@Override
	public void stop(){
		if (retry != null)
			retry.stop();
		retry = null;
	}
	
	public String toString(){
		return Objects.toStringHelper(this).add("pattern",getPattern()).toString();
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public Pattern getPattern() {
		return pattern;
	}

	public void setPattern(Pattern pattern) {
		this.pattern = pattern;
	}
}