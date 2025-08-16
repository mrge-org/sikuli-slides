package org.sikuli.slides.api.concurrent;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;

public class EscapeKeyLatch extends NativeInputLatch{
	public void nativeKeyPressed(NativeKeyEvent e) {
		if (e.getKeyCode() == NativeKeyEvent.VC_ESCAPE){
			release();
		}
	}
}