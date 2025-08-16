package org.sikuli.slides.api.concurrent;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.CountDownLatch;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseInputListener;

public class NativeInputLatch implements Latch, NativeKeyListener, NativeMouseInputListener {

	private CountDownLatch detectedSignal;

	public NativeInputLatch(){
		detectedSignal = new CountDownLatch(1);
	}	
	
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
	
	
	final public void await() {
		try {
			GlobalScreen.registerNativeHook();
		} catch (NativeHookException e1) {
			return;
		} 			
		
		GlobalScreen.addNativeKeyListener(this);
		GlobalScreen.addNativeMouseMotionListener(this);
		GlobalScreen.addNativeMouseListener(this);
		try {
			detectedSignal.await();
		} catch (InterruptedException e) {

		}
	}

	final public void release(){
		detectedSignal.countDown();
		GlobalScreen.removeNativeMouseListener(this);
		GlobalScreen.removeNativeMouseMotionListener(this);
		GlobalScreen.removeNativeKeyListener(this);
	}

	public void nativeKeyPressed(NativeKeyEvent e) {
	}
	public void nativeKeyReleased(NativeKeyEvent e) {
	}
	public void nativeKeyTyped(NativeKeyEvent e) {
	}

	public void nativeMouseClicked(NativeMouseEvent e) {	
	}
	public void nativeMousePressed(NativeMouseEvent e) {
	}
	public void nativeMouseReleased(NativeMouseEvent e) {
	}
	public void nativeMouseMoved(NativeMouseEvent e) {
	}
	public void nativeMouseDragged(NativeMouseEvent e) {
	}

}