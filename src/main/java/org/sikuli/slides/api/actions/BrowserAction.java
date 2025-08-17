package org.sikuli.slides.api.actions;

// import static org.sikuli.api.API.browse; // Legacy API removed

import java.net.URL;

import org.sikuli.slides.api.Context;

import com.google.common.base.Objects;

public class BrowserAction extends RobotAction {
	
	private URL url;

	@Override
	protected void doExecute(Context context) throws ActionExecutionException {
		// Use Java Desktop API instead of legacy sikuli browse
		try {
			if (java.awt.Desktop.isDesktopSupported()) {
				java.awt.Desktop.getDesktop().browse(url.toURI());
			} else {
				throw new ActionExecutionException("Desktop browsing not supported", this);
			}
		} catch (Exception e) {
			throw new ActionExecutionException("Failed to open browser: " + e.getMessage(), this);
		}
	}

	public URL getUrl() {
		return url;
	}

	public void setUrl(URL url) {
		this.url = url;
	}
	
	public String toString(){
		return Objects.toStringHelper(this).add("url",url).toString();
	}
}
