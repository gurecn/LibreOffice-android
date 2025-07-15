package org.libreoffice.ui;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.snackbar.Snackbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.Toast;
import org.libreoffice.application.CustomConstant;
import org.libreoffice.application.TheApplication;
import org.libreoffice.callback.EventCallback;
import org.libreoffice.callback.ZoomCallback;
import org.libreoffice.canvas.SelectionHandle;
import org.libreoffice.data.LOEvent;
import org.libreoffice.data.TileIdentifier;
import org.libreoffice.manager.InvalidationHandler;
import org.libreoffice.manager.LOKitInputConnectionHandler;
import org.libreoffice.R;
import org.libreoffice.data.SettingsListenerModel;
import org.libreoffice.manager.ToolbarController;
import org.libreoffice.adapter.DocumentPartViewListAdapter;
import org.libreoffice.data.DocumentPartView;
import org.libreoffice.manager.FontController;
import org.libreoffice.manager.FormattingController;
import org.libreoffice.manager.LOKitTileProvider;
import org.libreoffice.overlay.CalcHeadersController;
import org.libreoffice.overlay.DocumentOverlay;
import org.libreoffice.utils.FileUtilities;
import org.libreoffice.utils.ThumbnailCreator;
import org.mozilla.gecko.ZoomConstraints;
import org.mozilla.gecko.gfx.CairoImage;
import org.mozilla.gecko.gfx.ComposedTileLayer;
import org.mozilla.gecko.gfx.GeckoLayerClient;
import org.mozilla.gecko.gfx.ImmutableViewportMetrics;
import org.mozilla.gecko.gfx.JavaPanZoomController;
import org.mozilla.gecko.gfx.LayerView;
import org.mozilla.gecko.gfx.SubTile;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Main activity of the LibreOffice App. It is started in the UI thread.
 */
public class MainActivity extends AppCompatActivity implements SettingsListenerModel.OnSettingsPreferenceChangedListener, EventCallback {

    private static final int REQUEST_CODE_SAVEAS = 12345;
    private static final int REQUEST_CODE_EXPORT_TO_PDF = 12346;
    private GeckoLayerClient mLayerClient;
    private static boolean mIsExperimentalMode;
    private static boolean mIsDeveloperMode;
    private static boolean mbISReadOnlyMode;
    private DrawerLayout mDrawerLayout;
    Toolbar toolbarTop;
    private ListView mDrawerList;
    private final List<DocumentPartView> mDocumentPartView = new ArrayList<>();
    private DocumentPartViewListAdapter mDocumentPartViewListAdapter;
    private DocumentOverlay mDocumentOverlay;
    private Uri mDocumentUri;
    private File mTempFile = null;
    private File mTempSlideShowFile = null;
    BottomSheetBehavior<LinearLayout> bottomToolbarSheetBehavior;
    BottomSheetBehavior<LinearLayout> toolbarColorPickerBottomSheetBehavior;
    BottomSheetBehavior<LinearLayout> toolbarBackColorPickerBottomSheetBehavior;
    private FormattingController mFormattingController;
    private ToolbarController mToolbarController;
    private FontController mFontController;
    private CalcHeadersController mCalcHeadersController;
    private LOKitTileProvider mTileProvider;
    private String mPassword;
    private boolean mPasswordProtected;
    private boolean mbSkipNextRefresh;

    public GeckoLayerClient getLayerClient() {
        return mLayerClient;
    }

    public static boolean isExperimentalMode() {
        return mIsExperimentalMode;
    }

    public static boolean isDeveloperMode() {
        return mIsDeveloperMode;
    }

    /**
     * Make sure LOKitThread is running and send event to it.
     */
    @Override
    public void queueEvent(LOEvent event) {
        switch (event.mType) {
            case LOEvent.LOAD:
                loadDocument(event.filePath);
                break;
            case LOEvent.LOAD_NEW:
                loadNewDocument(event.filePath, event.fileType);
                break;
            case LOEvent.SAVE_AS:
                saveDocumentAs(event.filePath, event.fileType, true);
                break;
            case LOEvent.SAVE_COPY_AS:
                saveDocumentAs(event.filePath, event.fileType, false);
                break;
            case LOEvent.CLOSE:
                closeDocument();
                break;
            case LOEvent.SIZE_CHANGED:
                redraw(false);
                break;
            case LOEvent.CHANGE_PART:
                changePart(event.mPartIndex);
                break;
            case LOEvent.TILE_INVALIDATION:
                tileInvalidation(event.mInvalidationRect);
                break;
            case LOEvent.THUMBNAIL:
                createThumbnail(event.mTask);
                break;
            case LOEvent.TOUCH:
                touch(event.mTouchType, event.mDocumentCoordinate);
                break;
            case LOEvent.KEY_EVENT:
                keyEvent(event.mKeyEvent);
                break;
            case LOEvent.TILE_REEVALUATION_REQUEST:
                tileReevaluationRequest(event.mComposedTileLayer);
                break;
            case LOEvent.CHANGE_HANDLE_POSITION:
                changeHandlePosition(event.mHandleType, event.mDocumentCoordinate);
                break;
            case LOEvent.SWIPE_LEFT:
                if (null != mTileProvider) onSwipeLeft();
                break;
            case LOEvent.SWIPE_RIGHT:
                if (null != mTileProvider) onSwipeRight();
                break;
            case LOEvent.NAVIGATION_CLICK:
                mInvalidationHandler.changeStateTo(InvalidationHandler.OverlayState.NONE);
                break;
            case LOEvent.UNO_COMMAND:
                if (null != mTileProvider)
                    mTileProvider.postUnoCommand(event.mString, event.mValue);
                break;
            case LOEvent.UPDATE_PART_PAGE_RECT:
                updatePartPageRectangles();
                break;
            case LOEvent.UPDATE_ZOOM_CONSTRAINTS:
                updateZoomConstraints();
                break;
            case LOEvent.UPDATE_CALC_HEADERS:
                updateCalcHeaders();
                break;
            case LOEvent.UNO_COMMAND_NOTIFY:
                if (null != mTileProvider) mTileProvider.postUnoCommand(event.mString, event.mValue, event.mNotify);
                break;
            case LOEvent.REFRESH:
                refresh(false);
                break;
            case LOEvent.PAGE_SIZE_CHANGED:
                updatePageSize(event.mPageWidth, event.mPageHeight);
                break;
            case LOEvent.DOCUMENT_CHANGED:
                setDocumentChanged(true);
                break;

            case LOEvent.SAVE_PASSWORD:
                savePassword(event.mString);
                break;
        }
    }

