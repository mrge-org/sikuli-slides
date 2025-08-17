package org.sikuli.slides.api.generators;

import java.io.File;
// import org.sikuli.api.DesktopScreenRegion; // Legacy API removed
import org.sikuli.script.Screen;
import org.sikuli.script.Region;
import org.sikuli.script.Pattern;

class GeneratedProgram {

	Screen screen = new Screen();
	Region screenRegion = screen;

	// TODO: modify these settings if the default settings don't work for you
	public static int DEFAULT_WAIT_TIME = 5000;
	public static float DEFAULT_MINSCORE = 0.7f;
	
	// TODO: modify this path if you moved image files to another location
	public static String DEFAULT_IMAGE_DIRECTORY =  "images";
	
	public File findImageByName(String name){
		return new File(DEFAULT_IMAGE_DIRECTORY + File.pathSeparator + name);
	}

	// Source: Slide 1 
	// Action: click
	// Argument: 
	public boolean step1() {
	    	Pattern pattern = new Pattern("image1.png").similar(DEFAULT_MINSCORE);
	    	try {
	    		screenRegion.click(pattern);
	    		return true;
	    	} catch (org.sikuli.script.FindFailed e) {
	    		return false;
	    	}
	}


	// Source: Slide 2 
	// Action: rightClick
	// Argument: 
	public boolean step2() {
	    	Pattern pattern = new Pattern("image2.png").similar(DEFAULT_MINSCORE);
	    	try {
	    		screenRegion.rightClick(pattern);
	    		return true;
	    	} catch (org.sikuli.script.FindFailed e) {
	    		return false;
	    	}
	}


	// Source: Slide 3 
	// Action: type
	// Argument: something to type
	public boolean step3() {
	    	Pattern pattern = new Pattern("image3.png").similar(DEFAULT_MINSCORE);
	    	try {
	    		screenRegion.click(pattern);
	    		screenRegion.type("something to type");
	    		return true;
	    	} catch (org.sikuli.script.FindFailed e) {
	    		return false;
	    	}
	}


	// Source: Slide 4 
	// Action: BrowserAction
	// Argument: 
	public boolean step4() {
	    	// no action is generated for this step
	    	return true;
	}


	// Source: Slide 5 
	// Action: exist
	// Argument: 
	public boolean step5() {
	    	Pattern pattern = new Pattern("image5.png").similar(DEFAULT_MINSCORE);
	    	try {
	    		screenRegion.find(pattern);
	    		return true;
	    	} catch (org.sikuli.script.FindFailed e) {
	    		return false;
	    	}
	}


	// Source: Slide 6 
	// Action: notExist
	// Argument: 
	public boolean step6() {
	    	Pattern pattern = new Pattern("image6.png").similar(DEFAULT_MINSCORE);
	    	try {
	    		screenRegion.find(pattern);
	    		return false; // Found when not expected
	    	} catch (org.sikuli.script.FindFailed e) {
	    		return true; // Not found as expected
	    	}
	}


	// Source: Slide 7 
	// Action: type
	// Argument: something to type
	public boolean step7() {
	    	Pattern pattern = new Pattern("image7.png").similar(DEFAULT_MINSCORE);
	    	try {
	    		screenRegion.click(pattern);
	    		screenRegion.type("something to type");
	    		return true;
	    	} catch (org.sikuli.script.FindFailed e) {
	    		return false;
	    	}
	}


	public void executeAll(){

		step1();
		step2();
		step3();
		step4();
		step5();
		step6();
		step7();

		// TODO: Optionally handle the return value of each step
    	}

	static public void main(String... args){
		GeneratedProgram prog = new GeneratedProgram();
		prog.executeAll();
	} 
}