import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.AWTException;

class TestMouseClick {
    public static void main(String[] args) {
        try {
            System.out.println("Testing mouse click automation...");
            System.out.println("Move your mouse to a safe area (like Desktop) in 3 seconds...");
            Thread.sleep(3000);
            
            Robot robot = new Robot();
            
            // Get current mouse position
            java.awt.Point mousePos = java.awt.MouseInfo.getPointerInfo().getLocation();
            System.out.println("Current mouse position: " + mousePos.x + ", " + mousePos.y);
            
            // Perform a single click
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            System.out.println("Single click performed");
            
            Thread.sleep(1000);
            
            // Perform a double click
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            Thread.sleep(50);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            System.out.println("Double click performed");
            
            System.out.println("Mouse automation test completed successfully!");
            
        } catch (AWTException e) {
            System.err.println("AWT Exception: " + e.getMessage());
            System.err.println("This usually means accessibility permissions are missing.");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
