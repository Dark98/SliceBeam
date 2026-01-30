package com.dark98.santoku.boot;

import com.dark98.santoku.Santoku;
import com.dark98.santoku.utils.VibrationUtils;

public class VibrationUtilsTask extends BootTask {

    public VibrationUtilsTask() {
        super(() -> VibrationUtils.init(Santoku.INSTANCE));
        onWorker();
    }
}
