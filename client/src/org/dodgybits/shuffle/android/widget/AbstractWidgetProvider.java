package org.dodgybits.shuffle.android.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;
import com.google.inject.Inject;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.activity.flurry.Analytics;
import org.dodgybits.shuffle.android.core.model.Context;
import org.dodgybits.shuffle.android.core.model.Project;
import org.dodgybits.shuffle.android.core.model.Task;
import org.dodgybits.shuffle.android.core.model.persistence.*;
import org.dodgybits.shuffle.android.core.model.persistence.selector.TaskSelector;
import org.dodgybits.shuffle.android.list.config.StandardTaskQueries;
import org.dodgybits.shuffle.android.persistence.provider.ContextProvider;
import org.dodgybits.shuffle.android.persistence.provider.ProjectProvider;
import org.dodgybits.shuffle.android.persistence.provider.TaskProvider;
import org.dodgybits.shuffle.android.preference.model.ListPreferenceSettings;
import org.dodgybits.shuffle.android.preference.model.Preferences;
import roboguice.inject.ContentResolverProvider;
import roboguice.util.Ln;

import java.util.*;

import static org.dodgybits.shuffle.android.core.util.Constants.cIdType;
import static org.dodgybits.shuffle.android.core.util.Constants.cPackage;
import static org.dodgybits.shuffle.android.core.util.Constants.cStringType;

public abstract class AbstractWidgetProvider extends RoboAppWidgetProvider {
    private static final HashMap<String,Integer> sIdCache = new HashMap<String,Integer>();

    @Inject TaskPersister mTaskPersister;
    @Inject ProjectPersister mProjectPersister;
    @Inject EntityCache<Project> mProjectCache;
    @Inject ContextPersister mContextPersister;
    @Inject EntityCache<Context> mContextCache;

