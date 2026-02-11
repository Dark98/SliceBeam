package com.dark98.santoku.boot;

import ru.ytkab0bp.eventbus.EventBus;
import com.dark98.santoku.BuildConfig;

public class EventBusTask extends BootTask {

    public EventBusTask() {
        super(() -> {
            String appId = BuildConfig.APPLICATION_ID;
            if (BuildConfig.DEBUG && appId.endsWith(".debug")) {
                appId = appId.substring(0, appId.length() - ".debug".length());
            }
            EventBus.registerImpl(appId);
        });
        onWorker();
    }
}
