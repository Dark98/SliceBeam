package com.dark98.santoku.boot;

import ru.ytkab0bp.eventbus.EventBus;
import com.dark98.santoku.BuildConfig;

public class EventBusTask extends BootTask {

    public EventBusTask() {
        super(() -> EventBus.registerImpl(BuildConfig.APPLICATION_ID));
        onWorker();
    }
}
