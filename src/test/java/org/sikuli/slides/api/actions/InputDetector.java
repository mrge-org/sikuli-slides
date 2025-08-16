package org.sikuli.slides.api.actions;

import java.util.ArrayList;
import java.util.List;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseInputListener;
import org.sikuli.recorder.detector.EventDetector;

public class InputDetector {
	
	private MouseEventDetector mouseDetector;	
	private GlobalKeyListenerExample keyboardDetector;
	
	public InputDetector(){
		mouseDetector = new MouseEventDetector();
		keyboardDetector = new GlobalKeyListenerExample();
	}
	
	public void start() throws NativeHookException{
		GlobalScreen.registerNativeHook();
		GlobalScreen.addNativeKeyListener(keyboardDetector);
		GlobalScreen.addNativeMouseListener(mouseDetector);	
	}
	
	public void stop(){
		GlobalScreen.removeNativeMouseListener(mouseDetector);
		GlobalScreen.removeNativeKeyListener(keyboardDetector);
		try {
			GlobalScreen.unregisterNativeHook();
		} catch (NativeHookException e) {
			// ignore for tests
		}
	}
	
	public NativeMouseEvent getLastMouseEvent(){
		if (mouseDetector.events.size() == 0)
			return null;
		else
			return mouseDetector.events.get(mouseDetector.events.size()-1);		
	}
	
	
	public int getNumKeyEvents(){
		return  keyboardDetector.events.size();
	}
	
	public int getNumMouseEvents(){
		return  mouseDetector.events.size();
	}
	
	
	public NativeKeyEvent getLastKeyEvent(){
		if (keyboardDetector.events.size() == 0)
			return null;
		else
			return keyboardDetector.events.get(keyboardDetector.events.size()-1);		
	}
	
	static class GlobalKeyListenerExample implements NativeKeyListener {
		List<NativeKeyEvent> events = new ArrayList<NativeKeyEvent>();

		public void nativeKeyPressed(NativeKeyEvent e) {
	    }

	    public void nativeKeyReleased(NativeKeyEvent e) {
	    }

	    public void nativeKeyTyped(NativeKeyEvent e) {
	    	//System.out.println("Key Typed: " + e.getKeyText(e.getKeyCode()));
	    	System.out.println("Key Typed: " + e.getKeyChar());
	    	events.add(e);
	    }
	}

	static class MouseEventDetector extends EventDetector 
	implements NativeMouseInputListener {

		List<NativeMouseEvent> events = new ArrayList<NativeMouseEvent>();

		public void nativeMouseClicked(NativeMouseEvent e) {
			System.out.println("Mouse Clicked: x = " + e.getX() + ", y = " + e.getY() + ", button =  " + e.getButton() + ", count = " + e.getClickCount());
			events.add(e);
		}

		public void nativeMousePressed(NativeMouseEvent e) {
			//System.out.println("Mosue Pressed: " + e.getButton());
		}

		public void nativeMouseReleased(NativeMouseEvent e) {
			//System.out.println("Mosue Released: " + e.getButton());
		}

		public void nativeMouseMoved(NativeMouseEvent e) {
			//System.out.println("Mosue Moved: " + e.getX() + ", " + e.getY());
		}

		public void nativeMouseDragged(NativeMouseEvent e) {
			//System.out.println("Mosue Dragged: " + e.getX() + ", " + e.getY());
		}
	}
}