package org.libreoffice.manager;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import org.libreoffice.R;
import org.libreoffice.application.CustomConstant;
import org.libreoffice.application.TheApplication;
import org.libreoffice.callback.EventCallback;
import org.libreoffice.data.LOEvent;
import org.libreoffice.ui.MainActivity;

/**
 * Controls the changes to the toolbar.
 */
public class ToolbarController implements Toolbar.OnMenuItemClickListener {
    private static final String LOGTAG = ToolbarController.class.getSimpleName();
    private final Toolbar mToolbarTop;

    private final MainActivity mContext;
    private final Menu mMainMenu;

    private boolean isEditModeOn = false;
    private String clipboardText = null;
    ClipboardManager clipboardManager;
    ClipData clipData;
    private final EventCallback mCallback;

    public ToolbarController(MainActivity context, Toolbar toolbarTop, EventCallback callback) {
        mToolbarTop = toolbarTop;
        mContext = context;
        mCallback = callback;

        mToolbarTop.inflateMenu(R.menu.main_menu);
        mToolbarTop.setOnMenuItemClickListener(this);
        switchToViewMode();

        mMainMenu = mToolbarTop.getMenu();
        clipboardManager = (ClipboardManager)TheApplication.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
    }

    private void enableMenuItem(final int menuItemId, final boolean enabled) {
        TheApplication.getMainHandler().post(new Runnable() {
            public void run() {
                MenuItem menuItem = mMainMenu.findItem(menuItemId);
                if (menuItem != null) {
                    menuItem.setEnabled(enabled);
                } else {
                    Log.e(LOGTAG, "MenuItem not found.");
                }
            }
        });
    }

    public void setEditModeOn(boolean enabled) {
        isEditModeOn = enabled;
    }

    public boolean getEditModeStatus() {
        return isEditModeOn;
    }

    /**
     * Change the toolbar to edit mode.
     */
    public void switchToEditMode() {
        if (!TheApplication.getSPManager().getBoolean(CustomConstant.ENABLE_DEVELOPER_PREFS_KEY, false)) return;
        setEditModeOn(true);
        // Ensure the change is done on UI thread
        TheApplication.getMainHandler().post(new Runnable() {
            @Override
            public void run() {
                mMainMenu.setGroupVisible(R.id.group_edit_actions, true);
                mMainMenu.findItem(R.id.action_UNO_commands).setVisible(MainActivity.isDeveloperMode() || mMainMenu.findItem(R.id.action_UNO_commands) == null);
                if(mContext.getTileProvider() != null && mContext.getTileProvider().isSpreadsheet()){
                    mMainMenu.setGroupVisible(R.id.group_spreadsheet_options, true);
                } else if(mContext.getTileProvider() != null && mContext.getTileProvider().isPresentation()){
                    mMainMenu.setGroupVisible(R.id.group_presentation_options, true);
                }
                mToolbarTop.setNavigationIcon(R.drawable.ic_check);
                mToolbarTop.setLogo(null);
            }
        });
    }

    /**
     * Show clipboard Actions on the toolbar
     * */
    void showClipboardActions(final String value){
        TheApplication.getMainHandler().post(new Runnable() {
            @Override
            public void run() {
                if(value  != null){
                    mMainMenu.setGroupVisible(R.id.group_edit_actions, false);
                    mMainMenu.setGroupVisible(R.id.group_edit_clipboard, true);
                    if(getEditModeStatus()){
                        showHideClipboardCutAndCopy(true);
                    } else {
                        mMainMenu.findItem(R.id.action_cut).setVisible(false);
                        mMainMenu.findItem(R.id.action_paste).setVisible(false);
                    }
                    clipboardText = value;
                }
            }
        });
    }

    void hideClipboardActions(){
        TheApplication.getMainHandler().post(new Runnable() {
            @Override
            public void run() {
                mMainMenu.setGroupVisible(R.id.group_edit_actions, getEditModeStatus());
                mMainMenu.setGroupVisible(R.id.group_edit_clipboard, false);
            }
        });
    }

    void showHideClipboardCutAndCopy(final boolean option){
        TheApplication.getMainHandler().post(new Runnable() {
            @Override
            public void run() {
                mMainMenu.findItem(R.id.action_copy).setVisible(option);
                mMainMenu.findItem(R.id.action_cut).setVisible(option);
            }
        });
    }