    @Override
    public void handleReceive(android.content.Context context, Intent intent) {
        super.handleReceive(context, intent);

        String action = intent.getAction();
        if (TaskProvider.UPDATE_INTENT.equals(action) ||
                ProjectProvider.UPDATE_INTENT.equals(action) ||
                ContextProvider.UPDATE_INTENT.equals(action) ||
                Preferences.CLEAN_INBOX_INTENT.equals(action) ||
                ListPreferenceSettings.LIST_PREFERENCES_UPDATED.equals(action)) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            // Retrieve the identifiers for each instance of your chosen widget.
            ComponentName thisWidget = new ComponentName(context, getClass());
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
            if (appWidgetIds != null && appWidgetIds.length > 0) {
                this.onUpdate(context, appWidgetManager, appWidgetIds);
            }
        }
    }

    @Override
    public void onUpdate(android.content.Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Ln.d("onUpdate");
        ComponentName thisWidget = new ComponentName(context, getClass());
        int[] localAppWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        Arrays.sort(localAppWidgetIds);

        final int N = appWidgetIds.length;
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];
            if (Arrays.binarySearch(localAppWidgetIds, appWidgetId) >= 0) {
                String prefKey = Preferences.getWidgetQueryKey(appWidgetId);
                String queryName = Preferences.getWidgetQuery(context, prefKey);
                Ln.d("App widget %s found query %s for key %s", appWidgetId, queryName, prefKey);
                updateAppWidget(context, appWidgetManager, appWidgetId, queryName);
            } else {
                Ln.d("App widget %s not handled by this provider %s", appWidgetId, getClass());
            }
        }
    }

    @Override
    public void onDeleted(android.content.Context context, int[] appWidgetIds) {
        Ln.d("onDeleted");
        // When the user deletes the widget, delete the preference associated with it.
        final int N = appWidgetIds.length;
        SharedPreferences.Editor editor = Preferences.getEditor(context);
        for (int i=0; i<N; i++) {
            String prefKey = Preferences.getWidgetQueryKey(appWidgetIds[i]);
            editor.remove(prefKey);
        }
        editor.commit();
    }

    @Override
    public void onEnabled(android.content.Context context) {
    }

    @Override
    public void onDisabled(android.content.Context context) {
    }

    private void updateAppWidget(final android.content.Context androidContext, AppWidgetManager appWidgetManager,
            int appWidgetId, String queryName) {
        Ln.d("updateAppWidget appWidgetId=%s queryName=%s provider=%s", appWidgetId, queryName, getClass());

        RemoteViews views = new RemoteViews(androidContext.getPackageName(), getWidgetLayoutId());

        Cursor taskCursor = createCursor(androidContext, queryName);
        if (taskCursor == null) return;

        int titleId = getIdentifier(androidContext, "title_" + queryName, cStringType);
        views.setTextViewText(R.id.title, androidContext.getString(titleId) + " (" + taskCursor.getCount() + ")");

        setupFrameClickIntents(androidContext, views, queryName);

        int totalEntries = getTotalEntries();
        for (int taskCount = 1; taskCount <= totalEntries; taskCount++) {
            Task task = null;
            Project project = null;
            Context context = null;
            if (taskCursor.moveToNext()) {
                task = mTaskPersister.read(taskCursor);
                project = mProjectCache.findById(task.getProjectId());
                context = mContextCache.findById(task.getContextId());
            }

            int descriptionViewId = updateDescription(androidContext, views, task, taskCount);
            int projectViewId = updateProject(androidContext, views, project, taskCount);
            int contextIconId = updateContext(androidContext, views, context, taskCount);

            if (task != null) {
                Uri.Builder builder = TaskProvider.Tasks.CONTENT_URI.buildUpon();
                ContentUris.appendId(builder, task.getLocalId().getId());
                Uri taskUri = builder.build();
                Intent intent = new Intent(Intent.ACTION_VIEW, taskUri);
                Ln.d("Adding pending event for viewing uri %s", taskUri);
                int entryId = getIdIdentifier(androidContext, "entry_" + taskCount);
                PendingIntent pendingIntent = PendingIntent.getActivity(androidContext, 0, intent, 0);
                views.setOnClickPendingIntent(entryId, pendingIntent);
                views.setOnClickPendingIntent(descriptionViewId, pendingIntent);
                views.setOnClickPendingIntent(projectViewId, pendingIntent);
                if (contextIconId != 0) {
                    views.setOnClickPendingIntent(contextIconId, pendingIntent);
                }
            }

        }
        taskCursor.close();

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    protected Cursor createCursor(android.content.Context androidContext, String queryName) {
        TaskSelector query = StandardTaskQueries.getQuery(queryName);
        if (query == null) return null;

        String key = StandardTaskQueries.getFilterPrefsKey(queryName);
        ListPreferenceSettings settings = new ListPreferenceSettings(key);
        query = query.builderFrom().applyListPreferences(androidContext, settings).build();

        return androidContext.getContentResolver().query(
                TaskProvider.Tasks.CONTENT_URI,
                TaskProvider.Tasks.FULL_PROJECTION,
                query.getSelection(androidContext),
                query.getSelectionArgs(),
                query.getSortOrder());
    }

    abstract int getWidgetLayoutId();

    abstract int getTotalEntries();

    protected void setupFrameClickIntents(android.content.Context androidContext, RemoteViews views, String queryName){
        Intent intent = StandardTaskQueries.getActivityIntent(androidContext, queryName);
        PendingIntent pendingIntent = PendingIntent.getActivity(androidContext, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.title, pendingIntent);

        intent = new Intent(Intent.ACTION_INSERT, TaskProvider.Tasks.CONTENT_URI);
        pendingIntent = PendingIntent.getActivity(androidContext, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.add_task, pendingIntent);
    }

    protected int updateDescription(android.content.Context androidContext, RemoteViews views, Task task, int taskCount) {
        int descriptionViewId = getIdIdentifier(androidContext, "description_" + taskCount);
        if (descriptionViewId != 0) {
            views.setTextViewText(descriptionViewId, task != null ? task.getDescription() : "");
        }

        return descriptionViewId;
    }

    protected int updateProject(android.content.Context androidContext, RemoteViews views, Project project, int taskCount) {
        int projectViewId = getIdIdentifier(androidContext, "project_" + taskCount);
        views.setViewVisibility(projectViewId, project == null ? View.INVISIBLE : View.VISIBLE);
        views.setTextViewText(projectViewId, project != null ? project.getName() : "");

        return projectViewId;
    }

    abstract protected int updateContext(android.content.Context androidContext, RemoteViews views, Context context, int taskCount);



    static int getIdIdentifier(android.content.Context context, String name) {
        Integer id = sIdCache.get(name);
        if (id == null) {
            id = getIdentifier(context, name, cIdType);
            if (id == 0) return id;
            sIdCache.put(name, id);
        }
        Ln.d("Got id " + id + " for resource " + name);
        return id;
    }

    static int getIdentifier(android.content.Context context, String name, String type) {
        int id = context.getResources().getIdentifier(
                    name, type, cPackage);
        Ln.d("Got id " + id + " for resource " + name);
        return id;
    }

}
