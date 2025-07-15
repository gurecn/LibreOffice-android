package org.mozilla.gecko.gfx;

import android.content.Context;
import android.graphics.RectF;

import org.libreoffice.callback.EventCallback;

public class DynamicTileLayer extends ComposedTileLayer {
    public DynamicTileLayer(Context context, EventCallback callback) {
        super(context, callback);
    }

    @Override
    protected RectF getViewPort(ImmutableViewportMetrics viewportMetrics) {
        RectF rect = viewportMetrics.getViewport();
        return inflate(roundToTileSize(rect, tileSize), getInflateFactor());
    }

    @Override
    protected float getZoom(ImmutableViewportMetrics viewportMetrics) {
        return viewportMetrics.zoomFactor;
    }

    @Override
    protected int getTilePriority() {
        return 0;
    }

    private IntSize getInflateFactor() {
        return new IntSize(tileSize.width*2, tileSize.height*4);
    }
}
