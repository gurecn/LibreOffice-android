package org.libreoffice.callback;

import org.libreoffice.data.LOEvent;

public interface EventCallback {
    void queueEvent(LOEvent event);
}
