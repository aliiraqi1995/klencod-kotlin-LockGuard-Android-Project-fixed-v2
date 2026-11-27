package com.example.lockguard;

import android.Manifest;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView tvGuardStatus;
    private TextView tvDiagnostics;
    private TextView tvCapturesHeader;
    private TextView tvEmptyState;
    private MaterialButton btnToggleGuard;
    private RecyclerView rvCaptures;

    private final List<CaptureItem> captureList = new ArrayList<>();
    private CaptureAdapter adapter;

    private DevicePolicyManager devicePolicyManager;
    private ComponentName adminComponent;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                updateStatusUI();
                Toast.makeText(this, "تم تحديث حالة الأذونات", Toast.LENGTH_SHORT).show();
            });

    private final BroadcastReceiver captureSavedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadCapturesList();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(this, IntruderDeviceAdminReceiver.class);

        tvGuardStatus = findViewById(R.id.tvGuardStatus);
        tvDiagnostics = findViewById(R.id.tvDiagnostics);
        tvCapturesHeader = findViewById(R.id.tvCapturesHeader);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        btnToggleGuard = findViewById(R.id.btnToggleGuard);
        MaterialButton btnTestCapture = findViewById(R.id.btnTestCapture);
        MaterialButton btnPermissions = findViewById(R.id.btnPermissions);
        MaterialButton btnDeviceAdmin = findViewById(R.id.btnDeviceAdmin);
        MaterialButton btnBattery = findViewById(R.id.btnBattery);
        MaterialButton btnRefresh = findViewById(R.id.btnRefresh);
        rvCaptures = findViewById(R.id.rvCaptures);

        adapter = new CaptureAdapter(captureList, new CaptureAdapter.OnCaptureActionListener() {
            @Override
            public void onShare(CaptureItem item) {
                shareCaptureItem(item);
            }

            @Override
            public void onDelete(CaptureItem item, int position) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("حذف اللقطة")
                        .setMessage("هل تريد حذف هذه الصورة نهائياً؟")
                        .setPositiveButton("حذف", (d, w) -> {
                            if (item.file.exists() && item.file.delete()) {
                                captureList.remove(position);
                                adapter.notifyItemRemoved(position);
                                updateCapturesCounter();
                                Toast.makeText(MainActivity.this, "تم الحذف", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("إلغاء", null)
                        .show();
            }
        });

        rvCaptures.setLayoutManager(new LinearLayoutManager(this));
        rvCaptures.setAdapter(adapter);

        btnToggleGuard.setOnClickListener(v -> toggleGuardService());
        btnTestCapture.setOnClickListener(v -> {
            if (!hasCameraPermission()) {
                requestRequiredPermissions();
                return;
            }
            Intent testIntent = new Intent(this, SilentCaptureActivity.class);
            testIntent.putExtra(SilentCaptureActivity.EXTRA_IS_TEST, true);
            startActivity(testIntent);
        });

        btnPermissions.setOnClickListener(v -> requestRequiredPermissions());
        btnDeviceAdmin.setOnClickListener(v -> toggleDeviceAdmin());
        btnBattery.setOnClickListener(v -> requestIgnoreBatteryOptimizations());
        btnRefresh.setOnClickListener(v -> loadCapturesList());

        IntentFilter filter = new IntentFilter(SilentCaptureActivity.ACTION_CAPTURE_SAVED);
        ContextCompat.registerReceiver(this, captureSavedReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatusUI();
        loadCapturesList();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(captureSavedReceiver);
    }

    private void toggleGuardService() {
        boolean currentlyEnabled = GuardPrefs.isGuardEnabled(this);
        if (!currentlyEnabled) {
            if (!hasCameraPermission()) {
                Toast.makeText(this, "يرجى منح إذن الكاميرا أولاً", Toast.LENGTH_SHORT).show();
                requestRequiredPermissions();
                return;
            }
            if (!isDeviceAdminActive()) {
                Toast.makeText(this, "يرجى تفعيل خيار المسؤول لاكتشاف أخطاء القفل", Toast.LENGTH_LONG).show();
                toggleDeviceAdmin();
                return;
            }
            GuardPrefs.setGuardEnabled(this, true);
            Intent serviceIntent = new Intent(this, GuardForegroundService.class);
            ContextCompat.startForegroundService(this, serviceIntent);
            Toast.makeText(this, "تم تفعيل حارس قفل الشاشة", Toast.LENGTH_SHORT).show();
        } else {
            GuardPrefs.setGuardEnabled(this, false);
            stopService(new Intent(this, GuardForegroundService.class));
            Toast.makeText(this, "تم إيقاف حارس قفل الشاشة", Toast.LENGTH_SHORT).show();
        }
        updateStatusUI();
    }

    private void requestRequiredPermissions() {
        List<String> needed = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.CAMERA);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        if (!needed.isEmpty()) {
            permissionLauncher.launch(needed.toArray(new String[0]));
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent overlayIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(overlayIntent);
            Toast.makeText(this, "فعّل الظهور فوق التطبيقات لضمان العمل فوق شاشة القفل", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "جميع الأذونات الأساسية ممنوحة بالفعل", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleDeviceAdmin() {
        if (isDeviceAdminActive()) {
            devicePolicyManager.removeActiveAdmin(adminComponent);
            Toast.makeText(this, "تم إلغاء تفعيل صلاحية المسؤول", Toast.LENGTH_SHORT).show();
            updateStatusUI();
        } else {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "مطلوب لرصد محاولات إدخال رمز القفل الخاطئة والتقاط صورة المتطفل تلقائياً.");
            startActivity(intent);
        }
    }

    private void requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } else {
                Intent settingsIntent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                startActivity(settingsIntent);
                Toast.makeText(this, "التطبيق مستثنى بالفعل من قيود البطارية", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void shareCaptureItem(CaptureItem item) {
        if (!item.file.exists()) {
            return;
        }
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", item.file);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/jpeg");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.putExtra(Intent.EXTRA_TEXT, item.title + "\n" + item.dateTimeFormatted);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "مشاركة اللقطة عبر"));
    }

    private void loadCapturesList() {
        captureList.clear();
        captureList.addAll(CaptureStorage.loadAllCaptures(this));
        adapter.notifyDataSetChanged();
        updateCapturesCounter();
    }

    private void updateCapturesCounter() {
        int count = captureList.size();
        tvCapturesHeader.setText("اللقطات المصورة (" + count + ")");
        tvEmptyState.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
        rvCaptures.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isDeviceAdminActive() {
        return devicePolicyManager != null && devicePolicyManager.isAdminActive(adminComponent);
    }

    private boolean isBatteryUnrestricted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            return pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
        }
        return true;
    }

    private void updateStatusUI() {
        boolean guardOn = GuardPrefs.isGuardEnabled(this);
        tvGuardStatus.setText(guardOn ? "الحالة: نشط ويراقب قفل الشاشة" : "الحالة: متوقف");
        btnToggleGuard.setText(guardOn ? "إيقاف الحارس" : "تشغيل الحارس");

        String camState = hasCameraPermission() ? "ممنوحة" : "ناقصة";
        String adminState = isDeviceAdminActive() ? "مفعّل" : "غير مفعّل";
        String batState = isBatteryUnrestricted() ? "غير مقيدة" : "مقيدة";
        tvDiagnostics.setText("الأذونات: " + camState + " | المسؤول: " + adminState + " | البطارية: " + batState);
    }
}
