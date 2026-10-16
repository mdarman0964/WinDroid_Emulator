package com.winlator.cmod;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.winlator.cmod.container.ContainerManager;

import java.io.File;

public class Mt5InstallerFragment extends Fragment {
    private static final String MT5_INSTALLER_URL =
            "https://download.mql5.com/cdn/web/metaquotes.software.corp/mt5/mt5setup.exe";
    private static final String INSTALLER_NAME = "mt5setup.exe";

    @Nullable
    @Override
    public View onCreateView(@NonNull android.view.LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context context = requireContext();
        int pad = (int) (20 * getResources().getDisplayMetrics().density);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(pad, pad, pad, pad);
        layout.setGravity(Gravity.TOP);
        layout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView title = new TextView(context);
        title.setText("MetaTrader 5 for Windows");
        title.setTextSize(22);
        title.setTextColor(0xFF2196F3);
        layout.addView(title);

        TextView description = new TextView(context);
        description.setText("Download the official MetaTrader 5 installer from MetaQuotes. After downloading, open the installer from the WinDroid shared Download folder inside your Windows container.");
        description.setTextSize(15);
        description.setPadding(0, pad / 2, 0, pad);
        layout.addView(description);

        TextView path = new TextView(context);
        path.setText("Save location: Download/WinDroid/" + INSTALLER_NAME);
        path.setTextSize(13);
        path.setPadding(0, 0, 0, pad);
        layout.addView(path);

        Button download = new Button(context);
        download.setText("Download MT5 Installer");
        layout.addView(download, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Button prepareAll = new Button(context);
        prepareAll.setText("Prepare MT5 in All Containers");
        layout.addView(prepareAll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView status = new TextView(context);
        status.setTextSize(14);
        status.setPadding(0, pad / 2, 0, pad);
        layout.addView(status);

        Button openSite = new Button(context);
        openSite.setText("Open Official MetaTrader Website");
        layout.addView(openSite, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        File downloadDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "WinDroid");
        File installer = new File(downloadDir, INSTALLER_NAME);
        status.setText(installer.exists()
                ? "Installer already downloaded (" + formatSize(installer.length()) + "). You can download it again if needed."
                : "Installer not downloaded yet.");

        prepareAll.setOnClickListener(v -> {
            try {
                ContainerManager containerManager = new ContainerManager(context);
                int count = Mt5ContainerSetup.prepareAll(context, containerManager);
                if (count == 0) {
                    status.setText("No Containers found. Create a Container first, then try again.");
                    Toast.makeText(context, "No Containers found.", Toast.LENGTH_LONG).show();
                } else {
                    status.setText("MT5 installer shortcut prepared in " + count
                            + " Container(s). Download the installer, start a Container, then open the MetaTrader 5 Installer shortcut on its Windows desktop.");
                    Toast.makeText(context, "Prepared MT5 for " + count + " Container(s).", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                status.setText("Could not prepare MT5 in all Containers: " + e.getMessage());
                Toast.makeText(context, "Could not prepare MT5 shortcuts.", Toast.LENGTH_LONG).show();
            }
        });

        download.setOnClickListener(v -> {
            try {
                if (!downloadDir.exists() && !downloadDir.mkdirs()) {
                    Toast.makeText(context, "Could not create Download/WinDroid folder.", Toast.LENGTH_LONG).show();
                    return;
                }
                if (installer.exists() && !installer.delete()) {
                    Toast.makeText(context, "Could not replace the existing installer file.", Toast.LENGTH_LONG).show();
                    return;
                }
                DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                if (manager == null) {
                    Toast.makeText(context, "Android Download Manager is unavailable.", Toast.LENGTH_LONG).show();
                    return;
                }
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(MT5_INSTALLER_URL));
                request.setTitle("MetaTrader 5 for Windows");
                request.setDescription("Downloading the official MetaQuotes installer");
                request.setMimeType("application/vnd.microsoft.portable-executable");
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setAllowedOverMetered(true);
                request.setAllowedOverRoaming(true);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "WinDroid/" + INSTALLER_NAME);
                long id = manager.enqueue(request);
                status.setText("Download started. Android will show progress in notifications. Download ID: " + id);
            } catch (Exception e) {
                status.setText("Could not start download: " + e.getMessage());
            }
        });

        openSite.setOnClickListener(v -> {
            try {
                startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,
                        Uri.parse("https://www.metatrader5.com/en/download")));
            } catch (Exception e) {
                Toast.makeText(context, "No browser app is available.", Toast.LENGTH_LONG).show();
            }
        });

        return layout;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().setTitle("MT5 Installer");
            }
        }
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(java.util.Locale.US, "%.1f KB", bytes / 1024.0);
        return String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