    private boolean isKeyboardOpen = false;
    private boolean isFormattingToolbarOpen = false;
    private boolean isSearchToolbarOpen = false;
    private static boolean isDocumentChanged = false;
    private boolean isUNOCommandsToolbarOpen = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SettingsListenerModel.getInstance().setListener(this);
        updatePreferences();
        setContentView(R.layout.activity_main);
        toolbarTop = findViewById(R.id.toolbar);
        hideBottomToolbar();
        mToolbarController = new ToolbarController(this, toolbarTop, this);
        mFormattingController = new FormattingController(this, this);
        toolbarTop.setNavigationOnClickListener(view -> queueEvent(new LOEvent(LOEvent.NAVIGATION_CLICK)));
        mFontController = new FontController(this, this);
        mLayerClient = new GeckoLayerClient(this, this);
        LayerView layerView = findViewById(R.id.layer_view);
        ZoomCallback callback = new ZoomCallback() {
            @Override
            public void hideSoftKeyboard() {
                getDocumentOverlay().hidePageNumberRect();
            }
            @Override
            public void showPageNumberRect() {
                getDocumentOverlay().showPageNumberRect();
            }
            @Override
            public void hidePageNumberRect() {
            }
        };
        JavaPanZoomController panZoomController =new JavaPanZoomController(callback, mLayerClient, layerView, this);
        mLayerClient.setView(layerView, panZoomController);
        layerView.setInputConnectionHandler(new LOKitInputConnectionHandler());
        mLayerClient.notifyReady();
        layerView.setOnKeyListener((view, i, keyEvent) -> {
            if(!isReadOnlyMode() && keyEvent.getKeyCode() != KeyEvent.KEYCODE_BACK){
                setDocumentChanged(true);
            }
            return false;
        });
        // create TextCursorLayer
        mDocumentOverlay = new DocumentOverlay(this, layerView, this);
        mbISReadOnlyMode = !isExperimentalMode();
        final Uri docUri = getIntent().getData();
        if (docUri != null) {
            if (docUri.getScheme().equals(ContentResolver.SCHEME_CONTENT) || docUri.getScheme().equals(ContentResolver.SCHEME_ANDROID_RESOURCE)) {
                final boolean isReadOnlyDoc  = (getIntent().getFlags() & Intent.FLAG_GRANT_WRITE_URI_PERMISSION) == 0;
                mbISReadOnlyMode = !isExperimentalMode() || isReadOnlyDoc;
                String displayName = FileUtilities.retrieveDisplayNameForDocumentUri(getContentResolver(), docUri);
                toolbarTop.setTitle(displayName);
            } else if (docUri.getScheme().equals(ContentResolver.SCHEME_FILE)) {
                mbISReadOnlyMode = true;
                toolbarTop.setTitle(docUri.getLastPathSegment());
            }
            // create a temporary local copy to work with
            boolean copyOK = copyFileToTemp(docUri) && mTempFile != null;
            if (!copyOK) {
                return;
            }

            // if input doc is a template, a new doc is created and a proper URI to save to
            // will only be available after a "Save As"
            if (isTemplate(docUri)) {
                toolbarTop.setTitle(R.string.default_document_name);
            } else {
                mDocumentUri = docUri;
            }
            queueEvent(new LOEvent(mTempFile.getPath(), LOEvent.LOAD));
        } else if (getIntent().getStringExtra(HomeActivity.NEW_DOC_TYPE_KEY) != null) {
            // New document type string is not null, meaning we want to open a new document
            String newDocumentType = getIntent().getStringExtra(HomeActivity.NEW_DOC_TYPE_KEY);
            // create a temporary local file, will be copied to the actual URI when saving
            loadNewDocument(newDocumentType);
            toolbarTop.setTitle(getString(R.string.default_document_name));
        } else {
            return;
        }
        mbSkipNextRefresh = true;

        mDrawerLayout = findViewById(R.id.drawer_layout);

