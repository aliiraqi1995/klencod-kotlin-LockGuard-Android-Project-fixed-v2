package com.example.lockguard;

import java.io.File;

public class CaptureItem {
    public final File file;
    public final String title;
    public final String dateTimeFormatted;
    public final long timestamp;

    public CaptureItem(File file, String title, String dateTimeFormatted, long timestamp) {
        this.file = file;
        this.title = title;
        this.dateTimeFormatted = dateTimeFormatted;
        this.timestamp = timestamp;
    }
}
