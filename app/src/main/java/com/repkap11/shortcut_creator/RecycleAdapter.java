package com.repkap11.shortcut_creator;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.repkap11.shortcut_creater.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecycleAdapter extends RecyclerView.Adapter<RecycleAdapter.ViewHolder> {


    private static final String TAG = RecycleAdapter.class.getSimpleName();
    private final Callbacks mCallback;
    private final Handler mBackgroundHandler;
    private final HandlerThread mHandlerThread;
    private final Context mContext;
    private final Handler mMainHandler;
    private List<AppInfo> mAppsList;
    private boolean mShouldQuit = false;

    public RecycleAdapter(Context c, Callbacks callback) {
        //This is where we build our list of app details, using the app
        //object we created to store the label, package name and icon
        mCallback = callback;
        mContext = c.getApplicationContext();

        mMainHandler = new Handler(Looper.getMainLooper());
        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        mBackgroundHandler = new Handler(mHandlerThread.getLooper());
        mAppsList = Collections.emptyList();
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                readAppsList();
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        notifyDataSetChanged();
                    }
                });
            }
        });
    }

    private static boolean isSystem(ResolveInfo ri) {
        return (ri.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) + (ri.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
    }

    private static void appInfoFromResolve(ArrayList<AppInfo> appsList, PackageManager pm, ResolveInfo ri) {
        AppInfo app = new AppInfo();
        app.label = ri.loadLabel(pm);
        app.packageName = ri.activityInfo.packageName;
        app.activityClassName = ri.activityInfo.name;
        app.icon = ri.activityInfo.loadUnbadgedIcon(pm);
        app.isTitle = false;
        appsList.add(app);
    }

    private void readAppsList() {
        PackageManager pm = mContext.getPackageManager();

        ArrayList<AppInfo> appsList = new ArrayList<AppInfo>();

        Intent i = new Intent(Intent.ACTION_MAIN, null);
        i.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> allApps = pm.queryIntentActivities(i, 0);
        appsList.add(new AppInfo("Debuggable"));
        for (ResolveInfo ri : allApps) {
            if (isDebuggable(ri) && !isSystem(ri)) {
                appInfoFromResolve(appsList, pm, ri);
            }
        }
        appsList.add(new AppInfo("Non Debuggable"));
        for (ResolveInfo ri : allApps) {
            if (!isDebuggable(ri) && !isSystem(ri)) {
                appInfoFromResolve(appsList, pm, ri);
            }
        }
        appsList.add(new AppInfo("System"));
        for (ResolveInfo ri : allApps) {
            if (isSystem(ri)) {
                appInfoFromResolve(appsList, pm, ri);
            }
        }
        mAppsList = appsList;
    }

    private boolean isDebuggable(ResolveInfo ri) {
        return (ri.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    @Override
    public void onBindViewHolder(RecycleAdapter.ViewHolder viewHolder, int i) {
        AppInfo info = mAppsList.get(i);
        viewHolder.textView.setText(info.label);

        if (viewHolder.type == 0) {
            viewHolder.img.setImageDrawable(info.icon);
        } else {
            viewHolder.divider.setVisibility(i == 0 ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {

        //This method needs to be overridden so that Androids knows how many items
        //will be making it into the list

        return mAppsList.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (mAppsList.get(position).isTitle) {
            return 1;
        }
        return 0;
    }

    @Override
    public RecycleAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        //This is what adds the code we've written in here to our target view
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view;
        if (viewType == 0) {
            view = inflater.inflate(R.layout.fragment_main_element, parent, false);
        } else {
            view = inflater.inflate(R.layout.fragment_main_element_space, parent, false);
        }
        ViewHolder viewHolder = new ViewHolder(view, viewType);
        return viewHolder;
    }

    public void close() {
        mShouldQuit = true;
        mHandlerThread.quitSafely();
    }

    public interface Callbacks {
        void onIconClicked(AppInfo info);
    }

    public static class AppInfo {
        boolean isTitle;
        CharSequence label;
        String packageName;
        String activityClassName;
        Drawable icon;

        public AppInfo(String title) {
            label = title;
            isTitle = true;
        }

        public AppInfo() {
            this.isTitle = false;
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        public TextView textView;
        public ImageView img;
        public int type;
        private View divider;

        public ViewHolder(View itemView, int inType) {
            super(itemView);
            type = inType;
            textView = itemView.findViewById(R.id.text);
            divider = itemView.findViewById(R.id.divider);
            if (type == 0) {
                img = itemView.findViewById(R.id.img);
                itemView.setOnClickListener(this);
                itemView.setOnLongClickListener(this);
            }
        }

        @Override
        public void onClick(View v) {
            onLongClick(v);
        }

        @Override
        public boolean onLongClick(View v) {
            int pos = getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) {
                return false;
            }
            AppInfo info = mAppsList.get(pos);
            mCallback.onIconClicked(info);
            notifyDataSetChanged();
            return true;
        }
    }
}