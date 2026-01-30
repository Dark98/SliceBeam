package com.dark98.santoku.boot;

import org.json.JSONException;
import org.json.JSONObject;

import com.dark98.santoku.BeamServerData;
import com.dark98.santoku.Santoku;
import com.dark98.santoku.utils.Prefs;
import com.dark98.santoku.utils.ViewUtils;

public class BeamServerDataTask extends BootTask {
    public BeamServerDataTask() {
        super(() -> {
            try {
                Santoku.SERVER_DATA = new BeamServerData(new JSONObject(Prefs.getBeamServerData()));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            if (System.currentTimeMillis() - Prefs.getLastCheckedInfo() >= 86400000L) {
                ViewUtils.postOnMainThread(BeamServerData::load);
            }
        });
        onWorker();
    }
}
