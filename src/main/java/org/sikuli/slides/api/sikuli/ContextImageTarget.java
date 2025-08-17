package org.sikuli.slides.api.sikuli;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;

public class ContextImageTarget {

	static Logger logger = LoggerFactory.getLogger(ContextImageTarget.class);
	
	private URL contextImageURL;
	private BufferedImage contextImage;

	private double xmin;
	private double xmax;
	private double ymin;
	private double ymax;

	private boolean isPixels =false;

	private int x;
	private int y;
	private int width;
	private int height;

	public ContextImageTarget(URL contextImageURL, double xmin, double ymin, double xmax, double ymax){
		this.contextImageURL = contextImageURL;
		this.xmin = xmin;
		this.xmax = xmax;
		this.ymin = ymin;
		this.ymax = ymax;
		this.isPixels = false;
	}		

	public ContextImageTarget(URL contextImageURL, int x, int y, int width, int height){
		this.contextImageURL = contextImageURL;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.isPixels = true;
	}

	public Rectangle getTargetBounds(){
		BufferedImage image = getContextImage();		
		Rectangle r = new Rectangle();
		if (isPixels){
			r.x = Math.max(0,x);
			r.y = Math.max(0,y);
			r.width = Math.min(width + x, image.getWidth()) - x;
			r.height = Math.min(height + y, image.getHeight()) - y;
		}else{			
			r.x = (int) Math.round(xmin * image.getWidth());
			r.y = (int) Math.round(ymin * image.getHeight());
			r.width = (int) ((xmax - xmin) * image.getWidth());
			r.height = (int) ((ymax - ymin) * image.getHeight());
		}
		return r;
	}
	
	public BufferedImage getTargetImage(){
		Rectangle bounds = getTargetBounds();
		BufferedImage image = getContextImage();
		return image.getSubimage(bounds.x, bounds.y, bounds.width, bounds.height);
	}
	
	public org.sikuli.script.Pattern getTargetPattern(){
		BufferedImage targetImage = getTargetImage();
		org.sikuli.script.Image img = new org.sikuli.script.Image(targetImage);
		return new org.sikuli.script.Pattern(img);
	}

	public BufferedImage getContextImage() {
		if (contextImage == null){
			try {
				contextImage = ImageIO.read(contextImageURL);
			} catch (IOException e) {
			}
		}
		return contextImage;
	}

	public String toString(){
		if (isPixels){
			return Objects.toStringHelper(this)
				.add("image", contextImageURL)
				.add("x", x)
				.add("y", y)
				.add("width", width)
				.add("height", height)
				.toString();
			}else{
		return Objects.toStringHelper(this)
				.add("image", contextImageURL)
				.add("x", String.format("(%.2f,%.2f)", xmin, xmax))
				.add("y", String.format("(%.2f,%.2f)", ymin, ymax))
				.toString();
			}
	}
}
