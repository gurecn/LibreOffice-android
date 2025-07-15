package org.libreoffice.canvas;

import android.graphics.Bitmap;
import android.graphics.PointF;
import org.libreoffice.callback.EventCallback;
import org.libreoffice.data.LOEvent;
import org.mozilla.gecko.gfx.GeckoLayerClient;
import org.mozilla.gecko.gfx.ImmutableViewportMetrics;

/**
 * Selection handle is a common class for "start", "middle" and "end" types
 * of selection handles.
 */
public abstract class SelectionHandle extends BitmapHandle {
    private static final long MINIMUM_HANDLE_UPDATE_TIME = 50 * 1000000;

    private final PointF mDragStartPoint = new PointF();
    private final PointF mDragDocumentPosition = new PointF();
    private long mLastTime = 0;

    private final GeckoLayerClient mLayerClient;
    private final EventCallback mCallback;

    public SelectionHandle(GeckoLayerClient layerClient, EventCallback callback, Bitmap bitmap) {
        super(bitmap);
        mLayerClient = layerClient;
        mCallback = callback;
    }

    /**
     * Start of a touch and drag action on the handle.
     */
    public void dragStart(PointF point) {
        mDragStartPoint.x = point.x;
        mDragStartPoint.y = point.y;
        mDragDocumentPosition.x = mDocumentPosition.left;
        mDragDocumentPosition.y = mDocumentPosition.top;
    }

    /**
     * End of a touch and drag action on the handle.
     */
    public void dragEnd(PointF point) {
    }

    /**
     * Handle has been dragged.
     */
    public void dragging(PointF point) {
        long currentTime = System.nanoTime();
        if (currentTime - mLastTime > MINIMUM_HANDLE_UPDATE_TIME) {
            mLastTime = currentTime;
            signalHandleMove(point.x, point.y);
        }
    }

    /**
     * Signal to move the handle to a new position to LO.
     */
    private void signalHandleMove(float newX, float newY) {
        ImmutableViewportMetrics viewportMetrics = mLayerClient.getViewportMetrics();
        float zoom = viewportMetrics.zoomFactor;

        float deltaX = (newX - mDragStartPoint.x) / zoom;
        float deltaY = (newY - mDragStartPoint.y) / zoom;

        PointF documentPoint = new PointF(mDragDocumentPosition.x + deltaX, mDragDocumentPosition.y + deltaY);
        if(mCallback != null)mCallback.queueEvent(new LOEvent(LOEvent.CHANGE_HANDLE_POSITION, getHandleType(), documentPoint));
    }

    public abstract HandleType getHandleType();

    public enum HandleType { START, MIDDLE, END }
}