        if (mDocumentPartViewListAdapter == null) {
            mDrawerList = findViewById(R.id.left_drawer);

            mDocumentPartViewListAdapter = new DocumentPartViewListAdapter(this, R.layout.document_part_list_layout, mDocumentPartView, this);
            mDrawerList.setAdapter(mDocumentPartViewListAdapter);
            mDrawerList.setOnItemClickListener(new DocumentPartClickListener());
        }

        mToolbarController.setupToolbars();

        TabHost host = findViewById(R.id.toolbarTabHost);
        host.setup();

        TabHost.TabSpec spec = host.newTabSpec(getString(R.string.tabhost_character));
        spec.setContent(R.id.tab_character);
        spec.setIndicator(getString(R.string.tabhost_character));
        host.addTab(spec);

        spec = host.newTabSpec(getString(R.string.tabhost_paragraph));
        spec.setContent(R.id.tab_paragraph);
        spec.setIndicator(getString(R.string.tabhost_paragraph));
        host.addTab(spec);

        spec = host.newTabSpec(getString(R.string.tabhost_insert));
        spec.setContent(R.id.tab_insert);
        spec.setIndicator(getString(R.string.tabhost_insert));
        host.addTab(spec);

        spec = host.newTabSpec(getString(R.string.tabhost_style));
        spec.setContent(R.id.tab_style);
        spec.setIndicator(getString(R.string.tabhost_style));
        host.addTab(spec);

