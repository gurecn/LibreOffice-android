package org.libreoffice.overlay;

import android.graphics.RectF;
import android.util.Log;

import org.libreoffice.application.TheApplication;
import org.libreoffice.callback.EventCallback;
import org.libreoffice.ui.MainActivity;
import org.libreoffice.R;
import org.libreoffice.canvas.SelectionHandle;
import org.mozilla.gecko.gfx.Layer;
import org.mozilla.gecko.gfx.LayerView;
import org.mozilla.gecko.util.FloatUtils;
import java.util.List;

/**
 * The DocumentOverlay is an overlay over the document. This class is responsible
 * to setup the document overlay view, report visibility and position of its elements
 * when they change and report any changes to the viewport.
 */
public class DocumentOverlay {
    private static final String LOGTAG = DocumentOverlay.class.getSimpleName();

    private final DocumentOverlayView mDocumentOverlayView;
    private final DocumentOverlayLayer mDocumentOverlayLayer;

    private final long hidePageNumberRectDelayInMilliseconds = 500;

    /**
     * DocumentOverlayLayer responsibility is to get the changes to the viewport
     * and report them to DocumentOverlayView.
     */
    private class DocumentOverlayLayer extends Layer {
        private float mViewLeft;
        private float mViewTop;
        private float mViewZoom;

        /**
         * @see Layer#draw(RenderContext)
         */
        @Override
        public void draw(final RenderContext context) {
            if (FloatUtils.fuzzyEquals(mViewLeft, context.viewport.left)
                    && FloatUtils.fuzzyEquals(mViewTop, context.viewport.top)
                    && FloatUtils.fuzzyEquals(mViewZoom, context.zoomFactor)) {
                return;
            }

            mViewLeft = context.viewport.left;
            mViewTop = context.viewport.top;
            mViewZoom = context.zoomFactor;

            TheApplication.getMainHandler().post(new Runnable() {
                public void run() {
                    mDocumentOverlayView.repositionWithViewport(mViewLeft, mViewTop, mViewZoom);
                }
            });
        }
    }

    public DocumentOverlay(MainActivity context, LayerView layerView, EventCallback callback) {
        mDocumentOverlayView = context.findViewById(R.id.text_cursor_view);
        mDocumentOverlayLayer = new DocumentOverlayLayer();
        if (mDocumentOverlayView == null) {
            Log.e(LOGTAG, "Failed to initialize TextCursorLayer - CursorView is null");
        }
        layerView.addLayer(mDocumentOverlayLayer);
        mDocumentOverlayView.initialize(layerView, callback);
    }

    public void setPartPageRectangles(List<RectF> rectangles) {
        mDocumentOverlayView.setPartPageRectangles(rectangles);
    }

    /**
     * Show the cursor at the defined cursor position on the overlay.
     */
    public void showCursor() {
        TheApplication.getMainHandler().post(new Runnable() {
            public void run() {
                mDocumentOverlayView.showCursor();
            }
        });
    }

    /**
     * Hide the cursor at the defined cursor position on the overlay.
     */
    public void hideCursor() {
        TheApplication.getMainHandler().post(new Runnable() {
            public void run() {
                mDocumentOverlayView.hideCursor();
            }
        });
    }

    /**
     * Show the page number rectangle on the overlay.
     */
    public void showPageNumberRect() {
        TheApplication.getMainHandler().post(new Runnable() {
            public void run() {
                mDocumentOverlayView.showPageNumberRect();
            }
        });
    }

    /**
     * Hide the page number rectangle on the overlay.
     */
    public void hidePageNumberRect() {
        TheApplication.getMainHandler().postDelayed(new Runnable() {
            public void run() {
                mDocumentOverlayView.hidePageNumberRect();
            }
        }, hidePageNumberRectDelayInMilliseconds);
    }

    /**
     * Position the cursor to the input position on the overlay.
     */
    public void positionCursor(final RectF position) {
        TheApplication.getMainHandler().post(new Runnable() {
            public void run() {
                mDocumentOverlayView.changeCursorPosition(position);
            }
        });
    }

    /**
     * Show selections on the overlay.
     */
    public void showSelections() {
        TheApplication.getMainHandler().post(new Runnable() {
            public void run() {
                mDocumentOverlayView.showSelections();
            }
        });
    }

    /**
     * Hide selections on the overlay.
     */
    public void hideSelections() {
        TheApplication.getMainHandler().post(new Runnable() {
            public void run() {
                mDocumentOverlayView.hideSelections();
            }
        });
    }

    /**
     * Change the list of selections.
     */
    public void changeSelections(final List<RectF> selections) {
        TheApplication.getMainHandler().post(new Runnable() {
            public void run() {
                mDocumentOverlayView.changeSelections(selections);
            }
        });
    }

    /**
     * Show the graphic selection on the overlay.
     */
    public void showGraphicSelection() {
        TheApplication.getMainHandler().post(new Runnable() {
            public void run() {
                mDocumentOverlayView.showGraphicSelection();
            }
        });
    }

    /**
     * Hide the graphic selection.
     */
    public void hideGraphicSelection() {
        TheApplication.getMainHandler().post(new Runnable() {
            public void run() {
                mDocumentOverlayView.hideGraphicSelection();
            }
        });
    }

    /**
     * Change the graphic selection rectangle to the input rectangle.
     */
    public void changeGraphicSelection(final RectF rectangle) {
        TheApplication.getMainHandler().post(new Runnable() {
            public void run() {
                mDocumentOverlayView.changeGraphicSelection(rectangle);
            }
        });
    }

    /**
     * Show the handle (of input type) on the overlay.
     */
    public void showHandle(final SelectionHandle.HandleType type) {
        TheApplication.getMainHandler().post(new Runnable() {
            public void run() {
                mDocumentOverlayView.showHandle(type);
            }
        });
    }

    /**
     * Hide the handle (of input type).
     */
    public void hideHandle(final SelectionHandle.HandleType type) {
        TheApplication.getMainHandler().post(new Runnable() {
            public void run() {
                mDocumentOverlayView.hideHandle(type);
            }
        });
    }

    /**
     * Position the handle (of input type) position to the input rectangle.
     */
    public void positionHandle(final SelectionHandle.HandleType type, final RectF rectangle) {
        TheApplication.getMainHandler().post(new Runnable() {
            public void run() {
                mDocumentOverlayView.positionHandle(type, rectangle);
            }
        });
    }

    public RectF getCurrentCursorPosition() {
        return mDocumentOverlayView.getCurrentCursorPosition();
    }

    public void setCalcHeadersController(CalcHeadersController calcHeadersController) {
        mDocumentOverlayView.setCalcHeadersController(calcHeadersController);
    }

    public void showCellSelection(final RectF cellCursorRect) {
        TheApplication.getMainHandler().post(new Runnable() {
            public void run() {
                mDocumentOverlayView.showCellSelection(cellCursorRect);
            }
        });
    }

    public void showHeaderSelection(final RectF cellCursorRect) {
        TheApplication.getMainHandler().post(new Runnable() {
            public void run() {
                mDocumentOverlayView.showHeaderSelection(cellCursorRect);
            }
        });
    }

    public void showAdjustLengthLine(final boolean isRow, final CalcHeadersView view) {
        TheApplication.getMainHandler().post(new Runnable() {
            @Override
            public void run() {
                mDocumentOverlayView.showAdjustLengthLine(isRow, view);
            }
        });
    }
}


