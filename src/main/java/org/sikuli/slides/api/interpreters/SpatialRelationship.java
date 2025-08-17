package org.sikuli.slides.api.interpreters;

import org.sikuli.script.Region;
import org.sikuli.slides.api.Context;

public interface SpatialRelationship {
	Region apply(Context input);
}