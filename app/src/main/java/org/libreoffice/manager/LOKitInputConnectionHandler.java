package org.libreoffice.manager;

import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import org.libreoffice.callback.EventCallback;
import org.libreoffice.data.LOEvent;
import org.mozilla.gecko.gfx.InputConnectionHandler;

/**
 * Implementation of InputConnectionHandler. When a key event happens it is
 * directed to this class which is then directed further to LOKitThread.
 */
public class LOKitInputConnectionHandler implements InputConnectionHandler {

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        return null;
    }

    /**
     * When key pre-Ime happens.
     */
    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        return false;
    }

    /**
     * When key down event happens.
     */
    @Override
    public boolean onKeyDown(EventCallback callback, int keyCode, KeyEvent event) {
        if(callback != null)callback.queueEvent(new LOEvent(LOEvent.KEY_EVENT, event));
        return false;
    }

    /**
     * When key long press event happens.
     */
    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return false;
    }

    /**
     * When key multiple event happens. Key multiple event is triggered when
     * non-ascii characters are entered on soft keyboard.
     */
    @Override
    public boolean onKeyMultiple(EventCallback callback, int keyCode, int repeatCount, KeyEvent event) {
        if(callback != null)callback.queueEvent(new LOEvent(LOEvent.KEY_EVENT, event));
        return false;
    }

    /**
     * When key up event happens.
     */
    @Override
    public boolean onKeyUp(EventCallback callback,int keyCode, KeyEvent event) {
        if(callback != null)callback.queueEvent(new LOEvent(LOEvent.KEY_EVENT, event));
        return false;
    }
}


