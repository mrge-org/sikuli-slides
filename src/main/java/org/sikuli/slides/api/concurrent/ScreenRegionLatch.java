package org.sikuli.slides.api.concurrent;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import org.sikuli.script.Region;
import org.sikuli.script.Screen;

class ScreenRegionLatch extends NativeInputLatch {
	
	private Region screenRegion;
	private int screenOffsetX;
	private int screenOffsetY;

	ScreenRegionLatch(Region screenRegion){
		this.screenRegion = checkNotNull(screenRegion);		

		// calculate the x,y offsets of the target screen, which can be 
		// the secondary screen. So we can map the x,y given by NativeHook
		// to the x,y of the ScreenRegion object
		int id =  ((Screen) screenRegion.getScreen()).getID();
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice [] devices = ge.getScreenDevices();
		Rectangle bounds = devices[id].getDefaultConfiguration().getBounds();
		this.screenOffsetX = bounds.x; 
		this.screenOffsetY = bounds.y; 
	}
	
	/**
	 * Check the mouse action is within the screen region range.
	 * @param e native mouse event
	 * @return true if the mouse event was within the screen region range. 
	 * Otherwise, it returns false.
	 */
	protected boolean inRange(NativeMouseEvent e){
		Rectangle r = new Rectangle(screenRegion.getX(), screenRegion.getY(), screenRegion.getW(), screenRegion.getH());
		r.x += screenOffsetX;
		r.y += screenOffsetY;

		int x = e.getX();
		int y = e.getY();
		return r.contains(x,y);
	}
}