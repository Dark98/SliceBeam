package com.dark98.santoku.boot;

import java.util.Collections;

import com.dark98.santoku.cloud.CloudController;

public class CloudCachedInitTask extends BootTask {
    public CloudCachedInitTask() {
        super(Collections.singletonList(PrefsTask.class), CloudController::initCached);
        onWorker();
    }
}
