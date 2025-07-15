package org.mozilla.gecko.gfx;

import android.graphics.RectF;
import org.mozilla.gecko.util.FloatUtils;

public final class DisplayPortMetrics {
    private final RectF mPosition;
    private final float mResolution;

    public RectF getPosition() {
        return mPosition;
    }

    public float getResolution() {
        return mResolution;
    }

    public DisplayPortMetrics() {
        this(0, 0, 0, 0, 1);
    }

    public DisplayPortMetrics(float left, float top, float right, float bottom, float resolution) {
        mPosition = new RectF(left, top, right, bottom);
        mResolution = resolution;
    }

    public boolean contains(RectF rect) {
        return mPosition.contains(rect);
    }

    public boolean fuzzyEquals(DisplayPortMetrics metrics) {
        return RectUtils.fuzzyEquals(mPosition, metrics.mPosition)
            && FloatUtils.fuzzyEquals(mResolution, metrics.mResolution);
    }

    @Override
    public String toString() {
        return "DisplayPortMetrics v=(" + mPosition.left + "," + mPosition.top + "," + mPosition.right + "," + mPosition.bottom + ") z=" + mResolution;
    }
}
