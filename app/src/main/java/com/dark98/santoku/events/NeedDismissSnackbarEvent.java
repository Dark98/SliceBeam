package com.dark98.santoku.events;

import ru.ytkab0bp.eventbus.Event;

@Event
public class NeedDismissSnackbarEvent {
    public final String tag;

    public NeedDismissSnackbarEvent(String tag) {
        this.tag = tag;
    }
}
