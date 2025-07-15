package org.mozilla.gecko.gfx;

import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import org.libreoffice.callback.EventCallback;

public interface InputConnectionHandler
{
    InputConnection onCreateInputConnection(EditorInfo outAttrs);
    boolean onKeyPreIme(int keyCode, KeyEvent event);
    boolean onKeyDown(EventCallback callback, int keyCode, KeyEvent event);
    boolean onKeyLongPress(int keyCode, KeyEvent event);
    boolean onKeyMultiple(EventCallback callback, int keyCode, int repeatCount, KeyEvent event);
    boolean onKeyUp(EventCallback callback, int keyCode, KeyEvent event);
}
