package com.jingping.adshield.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.widget.Button;
import android.widget.ListView;

import com.jingping.adshield.App;
import com.jingping.adshield.R;
import com.jingping.adshield.core.Prefs;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 应用多选器（不依赖 AndroidX）。 */
public final class AppPicker {

    public static final class Entry {
        public final String pkg;
        public final String label;

        Entry(String pkg, String label) {
            this.pkg = pkg;
            this.label = label;
        }
    }

    private AppPicker() {
    }

    public static void show(final Activity a, final Runnable onSaved) {
        final Context app = a.getApplicationContext();
        App.IO.execute(() -> {
            final List<Entry> apps = load(app);
            App.MAIN.post(() -> {
                if (a.isFinishing()) {
                    return;
                }
                showDialog(a, apps, onSaved);
            });
        });
    }

    private static List<Entry> load(Context c) {
        List<Entry> out = new ArrayList<>();
        try {
            PackageManager pm = c.getPackageManager();
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> ris = pm.queryIntentActivities(i, PackageManager.MATCH_ALL);
            Set<String> seen = new HashSet<>();
            for (ResolveInfo ri : ris) {
                if (ri == null || ri.activityInfo == null || ri.activityInfo.applicationInfo == null) {
                    continue;
                }
                ApplicationInfo ai = ri.activityInfo.applicationInfo;
                String pkg = ai.packageName;
                if (pkg == null || pkg.equals(c.getPackageName()) || seen.contains(pkg)) {
                    continue;
                }
                if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                    continue;
                }
                seen.add(pkg);
                String label;
                try {
                    label = pm.getApplicationLabel(ai).toString();
                } catch (Throwable t) {
                    label = pkg;
                }
                out.add(new Entry(pkg, label));
            }
            final Collator collator = Collator.getInstance(Locale.CHINA);
            Collections.sort(out, new Comparator<Entry>() {
                @Override
                public int compare(Entry x, Entry y) {
                    int r = collator.compare(x.label, y.label);
                    return r != 0 ? r : x.pkg.compareTo(y.pkg);
                }
            });
        } catch (Throwable ignored) {
        }
        return out;
    }

    private static void showDialog(final Activity a, final List<Entry> apps, final Runnable onSaved) {
        if (apps.isEmpty()) {
            return;
        }
        Set<String> scope = Prefs.scope(a);
        final String[] labels = new String[apps.size()];
        final boolean[] checked = new boolean[apps.size()];
        for (int i = 0; i < apps.size(); i++) {
            Entry e = apps.get(i);
            labels[i] = e.label;
            checked[i] = scope.contains(e.pkg);
        }

        AlertDialog.Builder b = new AlertDialog.Builder(a);
        b.setTitle(R.string.scope_manage);
        b.setMultiChoiceItems(labels, checked, (dialog, which, isChecked) -> checked[which] = isChecked);
        b.setPositiveButton(R.string.dialog_save, (dialog, which) -> {
            Set<String> selected = new HashSet<>();
            for (int i = 0; i < apps.size(); i++) {
                if (checked[i]) {
                    selected.add(apps.get(i).pkg);
                }
            }
            Prefs.setScope(a, selected);
            if (onSaved != null) {
                onSaved.run();
            }
        });
        b.setNegativeButton(R.string.dialog_cancel, null);
        b.setNeutralButton("全选", null);

        final AlertDialog dialog = b.create();
        dialog.setOnShowListener(d -> {
            Button neutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            if (neutral == null) {
                return;
            }
            neutral.setOnClickListener(v -> {
                boolean anyOff = false;
                for (boolean c : checked) {
                    if (!c) {
                        anyOff = true;
                        break;
                    }
                }
                final boolean target = anyOff;
                ListView lv = dialog.getListView();
                for (int i = 0; i < checked.length; i++) {
                    checked[i] = target;
                    if (lv != null) {
                        lv.setItemChecked(i, target);
                    }
                }
                neutral.setText(target ? "清空" : "全选");
            });
        });
        dialog.show();
    }
}
