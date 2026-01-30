package com.dark98.santoku.components;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.SpannableStringBuilder;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;

import com.dark98.santoku.R;
import com.dark98.santoku.Santoku;
import com.dark98.santoku.cloud.CloudAPI;
import com.dark98.santoku.cloud.CloudController;
import com.dark98.santoku.events.NeedDismissSnackbarEvent;
import com.dark98.santoku.recycler.PreferenceSwitchItem;
import com.dark98.santoku.recycler.SimpleRecyclerAdapter;
import com.dark98.santoku.recycler.SimpleRecyclerItem;
import com.dark98.santoku.theme.ThemesRepo;
import com.dark98.santoku.utils.Prefs;
import com.dark98.santoku.utils.ViewUtils;
import com.dark98.santoku.view.TextColorImageSpan;

public class CloudManageBottomSheet extends BottomSheetDialog {
    public CloudManageBottomSheet(@NonNull Context context) {
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

        TextView title = new TextView(context);
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        title.setTypeface(ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM));
        title.setText(R.string.SettingsCloudManageButtonManage);
        title.setTextColor(ThemesRepo.getColor(android.R.attr.textColorPrimary));
        title.setGravity(Gravity.CENTER);
        title.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) {{
            leftMargin = rightMargin = ViewUtils.dp(21);
        }});
        ll.addView(title);

        TextView description = new TextView(context);
        description.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        description.setText(context.getString(R.string.SettingsCloudManageLoggedInAs, CloudController.getUserInfo().displayName));
        description.setTextColor(ThemesRepo.getColor(android.R.attr.textColorSecondary));
        description.setGravity(Gravity.CENTER);
        description.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) {{
            leftMargin = rightMargin = ViewUtils.dp(21);
            topMargin = ViewUtils.dp(8);
        }});
        ll.addView(description);

        int currentLevel = CloudController.getUserInfo().currentLevel;
        CloudAPI.SubscriptionLevel lvl = null;
        CloudAPI.UserFeatures features = CloudController.getUserFeatures();
        for (CloudAPI.SubscriptionLevel level : features.levels) {
            if (level.level != -1 && level.level <= currentLevel && (lvl == null || level.level > lvl.level)) {
                lvl = level;
            }
        }

        if (lvl != null) {
            List<SimpleRecyclerItem> items = new ArrayList<>();
            if (currentLevel >= features.syncRequiredLevel) {
                items.add(new PreferenceSwitchItem()
                        .setIcon(R.drawable.sync_outline_28)
                        .setTitle(context.getString(R.string.SettingsCloudManageFeatureCloudSync))
                        .setValueProvider(Prefs::isCloudProfileSyncEnabled)
                        .setChangeListener((buttonView, isChecked) -> {
                            Prefs.setCloudProfileSyncEnabled(isChecked);
                            if (isChecked) {
                                CloudController.notifyDataChanged();
                            } else {
                                Santoku.EVENT_BUS.fireEvent(new NeedDismissSnackbarEvent(CloudController.CLOUD_SYNC_TAG));
                            }
                        }));
            }
            if (!items.isEmpty()) {
                RecyclerView recyclerView = new RecyclerView(context);
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
                recyclerView.setBackground(ViewUtils.createRipple(0, ColorUtils.setAlphaComponent(ThemesRepo.getColor(android.R.attr.colorControlHighlight), 0x10), 16));
                SimpleRecyclerAdapter adapter = new SimpleRecyclerAdapter();
                adapter.setItems(items);
                recyclerView.setAdapter(adapter);
                ll.addView(recyclerView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) {{
                    topMargin = ViewUtils.dp(16);
                    leftMargin = rightMargin = ViewUtils.dp(16);
                }});
            }

            TextView manageButton = new TextView(context);
            SpannableStringBuilder sb = SpannableStringBuilder.valueOf(context.getString(R.string.SettingsCloudManageSubscription)).append(" ");
            Drawable dr = ContextCompat.getDrawable(context, R.drawable.external_link_outline_24);
            int size = ViewUtils.dp(16);
            dr.setBounds(0, 0, size, size);
            sb.append("d", new TextColorImageSpan(dr, ViewUtils.dp(2f)), SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
            manageButton.setText(sb);
            manageButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            manageButton.setTextColor(ThemesRepo.getColor(android.R.attr.textColorSecondary));
            manageButton.setTypeface(ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM));
            manageButton.setGravity(Gravity.CENTER);
            manageButton.setPadding(ViewUtils.dp(12), ViewUtils.dp(8), ViewUtils.dp(12), ViewUtils.dp(8));
            manageButton.setBackground(ViewUtils.createRipple(ThemesRepo.getColor(android.R.attr.colorControlHighlight), 16));
            CloudAPI.SubscriptionLevel finalLvl = lvl;
            manageButton.setOnClickListener(v -> v.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(finalLvl.manageUrl))));
            ll.addView(manageButton, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(48)) {{
                leftMargin = rightMargin = ViewUtils.dp(16);
                topMargin = bottomMargin = ViewUtils.dp(6);
            }});
        } else {
            ll.addView(new Space(context), new LinearLayout.LayoutParams(0, ViewUtils.dp(16)));
        }

        TextView buttonView = new TextView(context);
        buttonView.setText(R.string.SettingsCloudManageButtonLogOut);
        buttonView.setTextColor(ThemesRepo.getColor(R.attr.textColorOnAccent));
        buttonView.setTypeface(ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM));
        buttonView.setGravity(Gravity.CENTER);
        buttonView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        buttonView.setBackground(ViewUtils.createRipple(ThemesRepo.getColor(android.R.attr.colorControlHighlight), ThemesRepo.getColor(R.attr.textColorNegative), 16));
        buttonView.setOnClickListener(v-> {
            CloudController.logout();
            dismiss();
        });
        ll.addView(buttonView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(52)) {{
            leftMargin = rightMargin = ViewUtils.dp(16);
            bottomMargin = ViewUtils.dp(4);
        }});

        setContentView(ll);
    }
}
