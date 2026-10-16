package com.winlator.cmod;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DiagnosticsFragment extends Fragment {
    private static final String PREFS = "windroid_diagnostics";
    private static final String NOTE_KEY = "problem_notes";
    private static final int MAX_FILES = 150;
    private static final long MAX_READ_BYTES = 256 * 1024L;

    private LinearLayout fileList;
    private TextView scanStatus;

    @Nullable
    @Override
    public View onCreateView(@NonNull android.view.LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context context = requireContext();
        int pad = dp(16);

        ScrollView scroll = new ScrollView(context);
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);

        TextView notesTitle = heading("Problem Notes");
        root.addView(notesTitle);

        TextView notesHelp = new TextView(context);
        notesHelp.setText("Write down emulator issues and steps to reproduce them. Notes stay on this phone.");
        notesHelp.setTextSize(14);
        notesHelp.setPadding(0, dp(4), 0, dp(8));
        root.addView(notesHelp);

        EditText notes = new EditText(context);
        notes.setGravity(Gravity.TOP | Gravity.START);
        notes.setMinLines(4);
        notes.setMaxLines(10);
        notes.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        notes.setHint("Example: Container starts, but MT5 window does not appear…");
        notes.setText(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(NOTE_KEY, ""));
        root.addView(notes, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Button save = new Button(context);
        save.setText("Save Notes");
        root.addView(save);
        save.setOnClickListener(v -> {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putString(NOTE_KEY, notes.getText().toString()).apply();
            Toast.makeText(context, "Problem notes saved on this phone.", Toast.LENGTH_SHORT).show();
        });

        TextView filesTitle = heading("Log / Lock File Viewer");
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.topMargin = dp(20);
        root.addView(filesTitle, titleParams);

        TextView help = new TextView(context);
        help.setText("Scans WinDroid's accessible folders for log, crash, debug, and lock files. Tap a text file to inspect it.");
        help.setTextSize(14);
        help.setPadding(0, dp(4), 0, dp(8));
        root.addView(help);

        Button refresh = new Button(context);
        refresh.setText("Refresh File List");
        root.addView(refresh);

        scanStatus = new TextView(context);
        scanStatus.setTextSize(13);
        scanStatus.setPadding(0, dp(8), 0, dp(8));
        root.addView(scanStatus);

        fileList = new LinearLayout(context);
        fileList.setOrientation(LinearLayout.VERTICAL);
        root.addView(fileList);

        refresh.setOnClickListener(v -> scanFiles());
        scanFiles();
        return scroll;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().setTitle("Problem Notes & Logs");
            }
        }
    }

    private TextView heading(String value) {
        TextView view = new TextView(requireContext());
        view.setText(value);
        view.setTextSize(20);
        view.setTextColor(0xFF2196F3);
        return view;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void scanFiles() {
        if (fileList == null || scanStatus == null) return;
        fileList.removeAllViews();
        List<File> found = new ArrayList<>();
        addTree(found, requireContext().getFilesDir(), 0);
        addTree(found, requireContext().getCacheDir(), 0);
        File external = requireContext().getExternalFilesDir(null);
        if (external != null) addTree(found, external, 0);
        addTree(found, new File(Environment.getExternalStorageDirectory(), "Winlator"), 0);
        addTree(found, new File(Environment.getExternalStorageDirectory(), "WinDroid"), 0);
        addTree(found, new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "WinDroid"), 0);

        scanStatus.setText(found.size() + " matching file(s) found. Only readable text is shown.");
        if (found.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText("No matching log or lock files were found in the scanned folders.");
            empty.setPadding(0, dp(8), 0, dp(8));
            fileList.addView(empty);
            return;
        }

        for (File file : found) {
            Button item = new Button(requireContext());
            item.setAllCaps(false);
            item.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            item.setText(file.getName() + "  •  " + formatSize(file.length()) + "\n" + file.getAbsolutePath());
            fileList.addView(item, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            item.setOnClickListener(v -> showFile(file));
        }
    }

    private void addTree(List<File> found, File directory, int depth) {
        if (directory == null || depth > 4 || found.size() >= MAX_FILES || !directory.exists() || !directory.isDirectory()) return;
        File[] children = directory.listFiles();
        if (children == null) return;
        for (File file : children) {
            if (found.size() >= MAX_FILES) return;
            if (file.isDirectory()) {
                if (!file.getName().equals(".") && !file.getName().equals("..")) addTree(found, file, depth + 1);
            } else if (file.isFile() && isInteresting(file.getName())) {
                if (!found.contains(file)) found.add(file);
            }
        }
    }

    private boolean isInteresting(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".log") || lower.endsWith(".lock") || lower.endsWith(".lck")
                || lower.contains("crash") || lower.contains("debug") || lower.contains("wine-log")
                || lower.equals("stderr") || lower.equals("stdout");
    }

    private void showFile(File file) {
        StringBuilder text = new StringBuilder();
        try {
            if (file.length() > MAX_READ_BYTES) {
                text.append("[File truncated to first 256 KB]\n\n");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(file), StandardCharsets.UTF_8))) {
                char[] buffer = new char[4096];
                int total = 0;
                int read;
                while ((read = reader.read(buffer)) != -1 && total < MAX_READ_BYTES) {
                    int count = Math.min(read, (int) (MAX_READ_BYTES - total));
                    text.append(buffer, 0, count);
                    total += count;
                }
            }
            if (text.length() == 0) text.append("[File is empty]");
        } catch (Exception e) {
            text.append("Unable to read this file: ").append(e.getMessage());
        }

        TextView content = new TextView(requireContext());
        content.setText(text.toString());
        content.setTextIsSelectable(true);
        content.setTextSize(12);
        content.setPadding(dp(16), dp(12), dp(16), dp(12));
        ScrollView scroller = new ScrollView(requireContext());
        scroller.addView(content);
        new AlertDialog.Builder(requireContext())
                .setTitle(file.getName())
                .setView(scroller)
                .setPositiveButton("Close", null)
                .show();
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
        return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
