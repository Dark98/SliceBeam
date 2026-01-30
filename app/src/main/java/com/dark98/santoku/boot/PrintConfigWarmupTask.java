package com.dark98.santoku.boot;

import com.dark98.santoku.slic3r.PrintConfigDef;

public class PrintConfigWarmupTask extends BootTask {
    public PrintConfigWarmupTask() {
        super(PrintConfigDef::getInstance);
        onWorker();
    }
}
