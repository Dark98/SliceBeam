package com.dark98.santoku.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

import com.google.android.material.materialswitch.MaterialSwitch;

import com.dark98.santoku.R;
import com.dark98.santoku.theme.IThemeView;
import com.dark98.santoku.theme.ThemesRepo;

public class BeamSwitch extends MaterialSwitch implements IThemeView {
    public BeamSwitch(@NonNull Context context) {
        super(context);

        onApplyTheme();
    }

    @Override
    public void onApplyTheme() {
        setTrackTintList(new ColorStateList(new int[][] {
                {android.R.attr.state_enabled, android.R.attr.state_checked},
                {android.R.attr.state_enabled, -android.R.attr.state_checked}
        }, new int[] {
                ThemesRepo.getColor(android.R.attr.colorAccent),
                ThemesRepo.getColor(R.attr.switchThumbUncheckedColor)
        }));
    }

    @Override
    public boolean isLaidOut() {
        return true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        return false;
    }
}
