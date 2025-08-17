package org.sikuli.slides.api.interpreters;

import java.util.List;

import org.sikuli.script.Pattern;
import org.sikuli.script.Region;
import org.sikuli.slides.api.actions.Action;
import org.sikuli.slides.api.models.Slide;
import org.sikuli.slides.api.models.SlideElement;

public interface Interpreter {
	Action interpret(Slide slide);
}

interface TargetInterpreter {
	Pattern interpret(Slide slide);
}


interface SpatialRelationshipInterpreter {
	SpatialRelationship interpret(Slide slide, SlideElement element);
}