package org.sikuli.slides.apps;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.sikuli.script.Screen;
import org.sikuli.script.Region;
import org.sikuli.script.support.Commons;
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
import org.apache.log4j.RollingFileAppender;

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
            // Ensure we don't accumulate multiple console/file appenders
            LogManager.resetConfiguration();
            Logger root = Logger.getRootLogger();

            // Always attach our console appender (in addition to any existing ones)
            PatternLayout consoleLayout = new PatternLayout("%d{HH:mm:ss.SSS} [%t] %-5p %c - %m%n");
            root.addAppender(new ConsoleAppender(consoleLayout));

            // Attach a rolling file appender if slides.logfile is provided
            String logPath = System.getProperty("slides.logfile");
            boolean teeActive = Boolean.parseBoolean(System.getProperty("slides.tee.active", "false"));
            if (logPath != null && !logPath.isEmpty() && !teeActive) {
                try {
                    // Match user's preferred time/thread-only prefix
                    PatternLayout fileLayout = new PatternLayout("%d{HH:mm:ss.SSS} [%t] %-5p %c - %m%n");
                    RollingFileAppender rfa = new RollingFileAppender(fileLayout, logPath, true);
                    rfa.setMaxBackupIndex(3);
                    rfa.setMaxFileSize("5MB"); // Log4j 1.x API
                    rfa.activateOptions();
                    root.addAppender(rfa);
                } catch (Throwable t) {
                    // fall back silently; console will still work
                    LOG.warn("Failed to attach RollingFileAppender: " + t.getMessage());
                }
            }

            root.setLevel(level);
            // Align key categories with requested level
            Logger.getLogger("org.sikuli").setLevel(level);
            Logger.getLogger("org.sikuli.slides").setLevel(level);
            Logger.getLogger("org.sikuli.script").setLevel(level);
            // Intentionally do not log a 'configured' banner to avoid duplicates when reconfiguring later
        } catch (Throwable t) {
            LOG.warn("Failed to configure Log4j: " + t.getMessage());
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
            LOG.error("Error parsing arguments: " + e.getMessage());
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

        // Rename early-created run log to include the input base name once we know it
        try {
            String currentLog = System.getProperty("slides.logfile");
            if (currentLog != null && !currentLog.isEmpty() && url != null) {
                java.io.File cur = new java.io.File(currentLog);
                String ts = null;
                // Try to extract timestamp from existing name: sikuli-slides-run-YYYYMMDD_HHmmss.log
                String name = cur.getName();
                java.util.regex.Matcher m = java.util.regex.Pattern.compile(".*?(\\d{8}_\\d{6})\\.log$").matcher(name);
                if (m.find()) {
                    ts = m.group(1);
                } else {
                    // fallback to now
                    ts = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
                }
                // Derive <pptx name> from URL path
                String base = new java.io.File(url.getPath()).getName();
                int dot = base.lastIndexOf('.');
                if (dot > 0) base = base.substring(0, dot);
                java.io.File dir = cur.getParentFile() != null ? cur.getParentFile() : new java.io.File(".");
                java.io.File target = new java.io.File(dir, "sikuli-slides-" + base + "-" + ts + ".log");
                if (!target.equals(cur)) {
                    boolean ok = cur.renameTo(target);
                    if (ok) {
                        System.setProperty("slides.logfile", target.getAbsolutePath());
                        LOG.info("Run log renamed to: " + target.getName());
                    } else {
                        LOG.debug("Could not rename run log to final name: " + target.getAbsolutePath());
                    }
                }
            }
        } catch (Throwable t) {
            LOG.debug("run log rename skipped: " + t.getMessage());
        }

        // Single run banner and consolidated termination
        LOG.info("\n\n         DO NOT USE THE KEYBOARD OR MOUSE UNTIL TEST EXECUTION ENDS!\n\n");
        // Announce NCC-only single-scale mode once per run
        LOG.info("single-scale mode: using NCC-only search (no SikuliX find/wait)");

        int exitCode = 0;
        Integer failSlideNum = null;
        String failAction = null;
        try {
            Slides.execute(url, context);
        } catch (SlideExecutionException e) {
            exitCode = 1;
            LOG.error("Execution failed because " + e.getMessage());            
            if (e.getSlide() != null){
                LOG.error(String.format("On slide no. %d Failed to execute %s", e.getSlide().getNumber(), e.getAction()));
                failSlideNum = e.getSlide().getNumber();
                failAction = String.valueOf(e.getAction());
            }
        } finally {
            try {
                LOG.info("\n\nYou may now use the keyboard and mouse. Test execution finished.\n\n");
                // One-line summary for post-mortem grepability
                if (exitCode == 0) {
                    LOG.info("SUMMARY: Test passed");
                } else {
                    if (failSlideNum != null && failAction != null) {
                        LOG.info(String.format("SUMMARY: Test failed on slide %d: %s", failSlideNum, failAction));
                    } else if (failSlideNum != null) {
                        LOG.info(String.format("SUMMARY: Test failed on slide %d", failSlideNum));
                    } else {
                        LOG.info("SUMMARY: Test failed");
                    }
                }
            } catch (Throwable t) {
                // ignore console issues
            }
            try { LogManager.shutdown(); } catch (Throwable t) {}
            System.exit(exitCode);
        }
    }

    public static void main(String... args) {
        // Create a timestamped log file in the current working directory and tee stdout/stderr to it
        try {
            final String ts = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
            final String fname = "sikuli-slides-run-" + ts + ".log";
            final java.io.File dir = new java.io.File(System.getProperty("user.dir", "."));
            final java.io.File logFile = new java.io.File(dir, fname);

            final java.io.PrintStream origOut = System.out;
            final java.io.PrintStream origErr = System.err;
            final java.io.FileOutputStream fos = new java.io.FileOutputStream(logFile, true);

            System.setOut(new java.io.PrintStream(new TeeOutputStream(origOut, fos), true));
            System.setErr(new java.io.PrintStream(new TeeOutputStream(origErr, fos), true));
            // let Log4j know about the file path; configureLogging() will attach a file appender
            System.setProperty("slides.logfile", logFile.getAbsolutePath());
            // mark tee as active; configureLogging() will avoid attaching a file appender to prevent duplicate lines
            System.setProperty("slides.tee.active", "true");
            // Immediately configure logging so any early logs (e.g., SikuliX libs) use the preferred pattern.
            new ExecuteMain().configureLogging("INFO");
            Logger.getLogger(ExecuteMain.class).info("Console is being logged to: " + logFile.getAbsolutePath());
        } catch (Throwable t) {
            // best-effort; do not fail if logging to file cannot be established
        }
        // Ensure SikuliX native libs are available early (before any org.sikuli.* classes are initialized)
        ensureSikuliXLibs();
        // Ensure background thread failures cause a clean process exit
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            Logger logger = Logger.getLogger(ExecuteMain.class);
            logger.error("Uncaught exception in thread '" + t.getName() + "'", e);
            try { LogManager.shutdown(); } catch (Throwable ignored) {}
            System.exit(1);
        });
        ExecuteMain main = new ExecuteMain();
        main.execute(args);
    }

    // Simple tee that writes to two output streams
    private static final class TeeOutputStream extends java.io.OutputStream {
        private final java.io.OutputStream a;
        private final java.io.OutputStream b;
        TeeOutputStream(java.io.OutputStream a, java.io.OutputStream b) { this.a = a; this.b = b; }
        @Override public void write(int i) throws java.io.IOException { a.write(i); b.write(i); }
        @Override public void write(byte[] buf) throws java.io.IOException { a.write(buf); b.write(buf); }
        @Override public void write(byte[] buf, int off, int len) throws java.io.IOException { a.write(buf, off, len); b.write(buf, off, len); }
        @Override public void flush() throws java.io.IOException { a.flush(); b.flush(); }
        @Override public void close() throws java.io.IOException { try { a.close(); } finally { b.close(); } }
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

    /**
     * Best-effort setup to ensure SikuliX native libraries are discoverable.
     *
     * Strategy:
     * - If the user already configured a libs path via env or system properties, keep it.
     * - Otherwise, probe common repo-local locations (sikulixlibs/mac|macm1/libs) relative to
     *   the working directory and the jar directory, then set the system properties.
     * - Finally, trigger SikuliX init to extract/validate libs folder and print its path.
     */
    private static void ensureSikuliXLibs() {
        try {
            // Respect pre-configured settings first
            String sysProp = System.getProperty("sikulixlibs");
            String envVar = System.getenv("SIKULIX_LIBS");
            if (sysProp != null && !sysProp.isEmpty()) {
                Logger.getLogger(ExecuteMain.class).info("sikulixlibs system property preset: " + sysProp);
            }
            if (envVar != null && !envVar.isEmpty()) {
                Logger.getLogger(ExecuteMain.class).info("SIKULIX_LIBS env preset: " + envVar);
            }

            if ((sysProp == null || sysProp.isEmpty()) && (envVar == null || envVar.isEmpty())) {
                // Determine arch-specific subfolder on macOS
                String arch = System.getProperty("os.arch", "");
                boolean isArm = arch.contains("aarch64") || arch.contains("arm64");
                String[] candidates = new String[] {
                    // relative to working dir
                    isArm ? "sikulixlibs/macm1/libs" : "sikulixlibs/mac/libs",
                    // relative to jar dir
                    new File(getJarDir(), isArm ? "../sikulixlibs/macm1/libs" : "../sikulixlibs/mac/libs").getPath()
                };
                for (String c : candidates) {
                    File f = new File(c);
                    try { f = f.getCanonicalFile(); } catch (Exception ignore) {}
                    if (f.isDirectory()) {
                        System.setProperty("sikulixlibs", f.getAbsolutePath());
                        System.setProperty("SIKULIX_LIBS", f.getAbsolutePath());
                        Logger.getLogger(ExecuteMain.class).info("Configured SikuliX libs: " + f.getAbsolutePath());
                        break;
                    }
                }
            }

            // Touch SikuliX Commons to force initialization and report the effective folder
            try {
                Object libs = Commons.getLibsFolder();
                if (libs != null) {
                    if (libs instanceof java.io.File) {
                        Logger.getLogger(ExecuteMain.class).info("SikuliX libs folder resolved: " + ((java.io.File) libs).getAbsolutePath());
                    } else {
                        Logger.getLogger(ExecuteMain.class).info("SikuliX libs folder resolved: " + libs.toString());
                    }
                }
            } catch (Throwable t) {
                Logger.getLogger(ExecuteMain.class).info("SikuliX Commons init warning: " + t.getMessage());
            }
        } catch (Throwable t) {
            // Do not fail hard; fallback will be SikuliX's own extraction logic
            Logger.getLogger(ExecuteMain.class).info("ensureSikuliXLibs() non-fatal: " + t.getMessage());
        }
    }
}
