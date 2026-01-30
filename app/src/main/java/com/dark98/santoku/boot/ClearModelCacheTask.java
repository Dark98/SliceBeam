package com.dark98.santoku.boot;

import java.io.File;

import com.dark98.santoku.Santoku;

public class ClearModelCacheTask extends BootTask {
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public ClearModelCacheTask() {
        super(()->{
            File cache = Santoku.getModelCacheDir();
            if (cache.exists()) {
                for (File f : cache.listFiles()) {
                    f.delete();
                }
            }
        });
        nonCritical = true;
        onWorker();
    }
}
