package org.sikuli.slides.api.actions;

import java.awt.Robot;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.sikuli.script.Region;
import org.sikuli.script.ScreenImage;
import org.sikuli.slides.api.Context;
import org.sikuli.slides.api.ExecutionEvent;
import org.sikuli.slides.api.ExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 */
abstract public class RobotAction implements Action {
	
	Logger logger = LoggerFactory.getLogger(getClass());
	
	@Override
	public void execute(Context context) throws ActionExecutionException {
		ExecutionListener actionListener = context.getExecutionListener();
		if (actionListener != null){
			actionListener.beforeExecution(new ExecutionEvent(this, context));
		}
		
		logger.debug("executing {}", this);
		try {
			doExecute(context);
		} catch (ActionExecutionException e) {
			throw e;
		}finally{
			if (actionListener != null){
				actionListener.afterExecution(new ExecutionEvent(this, context));
			}			
		}
		return;
	}
	
	abstract protected void doExecute(Context context) throws ActionExecutionException;
	
	public void stop(){		
	}
	
	protected void savePreClickRegion(Context ctx, String label){
		try {
			// park mouse away from the region first to avoid pointer in capture
			parkMouse(ctx);
			Region r = ctx.getScreenRegion();
			if (r == null) return;
			ScreenImage si = r.getScreen().capture(r);
			if (si == null || si.getImage() == null) return;
			File dir = new File("target/clicks");
			if (!dir.exists()) {
				dir.mkdirs();
			}
			int slideNum = (ctx.getSlide() != null) ? ctx.getSlide().getNumber() : -1;
			File out = new File(dir, String.format("slide-%d-%s.png", slideNum, label));
			ImageIO.write(si.getImage(), "png", out);
			logger.info("Saved pre-click region image to {}", out.getAbsolutePath());
			// sound indication
			Toolkit.getDefaultToolkit().beep();
		} catch (IOException | RuntimeException t) {
			logger.warn("Could not save pre-click region image", t);
		}
	}

	protected void parkMouse(Context ctx){
		try {
			Region r = ctx.getScreenRegion();
			if (r == null) return;
			int parkX = Math.max(0, r.getX() - 30);
			int parkY = Math.max(0, r.getY() - 30);
			Robot robot = new Robot();
			robot.mouseMove(parkX, parkY);
			// small settle time could be added if needed
		} catch (Throwable t) {
			logger.debug("Mouse park failed (non-fatal): {}", t.toString());
		}
	}

}
