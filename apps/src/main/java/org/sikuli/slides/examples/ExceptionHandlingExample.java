package org.sikuli.slides.examples;

import org.sikuli.slides.api.SlideExecutionException;
import org.sikuli.slides.api.Slides;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExceptionHandlingExample {
    private static final Logger logger = LoggerFactory.getLogger(ExceptionHandlingExample.class);

    public static void main(String[] arg) {
        try {
            Slides.execute(Resources.fail_pptx);
        }catch (SlideExecutionException e) {
            logger.error("Failed to execute slide no. {}", e.getSlide().getNumber());
            logger.error(" because {}", e.getMessage());
            logger.error(" action: {}", e.getAction());
        } 
    }
}
