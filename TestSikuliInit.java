import java.awt.image.BufferedImage;
import org.sikuli.script.Finder;
import org.sikuli.script.Pattern;

public class TestSikuliInit {
    public static void main(String[] args) {
        try {
            System.out.println("Testing SikuliX initialization and OpenCV loading...");
            
            // Create a small test image
            BufferedImage testImage = new BufferedImage(100, 100, BufferedImage.TYPE_3BYTE_BGR);
            
            // Try to create a Finder instance - this will trigger OpenCV loading
            Finder finder = new Finder(testImage);
            
            System.out.println("SUCCESS: SikuliX and OpenCV initialized successfully!");
            System.out.println("OpenCV native library loaded without errors.");
            
        } catch (Exception e) {
            System.err.println("FAILED: Error initializing SikuliX/OpenCV: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
