package com.dark98.santoku.boot;

import java.util.Arrays;

import com.dark98.santoku.cloud.CloudController;

public class CloudInitTask extends BootTask {
    public CloudInitTask() {
        super(Arrays.asList(PrefsTask.class, TrueTimeTask.class, LoadSlic3rConfigTask.class, CloudCachedInitTask.class), CloudController::init);
        onWorker();
        nonCritical = true;
    }
}
