package org.sikuli.slides.apps;


import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.sikuli.script.Screen;
import org.sikuli.script.Region;
import org.sikuli.slides.api.Context;
import org.sikuli.slides.api.ExecutionFilter;
import org.sikuli.slides.api.ExecutionFilter.Factory;
import org.sikuli.slides.api.SlideExecutionException;
import org.sikuli.slides.api.Slides;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.LogManager;

import com.google.common.base.Objects;
import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;

public class ExecuteMain {

    private static final Logger LOG = Logger.getLogger(ExecuteMain.class);

    static final String EXE = "java -jar sikuli-slides-1.7.jar execute";
    static final String SYNTAX = "input [options]";

    @Argument(value = "help", description = "Print help message", required = false)
    private boolean help = false;

    @Argument(value = "screen", description = "The id of the connected screen/monitor (default is 0).", required = false)
    private Integer screenId = 0;

	@Argument(value = "min_score", description = "The minimum similarity score for a target to be considered as a match. It's on a 0 to 1 scale where 0 is the least precise search and 1.0 is the most precise search (default is 0.95).", required = false)
	private Float minScore = 0.9f;

	@Argument(value = "wait", description = "The maximum time to wait (ms) for a target to appear on the screen to perform an action on it (default is 5000).", required = false)
	private Long wait = 5000L;

	@Argument(value = "parameters", description = "Parameters as name=value pairs joined by ;", required = false, delimiter = ";")
	private String[] params = new String[]{};

	@Argument(value = "range", description = "The range of the slide(s) to execute. e.g., \"1\" executes only slide 1, \"2-4\" executes slide 2 to 4, \"2-\" executes slide 2 till the end", required = false)
	private String range = null;

    @Argument(value = "bookmark", description = "The bookmark to start executing from.", required = false)
    private String bookmark = null;

    @Argument(value = "log", description = "The level of log messages to print to the console. Choices are ALL, TRACE, DEBUG, INFO, WARN, ERROR, OFF (default: INFO).", required = false)
    private String logLevel = "INFO";

    @Argument(value = "region", description = "Optional region within the selected screen to operate on, in the form x,y,w,h (relative to the selected -screen).", required = false)
    private String regionRect = null;

    // Hint region is computed automatically from slide content; no CLI flag.

    @Argument(value = "use_awt_robot", description = "Use Java AWT Robot for mouse clicks instead of SikuliX (default: false)", required = false)
    private boolean useAwtRobot = false;

    @Argument(value = "exhaustive", description = "Perform a single exhaustive scan without time-based waiting/retries (default: false)", required = false)
    private boolean exhaustive = false;

    Context context;
    URL url;
    private boolean helpRequested = false;

    public ExecutionFilter parseBookmark(){
        if (bookmark == null)
            return null;        
        return Factory.createStartFromBookmarkFilter(bookmark);
    }

    public ExecutionFilter parseRange(){
        if (range == null)
            return null;

        String[] toks = range.split("-");        
        if (toks.length == 1){
            final int i = Integer.parseInt(toks[0]);               
            // handles "2-"
            if (range.endsWith("-")){
                return Factory.createStartFromSlideFilter(i);               
            }else{
                // handles "2"
                return Factory.createSingleSlideFilter(i);
            }
        } else if (toks.length == 2) {
            final int i = Integer.parseInt(toks[0]);
            final int j = Integer.parseInt(toks[1]);
            return Factory.createRangeFilter(i,j);
        }
        
        return null;
    }

