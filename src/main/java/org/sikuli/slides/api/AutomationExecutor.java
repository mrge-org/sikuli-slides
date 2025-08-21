package org.sikuli.slides.api;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import org.sikuli.slides.api.actions.Action;
import org.sikuli.slides.api.actions.ActionExecutionException;
import org.sikuli.slides.api.actions.Actions;
import org.sikuli.slides.api.actions.TargetAction;
import org.sikuli.slides.api.interpreters.DefaultInterpreter;
import org.sikuli.slides.api.interpreters.Interpreter;
import org.sikuli.slides.api.models.Slide;
import org.sikuli.script.Region;
import org.sikuli.script.ScreenImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

class AutomationExecutor implements SlidesExecutor {
	
	static Logger logger = LoggerFactory.getLogger(AutomationExecutor.class);
	
	private Context context;

	public AutomationExecutor(Context context){
		this.context = context;
	}
	
	public AutomationExecutor(){
		context = new Context();
	}

	@Override
	public void execute(List<Slide> slides) throws SlideExecutionException {
				
		logger.trace("Execute {} slide(s)", slides.size());
		
		int executedCount = 0;
        
        for (int i = 0; i < slides.size(); ++i){
            
            Slide slide = slides.get(i);
            ExecutionEvent event = new ExecutionEvent(slide ,context);
            
            
            if (!context.getExecutionFilter().accept(event)){
                logger.info("Slide {} of {} is skipped", slide.getNumber(), slides.size());                
                continue;                            
            }else{ 
                logger.info("Execute slide {} of {}", slide.getNumber(), slides.size());                
                executedCount++;
                boolean isFirstExecutedSlide = (executedCount == 1);
                try {
                    // make the slide available in Context so actions can name artifacts
                    context.setSlide(slide);
                    if (isFirstExecutedSlide) {
                        context.addParameter("firstExecutedSlide", Boolean.TRUE);
                    }
                    slide.execute(context);
                } catch (ActionExecutionException e) {
                    SlideExecutionException ex = new SlideExecutionException(e);
                    ex.setAction(e.getAction());
                    ex.setSlide(slide);
                    if (isFirstExecutedSlide && e.getAction() instanceof TargetAction){
                        logWorkingDirectory();
                        saveFailedPatternImage((TargetAction) e.getAction(), slide.getNumber());
                        saveFailedSearchRegionImage(context, slide.getNumber());
                    }
                    throw ex;
                } finally {
                    if (isFirstExecutedSlide) {
                        // clear the flag for subsequent slides
                        context.addParameter("firstExecutedSlide", Boolean.FALSE);
                    }
                }
            }
        }
    }

    private void saveFailedPatternImage(TargetAction action, int slideNumber){
        try {
            org.sikuli.script.Image img = action.getPattern().getImage();
            if (img == null){
                logger.info("Pattern image is null; nothing to save for slide {}", slideNumber);
                return;
            }
            BufferedImage bi = img.get();
            if (bi == null){
                logger.info("Pattern image buffer is null; nothing to save for slide {}", slideNumber);
                return;
            }
            File dir = new File("target/failed-patterns");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File out = new File(dir, String.format("slide-%d-pattern.png", slideNumber));
            ImageIO.write(bi, "png", out);
            logger.info("Saved failed pattern image to {}", out.getAbsolutePath());
        } catch (IOException | RuntimeException t) {
            logger.warn("Could not save failed pattern image for slide {}", slideNumber, t);
        }
    }

    private void saveFailedSearchRegionImage(Context ctx, int slideNumber){
        try {
            Region r = ctx.getScreenRegion();
            if (r == null) return;
            ScreenImage si = r.getScreen().capture(r);
            if (si == null || si.getImage() == null){
                logger.info("Search region capture returned null for slide {}", slideNumber);
                return;
            }
            File dir = new File("target/failed-search");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File out = new File(dir, String.format("slide-%d-region.png", slideNumber));
            ImageIO.write(si.getImage(), "png", out);
            logger.info("Saved failed search region image to {}", out.getAbsolutePath());
        } catch (IOException | RuntimeException t) {
            logger.warn("Could not save failed search region image for slide {}", slideNumber, t);
        }
    }

    private void logWorkingDirectory(){
        try {
            String cwd = new File(".").getCanonicalPath();
            logger.info("Current working directory: {}", cwd);
        } catch (IOException ignored) {
        }
    }

}
