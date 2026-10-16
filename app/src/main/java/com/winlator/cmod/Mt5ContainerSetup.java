package com.winlator.cmod;

import android.content.Context;

import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.ContainerManager;
import com.winlator.cmod.xenvironment.ImageFs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Adds an MT5 installer launcher to each container's Windows desktop.
 * The installer itself stays in Android's shared Download/WinDroid folder;
 * Wine's D: drive is mapped to Android Downloads by default.
 */
public final class Mt5ContainerSetup {
    private static final String SHORTCUT_NAME = "MetaTrader 5 Installer.desktop";
    private static final String EXECUTABLE_PATH = "D:/WinDroid/mt5setup.exe";

    private Mt5ContainerSetup() {}

    public static int prepareAll(Context context, ContainerManager manager) throws IOException {
        int prepared = 0;
        for (Container container : manager.getContainers()) {
            prepare(context, container);
            prepared++;
        }
        return prepared;
    }

    public static void prepare(Context context, Container container) throws IOException {
        File desktopDir = container.getDesktopDir();
        if (!desktopDir.exists() && !desktopDir.mkdirs()) {
            throw new IOException("Could not create desktop folder for " + container.getName());
        }

        ImageFs imageFs = ImageFs.find(context);
        String winePrefix = imageFs.wineprefix;
        String desktopEntry = "[Desktop Entry]\n"
                + "Name=MetaTrader 5 Installer\n"
                + "Comment=Install MetaTrader 5 in this container\n"
                + "Exec=env WINEPREFIX=\"" + winePrefix + "\" wine \"" + EXECUTABLE_PATH + "\"\n"
                + "Type=Application\n"
                + "Terminal=false\n"
                + "StartupNotify=true\n";

        File shortcut = new File(desktopDir, SHORTCUT_NAME);
        try (FileOutputStream output = new FileOutputStream(shortcut, false)) {
            output.write(desktopEntry.getBytes(StandardCharsets.UTF_8));
            output.flush();
        }
        if (!shortcut.isFile() || shortcut.length() == 0) {
            throw new IOException("Could not write MT5 shortcut for " + container.getName());
        }
    }
}