        LinearLayout bottomToolbarLayout = findViewById(R.id.toolbar_bottom);
        LinearLayout toolbarColorPickerLayout = findViewById(R.id.toolbar_color_picker);
        LinearLayout toolbarBackColorPickerLayout = findViewById(R.id.toolbar_back_color_picker);
        bottomToolbarSheetBehavior = BottomSheetBehavior.from(bottomToolbarLayout);
        toolbarColorPickerBottomSheetBehavior = BottomSheetBehavior.from(toolbarColorPickerLayout);
        toolbarBackColorPickerBottomSheetBehavior = BottomSheetBehavior.from(toolbarBackColorPickerLayout);
        bottomToolbarSheetBehavior.setHideable(true);
        toolbarColorPickerBottomSheetBehavior.setHideable(true);
        toolbarBackColorPickerBottomSheetBehavior.setHideable(true);
    }

    private void updatePreferences() {
        mIsExperimentalMode = TheApplication.getSPManager().getBoolean(CustomConstant.ENABLE_EXPERIMENTAL_PREFS_KEY, false);
        mIsDeveloperMode = mIsExperimentalMode && TheApplication.getSPManager().getBoolean(CustomConstant.ENABLE_DEVELOPER_PREFS_KEY, false);
    }

    // Loads a new Document and saves it to a temporary file
    private void loadNewDocument(String newDocumentType) {
        String tempFileName = "LibreOffice_" + UUID.randomUUID().toString();
        mTempFile = new File(this.getCacheDir(), tempFileName);
        queueEvent(new LOEvent(mTempFile.getPath(), newDocumentType, LOEvent.LOAD_NEW));
    }

    public RectF getCurrentCursorPosition() {
        return mDocumentOverlay.getCurrentCursorPosition();
    }

    private boolean copyFileToTemp(Uri documentUri) {
        String suffix = null;
        String intentType = getIntent().getType();
        if ("text/comma-separated-values".equals(intentType) || "text/csv".equals(intentType)) suffix = ".csv";
        try {
            mTempFile = File.createTempFile("LibreOffice", suffix, this.getCacheDir());
            final FileOutputStream outputStream = new FileOutputStream(mTempFile);
            return copyUriToStream(documentUri, outputStream);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Save the document.
     */
    public void saveDocument() {
        Toast.makeText(this, R.string.message_saving, Toast.LENGTH_SHORT).show();
        queueEvent(new LOEvent(LOEvent.UNO_COMMAND_NOTIFY, ".uno:Save", true));
    }

    /**
     * Open file chooser and save the document to the URI
     * selected there.
     */
    public void saveDocumentAs() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        String mimeType = getODFMimeTypeForDocument();
        intent.setType(mimeType);
        if (Build.VERSION.SDK_INT >= 26) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, mDocumentUri);
        }
        startActivityForResult(intent, REQUEST_CODE_SAVEAS);
    }

    /**
     * Saves the document under the given URI using ODF format
     * and uses that URI from now on for all operations.
     * @param newUri URI to save the document and use from now on.
     */
    private void saveDocumentAs(Uri newUri) {
        mDocumentUri = newUri;
        mTileProvider.saveDocumentAs(mTempFile.getPath(), true);
        saveFileToOriginalSource();
        String displayName = FileUtilities.retrieveDisplayNameForDocumentUri(getContentResolver(), mDocumentUri);
        toolbarTop.setTitle(displayName);
        mbISReadOnlyMode = !isExperimentalMode();
        getToolbarController().setupToolbars();
    }

    public void exportToPDF() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(FileUtilities.MIMETYPE_PDF);
        if (Build.VERSION.SDK_INT >= 26) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, mDocumentUri);
        }
        final String displayName = toolbarTop.getTitle().toString();
        final String suggestedFileName = FileUtilities.stripExtensionFromFileName(displayName) + ".pdf";
        intent.putExtra(Intent.EXTRA_TITLE, suggestedFileName);
        startActivityForResult(intent, REQUEST_CODE_EXPORT_TO_PDF);
    }

    private void exportToPDF(final Uri uri) {
        boolean exportOK = false;
        File tempFile = null;
        try {
            tempFile = File.createTempFile("LibreOffice_", ".pdf");
            mTileProvider.saveDocumentAs(tempFile.getAbsolutePath(),"pdf", false);
            try {
                FileInputStream inputStream = new FileInputStream(tempFile);
                exportOK = copyStreamToUri(inputStream, uri);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }

        int msgId = exportOK ? R.string.pdf_export_finished : R.string.unable_to_export_pdf;
        TheApplication.getMainHandler().post(() -> showCustomStatusMessage(getString(msgId)));
    }

    /**
     * Returns the ODF MIME type that can be used for the current document,
     * regardless of whether the document is an ODF Document or not
     * (e.g. returns FileUtilities.MIMETYPE_OPENDOCUMENT_TEXT for a DOCX file).
     * @return MIME type, or empty string, if no appropriate MIME type could be found.
     */
    private String getODFMimeTypeForDocument() {
        if (mTileProvider.isTextDocument())
            return FileUtilities.MIMETYPE_OPENDOCUMENT_TEXT;
        else if (mTileProvider.isSpreadsheet())
            return FileUtilities.MIMETYPE_OPENDOCUMENT_SPREADSHEET;
        else if (mTileProvider.isPresentation())
            return FileUtilities.MIMETYPE_OPENDOCUMENT_PRESENTATION;
        else if (mTileProvider.isDrawing())
            return FileUtilities.MIMETYPE_OPENDOCUMENT_GRAPHICS;
        else {
            return "";
        }
    }

    /**
     * Returns whether the MIME type for the URI is considered one for a document template.
     */
    private boolean isTemplate(final Uri documentUri) {
        final String mimeType = getContentResolver().getType(documentUri);
        return FileUtilities.isTemplateMimeType(mimeType);
    }

    public void saveFileToOriginalSource() {
        if (mTempFile == null || mDocumentUri == null || !mDocumentUri.getScheme().equals(ContentResolver.SCHEME_CONTENT))
            return;

        boolean copyOK = false;
        try {
            final FileInputStream inputStream = new FileInputStream(mTempFile);
            copyOK = copyStreamToUri(inputStream, mDocumentUri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (copyOK) {
            runOnUiThread(() -> Toast.makeText(this, R.string.message_saved, Toast.LENGTH_SHORT).show());
            setDocumentChanged(false);
        } else {
            runOnUiThread(() -> Toast.makeText(this, R.string.message_saving_failed, Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePreferences();
        if (mToolbarController.getEditModeStatus() && isExperimentalMode()) {
            mToolbarController.switchToEditMode();
        } else {
            mToolbarController.switchToViewMode();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mbSkipNextRefresh) {
            queueEvent(new LOEvent(LOEvent.REFRESH));
        }
        mbSkipNextRefresh = false;
    }

    @Override
    protected void onStop() {
        hideSoftKeyboardDirect();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        queueEvent(new LOEvent(LOEvent.CLOSE));
        mLayerClient.destroy();
        super.onDestroy();

        if (isFinishing()) { // Not an orientation change
            if (mTempFile != null) {
                // noinspection ResultOfMethodCallIgnored
                mTempFile.delete();
            }
            if (mTempSlideShowFile != null && mTempSlideShowFile.exists()) {
                // noinspection ResultOfMethodCallIgnored
                mTempSlideShowFile.delete();
            }
        }
    }
    @Override
    public void onBackPressed() {
        if (!isDocumentChanged) {
            super.onBackPressed();
            return;
        }

        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    mTileProvider.saveDocument();
                    isDocumentChanged=false;
                    onBackPressed();
                    break;
                case DialogInterface.BUTTON_NEUTRAL:
                    isDocumentChanged=false;
                    onBackPressed();
                    break;
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.save_alert_dialog_title)
                .setPositiveButton(R.string.save_document, dialogClickListener)
                .setNegativeButton(R.string.action_cancel, dialogClickListener)
                .setNeutralButton(R.string.no_save_document, dialogClickListener)
                .show();

    }

    public List<DocumentPartView> getDocumentPartView() {
        return mDocumentPartView;
    }

    public void disableNavigationDrawer() {
        // Only the original thread that created mDrawerLayout should touch its views.
        TheApplication.getMainHandler().post(() -> mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, mDrawerList));
    }

    public DocumentPartViewListAdapter getDocumentPartViewListAdapter() {
        return mDocumentPartViewListAdapter;
    }

    /**
     * Show software keyboard.
     * Force the request on main thread.
     */
    public void showSoftKeyboard() {

        TheApplication.getMainHandler().post(() -> {
            if(!isKeyboardOpen) showSoftKeyboardDirect();
            else hideSoftKeyboardDirect();
        });

    }

    private void showSoftKeyboardDirect() {
        LayerView layerView = findViewById(R.id.layer_view);
        if (layerView.requestFocus()) {
            InputMethodManager inputMethodManager = (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.showSoftInput(layerView, InputMethodManager.SHOW_FORCED);
        }
        isKeyboardOpen=true;
        isSearchToolbarOpen=false;
        isFormattingToolbarOpen=false;
        isUNOCommandsToolbarOpen=false;
        hideBottomToolbar();
    }

    public void showSoftKeyboardOrFormattingToolbar() {
        TheApplication.getMainHandler().post(() -> {
            if (findViewById(R.id.toolbar_bottom).getVisibility() != View.VISIBLE
                    && findViewById(R.id.toolbar_color_picker).getVisibility() != View.VISIBLE) {
                showSoftKeyboardDirect();
            }
        });
    }

    /**
     * Hides software keyboard on UI thread.
     */
    public void hideSoftKeyboard() {
        TheApplication.getMainHandler().post(() -> hideSoftKeyboardDirect());
    }

    /**
     * Hides software keyboard.
     */
    private void hideSoftKeyboardDirect() {
        if (getCurrentFocus() != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            isKeyboardOpen=false;
        }
    }

    public void showBottomToolbar() {
        TheApplication.getMainHandler().post(() -> bottomToolbarSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED));
    }

    public void hideBottomToolbar() {
        TheApplication.getMainHandler().post(() -> {
            bottomToolbarSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            toolbarColorPickerBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            toolbarBackColorPickerBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            findViewById(R.id.search_toolbar).setVisibility(View.GONE);
            findViewById(R.id.UNO_commands_toolbar).setVisibility(View.GONE);
            isFormattingToolbarOpen=false;
            isSearchToolbarOpen=false;
            isUNOCommandsToolbarOpen=false;
        });
    }

    public void showFormattingToolbar() {
        TheApplication.getMainHandler().post(() -> {
            if (isFormattingToolbarOpen) {
                hideFormattingToolbar();
            } else {
                showBottomToolbar();
                findViewById(R.id.search_toolbar).setVisibility(View.GONE);
                findViewById(R.id.formatting_toolbar).setVisibility(View.VISIBLE);
                findViewById(R.id.search_toolbar).setVisibility(View.GONE);
                findViewById(R.id.UNO_commands_toolbar).setVisibility(View.GONE);
                hideSoftKeyboardDirect();
                isSearchToolbarOpen=false;
                isFormattingToolbarOpen=true;
                isUNOCommandsToolbarOpen=false;
            }

        });
    }

    public void hideFormattingToolbar() {
        TheApplication.getMainHandler().post(() -> hideBottomToolbar());
    }

    public void showSearchToolbar() {
        TheApplication.getMainHandler().post(() -> {
            if (isSearchToolbarOpen) {
                hideSearchToolbar();
            } else {
                showBottomToolbar();
                findViewById(R.id.formatting_toolbar).setVisibility(View.GONE);
                toolbarColorPickerBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                toolbarBackColorPickerBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                findViewById(R.id.search_toolbar).setVisibility(View.VISIBLE);
                findViewById(R.id.UNO_commands_toolbar).setVisibility(View.GONE);
                hideSoftKeyboardDirect();
                isFormattingToolbarOpen=false;
                isSearchToolbarOpen=true;
                isUNOCommandsToolbarOpen=false;
            }
        });
    }

    public void hideSearchToolbar() {
        TheApplication.getMainHandler().post(this::hideBottomToolbar);
    }

    public void showUNOCommandsToolbar() {
        TheApplication.getMainHandler().post(() -> {
            if(isUNOCommandsToolbarOpen){
                hideUNOCommandsToolbar();
            }else{
                showBottomToolbar();
                findViewById(R.id.formatting_toolbar).setVisibility(View.GONE);
                findViewById(R.id.search_toolbar).setVisibility(View.GONE);
                findViewById(R.id.UNO_commands_toolbar).setVisibility(View.VISIBLE);
                hideSoftKeyboardDirect();
                isFormattingToolbarOpen=false;
                isSearchToolbarOpen=false;
                isUNOCommandsToolbarOpen=true;
            }
        });
    }

    public void hideUNOCommandsToolbar() {
        TheApplication.getMainHandler().post(this::hideBottomToolbar);
    }

    public void showProgressSpinner() {
        toolbarTop.post(() -> findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE));
    }

    public void hideProgressSpinner() {
        toolbarTop.post(() -> findViewById(R.id.loadingPanel).setVisibility(View.GONE));
    }

    public void showAlertDialog(String message) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(R.string.error);
        alertDialogBuilder.setMessage(message);
        alertDialogBuilder.setNeutralButton(R.string.alert_ok, (dialog, id) -> finish());
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    public DocumentOverlay getDocumentOverlay() {
        return mDocumentOverlay;
    }

    public CalcHeadersController getCalcHeadersController() {
        return mCalcHeadersController;
    }

    public ToolbarController getToolbarController() {
        return mToolbarController;
    }

    public FontController getFontController() {
        return mFontController;
    }

    public FormattingController getFormattingController() {
        return mFormattingController;
    }

    public void openDrawer() {
        mDrawerLayout.openDrawer(mDrawerList);
        hideBottomToolbar();
    }

    public void showAbout() {
        AboutDialogFragment aboutDialogFragment = new AboutDialogFragment();
        aboutDialogFragment.show(getSupportFragmentManager(), "AboutDialogFragment");
    }

    public void addPart(){
        mTileProvider.addPart();
        mDocumentPartViewListAdapter.notifyDataSetChanged();
        setDocumentChanged(true);
    }

    public void renamePart(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.enter_part_name);
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton(R.string.alert_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mTileProvider.renamePart( input.getText().toString());
            }
        });
        builder.setNegativeButton(R.string.alert_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    public void deletePart() {
        mTileProvider.removePart();
    }

    public void showSettings() {
        startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
    }

    public boolean isDrawerEnabled() {
        boolean isDrawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        boolean isDrawerLocked = mDrawerLayout.getDrawerLockMode(mDrawerList) != DrawerLayout.LOCK_MODE_UNLOCKED;
        return !isDrawerOpen && !isDrawerLocked;
    }

    @Override
    public void settingsPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.matches(CustomConstant.ENABLE_EXPERIMENTAL_PREFS_KEY)) {
            mIsExperimentalMode = sharedPreferences.getBoolean(CustomConstant.ENABLE_EXPERIMENTAL_PREFS_KEY, false);
        }
    }

    public void promptForPassword() {
        PasswordDialogFragment passwordDialogFragment = new PasswordDialogFragment(this);
        passwordDialogFragment.show(getSupportFragmentManager(), "PasswordDialogFragment");
    }
    public void setPassword() {
        mTileProvider.setDocumentPassword("file://" + mTempFile.getPath(), mPassword);
    }
    public void setTileProvider(LOKitTileProvider loKitTileProvider) {
        mTileProvider = loKitTileProvider;
    }

    public LOKitTileProvider getTileProvider() {
        return mTileProvider;
    }

    public void savePassword(String pwd) {
        mPassword = pwd;
        synchronized (mTileProvider.getMessageCallback()) {
            mTileProvider.getMessageCallback().notifyAll();
        }
    }

    public void setPasswordProtected(boolean b) {
        mPasswordProtected = b;
    }

    public boolean isPasswordProtected() {
        return mPasswordProtected;
    }

    public void initializeCalcHeaders() {
        mCalcHeadersController = new CalcHeadersController(this, mDocumentOverlay, mLayerClient.getView(), this);
        mCalcHeadersController.setupHeaderPopupView();
        TheApplication.getMainHandler().post(() -> {
            findViewById(R.id.calc_header_top_left).setVisibility(View.VISIBLE);
            findViewById(R.id.calc_header_row).setVisibility(View.VISIBLE);
            findViewById(R.id.calc_header_column).setVisibility(View.VISIBLE);
            findViewById(R.id.calc_address).setVisibility(View.VISIBLE);
            findViewById(R.id.calc_formula).setVisibility(View.VISIBLE);
        });
    }

    public static boolean isReadOnlyMode() {
        return mbISReadOnlyMode;
    }

    public boolean hasLocationForSave() {
        return mDocumentUri != null;
    }

    public static void setDocumentChanged (boolean changed) {
        isDocumentChanged = changed;
    }

    private class DocumentPartClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            DocumentPartView partView = mDocumentPartViewListAdapter.getItem(position);
            queueEvent(new LOEvent(LOEvent.CHANGE_PART, partView.partIndex));
            mDrawerLayout.closeDrawer(mDrawerList);
        }
    }



    /**
     * Copies everything from the given input stream to the given output stream
     * and closes both streams in the end.
     * @return Whether copy operation was successful.
     */
    private boolean copyStream(InputStream inputStream, OutputStream outputStream) {
        try {
            byte[] buffer = new byte[4096];
            int readBytes = inputStream.read(buffer);
            while (readBytes != -1) {
                outputStream.write(buffer, 0, readBytes);
                readBytes = inputStream.read(buffer);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                inputStream.close();
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Copies everything from the given Uri to the given OutputStream
     * and closes the OutputStream in the end.
     * The copy operation runs in a separate thread, but the method only returns
     * after the thread has finished its execution.
     * This can be used to copy in a blocking way when network access is involved,
     * which is not allowed from the main thread, but that may happen when an underlying
     * DocumentsProvider (like the NextCloud one) does network access.
     */
    private boolean copyUriToStream(final Uri inputUri, final OutputStream outputStream) {
        class CopyThread extends Thread {
            /** Whether copy operation was successful. */
            private boolean result = false;

            @Override
            public void run() {
                final ContentResolver contentResolver = getContentResolver();
                try {
                    InputStream inputStream = contentResolver.openInputStream(inputUri);
                    result = copyStream(inputStream, outputStream);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        CopyThread copyThread = new CopyThread();
        copyThread.start();
        try {
            copyThread.join();
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
        return copyThread.result;
    }

    /**
     * Copies everything from the given InputStream to the given URI and closes the
     * InputStream in the end.
     */
    private boolean copyStreamToUri(final InputStream inputStream, final Uri outputUri) {
        class CopyThread extends Thread {
            private boolean result = false;
            @Override
            public void run() {
                final ContentResolver contentResolver = getContentResolver();
                try {
                    OutputStream outputStream = contentResolver.openOutputStream(outputUri);
                    result = copyStream(inputStream, outputStream);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        CopyThread copyThread = new CopyThread();
        copyThread.start();
        try {
            copyThread.join();
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
        return copyThread.result;
    }

    public void showCustomStatusMessage(String message){
        Snackbar.make(mDrawerLayout, message, Snackbar.LENGTH_LONG).show();
    }

    public void preparePresentation() {
        if (getExternalCacheDir() != null) {
            String tempPath = getExternalCacheDir().getPath() + "/" + mTempFile.getName() + ".svg";
            mTempSlideShowFile = new File(tempPath);
            if (mTempSlideShowFile.exists() && !isDocumentChanged) {
                startPresentation("file://" + tempPath);
            } else {
                queueEvent(new LOEvent(tempPath, "svg", LOEvent.SAVE_COPY_AS));
            }
        }
    }

    public void startPresentation(String tempPath) {
        Intent intent = new Intent(this, PresentationActivity.class);
        intent.setData(Uri.parse(tempPath));
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SAVEAS && resultCode == RESULT_OK) {
            final Uri fileUri = data.getData();
            saveDocumentAs(fileUri);
        } else if (requestCode == REQUEST_CODE_EXPORT_TO_PDF && resultCode == RESULT_OK) {
            final Uri fileUri = data.getData();
            exportToPDF(fileUri);
        } else {
            mFormattingController.handleActivityResult(requestCode, resultCode, data);
            hideBottomToolbar();
        }
    }

// todo
    private InvalidationHandler mInvalidationHandler;
    private ImmutableViewportMetrics mViewportMetrics;

    /**
     * Viewport changed, Recheck if tiles need to be added / removed.
     */
    private void tileReevaluationRequest(ComposedTileLayer composedTileLayer) {
        if (mTileProvider == null) {
            return;
        }
        List<SubTile> tiles = new ArrayList<SubTile>();

        mLayerClient.beginDrawing();
        composedTileLayer.addNewTiles(tiles);
        mLayerClient.endDrawing();

        for (SubTile tile : tiles) {
            TileIdentifier tileId = tile.id;
            CairoImage image = mTileProvider.createTile(tileId.x, tileId.y, tileId.size, tileId.zoom);
            mLayerClient.beginDrawing();
            if (image != null) {
                tile.setImage(image);
            }
            mLayerClient.endDrawing();
            mLayerClient.forceRender();
        }

        mLayerClient.beginDrawing();
        composedTileLayer.markTiles();
        composedTileLayer.clearMarkedTiles();
        mLayerClient.endDrawing();
        mLayerClient.forceRender();
    }

    /**
     * Invalidate tiles that intersect the input rect.
     */
    private void tileInvalidation(RectF rect) {
        if (mLayerClient == null || mTileProvider == null) {
            return;
        }

        mLayerClient.beginDrawing();

        List<SubTile> tiles = new ArrayList<SubTile>();
        mLayerClient.invalidateTiles(tiles, rect);

        for (SubTile tile : tiles) {
            CairoImage image = mTileProvider.createTile(tile.id.x, tile.id.y, tile.id.size, tile.id.zoom);
            tile.setImage(image);
            tile.invalidate();
        }
        mLayerClient.endDrawing();
        mLayerClient.forceRender();
    }

    /**
     * Handle the geometry change + draw.
     */
    private void redraw(boolean resetZoomAndPosition) {
        if (mLayerClient == null || mTileProvider == null) {
            // called too early...
            return;
        }

        mLayerClient.setPageRect(0, 0, mTileProvider.getPageWidth(), mTileProvider.getPageHeight());
        mViewportMetrics = mLayerClient.getViewportMetrics();
        mLayerClient.setViewportMetrics(mViewportMetrics);
        if (resetZoomAndPosition) {
            toolbarTop.post(this::zoomAndRepositionTheDocument);
        }
        mLayerClient.forceRedraw();
        mLayerClient.forceRender();
    }

    /**
     * Reposition the view (zoom and position) when the document is firstly shown. This is document type dependent.
     */
    private void zoomAndRepositionTheDocument() {
        if (mTileProvider.isSpreadsheet()) {
            // Don't do anything for spreadsheets - show at 100%
        } else if (mTileProvider.isTextDocument()) {
            // Always zoom text document to the beginning of the document and centered by width
            float centerY = mViewportMetrics.getCssViewport().centerY();
            mLayerClient.zoomTo(new RectF(0, centerY, mTileProvider.getPageWidth(), centerY));
        } else {
            // Other documents - always show the whole document on the screen,
            // regardless of document shape and orientation.
            if (mViewportMetrics.getViewport().width() < mViewportMetrics.getViewport().height()) {
                mLayerClient.zoomTo(mTileProvider.getPageWidth(), 0);
            } else {
                mLayerClient.zoomTo(0, mTileProvider.getPageHeight());
            }
        }
    }

    /**
     * Invalidate everything + handle the geometry change
     */
    private void refresh(boolean resetZoomAndPosition) {
        mLayerClient.clearAndResetlayers();
        redraw(resetZoomAndPosition);
        updatePartPageRectangles();
        if (mTileProvider != null && mTileProvider.isSpreadsheet()) {
            updateCalcHeaders();
        }
    }

    /**
     * Update part page rectangles which hold positions of each document page.
     * Result is stored in DocumentOverlayView class.
     */
    private void updatePartPageRectangles() {
        if (mTileProvider == null) {
            return;
        }
        String partPageRectString = ((LOKitTileProvider) mTileProvider).getPartPageRectangles();
        List<RectF> partPageRectangles = mInvalidationHandler.convertPayloadToRectangles(partPageRectString);
        this.getDocumentOverlay().setPartPageRectangles(partPageRectangles);
    }

    private void updatePageSize(int pageWidth, int pageHeight){
        mTileProvider.setDocumentSize(pageWidth, pageHeight);
        redraw(true);
    }

    private void updateZoomConstraints() {
        if (mTileProvider == null) return;
        // Set default zoom to the page width and min zoom so that the whole page is visible
        final float pageHeightZoom = mLayerClient.getViewportMetrics().getHeight() / mTileProvider.getPageHeight();
        final float pageWidthZoom = mLayerClient.getViewportMetrics().getWidth() / mTileProvider.getPageWidth();
        final float minZoom = Math.min(pageWidthZoom, pageHeightZoom);
        mLayerClient.setZoomConstraints(new ZoomConstraints(pageWidthZoom, minZoom, 0f));
    }

    /**
     * Change part of the document.
     */
    private void changePart(int partIndex) {
        showProgressSpinner();
        mTileProvider.changePart(partIndex);
        mViewportMetrics = mLayerClient.getViewportMetrics();
        // mLayerClient.setViewportMetrics(mViewportMetrics.scaleTo(0.9f, new PointF()));
        refresh(true);
        hideProgressSpinner();
    }

    /**
     * Handle load document event.
     * @param filePath - filePath to where the document is located
     * @return Whether the document has been loaded successfully.
     */
    private boolean loadDocument(String filePath) {
        mInvalidationHandler = new InvalidationHandler(this, this);
        mTileProvider = new LOKitTileProvider(this, mInvalidationHandler, filePath, this);
        if (mTileProvider.isReady()) {
            showProgressSpinner();
            updateZoomConstraints();
            refresh(true);
            hideProgressSpinner();
            return true;
        } else {
            closeDocument();
            return false;
        }
    }

    /**
     * Handle load new document event.
     * @param filePath - filePath to where new document is to be created
     * @param fileType - fileType what type of new document is to be loaded
     */
    private void loadNewDocument(String filePath, String fileType) {
        boolean ok = loadDocument(fileType);
        if (ok) {
            mTileProvider.saveDocumentAs(filePath, true);
        }
    }

    /**
     * Save the currently loaded document.
     */
    private void saveDocumentAs(String filePath, String fileType, boolean bTakeOwnership) {
        if (mTileProvider != null) {
            mTileProvider.saveDocumentAs(filePath, fileType, bTakeOwnership);
        }
    }

    /**
     * Close the currently loaded document.
     */
    private void closeDocument() {
        if (mTileProvider != null) {
            mTileProvider.close();
            mTileProvider = null;
        }
    }

    private void updateCalcHeaders() {
        if (null == mTileProvider) return;
        LOKitTileProvider tileProvider = mTileProvider;
        String values = tileProvider.getCalcHeaders();
        if(mCalcHeadersController != null)mCalcHeadersController.setHeaders(values);
    }

    /**
     * Request a change of the handle position.
     */
    private void changeHandlePosition(SelectionHandle.HandleType handleType, PointF documentCoordinate) {
        switch (handleType) {
            case MIDDLE:
                mTileProvider.setTextSelectionReset(documentCoordinate);
                break;
            case START:
                mTileProvider.setTextSelectionStart(documentCoordinate);
                break;
            case END:
                mTileProvider.setTextSelectionEnd(documentCoordinate);
                break;
        }
    }

    /**
     * Processes key events.
     */
    private void keyEvent(KeyEvent keyEvent) {
        if (!TheApplication.getSPManager().getBoolean(CustomConstant.ENABLE_DEVELOPER_PREFS_KEY, false)) return;
        if (mTileProvider == null) {
            return;
        }
        mInvalidationHandler.keyEvent();
        mTileProvider.sendKeyEvent(keyEvent);
    }

    /**
     * Process swipe left event.
     */
    private void onSwipeLeft() {
        mTileProvider.onSwipeLeft();
    }

    /**
     * Process swipe right event.
     */
    private void onSwipeRight() {
        mTileProvider.onSwipeRight();
    }

    /**
     * Processes touch events.
     */
    private void touch(String touchType, PointF documentCoordinate) {
        if (mTileProvider == null || mViewportMetrics == null) return;
        // to handle hyperlinks, enable single tap even in the Viewer
        boolean editing = TheApplication.getSPManager().getBoolean(CustomConstant.ENABLE_DEVELOPER_PREFS_KEY, false);
        float zoomFactor = mViewportMetrics.getZoomFactor();
        if (touchType.equals("LongPress")) {
            mInvalidationHandler.changeStateTo(InvalidationHandler.OverlayState.TRANSITION);
            mTileProvider.mouseButtonDown(documentCoordinate, 1, zoomFactor);
            mTileProvider.mouseButtonUp(documentCoordinate, 1, zoomFactor);
            mTileProvider.mouseButtonDown(documentCoordinate, 2, zoomFactor);
            mTileProvider.mouseButtonUp(documentCoordinate, 2, zoomFactor);
        } else if (touchType.equals("SingleTap")) {
            mInvalidationHandler.changeStateTo(InvalidationHandler.OverlayState.TRANSITION);
            mTileProvider.mouseButtonDown(documentCoordinate, 1, zoomFactor);
            mTileProvider.mouseButtonUp(documentCoordinate, 1, zoomFactor);
        } else if (touchType.equals("GraphicSelectionStart") && editing) {
            mTileProvider.setGraphicSelectionStart(documentCoordinate);
        } else if (touchType.equals("GraphicSelectionEnd") && editing) {
            mTileProvider.setGraphicSelectionEnd(documentCoordinate);
        }
    }

    /**
     * Create thumbnail for the requested document task.
     */
    private void createThumbnail(final ThumbnailCreator.ThumbnailCreationTask task) {
        final Bitmap bitmap = task.getThumbnail(mTileProvider);
        task.applyBitmap(bitmap);
    }

}


