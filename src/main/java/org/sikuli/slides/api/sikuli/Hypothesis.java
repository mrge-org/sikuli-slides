package org.sikuli.slides.api.sikuli;

import org.sikuli.script.Pattern;
import org.sikuli.script.Region;

public interface Hypothesis {
    Pattern getPattern();
    Region interpretResult(Region rawResult);
}