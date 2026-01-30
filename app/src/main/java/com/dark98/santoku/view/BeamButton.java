package com.dark98.santoku.view;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;

import androidx.appcompat.widget.AppCompatTextView;

import com.dark98.santoku.R;
import com.dark98.santoku.theme.IThemeView;
import com.dark98.santoku.theme.ThemesRepo;
import com.dark98.santoku.utils.ViewUtils;

public class BeamButton extends AppCompatTextView implements IThemeView {
    private int colorRes = android.R.attr.colorAccent;
    private int color;

    public BeamButton(Context context) {
        super(context);
        setGravity(Gravity.CENTER);
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        setTypeface(ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM));
        setPadding(ViewUtils.dp(21), 0, ViewUtils.dp(21), 0);
        onApplyTheme();
    }

    public void setColor(int color) {
        this.color = color;
        this.colorRes = 0;
        onApplyTheme();
    }

    public void setColorRes(int colorRes) {
        this.colorRes = colorRes;
        onApplyTheme();
    }

    @Override
    public void onApplyTheme() {
        setBackground(ViewUtils.createRipple(ThemesRepo.getColor(android.R.attr.colorControlHighlight), colorRes != 0 ? ThemesRepo.getColor(colorRes) : color, 16));
        setTextColor(ThemesRepo.getColor(R.attr.textColorOnAccent));
    }
}
