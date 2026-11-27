package com.example.lockguard;

import android.content.Context;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CaptureStorage {

    public static File getCapturesDir(Context context) {
        File dir = new File(context.getFilesDir(), "captures");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static File createNewCaptureFile(Context context, String prefix) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = prefix + "_" + timeStamp + ".jpg";
        return new File(getCapturesDir(context), fileName);
    }

    public static List<CaptureItem> loadAllCaptures(Context context) {
        List<CaptureItem> list = new ArrayList<>();
        File dir = getCapturesDir(context);
        File[] files = dir.listFiles((d, name) -> name.toLowerCase(Locale.US).endsWith(".jpg"));
        if (files == null || files.length == 0) {
            return list;
        }

        Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        SimpleDateFormat displayFmt = new SimpleDateFormat("yyyy-MM-dd | hh:mm:ss a", Locale.getDefault());

        for (File f : files) {
            String title = f.getName().startsWith("TEST_")
                    ? "لقطة تجريبية يدوية"
                    : "محاولة فتح قفل خاطئة";
            String dt = "التاريخ والوقت: " + displayFmt.format(new Date(f.lastModified()));
            list.add(new CaptureItem(f, title, dt, f.lastModified()));
        }
        return list;
    }
}
