package org.sikuli.slides.api.actions;

import org.sikuli.script.Region;
import org.sikuli.slides.api.Context;

import com.google.common.base.Objects;

// Execute a target action on a new screen region relative to
// the screen region given in the context. It can be used in conjunction
// with a TargetAction to execute an action not directly on a target
// but in another area relative to the target.
public class RelativeAction extends ChainedAction {
	
	private int x = 0;
	private int y = 0;
	private int width = 0;
	private int height = 0;	
	
	private double xmin = 0;
	private double xmax = 1;
	private double ymin = 0;
	private double ymax = 1;
	
	private boolean isPixelUnit;
	
	public RelativeAction(int x, int y, int width, int height, Action targetAction){
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.isPixelUnit = true;
		setChild(targetAction);
	}
	
	public RelativeAction(double xmin, double ymin, double xmax, double ymax, Action targetAction){
		this.xmin = xmin;
		this.xmax = xmax;
		this.ymin = ymin;
		this.ymax = ymax;
		this.isPixelUnit = false;
		setChild(targetAction);
	}
	
	@Override
	public void execute(Context context) throws ActionExecutionException {
		Region base = context.getScreenRegion();
		
		Region targetRegion;
		if (isPixelUnit){
			int tx = base.getX() + x;
			int ty = base.getY() + y;
			targetRegion = new Region(tx, ty, width, height);
		}else{
			int bw = base.getW();
			int bh = base.getH();
			int tx = base.getX() + (int) Math.round(xmin * bw);
			int ty = base.getY() + (int) Math.round(ymin * bh);
			int tw = (int) Math.round((xmax - xmin) * bw);
			int th = (int) Math.round((ymax - ymin) * bh);
			targetRegion = new Region(tx, ty, tw, th);
		}
					
		Action child = getChild();
		if (child != null){
			Context childContext = new Context(context, targetRegion);
			child.execute(childContext);
		}			
	}
	
	public String toString(){
		if (isPixelUnit){
		return Objects.toStringHelper(this)
				.add("x", x)
				.add("y", y)
				.add("width", width)
				.add("height", height)
				.add("child", getChild())
				.toString();
		}else{
			return Objects.toStringHelper(this)
					.add("x", String.format("(%.2f,%.2f)", xmin, xmax))
					.add("y", String.format("(%.2f,%.2f)", ymin, ymax))
					.add("child", getChild())
					.toString();			
		}
	}

	public double getMinX() {
		return xmin;
	}
	
	public double getMaxX() {
		return xmax;
	}

	public void setMinX(double minX) {
		this.xmin = minX;
	}
}