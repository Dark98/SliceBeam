package com.dark98.santoku.events;

import android.view.View;

import ru.ytkab0bp.eventbus.Event;
import com.dark98.santoku.Santoku;
import com.dark98.santoku.view.SnackbarsLayout;

@Event
public class NeedSnackbarEvent {
    public final CharSequence title;
    public SnackbarsLayout.Type type = SnackbarsLayout.Type.DONE;
    public String tag;

    public CharSequence buttonTitle;
    public View.OnClickListener buttonClick;

    public NeedSnackbarEvent(SnackbarsLayout.Type type, CharSequence title) {
        this.type = type;
        this.title = title;
    }

    public NeedSnackbarEvent(CharSequence title) {
        this.title = title;
    }

    public NeedSnackbarEvent(int title, Object... args) {
        this.title = Santoku.INSTANCE.getString(title, args);
    }

    public NeedSnackbarEvent(SnackbarsLayout.Type type, int title, Object... args) {
        this.type = type;
        this.title = Santoku.INSTANCE.getString(title, args);
    }

    public NeedSnackbarEvent tag(String tag) {
        this.tag = tag;
        return this;
    }

    public NeedSnackbarEvent button(int title, View.OnClickListener click) {
        this.buttonTitle = Santoku.INSTANCE.getString(title);
        this.buttonClick = click;
        return this;
    }
}
