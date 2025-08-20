import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.AWTException;

class TestSimulatorClick {
    public static void main(String[] args) {
        try {
            System.out.println("Testing mouse click on iOS Simulator...");
            System.out.println("Please position your mouse over the iOS Simulator app icon or button");
            System.out.println("Starting in 5 seconds...");
            Thread.sleep(5000);
            
            Robot robot = new Robot();
            
            // Get current mouse position
            java.awt.Point mousePos = java.awt.MouseInfo.getPointerInfo().getLocation();
            System.out.println("Clicking at position: " + mousePos.x + ", " + mousePos.y);
            
            // Perform a double click
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            Thread.sleep(100);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            
            System.out.println("Double click performed on iOS Simulator");
            System.out.println("Did you see the click take effect in the simulator? (Check manually)");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
