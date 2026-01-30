package com.dark98.santoku.boot;

import com.dark98.santoku.Santoku;
import com.dark98.santoku.utils.Prefs;

public class PrefsTask extends BootTask {
    public PrefsTask() {
        super(()->Prefs.init(Santoku.INSTANCE));
    }
}
