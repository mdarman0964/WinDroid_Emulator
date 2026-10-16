package com.winlator.cmod.xenvironment;

import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;

import com.winlator.cmod.MainActivity;
import com.winlator.cmod.R;
import com.winlator.cmod.SettingsFragment;
import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.ContainerManager;
import com.winlator.cmod.contents.AdrenotoolsManager;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.DownloadProgressDialog;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.PreloaderDialog;
import com.winlator.cmod.core.TarCompressorUtils;
import com.winlator.cmod.core.WineInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public abstract class ImageFsInstaller {
    public static final byte LATEST_VERSION = 21;
    private static final String EXTRA_BASE = "https://gitlab.com/winlator3/winlator-extra/-/raw/main/";
    private static final String IMAGEFS_CHECKSUM = EXTRA_BASE + "imagefs/imagefs.txz.sha256sum";
    private static final String PROTON_ARM64EC = "proton-9.0-arm64ec";
    private static final String PROTON_X86_64 = "proton-9.0-x86_64";

    private static File getRuntimeCache(Context context) {
        File directory = new File(context.getFilesDir(), "runtime-downloads");
        if (!directory.exists()) directory.mkdirs();
        return directory;
    }

    private static void resetContainerImgVersions(Context context) {
        ContainerManager manager = new ContainerManager(context);
        for (Container container : manager.getContainers()) {
            String imgVersion = container.getExtra("imgVersion");
            String wineVersion = container.getWineVersion();
            if (!imgVersion.isEmpty() && WineInfo.isMainWineVersion(wineVersion) && Short.parseShort(imgVersion) <= 5) {
                container.putExtra("wineprefixNeedsUpdate", "t");
            }

            container.putExtra("imgVersion", null);
            container.saveData();
        }
    }

    public static void installWineFromAssets(final MainActivity activity) {
        String[] versions = activity.getResources().getStringArray(R.array.wine_entries);
        File rootDir = ImageFs.find(activity).getRootDir();
        File runtimeCache = getRuntimeCache(activity);
        for (String version : versions) {
            File outFile = new File(rootDir, "/opt/" + version);
            outFile.mkdirs();
            File archive = new File(runtimeCache, version + ".txz");
            if (archive.isFile()) {
                TarCompressorUtils.extract(TarCompressorUtils.Type.XZ, archive, outFile);
            } else {
                TarCompressorUtils.extract(TarCompressorUtils.Type.XZ, activity, version + ".txz", outFile);
            }
        }
    }

    public static void installDriversFromAssets(final MainActivity activity) {
        AdrenotoolsManager adrenotoolsManager = new AdrenotoolsManager(activity);
        String[] adrenotoolsAssetDrivers = activity.getResources().getStringArray(R.array.wrapper_graphics_driver_version_entries);

        for (String driver : adrenotoolsAssetDrivers)
            adrenotoolsManager.extractDriverFromResources(driver);
    }

    public static void installFromAssets(final MainActivity activity) {
        AppUtils.keepScreenOn(activity);
        ImageFs imageFs = ImageFs.find(activity);
        File rootDir = imageFs.getRootDir();
        File runtimeCache = getRuntimeCache(activity);
        File imageFsArchive = new File(runtimeCache, "imagefs.txz");

        SettingsFragment.resetEmulatorsVersion(activity);

        final DownloadProgressDialog dialog = new DownloadProgressDialog(activity);
        dialog.show(R.string.installing_system_files);
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                if (!downloadImageFs(imageFsArchive, activity, dialog)
                        || !downloadAndVerify(
                                EXTRA_BASE + "proton/" + PROTON_ARM64EC + ".txz",
                                EXTRA_BASE + "proton/" + PROTON_ARM64EC + ".txz.sha256sum",
                                new File(runtimeCache, PROTON_ARM64EC + ".txz"), activity, dialog, 25, 40)
                        || !downloadAndVerify(
                                EXTRA_BASE + "proton/" + PROTON_X86_64 + ".txz",
                                EXTRA_BASE + "proton/" + PROTON_X86_64 + ".txz.sha256sum",
                                new File(runtimeCache, PROTON_X86_64 + ".txz"), activity, dialog, 40, 55)) {
                    throw new IOException("A required runtime archive could not be downloaded or verified.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                AppUtils.showToast(activity, R.string.unable_to_install_system_files);
                dialog.closeOnUiThread();
                return;
            }

            clearRootDir(rootDir);
            final byte compressionRatio = 22;
            final long contentLength = Math.max(1L, imageFsArchive.length() * (100L / compressionRatio));
            AtomicLong totalSizeRef = new AtomicLong();

            boolean success = TarCompressorUtils.extract(TarCompressorUtils.Type.XZ, imageFsArchive, rootDir, (file, size) -> {
                if (size > 0) {
                    long totalSize = totalSizeRef.addAndGet(size);
                    final int progress = 55 + (int) (((float) totalSize / contentLength) * 45.0f);
                    activity.runOnUiThread(() -> dialog.setProgress(progress));
                }
                return file;
            });

            if (success) {
                installWineFromAssets(activity);
                installDriversFromAssets(activity);
                imageFs.createImgVersionFile(LATEST_VERSION);
                resetContainerImgVersions(activity);
                imageFsArchive.delete();
                new File(runtimeCache, PROTON_ARM64EC + ".txz").delete();
                new File(runtimeCache, PROTON_X86_64 + ".txz").delete();
            } else {
                AppUtils.showToast(activity, R.string.unable_to_install_system_files);
            }

            dialog.closeOnUiThread();
        });
    }

    private static boolean downloadImageFs(File destination, MainActivity activity, DownloadProgressDialog dialog) throws Exception {
        if (destination.isFile() && destination.length() > 0 && checksumMatches(destination, IMAGEFS_CHECKSUM)) {
            updateProgress(activity, dialog, 25);
            return true;
        }

        File directory = destination.getParentFile();
        if (directory != null) directory.mkdirs();
        File assembled = new File(destination.getParentFile(), "imagefs.txz.part");
        assembled.delete();

        try (OutputStream output = new BufferedOutputStream(new FileOutputStream(assembled))) {
            for (int i = 0; i < 4; i++) {
                String partUrl = EXTRA_BASE + "imagefs/imagefs.txz." + String.format(Locale.US, "%02d", i);
                HttpURLConnection connection = openConnection(partUrl);
                try (InputStream input = new BufferedInputStream(connection.getInputStream())) {
                    byte[] buffer = new byte[128 * 1024];
                    int read;
                    long partBytes = 0;
                    int partLength = Math.max(1, connection.getContentLength());
                    while ((read = input.read(buffer)) != -1) {
                        output.write(buffer, 0, read);
                        partBytes += read;
                        int progress = (int) (i * 6.25f + Math.min(6.25f, ((float) partBytes / partLength) * 6.25f));
                        updateProgress(activity, dialog, progress);
                    }
                } finally {
                    connection.disconnect();
                }
            }
        }

        if (!checksumMatches(assembled, IMAGEFS_CHECKSUM)) {
            assembled.delete();
            throw new IOException("ImageFS checksum verification failed.");
        }
        if (destination.exists()) destination.delete();
        if (!assembled.renameTo(destination)) {
            FileUtils.copy(assembled, destination);
            assembled.delete();
        }
        updateProgress(activity, dialog, 25);
        return destination.isFile() && destination.length() > 0;
    }

    private static boolean downloadAndVerify(String fileUrl, String checksumUrl, File destination,
                                             MainActivity activity, DownloadProgressDialog dialog,
                                             int progressStart, int progressEnd) throws Exception {
        if (destination.isFile() && destination.length() > 0 && checksumMatches(destination, checksumUrl)) {
            updateProgress(activity, dialog, progressEnd);
            return true;
        }

        File parent = destination.getParentFile();
        if (parent != null) parent.mkdirs();
        File partial = new File(destination.getAbsolutePath() + ".part");
        partial.delete();

        HttpURLConnection connection = openConnection(fileUrl);
        try (InputStream input = new BufferedInputStream(connection.getInputStream());
             OutputStream output = new BufferedOutputStream(new FileOutputStream(partial))) {
            byte[] buffer = new byte[128 * 1024];
            int read;
            long total = 0;
            int length = Math.max(1, connection.getContentLength());
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
                total += read;
                int progress = progressStart + (int) (((float) total / length) * (progressEnd - progressStart));
                updateProgress(activity, dialog, Math.min(progressEnd, progress));
            }
        } finally {
            connection.disconnect();
        }

        if (!checksumMatches(partial, checksumUrl)) {
            partial.delete();
            throw new IOException("Checksum verification failed for " + destination.getName());
        }
        if (destination.exists()) destination.delete();
        if (!partial.renameTo(destination)) {
            FileUtils.copy(partial, destination);
            partial.delete();
        }
        updateProgress(activity, dialog, progressEnd);
        return destination.isFile() && destination.length() > 0;
    }

    private static HttpURLConnection openConnection(String address) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(60000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", "WinDroid-MT5/1.0");
        int status = connection.getResponseCode();
        if (status < 200 || status >= 300) {
            connection.disconnect();
            throw new IOException("Download server returned HTTP " + status);
        }
        return connection;
    }

    private static boolean checksumMatches(File file, String checksumUrl) {
        try {
            HttpURLConnection connection = openConnection(checksumUrl);
            String expected;
            try (InputStream input = connection.getInputStream()) {
                java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int read;
                while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
                expected = new String(output.toByteArray(), java.nio.charset.StandardCharsets.UTF_8)
                        .trim().split("\\s+")[0].toLowerCase(Locale.ROOT);
            } finally {
                connection.disconnect();
            }

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = new BufferedInputStream(new FileInputStream(file))) {
                byte[] buffer = new byte[128 * 1024];
                int read;
                while ((read = input.read(buffer)) != -1) digest.update(buffer, 0, read);
            }
            StringBuilder actual = new StringBuilder(64);
            for (byte value : digest.digest()) actual.append(String.format(Locale.US, "%02x", value & 0xff));
            return expected.equals(actual.toString());
        } catch (Exception e) {
            return false;
        }
    }

    private static void updateProgress(MainActivity activity, DownloadProgressDialog dialog, int progress) {
        activity.runOnUiThread(() -> dialog.setProgress(progress));
    }

    public static void installIfNeeded(final MainActivity activity) {
        ImageFs imageFs = ImageFs.find(activity);
        if (!imageFs.isValid() || imageFs.getVersion() < LATEST_VERSION) installFromAssets(activity);
    }

    private static void clearRootDir(File rootDir) {
        if (rootDir.isDirectory()) {
            File[] files = rootDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory() && file.getName().equals("home")) continue;
                    FileUtils.delete(file);
                }
            }
        } else {
            rootDir.mkdirs();
        }
    }
}
