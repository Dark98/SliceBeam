package com.dark98.santoku.boot;

import java.io.File;
import java.io.IOException;

import com.dark98.santoku.Santoku;
import com.dark98.santoku.slic3r.Slic3rConfigWrapper;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class LoadSlic3rConfigTask extends BootTask {
    public LoadSlic3rConfigTask() {
        super(() -> {
            File cfgFile = Santoku.getConfigFile();
            Santoku.getCurrentConfigFile().delete();
            if (cfgFile.exists()) {
                try {
                    Santoku.CONFIG = new Slic3rConfigWrapper(cfgFile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        onWorker();
    }
}
