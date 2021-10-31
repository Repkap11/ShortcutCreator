package com.repkap11.shortcut_creator;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.repkap11.shortcut_creater.R;

public class MainFragment extends Fragment implements RecycleAdapter.Callbacks {
    private RecycleAdapter mAdapter;

    private static int getAppIconSize(Context context) {
        ActivityManager activityManager = context.getSystemService(ActivityManager.class);
        return activityManager.getLauncherLargeIconSize();
    }

    private static IconCompat convertAppIconDrawableToBitmap(Context context, Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            Log.i("Paul", "convertAppIconDrawableToBitmap: Is BitmapDrawable");
            return IconCompat.createWithBitmap(((BitmapDrawable) drawable).getBitmap());
        }
        int appIconSize = getAppIconSize(context);
//        int appIconDensity = getAppIconDensity(context);

//        final float screenDensity = context.getResources().getDisplayMetrics().density;
        final int adaptiveIconOuterSides = (int) Math.ceil(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, context.getResources().getDisplayMetrics()));
        final int adaptiveIconSize = appIconSize + adaptiveIconOuterSides;

        Log.i("Paul", "convertAppIconDrawableToBitmap: Icon size:" + appIconSize + " outer:" + adaptiveIconOuterSides);

        final Bitmap bitmap = Bitmap.createBitmap(adaptiveIconSize, adaptiveIconSize, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        drawable.setBounds(adaptiveIconOuterSides, adaptiveIconOuterSides, adaptiveIconSize - adaptiveIconOuterSides, adaptiveIconSize - adaptiveIconOuterSides);
        drawable.draw(canvas);
        return IconCompat.createWithAdaptiveBitmap(bitmap);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mAdapter = new RecycleAdapter(requireContext(), this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mAdapter.close();
        mAdapter = null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        RecyclerViewEmptySupport rv = rootView.findViewById(R.id.recycle);
        rv.setEmptyView(rootView.findViewById(R.id.fragment_main_empty_view));

        GridLayoutManager glm = (GridLayoutManager) rv.getLayoutManager();
        glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                switch (mAdapter.getItemViewType(position)) {
                    case 0:
                        return 1;

                    default:
                        return glm.getSpanCount();
                }
            }
        });
        rv.setAdapter(mAdapter);
        return rootView;
    }

    public void onIconClicked(RecycleAdapter.AppInfo info) {
        Context context = requireContext().getApplicationContext();
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            Toast.makeText(context, "Pinned Shortcuts Not Supported", Toast.LENGTH_SHORT).show();
            return;
        }

        String settingsPackage = info.packageName;
        String settingsClass = info.activityClassName;

        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(settingsPackage);
        launchIntent.setClassName(settingsPackage, settingsClass);

        ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(context, context.getPackageName() + ":" + MainActivity.class.getName() + ":" + settingsPackage + ":" + settingsClass)
                .setShortLabel(info.label)
                .setIcon(convertAppIconDrawableToBitmap(context, info.icon))
                .setAlwaysBadged()
                .setIntent(launchIntent)
                .build();

        ShortcutManagerCompat.requestPinShortcut(context, shortcut, null);
    }

//    private static int getAppIconDensity(Context context) {
//        ActivityManager activityManager = context.getSystemService(ActivityManager.class);
//        return activityManager.getLauncherLargeIconDensity();
//    }


    @Override
    public void onResume() {
        super.onResume();
        mAdapter.notifyDataSetChanged();
    }
}