    public Context parseContext() {
        Context context = new Context();

        // initialize logging early so subsequent messages are visible
        configureLogging(logLevel);

        // set parameter values
        for (String param : params){
            String[] toks = param.split("=");
            if (toks.length == 2){
                String name = toks[0];
                String value = toks[1];
                context.addParameter(name,  value);                
            }
        }

        // pass explicit flags as parameters for downstream consumers
        context.addParameter("exhaustive", exhaustive);

        // set min score
        if (minScore < 0 || minScore > 1){
            throw new IllegalArgumentException("" + minScore + " is not a valid value for min_score. Please specify a score between 0 and 1.");
        }        
        context.setMinScore(minScore);

        // set wait time
        context.setWaitTime(wait);

        // set screen region (full screen of the selected monitor)
        Region screenRegion = new Screen(screenId);
        context.setScreenRegion(screenRegion);

        // set click implementation
        context.setUseAwtRobot(useAwtRobot);

        // optionally narrow to a sub-region relative to the selected screen
        if (regionRect != null) {
            try {
                String[] p = regionRect.split(",");
                if (p.length != 4) {
                    throw new IllegalArgumentException("Invalid -region format. Expected x,y,w,h but got: " + regionRect);
                }
                int rx = Integer.parseInt(p[0].trim());
                int ry = Integer.parseInt(p[1].trim());
                int rw = Integer.parseInt(p[2].trim());
                int rh = Integer.parseInt(p[3].trim());

                int ax = screenRegion.getX() + rx;
                int ay = screenRegion.getY() + ry;
                Region sub = new Region(ax, ay, rw, rh);
                context.setScreenRegion(sub);
                LOG.info(String.format("using region relative=%s -> absolute=%s", regionRect, sub));
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("Invalid -region numbers: " + regionRect);
            }
        }

        // no CLI hint; any prioritized search region will be computed by actions.

        // enumerate screens to help users choose the right monitor id
        try {
            int n = Screen.getNumberScreens();
            LOG.info("Detected screens: " + n);
            for (int i = 0; i < n; i++) {
                Region r = new Screen(i);
                LOG.info(String.format("screen[%d]=%s", i, r));
            }
            LOG.info(String.format("selected screenId=%d region=%s use_awt_robot=%s", screenId, context.getScreenRegion(), useAwtRobot));
        } catch (Throwable t) {
            // best-effort logging; do not fail if screen enumeration throws
            LOG.debug("Unable to enumerate screens: " + t.getMessage());
        }

        // set filter
        ExecutionFilter slideSelector = parseRange();
        if (slideSelector != null)
            context.setExecutionFilter(slideSelector);

        // set bookmark, which overrides filter if specified
        slideSelector = parseBookmark();
        if (slideSelector != null)
            context.setExecutionFilter(slideSelector);
        
        return context;
    }
    
    void configureLogging(String logLevelString){
        try {
            Level level = Level.toLevel(logLevelString, Level.INFO);
            Logger root = Logger.getRootLogger();

            // Ensure there is at least a console appender
            if (!root.getAllAppenders().hasMoreElements()) {
                PatternLayout layout = new PatternLayout("[%t] %-5p %c %m%n");
                root.addAppender(new ConsoleAppender(layout));
            }

            root.setLevel(level);
            // Align key categories with requested level
            Logger.getLogger("org.sikuli").setLevel(level);
            Logger.getLogger("org.sikuli.slides").setLevel(level);
            Logger.getLogger("org.sikuli.script").setLevel(level);

            System.out.println("Log4j configured: level=" + level);
        } catch (Throwable t) {
            System.err.println("Failed to configure Log4j: " + t.getMessage());
        }
    }
    void parseArgs(String... args) throws IllegalArgumentException {
        List<String> rest = null;
        rest = Args.parse(this, args);
        // If help flag provided, don't require input
        if (help) {
            helpRequested = true;
            return;
        }
        if (rest == null || rest.size() != 1) {
            //exit("Invalid syntax");
            throw new IllegalArgumentException("missing input");
        }
        context = parseContext();
        String input = rest.get(0);
        url = parseInputAsURL(input);
    }

    public void execute(String... args){
        try{
            parseArgs(args);
        }catch(IllegalArgumentException e){
            System.err.println("Error parsing arguments: " + e.getMessage());
            Args.usage(this, EXE + " " + SYNTAX);
            // ensure clean shutdown on argument errors
            try { LogManager.shutdown(); } catch (Throwable t) {}
            System.exit(2);
            return; // unreachable, but keeps compiler happy
        }

        if (helpRequested){
            Args.usage(this, EXE + " " + SYNTAX);
            // help path: exit success
            try { LogManager.shutdown(); } catch (Throwable t) {}
            System.exit(0);
            return;
        }
        
        try {
            Slides.execute(url, context);
        } catch (SlideExecutionException e) {
            System.err.println("Execution failed because " + e.getMessage());            
            if (e.getSlide() != null){
                System.err.print("On slide no. " + e.getSlide().getNumber());
                System.err.println(" Failed to execute " + e.getAction());
            }
            // make sure to terminate on failure (avoid lingering non-daemon threads)
            try { LogManager.shutdown(); } catch (Throwable t) {}
            System.exit(1);
            return;
        }
        // success path: terminate explicitly
        try { LogManager.shutdown(); } catch (Throwable t) {}
        System.exit(0);
    }

