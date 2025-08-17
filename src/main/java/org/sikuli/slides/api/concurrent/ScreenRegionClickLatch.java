package org.sikuli.slides.api.concurrent;

import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import org.sikuli.script.Region;

public class ScreenRegionClickLatch extends ScreenRegionLatch {
	

	public ScreenRegionClickLatch(Region screenRegion){
		super(screenRegion);
	}
	
	@Override
	public void nativeMouseClicked(NativeMouseEvent e) {
		if (inRange(e))
			release();
	}	
}