    /**
     * Change the toolbar to view mode.
     */
    public void switchToViewMode() {
        // Ensure the change is done on UI thread
        TheApplication.getMainHandler().post(new Runnable() {
            @Override
            public void run() {
                mMainMenu.setGroupVisible(R.id.group_edit_actions, false);
                mToolbarTop.setLogo(null);
                setEditModeOn(false);
                mContext.hideBottomToolbar();
                mContext.hideSoftKeyboard();
                if(mContext.getTileProvider() != null && mContext.getTileProvider().isSpreadsheet()){
                    mMainMenu.setGroupVisible(R.id.group_spreadsheet_options, false);
                } else if(mContext.getTileProvider() != null && mContext.getTileProvider().isPresentation()){
                    mMainMenu.setGroupVisible(R.id.group_presentation_options, false);
                }
            }
        });
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.action_keyboard) {
            mContext.showSoftKeyboard();
        } else if (itemId == R.id.action_format) {
            mContext.showFormattingToolbar();
        } else if (itemId == R.id.action_about) {
            mContext.showAbout();
            return true;
        } else if (itemId == R.id.action_save) {
            mContext.getTileProvider().saveDocument();
            return true;
        } else if (itemId == R.id.action_save_as) {
            mContext.saveDocumentAs();
            return true;
        } else if (itemId == R.id.action_parts) {
            mContext.openDrawer();
            return true;
        } else if (itemId == R.id.action_exportToPDF) {
            mContext.exportToPDF();
            return true;
        } else if (itemId == R.id.action_print) {
            mContext.getTileProvider().printDocument();
            return true;
        } else if (itemId == R.id.action_settings) {
            mContext.showSettings();
            return true;
        } else if (itemId == R.id.action_search) {
            mContext.showSearchToolbar();
            return true;
        } else if (itemId == R.id.action_undo) {
            if(mCallback != null)mCallback.queueEvent( new LOEvent(LOEvent.UNO_COMMAND, ".uno:Undo"));
            return true;
        } else if (itemId == R.id.action_redo) {
            if(mCallback != null)mCallback.queueEvent( new LOEvent(LOEvent.UNO_COMMAND, ".uno:Redo"));
            return true;
        } else if (itemId == R.id.action_presentation) {
            mContext.preparePresentation();
            return true;
        } else if (itemId == R.id.action_add_slide || itemId == R.id.action_add_worksheet) {
            mContext.addPart();
            return true;
        } else if (itemId == R.id.action_rename_worksheet || itemId == R.id.action_rename_slide) {
            mContext.renamePart();
            return true;
        } else if (itemId == R.id.action_delete_worksheet || itemId == R.id.action_delete_slide) {
            mContext.deletePart();
            return true;
        } else if (itemId == R.id.action_back) {
            hideClipboardActions();
            return true;
        } else if (itemId == R.id.action_copy) {
            if(mCallback != null)mCallback.queueEvent( new LOEvent(LOEvent.UNO_COMMAND, ".uno:Copy"));
            clipData = ClipData.newPlainText("clipboard data", clipboardText);
            clipboardManager.setPrimaryClip(clipData);
            Toast.makeText(mContext, mContext.getResources().getString(R.string.action_text_copied), Toast.LENGTH_SHORT).show();
            return true;
        } else if (itemId == R.id.action_paste) {
            clipData = clipboardManager.getPrimaryClip();
            ClipData.Item clipItem = clipData.getItemAt(0);
            if(mCallback != null)mCallback.queueEvent(new LOEvent(LOEvent.DOCUMENT_CHANGED));
            return mContext.getTileProvider().paste("text/plain;charset=utf-16", clipItem.getText().toString());
        } else if (itemId == R.id.action_cut) {
            clipData = ClipData.newPlainText("clipboard data", clipboardText);
            clipboardManager.setPrimaryClip(clipData);
            if(mCallback != null)mCallback.queueEvent(new LOEvent(LOEvent.KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL)));
            if(mCallback != null)mCallback.queueEvent(new LOEvent(LOEvent.DOCUMENT_CHANGED));
            return true;
        } else if (itemId == R.id.action_UNO_commands) {
            mContext.showUNOCommandsToolbar();
            return true;
        }
        return false;
    }

    public void setupToolbars() {
        boolean isExperimentalMode = TheApplication.getSPManager().getBoolean(CustomConstant.ENABLE_EXPERIMENTAL_PREFS_KEY, false);
        if (isExperimentalMode) {
            boolean enableSaveEntry = !MainActivity.isReadOnlyMode() && mContext.hasLocationForSave();
            enableMenuItem(R.id.action_save, enableSaveEntry);
            if (MainActivity.isReadOnlyMode()) {
                // show message in case experimental mode is enabled (i.e. editing is supported in general),
                // but current document is readonly
                Toast.makeText(mContext, mContext.getString(R.string.readonly_file), Toast.LENGTH_LONG).show();
            }
        } else {
            hideItem(R.id.action_save);
        }
        mMainMenu.findItem(R.id.action_parts).setVisible(mContext.isDrawerEnabled());
    }

    public void showItem(final int item){
        TheApplication.getMainHandler().post(new Runnable() {
            @Override
            public void run() {
                mMainMenu.findItem(item).setVisible(true);

            }
        });
    }

    public void hideItem(final int item){
        TheApplication.getMainHandler().post(new Runnable() {
            @Override
            public void run() {
                mMainMenu.findItem(item).setVisible(false);

            }
        });
    }

}


