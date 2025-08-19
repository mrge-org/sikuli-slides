import org.sikuli.script.support.Commons;

public class DebugLibsPath {
    public static void main(String[] args) {
        try {
            System.out.println("SikuliX libs folder path: " + Commons.getLibsFolder());
        } catch (Exception e) {
            System.err.println("Error getting libs folder: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
