package com.dark98.santoku.boot;

import java.io.IOException;

import com.dark98.santoku.Santoku;

public class CheckUpdateJsonTask extends BootTask {
    public CheckUpdateJsonTask() {
        super(() -> {
            try {
                Santoku.INSTANCE.getAssets().open("update.json").close();
                Santoku.hasUpdateInfo = true;
            } catch (IOException e) {
                Santoku.hasUpdateInfo = false;
            }
        });
        onWorker();
    }
}
