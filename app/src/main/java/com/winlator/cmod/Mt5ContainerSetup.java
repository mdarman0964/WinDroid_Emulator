package com.winlator.cmod;

import android.content.Context;

import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.ContainerManager;
import com.winlator.cmod.core.MSLink;

import java.io.File;
import java.io.IOException;

/**
 * Adds an installer shortcut to each container's Windows desktop.
 * The installer is stored in Android Download/WinDroid, mapped as D:/WinDroid.
 */
public final class Mt5ContainerSetup {
    private static final String SHORTCUT_BASENAME = "MetaTrader 5 Installer";
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

        File linkFile = new File(desktopDir, SHORTCUT_BASENAME + ".lnk");
        File desktopFile = new File(desktopDir, SHORTCUT_BASENAME + ".desktop");
        MSLink.createFile(EXECUTABLE_PATH, linkFile);
        MSLink.createDesktopFile(linkFile, context);

        if (!linkFile.isFile() || linkFile.length() == 0
                || !desktopFile.isFile() || desktopFile.length() == 0) {
            throw new IOException("Could not create MT5 installer shortcut for " + container.getName());
        }
    }
}