    public static void main(String... args) {
        // Ensure SikuliX native libs are available early (before any org.sikuli.* classes are initialized)
        ensureSikuliXLibs();
        // Ensure background thread failures cause a clean process exit
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            System.err.println("Uncaught exception in thread '" + t.getName() + "': " + e);
            e.printStackTrace(System.err);
            try { LogManager.shutdown(); } catch (Throwable ignored) {}
            System.exit(1);
        });
        ExecuteMain main = new ExecuteMain();		
        main.execute(args);
    }

    // Try to auto-set the SikuliX native libs folder on Windows when not provided via -Dsikulixlibs
    private static void ensureSikuliXLibs() {
        try {
            String propPath = System.getProperty("sikulixlibs");
            if (propPath != null) {
                // User provided path; still try to proactively load OpenCV to fail fast with a clearer message
                File f = new File(propPath);
                if (f.isDirectory()) {
                    File[] dlls = f.listFiles((dir, name) -> name.toLowerCase().startsWith("opencv_java") && name.toLowerCase().endsWith(".dll"));
                    if (dlls != null && dlls.length > 0) {
                        try {
                            File chosen = dlls[0];
                            System.out.println("Attempting to load OpenCV: " + chosen.getAbsolutePath());
                            System.load(chosen.getAbsolutePath());
                            System.out.println("Loaded OpenCV successfully: " + chosen.getName());
                        } catch (UnsatisfiedLinkError ule) {
                            System.err.println("Warning: Failed to pre-load OpenCV DLL from -Dsikulixlibs: " + ule.getMessage());
                        } catch (Throwable t2) {
                            System.err.println("Warning: Unexpected error while pre-loading OpenCV from -Dsikulixlibs: " + t2.getMessage());
                        }
                    } else {
                        System.err.println("Warning: No opencv_java*.dll found under -Dsikulixlibs=" + f.getAbsolutePath());
                    }
                } else {
                    System.err.println("Warning: -Dsikulixlibs path does not exist: " + f.getAbsolutePath());
                }
                return; // user provided
            }
            String os = System.getProperty("os.name", "").toLowerCase();
            if (!os.contains("win")) {
                return; // only intervene on Windows
            }
            File base = getJarDir();
            String[] candidates = new String[] {
                "sikulixlibs\\windows\\libs",
                "sikulixlibs\\win\\libs",
                "sikulixlibs\\windows\\x86_64",
                "sikulixlibs\\windows"
            };
            for (String rel : candidates) {
                File f = new File(base, rel);
                if (f.isDirectory()) {
                    // Basic sanity: see if there is any opencv dll inside
                    File[] dlls = f.listFiles((dir, name) -> name.toLowerCase().startsWith("opencv_java") && name.toLowerCase().endsWith(".dll"));
                    if (dlls != null && dlls.length > 0) {
                        System.setProperty("sikulixlibs", f.getAbsolutePath());
                        System.out.println("SikuliX libs path set to: " + f.getAbsolutePath());
                        // Proactively try to load the first matching OpenCV DLL to avoid classloader resolution issues
                        try {
                            File chosen = dlls[0];
                            System.out.println("Attempting to load OpenCV: " + chosen.getAbsolutePath());
                            System.load(chosen.getAbsolutePath());
                            System.out.println("Loaded OpenCV successfully: " + chosen.getName());
                        } catch (UnsatisfiedLinkError ule) {
                            System.err.println("Warning: Failed to pre-load OpenCV DLL: " + ule.getMessage());
                        } catch (Throwable t2) {
                            System.err.println("Warning: Unexpected error while pre-loading OpenCV: " + t2.getMessage());
                        }
                        break;
                    }
                }
            }
        } catch (Throwable t) {
            // best effort; do not fail
        }
    }

    private static File getJarDir() {
        try {
            java.net.URL url = ExecuteMain.class.getProtectionDomain().getCodeSource().getLocation();
            java.io.File loc = new java.io.File(url.toURI());
            return loc.isFile() ? loc.getParentFile() : loc;
        } catch (Throwable t) {
            return new java.io.File(".");
        }
    }

    private static URL parseInputAsURL(String input) {
        URL webUrl = null;
        URL fileUrl = null;
        try {
            webUrl = new URL(input);
        } catch (MalformedURLException e1) {
        }
        try {
            fileUrl = (new File(input)).toURI().toURL();
        } catch (MalformedURLException e1) {
        }
        if (webUrl == null && fileUrl == null) {
            throw new IllegalArgumentException("Not a valid input file: " + input);
        }
        return Objects.firstNonNull(webUrl, fileUrl);
    }
}
