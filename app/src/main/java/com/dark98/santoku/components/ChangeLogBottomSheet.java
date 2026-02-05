package com.dark98.santoku.components;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import com.dark98.santoku.R;
import com.dark98.santoku.Santoku;
import com.dark98.santoku.theme.ThemesRepo;
import com.dark98.santoku.utils.ViewUtils;
import com.dark98.santoku.view.BeamButton;

public class ChangeLogBottomSheet extends BottomSheetDialog {
    private ScrollView scrollView;

    public ChangeLogBottomSheet(@NonNull Context context) {
        super(context);

        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadii(new float[] {
                ViewUtils.dp(28), ViewUtils.dp(28),
                ViewUtils.dp(28), ViewUtils.dp(28),
                0, 0,
                0, 0
        });
        gd.setColor(ThemesRepo.getColor(R.attr.dialogBackground));
        ll.setBackground(gd);
        ll.setPadding(0, ViewUtils.dp(12), 0, ViewUtils.dp(12));

        FrameLayout fl = new FrameLayout(context);
        TextView titleA = new TextView(context);
        titleA.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        titleA.setTypeface(ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM));
        titleA.setText(R.string.Changelog);
        titleA.setTextColor(ThemesRepo.getColor(android.R.attr.textColorPrimary));
        titleA.setGravity(Gravity.CENTER);
        titleA.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) {{
            leftMargin = rightMargin = ViewUtils.dp(21);
        }});
        fl.addView(titleA);

        ll.addView(fl);

        scrollView = new ScrollView(context);
        TextView text = new TextView(context);
        text.setTextColor(ThemesRepo.getColor(android.R.attr.textColorPrimary));
        text.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        text.setPadding(ViewUtils.dp(16), ViewUtils.dp(12), ViewUtils.dp(16), ViewUtils.dp(12));

        try {
            InputStream in = getContext().getAssets().open("update.json");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[10240]; int c;
            while ((c = in.read(buffer)) != -1) {
                bos.write(buffer, 0, c);
            }
            bos.close();
            in.close();

            JSONObject obj = new JSONObject(bos.toString());
            String code = Locale.getDefault().getLanguage();
            if (obj.has(code)) {
                text.setText(obj.getString(code));
            } else {
                text.setText(obj.getString("en"));
            }
        } catch (Exception e) {
            Log.e("Changelog", "Failed to open update file", e);
        }
        scrollView.addView(text);

        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        ll.addView(scrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (dm.heightPixels * 0.45f)));

        BeamButton btn = new BeamButton(context);
        btn.setText(R.string.ChangelogOK);
        btn.setOnClickListener(v -> dismiss());
        ll.addView(btn, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(48)) {{
            leftMargin = topMargin = rightMargin = bottomMargin = ViewUtils.dp(12);
        }});

        ll.setFitsSystemWindows(true);
        setContentView(ll);

    }

    @Override
    public void show() {
        super.show();
        getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
    }
}
