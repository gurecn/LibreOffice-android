package org.libreoffice.utils;

import android.util.DisplayMetrics;
import org.libreoffice.application.TheApplication;

public class DeviceUtils {
    public static float getDpi() {
        DisplayMetrics metrics = TheApplication.getContext().getResources().getDisplayMetrics();
        return metrics.density * 160;
    }

}
