package org.sikuli.slides.api.actions;

import java.util.List;

import org.sikuli.script.Region;
import org.sikuli.script.Key;
import org.sikuli.slides.api.Context;
import org.apache.commons.lang.StringEscapeUtils;
import org.sikuli.slides.api.interpreters.TypeStringParser;
import org.sikuli.slides.api.interpreters.TypeStringParser.TypeStringPart;

import com.google.common.base.Objects;

public class TypeAction extends RobotAction {
	
	private String text;

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
	
	String interpretAsKeyString(String name){
		if (name.equals("ENTER")){
			return Key.ENTER;
		}else if (name.equals("ESC")){
			return Key.ESC;
		}else if (name.equals("LEFT")){
			return Key.LEFT;
		}else if (name.equals("UP")){
			return Key.UP;
		}else if (name.equals("RIGHT")){
			return Key.RIGHT;
		}else if (name.equals("DOWN")){
			return Key.DOWN;
		}else if (name.equals("TAB")){
			return Key.TAB;
		}else if (name.equals("BACKSPACE")){
			return Key.BACKSPACE;
		}else if (name.equals("PAGEUP")){
			return Key.PAGE_UP;
		}else if (name.equals("PAGEDOWN")){
			return Key.PAGE_DOWN;
		}
		return null;
	}

	@Override
	protected void doExecute(Context context) {
		String textToType = context.render(getText());
		textToType = StringEscapeUtils.unescapeJava(textToType);

		Region screenRegion = context.getScreenRegion();
		screenRegion.click();
		
		TypeStringParser p = new TypeStringParser();
		List<TypeStringPart> parts = p.parse(textToType);		
		for (TypeStringPart part : parts){
		
			if (part.getType() == TypeStringPart.Type.Key){
				String keyText = part.getText();							
				String key = interpretAsKeyString(keyText);
				if (key != null){
					screenRegion.type(key);				
				}
			}else if (part.getType() == TypeStringPart.Type.Text){			
				screenRegion.type(part.getText());
			}			
		}
			
				
	}
	
	public String toString(){
		return Objects.toStringHelper(this).add("text", text).toString();
	}

}
