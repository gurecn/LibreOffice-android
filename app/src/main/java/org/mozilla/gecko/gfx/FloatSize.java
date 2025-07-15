package org.mozilla.gecko.gfx;

import org.mozilla.gecko.util.FloatUtils;

public class FloatSize {
    public final float width, height;

    public FloatSize(float aWidth, float aHeight) { width = aWidth; height = aHeight; }

    @Override
    public String toString() { return "(" + width + "," + height + ")"; }

    public boolean fuzzyEquals(FloatSize size) {
        return (FloatUtils.fuzzyEquals(size.width, width) && FloatUtils.fuzzyEquals(size.height, height));
    }

    public FloatSize scale(float factor) {
        return new FloatSize(width * factor, height * factor);
    }

}

