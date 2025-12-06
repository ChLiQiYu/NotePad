/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.notepad;

import android.app.LoaderManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.SearchView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.android.notepad.NotePad.Notes;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Displays a list of notes. Will display notes from the {@link Uri}
 * provided in the incoming Intent if there is one, otherwise it defaults to displaying the
 * contents of the {@link NotePadProvider}.
 *
 * Uses LoaderManager for asynchronous database operations to avoid ANR on API 23 devices.
 */
public class NotesList extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    // For logging and debugging
    private static final String TAG = "NotesList";

    /**
     * The columns needed by the cursor adapter
     */
    private static final String[] PROJECTION = new String[] {
            Notes._ID, // 0
            Notes.COLUMN_NAME_TITLE, // 1
            Notes.COLUMN_NAME_MODIFICATION_DATE, // 2
            Notes.COLUMN_NAME_STATUS, // 3
            Notes.COLUMN_NAME_CATEGORY_ID // 4 - 新增分类ID字段
    };

    /** The index of the title column */
    private static final int COLUMN_INDEX_TITLE = 1;
    /** The index of the modification date column */
    private static final int COLUMN_INDEX_MODIFICATION_DATE = 2;
    /** The index of the status column */
    private static final int COLUMN_INDEX_STATUS = 3;
    /** The index of the category id column */
    private static final int COLUMN_INDEX_CATEGORY_ID = 4;

    private ListView mListView; // 添加ListView引用
    private SearchView mSearchView;
    private Spinner mCategoryFilterSpinner;
    private String mCurrentFilter = "";
    private Uri mCurrentUri = Notes.CONTENT_URI;
    private long mCurrentCategoryId = 0;
    private NotesCursorAdapter mAdapter;
    private Menu mOptionsMenu;
    private static final int LOADER_ID = 0;
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private Toolbar mToolbar; // 添加Toolbar引用
    
    // 添加分类数据源和缓存
    private CategoryDataSource mCategoryDataSource;
    private Map<Long, String> mCategoryCache;
    


    /**
     * Custom adapter to handle note list display with status icons and formatted dates
     */
    private class NotesCursorAdapter extends ResourceCursorAdapter {

        private final SimpleDateFormat mDateFormat;

        public NotesCursorAdapter(Context context, Cursor c) {
            super(context, R.layout.noteslist_item, c, 0);
            // Initialize date format with locale to ensure proper formatting
            mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            // Title text
            TextView titleView = (TextView) view.findViewById(android.R.id.text1);
            titleView.setText(cursor.getString(COLUMN_INDEX_TITLE));

            // Modification date
            TextView dateView = (TextView) view.findViewById(android.R.id.text2);
            long timestamp = cursor.getLong(COLUMN_INDEX_MODIFICATION_DATE);
            try {
                // Use Android's built-in date formatting for better localization
                if (DateUtils.isToday(timestamp)) {
                    dateView.setText(DateUtils.formatDateTime(context, timestamp,
                            DateUtils.FORMAT_SHOW_TIME));
                } else {
                    dateView.setText(mDateFormat.format(new Date(timestamp)));
                }
            } catch (Exception e) {
                dateView.setText("");
            }

            // Status label
            TextView statusLabel = (TextView) view.findViewById(R.id.status_label);
            if (statusLabel != null) {
                int status = cursor.getInt(COLUMN_INDEX_STATUS);
                switch (status) {
                    case Notes.STATUS_COMPLETED:
                        statusLabel.setText(context.getString(R.string.status_completed));
                        statusLabel.setBackgroundResource(R.drawable.status_completed_background);
                        statusLabel.setVisibility(View.VISIBLE);
                        break;
                    case Notes.STATUS_PENDING:
                    default:
                        statusLabel.setText(context.getString(R.string.status_pending));
                        statusLabel.setBackgroundResource(R.drawable.status_pending_background);
                        statusLabel.setVisibility(View.VISIBLE);
                        break;
                }
            }

            // Status icon
            ImageView statusIcon = (ImageView) view.findViewById(R.id.status_icon);
            if (statusIcon != null) {
                int status = cursor.getInt(COLUMN_INDEX_STATUS);
                // Use compat drawables for API 23
                if (status == Notes.STATUS_COMPLETED) {
                    statusIcon.setImageResource(R.drawable.ic_check_box_black_24dp);
                    // 设置已完成状态的图标颜色（兼容API 23）
                    statusIcon.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_green_dark));
                } else {
                    statusIcon.setImageResource(R.drawable.ic_radio_button_unchecked_black_24dp);
                    // 设置待办状态的图标颜色（兼容API 23）
                    statusIcon.setColorFilter(ContextCompat.getColor(context, android.R.color.darker_gray));
                }
            }
            
            // Category label
            TextView categoryLabel = (TextView) view.findViewById(R.id.category_label);
            if (categoryLabel != null) {
                long categoryId = cursor.getLong(COLUMN_INDEX_CATEGORY_ID);
                if (categoryId > 0) {
                    // Try to get category name from cache first
                    String categoryName = mCategoryCache.get(categoryId);
                    
                    // If not in cache, try to fetch from database
                    if (categoryName == null) {
                        Category category = mCategoryDataSource.getCategoryById(categoryId);
                        if (category != null) {
                            categoryName = category.getName();
                            // Add to cache for future use
                            mCategoryCache.put(categoryId, categoryName);
                        }
                    }
                    
                    // Display category name if found
                    if (categoryName != null) {
                        categoryLabel.setText(categoryName);
                        categoryLabel.setVisibility(View.VISIBLE);
                        // 设置分类标签的背景颜色
                        categoryLabel.setBackgroundResource(R.drawable.status_background);
                    } else {
                        // Hide label if category not found
                        categoryLabel.setVisibility(View.GONE);
                    }
                } else {
                    // Hide label for notes with no category
                    categoryLabel.setVisibility(View.GONE);
                }
            }
        }
    }

    /**
     * onCreate is called when Android starts this Activity from scratch.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 设置布局文件
        setContentView(R.layout.activity_notes_list);
        
        // 初始化Toolbar
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        // 获取ListView引用
        mListView = (ListView) findViewById(R.id.notes_list);
        
        // 为ListView注册上下文菜单
        registerForContextMenu(mListView);

        // The user does not need to hold down the key to use menu shortcuts.
        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);

        /* If no data is given in the Intent that started this Activity, then this Activity
         * was started when the intent filter matched a MAIN action. We should use the default
         * provider URI.
         */
        // Gets the intent that started this Activity.
        Intent intent = getIntent();

        // If there is no data associated with the Intent, sets the data to the default URI, which
        // accesses a list of notes.
        if (intent.getData() == null) {
            intent.setData(Notes.CONTENT_URI);
            mCurrentUri = Notes.CONTENT_URI;
        } else {
            mCurrentUri = intent.getData();
        }

        // Create adapter with null cursor initially
        mAdapter = new NotesCursorAdapter(this, null);
        mListView.setAdapter(mAdapter);

        // Initialize category data source and cache
        mCategoryDataSource = new CategoryDataSource(this);
        mCategoryCache = new HashMap<>();
        initializeCategoryCache();

        // Check and request permissions
        checkAndRequestPermissions();

        // Initialize loader to load notes data
        getLoaderManager().initLoader(LOADER_ID, null, this);
        
        // Setup category filter spinner
        setupCategoryFilterSpinner();
        
        // 为ListView设置点击监听器
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onListItemClick(mListView, view, position, id);
            }
        });
    }

    /**
     * Setup category filter spinner with all available categories
     */
    private void setupCategoryFilterSpinner() {
        // This will be implemented when we modify the layout to include the spinner
    }
    
    /**
     * Initialize category cache with all available categories
     */
    private void initializeCategoryCache() {
        // Clear existing cache
        mCategoryCache.clear();
        
        // Get all categories
        List<Category> categories = mCategoryDataSource.getAllCategories();
        
        // Populate cache
        for (Category category : categories) {
            mCategoryCache.put(category.getId(), category.getName());
        }
    }

    /**
     * Setup category filter menu with all available categories
     */
    private void setupCategoryFilterMenu(Menu menu) {
        // Get category data source
        CategoryDataSource categoryDataSource = new CategoryDataSource(this);
        
        // Get all categories
        List<Category> categories = categoryDataSource.getAllCategories();
        
        // Find the category filter menu item
        MenuItem moreItem = menu.findItem(R.id.action_more);
        if (moreItem != null) {
            // Get the submenu
            SubMenu moreSubMenu = moreItem.getSubMenu();
            if (moreSubMenu != null) {
                // Find the category filter menu item within the more submenu
                MenuItem categoryFilterItem = moreSubMenu.findItem(R.id.menu_filter_by_category);
                if (categoryFilterItem != null) {
                    // Get the submenu of the category filter item
                    SubMenu subMenu = categoryFilterItem.getSubMenu();
                    if (subMenu != null) {
                        // Clear existing items
                        subMenu.clear();
                        
                        // Add "All" option
                        MenuItem allItem = subMenu.add(Menu.NONE, R.id.menu_show_all, Menu.NONE, "所有笔记");
                        allItem.setCheckable(true);
                        if (mCurrentCategoryId == 0) {
                            allItem.setChecked(true);
                        }
                        
                        // Add categories
                        for (Category category : categories) {
                            MenuItem item = subMenu.add(Menu.NONE, Menu.FIRST + (int) category.getId(), Menu.NONE, category.getName());
                            item.setCheckable(true);
                            if (mCurrentCategoryId == category.getId()) {
                                item.setChecked(true);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate menu from XML resource
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);

        // Get the SearchView and set up the listener
        MenuItem searchItem = menu.findItem(R.id.action_search);
        mSearchView = (SearchView) searchItem.getActionView();
        setupSearchView();

        // Add category filter options
        setupCategoryFilterMenu(menu);
        
        // Store reference to menu for later updates
        mOptionsMenu = menu;

        return true;
    }

    private void setupSearchView() {
        if (mSearchView == null) return;

        mSearchView.setIconifiedByDefault(true);
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mCurrentFilter = query;
                restartLoader();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mCurrentFilter = newText;
                restartLoader();
                return true;
            }
        });

        mSearchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus && mSearchView != null) {
                    if (mSearchView.getQuery().toString().isEmpty()) {
                        mCurrentFilter = "";
                        restartLoader();
                    }
                }
            }
        });
    }

    private void restartLoader() {
        Bundle args = new Bundle();
        args.putString("filter", mCurrentFilter);
        args.putParcelable("uri", mCurrentUri);
        getLoaderManager().restartLoader(LOADER_ID, args, this);
        
        // Update category filter menu
        if (mOptionsMenu != null) {
            setupCategoryFilterMenu(mOptionsMenu);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // The paste menu item is enabled if there is data on the clipboard.
        ClipboardManager clipboard = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);

        MenuItem mPasteItem = menu.findItem(R.id.menu_paste);

        // If the clipboard contains an item, enables the Paste option on the menu.
        boolean hasClipData = clipboard != null && clipboard.hasPrimaryClip() &&
                clipboard.getPrimaryClip() != null && clipboard.getPrimaryClip().getItemCount() > 0;

        if (mPasteItem != null) {
            mPasteItem.setEnabled(hasClipData);
        }

        // Gets the number of notes currently being displayed.
        final boolean haveItems = mAdapter.getCount() > 0;

        // If there are any notes in the list (which implies that one of
        // them is selected), then we need to generate the actions that
        // can be performed on the current selection.  This will be a combination
        // of our own specific actions along with any extensions that can be
        // found.
        if (haveItems) {

            // This is the selected item.
            // Since we're using AppCompatActivity instead of ListActivity, we need to get the selected item differently
            long selectedId = mListView.getSelectedItemId();
            // Check if a valid item is selected
            if (selectedId == AdapterView.INVALID_ROW_ID) {
                // No item selected, remove alternative actions
                menu.removeGroup(Menu.CATEGORY_ALTERNATIVE);
                return true;
            }
            Uri uri = ContentUris.withAppendedId(getIntent().getData(), selectedId);
            // Creates an array of Intents with one element. This will be used to send an Intent
            // based on the selected menu item.
            Intent[] specifics = new Intent[1];

            // Sets the Intent in the array to be an EDIT action on the URI of the selected note.
            specifics[0] = new Intent(Intent.ACTION_EDIT, uri);

            // Creates an array of menu items with one element. This will contain the EDIT option.
            MenuItem[] items = new MenuItem[1];

            // Creates an Intent with no specific action, using the URI of the selected note.
            Intent intent = new Intent(null, uri);

            /* Adds the category ALTERNATIVE to the Intent, with the note ID URI as its
             * data. This prepares the Intent as a place to group alternative options in the
             * menu.
             */
            intent.addCategory(Intent.CATEGORY_ALTERNATIVE);

            /*
             * Add alternatives to the menu
             */
            menu.addIntentOptions(
                    Menu.CATEGORY_ALTERNATIVE,  // Add the Intents as options in the alternatives group.
                    Menu.NONE,                  // A unique item ID is not required.
                    Menu.NONE,                  // The alternatives don't need to be in order.
                    null,                       // The caller's name is not excluded from the group.
                    specifics,                  // These specific options must appear first.
                    intent,                     // These Intent objects map to the options in specifics.
                    Menu.NONE,                  // No flags are required.
                    items                       // The menu items generated from the specifics-to-
                    // Intents mapping
            );
            // If the Edit menu item exists, adds shortcuts for it.
            if (items[0] != null) {

                // Sets the Edit menu item shortcut to numeric "1", letter "e"
                items[0].setShortcut('1', 'e');
            }
        } else {
            // If the list is empty, removes any existing alternative actions from the menu
            menu.removeGroup(Menu.CATEGORY_ALTERNATIVE);
        }

        // Displays the menu
        return true;
    }

    /**
     * This method is called when the user selects an option from the menu, but no item
     * in the list is selected. If the option was INSERT, then a new Intent is sent out with action
     * ACTION_INSERT. The data from the incoming Intent is put into the new Intent. In effect,
     * this triggers the NoteEditor activity in the NotePad application.
     *
     * If the item was not INSERT, then most likely it was an alternative option from another
     * application. The parent method is called to process the item.
     * @param item The menu item that was selected by the user
     * @return True, if the INSERT menu item was selected; otherwise, the result of calling
     * the parent method.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        
        // 处理Toolbar菜单项
        if (itemId == R.id.action_add) {
            // 修复Intent调用非导出组件问题
            Intent insertIntent = new Intent(Intent.ACTION_INSERT, getIntent().getData());
            insertIntent.setClass(this, NoteEditor.class);
            startActivity(insertIntent);
            return true;
        } else if (itemId == R.id.action_more) {
            // 更多操作菜单已自动展开
            return true;
        } else if (itemId == R.id.menu_paste) {
            // 修复Intent调用非导出组件问题
            Intent pasteIntent = new Intent(Intent.ACTION_PASTE, getIntent().getData());
            pasteIntent.setClass(this, NoteEditor.class);
            startActivity(pasteIntent);
            return true;
        }
        
        // 处理更多操作子菜单项
        if (itemId == R.id.menu_manage_categories) {
            startActivity(new Intent(this, CategoriesListActivity.class));
            return true;
        } else if (itemId == R.id.menu_show_all) {
            mCurrentUri = Notes.CONTENT_URI;
            mCurrentCategoryId = 0;
            restartLoader();
            return true;
        } else if (itemId == R.id.menu_show_todo) {
            // Use proper URI for todo notes
            mCurrentUri = Uri.withAppendedPath(Notes.CONTENT_URI, "todo");
            mCurrentCategoryId = 0;
            restartLoader();
            return true;
        } else if (itemId == R.id.menu_show_normal) {
            // Use proper URI for normal notes
            mCurrentUri = Uri.withAppendedPath(Notes.CONTENT_URI, "normal");
            mCurrentCategoryId = 0;
            restartLoader();
            return true;
        } else if (itemId == R.id.menu_filter_by_category) {
            // 显示分类筛选Bottom Sheet
            showCategoryFilterBottomSheet();
            return true;
        } else if (itemId >= Menu.FIRST && itemId <= Menu.FIRST + 1000) {
            // Handle category filter
            long categoryId = itemId - Menu.FIRST;
            filterByCategory(categoryId);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This method is called when the user context-clicks a note in the list. NotesList registers
     * itself as the handler for context menus in its ListView (this is done in onCreate()).
     *
     * The only available options are COPY and DELETE.
     *
     * Context-click is equivalent to long-press.
     *
     * @param menu A ContexMenu object to which items should be added.
     * @param view The View for which the context menu is being constructed.
     * @param menuInfo Data associated with view.
     * @throws ClassCastException
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {

        // The data from the menu item.
        AdapterView.AdapterContextMenuInfo info;

        // Tries to get the position of the item in the ListView that was long-pressed.
        try {
            // Casts the incoming data object into the type for AdapterView objects.
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            // If the menu object can't be cast, logs an error.
            Log.e(TAG, "bad menuInfo", e);
            return;
        }

        Cursor cursor = mAdapter.getCursor();
        if (cursor == null || !cursor.moveToPosition(info.position)) {
            return;
        }

        // Inflate menu from XML resource
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_context_menu, menu);

        // Sets the menu header to be the title of the selected note.
        menu.setHeaderTitle(cursor.getString(COLUMN_INDEX_TITLE));
    }

    /**
     * This method is called when the user selects an item from the context menu
     * (see onCreateContextMenu()). The only menu items that are actually handled are DELETE and
     * COPY. Anything else is an alternative option, for which default handling should be done.
     *
     * @param item The selected menu item
     * @return True if the menu item was DELETE, and no default processing is need, otherwise false,
     * which triggers the default handling of the item.
     * @throws ClassCastException
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // The data from the menu item.
        AdapterView.AdapterContextMenuInfo info;

        /*
         * Gets the extra info from the menu item. When an note in the Notes list is long-pressed, a
         * context menu appears. The menu items for the menu automatically get the data
         * associated with the note that was long-pressed. The data comes from the provider that
         * backs the list.
         *
         * The note's data is passed to the context menu creation routine in a ContextMenuInfo
         * object.
         *
         * When one of the context menu items is clicked, the same data is passed, along with the
         * note ID, to onContextItemSelected() via the item parameter.
         */
        try {
            // Casts the data object in the item into the type for AdapterView objects.
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {

            // If the object can't be cast, logs an error
            Log.e(TAG, "bad menuInfo", e);

            // Triggers default processing of the menu item.
            return false;
        }

        Cursor cursor = mAdapter.getCursor();
        if (cursor == null || !cursor.moveToPosition(info.position)) {
            return false;
        }

        long id = cursor.getLong(0); // ID is at index 0
        Uri noteUri = ContentUris.withAppendedId(getIntent().getData(), id);

        /*
         * Gets the menu item's ID and compares it to known actions.
         */
        int itemId = item.getItemId();
        
        // 使用if-else替换switch-case以解决常量表达式问题
        if (itemId == R.id.context_open) {
            // 修复Intent调用非导出组件问题
            Intent editIntent = new Intent(Intent.ACTION_EDIT, noteUri);
            editIntent.setClass(this, NoteEditor.class);
            startActivity(editIntent);
            return true;
        // Copy functionality disabled as per requirements
        } else if (itemId == R.id.context_delete) {
            // Deletes the note from the provider by passing in a URI in note ID format.
            int rowsDeleted = getContentResolver().delete(
                    noteUri,  // The URI of the provider
                    null,     // No where clause is needed, since only a single note ID is being
                    null      // No where clause is used, so no where arguments are needed.
            );

            if (rowsDeleted > 0) {
                Toast.makeText(this, R.string.note_deleted, Toast.LENGTH_SHORT).show();
                restartLoader();
            }
            return true;
        } else if (itemId == R.id.context_toggle_status) {
            // Toggle the todo status of the note
            ContentValues values = new ContentValues();
            int currentStatus = cursor.getInt(COLUMN_INDEX_STATUS);
            int newStatus = (currentStatus == Notes.STATUS_COMPLETED) ?
                    Notes.STATUS_PENDING : Notes.STATUS_COMPLETED;

            values.put(Notes.COLUMN_NAME_STATUS, newStatus);
            int rowsUpdated = getContentResolver().update(noteUri, values, null, null);

            if (rowsUpdated > 0) {
                Toast.makeText(this,
                        newStatus == Notes.STATUS_COMPLETED ?
                                R.string.note_marked_complete : R.string.note_marked_incomplete,
                        Toast.LENGTH_SHORT).show();
                restartLoader();
            }
            return true;
        } else {
            return super.onContextItemSelected(item);
        }
    }

    /**
     * Filter notes by category
     * @param categoryId the category ID to filter by
     */
    private void filterByCategory(long categoryId) {
        mCurrentCategoryId = categoryId;
        restartLoader();
        
        // Update menu item checked state
        if (mOptionsMenu != null) {
            MenuItem moreItem = mOptionsMenu.findItem(R.id.action_more);
            if (moreItem != null) {
                SubMenu moreSubMenu = moreItem.getSubMenu();
                if (moreSubMenu != null) {
                    MenuItem categoryFilterItem = moreSubMenu.findItem(R.id.menu_filter_by_category);
                    if (categoryFilterItem != null) {
                        SubMenu subMenu = categoryFilterItem.getSubMenu();
                        if (subMenu != null) {
                            // Uncheck all items
                            for (int i = 0; i < subMenu.size(); i++) {
                                MenuItem item = subMenu.getItem(i);
                                item.setChecked(false);
                            }
                            
                            // Check the selected item
                            for (int i = 0; i < subMenu.size(); i++) {
                                MenuItem item = subMenu.getItem(i);
                                if ((categoryId == 0 && item.getItemId() == R.id.menu_show_all) ||
                                    (categoryId > 0 && item.getItemId() == Menu.FIRST + categoryId)) {
                                    item.setChecked(true);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 显示分类筛选Bottom Sheet
     */
    private void showCategoryFilterBottomSheet() {
        // 创建BottomSheetDialog
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        
        // 加载布局
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_category_filter, null);
        bottomSheetDialog.setContentView(view);
        
        // 获取ListView并设置数据
        ListView categoryListView = (ListView) view.findViewById(R.id.category_list_view);
        
        // 获取所有分类
        List<Category> categories = mCategoryDataSource.getAllCategories();
        
        // 创建分类列表项
        List<String> categoryNames = new ArrayList<>();
        categoryNames.add("所有笔记"); // 添加"所有笔记"选项
        
        // 添加实际分类
        for (Category category : categories) {
            categoryNames.add(category.getName());
        }
        
        // 创建适配器
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_list_item_1, categoryNames);
        categoryListView.setAdapter(adapter);
        
        // 设置列表项点击监听器
        categoryListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 关闭Bottom Sheet
                bottomSheetDialog.dismiss();
                
                // 处理分类筛选
                if (position == 0) {
                    // 显示所有笔记
                    filterByCategory(0);
                } else {
                    // 根据分类名称查找分类ID
                    String selectedCategoryName = categoryNames.get(position);
                    long selectedCategoryId = 0;
                    
                    for (Category category : categories) {
                        if (category.getName().equals(selectedCategoryName)) {
                            selectedCategoryId = category.getId();
                            break;
                        }
                    }
                    
                    // 应用筛选
                    filterByCategory(selectedCategoryId);
                }
            }
        });
        
        // 显示Bottom Sheet
        bottomSheetDialog.show();
    }

    /**
     * This method is called when the user clicks a note in the displayed list.
     *
     * This method handles incoming actions of either PICK (get data from the provider) or
     * GET_CONTENT (get or create data). If the incoming action is EDIT, this method sends a
     * new Intent to start NoteEditor.
     * @param l The ListView that contains the clicked item
     * @param v The View of the individual item
     * @param position The position of v in the displayed list
     * @param id The row ID of the clicked item
     */
    protected void onListItemClick(ListView l, View v, int position, long id) {
        // Check if intent data is valid
        if (getIntent().getData() == null) {
            Log.e(TAG, "Intent data is null, cannot construct URI");
            return;
        }
        
        // Construct the URI for the selected item
        Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);

        // Gets the action from the incoming Intent
        String action = getIntent().getAction();

        // Handles requests for note data
        if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {
            // Sets the result to return to the component that called this Activity. The
            // result contains the new URI
            setResult(RESULT_OK, new Intent().setData(uri));
            finish();
        } else {
            // 修复Intent调用非导出组件问题
            Intent editIntent = new Intent(Intent.ACTION_EDIT, uri);
            editIntent.setClass(this, NoteEditor.class);
            startActivity(editIntent);
        }
    }

    // LoaderManager.LoaderCallbacks<Cursor> implementation

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String selection = null;
        String[] selectionArgs = null;
        String filter = "";
        Uri uri = mCurrentUri;

        if (args != null) {
            filter = args.getString("filter", "");
            if (args.containsKey("uri")) {
                uri = args.getParcelable("uri");
            }
        }

        // Build selection criteria
        StringBuilder selectionBuilder = new StringBuilder();
        List<String> selectionArgsList = new ArrayList<>();

        // Apply search filter if exists
        if (!filter.isEmpty()) {
            selectionBuilder.append("(").append(Notes.COLUMN_NAME_TITLE).append(" LIKE ? OR ")
                    .append(Notes.COLUMN_NAME_NOTE).append(" LIKE ?)");
            selectionArgsList.add("%" + filter + "%");
            selectionArgsList.add("%" + filter + "%");
        }

        // Apply category filter if exists
        if (mCurrentCategoryId > 0) {
            if (selectionBuilder.length() > 0) {
                selectionBuilder.append(" AND ");
            }
            selectionBuilder.append(Notes.COLUMN_NAME_CATEGORY_ID).append(" = ?");
            selectionArgsList.add(String.valueOf(mCurrentCategoryId));
        }

        // Convert to arrays
        if (selectionBuilder.length() > 0) {
            selection = selectionBuilder.toString();
            selectionArgs = selectionArgsList.toArray(new String[0]);
        }

        // Return the new loader
        return new CursorLoader(this, uri, PROJECTION, selection, selectionArgs,
                Notes.DEFAULT_SORT_ORDER);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    /**
     * Check and request storage permissions if needed
     */
    private void checkAndRequestPermissions() {
        // Check if we have write permission
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Request the permission
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                 android.Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission was granted, yay!
                    Toast.makeText(this, "存储权限已授予", Toast.LENGTH_SHORT).show();
                } else {
                    // Permission denied, boo!
                    Toast.makeText(this, "存储权限被拒绝，某些功能可能无法正常工作", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh category cache when resuming
        initializeCategoryCache();
        restartLoader();
    }
}