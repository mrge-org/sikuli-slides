package org.sikuli.slides.api.sikuli;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import org.sikuli.script.Image;
import org.sikuli.script.Pattern;
import org.sikuli.script.Region;

import com.google.common.base.Objects;

class ContextTargetHypothesis implements Hypothesis {      
    public ContextTargetHypothesis(BufferedImage contextImage, Rectangle originalTargetRect, Rectangle contextTargetRect) {
        super();
        this.contextImage = contextImage;
        this.originalTargetRect = originalTargetRect;
        this.contextTargetRect = contextTargetRect;
    }

    final BufferedImage contextImage;
    final Rectangle originalTargetRect;
    final Rectangle contextTargetRect;
    
    @Override
    public Pattern getPattern(){
        Rectangle r = contextTargetRect;
        BufferedImage targetImage = contextImage.getSubimage(r.x, r.y, r.width, r.height);
        Image img = new Image(targetImage);
        return new Pattern(img);
    }
    
    @Override
    public Region interpretResult(Region rawResult){
        int xoffset = originalTargetRect.x - contextTargetRect.x;
        int yoffset = originalTargetRect.y - contextTargetRect.y;
        return new Region(rawResult.getX() + xoffset, rawResult.getY() + yoffset,
                originalTargetRect.width, originalTargetRect.height);
    }
    
    public String toString(){
        Rectangle r = contextTargetRect;
        return Objects.toStringHelper(this).add("rect", String.format("%d,%d,%d,%d", r.x, r.y, r.width, r.height)).toString();
    }
}