package org.sikuli.slides.api.concurrent;

import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import org.sikuli.script.Region;

public class ScreenRegionHoverLatch extends ScreenRegionLatch {
	

	public ScreenRegionHoverLatch(Region screenRegion){
		super(screenRegion);
	}
	
	@Override
	public void nativeMouseMoved(NativeMouseEvent e) {
		if (inRange(e))
			release();
	}	
}