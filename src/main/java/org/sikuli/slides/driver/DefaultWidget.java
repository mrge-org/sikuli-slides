package org.sikuli.slides.driver;

import org.sikuli.script.Region;
import org.sikuli.script.Pattern;
import org.sikuli.script.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;

class DefaultWidget implements Widget {

    private static final Logger logger = LoggerFactory.getLogger(DefaultWidget.class);

	private Pattern pattern;
	private String label;
	private Region screenRegion = null;
	private Screen screen = new Screen();
	
	public DefaultWidget(){
		
	}

	public DefaultWidget(Pattern p, String label){
		this.pattern = p;
		this.label = label;
	}

	public Pattern getPattern() {
		return pattern;
	}

	public String getLabel() {
		return label;
	}	
	
	public void click(){
		Region r = getScreenRegion();
		if (r != null){
			r.click();
		}
	}
	
	@Override
	public void doubleClick() {
		Region r = getScreenRegion();
		if (r != null){
			r.doubleClick();
		}		
	}
	
	@Override
	public void type(String text) {
		Region r = getScreenRegion();
		if (r != null){
			r.click();
			r.type(text);
		}	
	}	
	
	@Override
	public void paste(String text) {
		Region r = getScreenRegion();
		if (r != null){
			r.click();
			r.paste(text);
		}	
	}		
	
	public String toString(){
		return Objects.toStringHelper(getClass().getSimpleName())
				.add("label", label)
				.add("pattern", pattern)				
				.toString();
	}
	
	@Override
	public void setScreenRegion(Region screenRegion) {
		this.screenRegion = screenRegion;
	}
	
	@Override
	public Region getScreenRegion(){
		return screenRegion;
	}

	@Override
	public void highlight() {
		Region r = getScreenRegion();
		if (r != null){
			// Visual highlighting not supported in SikuliX 2.x
			r.highlight(1);
		}
	}

	@Override
	public void rightClick() {
		Region r = getScreenRegion();
		if (r != null){
			r.rightClick();
		}		
	}

	@Override
	public void hover() {
		Region r = getScreenRegion();
		if (r != null){
			r.hover();
		}	
	}

	@Override
	public void drag() {
		Region r = getScreenRegion();
		if (r != null){
			r.dragDrop(r); // Drag to same location as placeholder
		}			
	}

	@Override
	public void drop() {
		Region r = getScreenRegion();
		if (r != null){
			// Drop functionality is part of dragDrop in SikuliX 2.x
			logger.info("Drop operation - use dragDrop instead");
		}            
	} 
    
}