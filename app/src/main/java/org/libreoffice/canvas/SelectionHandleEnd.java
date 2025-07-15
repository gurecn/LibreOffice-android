package org.libreoffice.canvas;

import org.libreoffice.application.TheApplication;
import org.libreoffice.callback.EventCallback;
import org.libreoffice.R;
import org.mozilla.gecko.gfx.GeckoLayerClient;

/**
 * Selection handle for showing and manipulating the end of a selection.
 */
public class SelectionHandleEnd extends SelectionHandle {
    public SelectionHandleEnd(GeckoLayerClient layerClient, EventCallback callback) {
        super(layerClient, callback, getBitmapForDrawable(TheApplication.getContext(), R.drawable.handle_alias_end));
    }

    /**
     * Define the type of the handle.
     */
    @Override
    public HandleType getHandleType() {
        return HandleType.END;
    }
}
