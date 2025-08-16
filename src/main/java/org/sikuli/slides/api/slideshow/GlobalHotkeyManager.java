package org.sikuli.slides.api.slideshow;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

abstract class GlobalHotkeyManager implements NativeKeyListener {
	
	boolean isMetaPressed(NativeKeyEvent e){
		return (e.getModifiers() & NativeKeyEvent.META_MASK) > 0;
	}	
	boolean isAltPressed(NativeKeyEvent e){
		return (e.getModifiers() & NativeKeyEvent.ALT_MASK) > 0;
	}
	boolean isShiftPressed(NativeKeyEvent e){
		return (e.getModifiers() & NativeKeyEvent.SHIFT_MASK) > 0;
	}
	boolean isCtrlPressed(NativeKeyEvent e){
		return (e.getModifiers() & NativeKeyEvent.CTRL_MASK) > 0;
	}

	final public void start() {
		try {
			GlobalScreen.registerNativeHook();
		} catch (NativeHookException e1) {
			return;
		} 			

		GlobalScreen.addNativeKeyListener(this);
	}

	final public void stop(){
		GlobalScreen.removeNativeKeyListener(this);
	}
	
	final public void nativeKeyReleased(NativeKeyEvent e) {
	}
	final public void nativeKeyTyped(NativeKeyEvent e) {
	}
}