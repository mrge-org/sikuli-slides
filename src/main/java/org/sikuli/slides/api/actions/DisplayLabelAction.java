package org.sikuli.slides.api.actions;

import java.awt.Color;

import org.sikuli.script.Region;
import org.sikuli.slides.api.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;

public class DisplayLabelAction implements Action {
	
	private static final Logger logger = LoggerFactory.getLogger(DisplayLabelAction.class);

	private String text = "";
	private int fontSize = 12;
	private int duration = 3000;
	private Color backgroundColor = Color.yellow;

	@Override
	public void execute(Context context){
		Region targetRegion = context.getScreenRegion();		
		String textToDisplay = context.render(text);
		
		// Visual display not supported in SikuliX 2.x
		// Consider using logger or console output instead
		logger.info("Display Label: {}", textToDisplay);
	}
	
	@Override
	public void stop(){
		// Visual canvas not supported in SikuliX 2.x
		logger.info("Label display stopped");
	}
	
	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public int getFontSize() {
		return fontSize;
	}

	public void setFontSize(int fontSize) {
		this.fontSize = fontSize;
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public Color getBackgroundColor() {
		return backgroundColor;
	}

	public void setBackgroundColor(Color backgroundColor) {
		this.backgroundColor = backgroundColor;
	}

	
	public String toString(){
		return Objects.toStringHelper(this).add("text", text).toString();
	}
}
