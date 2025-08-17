package org.sikuli.recorder.detector;

import org.sikuli.recorder.DefaultEventWriter;
import org.sikuli.recorder.EventWriter;
import org.sikuli.recorder.event.Event;
import org.sikuli.script.Region;
import org.sikuli.script.Screen;

public class EventDetector {
    private EventWriter writer;
    private Region regionOfInterest;

    public EventDetector(){
        writer = new DefaultEventWriter();
        // Full primary screen as default region of interest
        regionOfInterest = new Screen();
    }

    public void eventDetected(Event event){
        if (writer != null)
            writer.write(event);
    }

    public void start(){
    }

    public void stop(){
    }

    public void setWriter(EventWriter writer) {
        this.writer = writer;
    }

    // set the screen region to detect events
    // events occurring outside the region should be discarded
    public void setRegionOfInterest(Region region){
        regionOfInterest = region;
    }

    public Region getRegionOfInterest(){
        return regionOfInterest;
    }
}