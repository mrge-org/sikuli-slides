package org.sikuli.recorder;

public class CaptureContext {
    public enum Backend {
        sikuli,
        awt_raw,
        both
    }

    private volatile Backend backend = Backend.sikuli;
    private volatile double scale = 1.0; // imagePixels / roiPixels

    public Backend getBackend() {
        return backend;
    }

    public void setBackend(Backend backend) {
        if (backend != null) this.backend = backend;
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }
}
