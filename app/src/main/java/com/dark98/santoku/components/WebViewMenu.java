package com.dark98.santoku.components;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Locale;

import com.dark98.santoku.BuildConfig;
import com.dark98.santoku.R;
import com.dark98.santoku.Santoku;
import com.dark98.santoku.events.NeedDismissCalibrationsMenu;
import com.dark98.santoku.fragment.BedFragment;
import com.dark98.santoku.theme.ThemesRepo;
import com.dark98.santoku.utils.ViewUtils;
import com.dark98.santoku.view.DividerView;

public class WebViewMenu extends UnfoldMenu {
    private final Uri uri;
    private String javascript;
    private BedFragment fragment;

    private FileOutputStream fileStream;
    private File cacheFile;

    public WebViewMenu(Uri uri) {
        this.uri = uri;
    }

    public WebViewMenu(Uri uri, String javascript) {
        this(uri);
        this.javascript = javascript;
    }

    public WebViewMenu setFragment(BedFragment fragment) {
        this.fragment = fragment;
        return this;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected View onCreateView(Context ctx, boolean portrait) {
        LinearLayout ll = new LinearLayout(ctx);
        ll.setOrientation(LinearLayout.VERTICAL);

        LinearLayout toolbar = new LinearLayout(ctx);
        toolbar.setPadding(ViewUtils.dp(12), 0, ViewUtils.dp(12), 0);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setBackground(ViewUtils.createRipple(ThemesRepo.getColor(android.R.attr.colorControlHighlight), 0));
        toolbar.setOnClickListener(v -> dismiss());

        ImageView icon = new ImageView(ctx);
        icon.setImageResource(R.drawable.arrow_left_outline_28);
        icon.setColorFilter(ThemesRepo.getColor(android.R.attr.textColorSecondary));
        toolbar.addView(icon, new LinearLayout.LayoutParams(ViewUtils.dp(28), ViewUtils.dp(28)));

        TextView title = new TextView(ctx);
        title.setText(R.string.MenuOrientationPositionBack);
        title.setTypeface(ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM));
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        title.setTextColor(ThemesRepo.getColor(android.R.attr.textColorPrimary));
        toolbar.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) {{
            leftMargin = ViewUtils.dp(12);
        }});
        ll.addView(toolbar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(52)));

        ll.addView(new DividerView(ctx), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(1f)));

        WebView webView = new WebView(ctx) {
            @Override
            public boolean dispatchTouchEvent(MotionEvent ev) {
                if (fileStream != null) {
                    return true;
                }
                return super.dispatchTouchEvent(ev);
            }
        };
        webView.addJavascriptInterface(new Bridge(), "Santoku");

        WebSettings settings = webView.getSettings();
        settings.setUserAgentString(String.format(Locale.ROOT, "Santoku/%s-%d", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setGeolocationEnabled(false);

        webView.loadUrl(uri.toString());
        webView.setAlpha(0f);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                if (javascript != null) {
                    webView.evaluateJavascript(javascript, value -> ViewUtils.postOnMainThread(() -> webView.animate().alpha(1f).start()));
                } else {
                    webView.animate().alpha(1f).start();
                }
            }
        });

        ll.addView(webView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        return ll;
    }

    @Override
    public int getRequestedSize(FrameLayout into, boolean portrait) {
        return portrait ? into.getHeight() - into.getPaddingTop() - into.getPaddingBottom() : into.getWidth();
    }

    private final class Bridge {

        @JavascriptInterface
        public void beginDownload(String filename) {
            cacheFile = new File(Santoku.getModelCacheDir(), filename);
            try {
                fileStream = new FileOutputStream(cacheFile);
            } catch (Exception e) {
                Log.e("WebViewMenu", "Failed to begin download", e);
            }
        }

        @JavascriptInterface
        public void writeData(String data) {
            try {
                fileStream.write(Base64.decode(data, 0));
            } catch (Exception e) {
                Log.e("WebViewMenu", "Failed to write to stream", e);
            }
        }

        @JavascriptInterface
        public void finishDownload() {
            try {
                fileStream.close();

                ViewUtils.postOnMainThread(() -> {
                    dismiss(true);
                    Santoku.EVENT_BUS.fireEvent(new NeedDismissCalibrationsMenu());
                    ViewUtils.postOnMainThread(() -> fragment.loadGCode(cacheFile), 200);
                });
            } catch (Exception e) {
                Log.e("WebViewMenu", "Failed to finish file", e);
            }
        }
    }
}
