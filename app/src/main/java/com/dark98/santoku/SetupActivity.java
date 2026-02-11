package com.dark98.santoku;

import static android.opengl.GLES30.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES30.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES30.GL_DEPTH_TEST;
import static android.opengl.GLES30.glClear;
import static android.opengl.GLES30.glClearColor;
import static android.opengl.GLES30.glDisable;
import static android.opengl.GLES30.glEnable;
import static android.opengl.GLES30.glViewport;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.EditText;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.dynamicanimation.animation.FloatValueHolder;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import ru.ytkab0bp.sapil.APICallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import cz.msebera.android.httpclient.Header;
import ru.ytkab0bp.eventbus.EventHandler;
import com.dark98.santoku.cloud.CloudAPI;
import com.dark98.santoku.cloud.CloudController;
import com.dark98.santoku.components.BeamAlertDialogBuilder;
import com.dark98.santoku.components.CloudManageBottomSheet;
import com.dark98.santoku.config.ConfigObject;
import com.dark98.santoku.events.CloudLoginStateUpdatedEvent;
import com.dark98.santoku.events.CloudSyncFinishedEvent;
import com.dark98.santoku.recycler.BigHeaderItem;
import com.dark98.santoku.recycler.PreferenceItem;
import com.dark98.santoku.recycler.SimpleRecyclerAdapter;
import com.dark98.santoku.recycler.SimpleRecyclerItem;
import com.dark98.santoku.recycler.TextHintRecyclerItem;
import com.dark98.santoku.slic3r.GLModel;
import com.dark98.santoku.slic3r.GLShaderProgram;
import com.dark98.santoku.slic3r.GLShadersManager;
import com.dark98.santoku.slic3r.Slic3rConfigWrapper;
import com.dark98.santoku.slic3r.Slic3rUtils;
import com.dark98.santoku.theme.BeamTheme;
import com.dark98.santoku.theme.IThemeView;
import com.dark98.santoku.theme.ThemesRepo;
import com.dark98.santoku.utils.Prefs;
import com.dark98.santoku.utils.ViewUtils;
import com.dark98.santoku.view.BeamSwitch;
import com.dark98.santoku.view.FadeRecyclerView;
import com.dark98.santoku.view.MiniColorView;
import com.dark98.santoku.view.TextColorImageSpan;

public class SetupActivity extends AppCompatActivity {
    public final static String EXTRA_ABOUT = "about";
    public final static String EXTRA_CLOUD_PROFILE = "cloud_profile";
    public final static String EXTRA_CLOUD_IMPORT_FROM_SETUP = "cloud_import_from_setup";

    private final static String TAG = "SetupActivity";

    private final static List<String> REPOS_URLS = Arrays.asList(
            "https://preset-repo-api.prusa3d.com/v1/repos",
            "https://raw.githubusercontent.com/Dark98/SliceBeam/refs/heads/master/.profiledumpsrepo/manifest.json"
    );

    private final static int REPOS_INDEX = 1;
    private final static int PROFILES_INDEX = 2;

    private final static int TYPE_PRINTER = 0, TYPE_PRINT_CONFIG = 1, TYPE_FILAMENT = 2;

    private ViewPager2 pager;
    private SimpleRecyclerAdapter adapter;
    private TextView title;

    private GLSurfaceView backgroundView;
    private GLModel backgroundModel;
    private GLShadersManager shadersManager;

    private int titleY;
    private float backgroundProgress;

    private SpringAnimation fakeScroller;

    private AsyncHttpClient client = new AsyncHttpClient();

    private List<ProfilesRepo> repos = new ArrayList<>();
    private ReposItem reposItem;
    private ProfilesItem profilesItem;
    private CloudProfileItem cloudItem;
    private boolean isReposLoaded;
    private boolean limitRepoFragmentCount = true;
    private boolean limitProfileFragmentCount = true;
    private boolean isLoading;

    private Map<ProfilesRepo, List<Slic3rConfigWrapper>> profilesMap = new HashMap<>();
    private boolean isProfilesLoaded;
    private boolean about;
    private boolean cloudProfile;
    private boolean cloudImport;

    private List<ConfigObject> enabledPrinters = new ArrayList<>();

    {
        client.setUserAgent(String.format(Locale.ROOT, "Santoku/%s-%d", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
        client.setEnableRedirects(true);
        client.setLoggingEnabled(false);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        Santoku.EVENT_BUS.registerListener(this);

        about = getIntent().getBooleanExtra(EXTRA_ABOUT, false);
        cloudProfile = getIntent().getBooleanExtra(EXTRA_CLOUD_PROFILE, false);
        cloudImport = getIntent().getBooleanExtra(EXTRA_CLOUD_IMPORT_FROM_SETUP, false);

        if (!about && !cloudProfile) {
            new BeamAlertDialogBuilder(this)
                    .setTitle(R.string.IntroEarlyAccess)
                    .setMessage(R.string.IntroEarlyAccessMessage)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }

        if (cloudProfile) {
            backgroundProgress = 1f;
        }

        pager = new ViewPager2(this);
        adapter = new SimpleRecyclerAdapter() {
            @Override
            public int getItemCount() {
                return about || cloudProfile ? 1 : limitRepoFragmentCount ? REPOS_INDEX + 1 : limitProfileFragmentCount ? PROFILES_INDEX + 1 : super.getItemCount();
            }
        };
        setItems();
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    if (pager.getCurrentItem() <= REPOS_INDEX && !limitRepoFragmentCount) {
                        ViewUtils.postOnMainThread(() -> {
                            isProfilesLoaded = false;
                            profilesMap.clear();
                            adapter.notifyItemChanged(PROFILES_INDEX);

                            int realCount = adapter.getItemCount();
                            limitRepoFragmentCount = true;
                            adapter.notifyItemRangeRemoved(REPOS_INDEX + 1, realCount - REPOS_INDEX - 1);
                        });
                    }
                    if (pager.getCurrentItem() <= PROFILES_INDEX && !limitProfileFragmentCount) {
                        ViewUtils.postOnMainThread(() -> {
                            int realCount = adapter.getItemCount();
                            limitProfileFragmentCount = true;
                            adapter.notifyItemRangeRemoved(PROFILES_INDEX + 1, realCount - PROFILES_INDEX - 1);
                        });
                    }
                }
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (position == 0 && !cloudProfile) {
                    backgroundProgress = positionOffset;
                } else {
                    backgroundProgress = 1f;
                }

                if (position == REPOS_INDEX) {
                    if (!isReposLoaded && !isLoading) {
                        loadRepos(true);
                        pager.setUserInputEnabled(false);
                    }
                    if (isLoading) {
                        pager.setUserInputEnabled(false);
                    }
                } else if (position == PROFILES_INDEX) {
                    if (!isProfilesLoaded && !isLoading && !profilesItem.useCustomProfile) {
                        AtomicInteger loadedCount = new AtomicInteger();
                        AtomicInteger totalNeeded = new AtomicInteger();
                        Runnable onLoadedAll = () -> {
                            isProfilesLoaded = true;
                            isLoading = false;
                            pager.setUserInputEnabled(true);
                            profilesItem.onProfilesLoaded();
                        };

                        for (ProfilesRepo repo : repos) {
                            if (repo.checked) {
                                totalNeeded.incrementAndGet();

                                client.get(repo.indexUrl, new AsyncHttpResponseHandler() {
                                    @Override
                                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                                        List<Slic3rConfigWrapper> vendorProfiles = new ArrayList<>();
                                        Runnable onVendorsLoaded = () -> {
                                            profilesMap.put(repo, vendorProfiles);
                                            loadedCount.incrementAndGet();

                                            if (loadedCount.get() == totalNeeded.get()) {
                                                ViewUtils.postOnMainThread(onLoadedAll);
                                            }
                                        };

                                        AtomicInteger loadedVendorsCount = new AtomicInteger();
                                        AtomicInteger totalNeededVendors = new AtomicInteger();
                                        List<Runnable> loadRunners = new ArrayList<>();

                                        try {
                                            ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(responseBody));
                                            ZipEntry en;
                                            while ((en = zis.getNextEntry()) != null) {
                                                String version = parseVendorVersion(zis);
                                                String baseUrl = repo.url + "/" + en.getName().substring(0, en.getName().length() - 4);
                                                String iniUrl = baseUrl + "/" + version + ".ini";

                                                totalNeededVendors.incrementAndGet();
                                                loadRunners.add(()-> client.get(iniUrl, new AsyncHttpResponseHandler() {
                                                    @Override
                                                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                                                        loadedVendorsCount.incrementAndGet();

                                                        try {
                                                            Slic3rConfigWrapper cfg = new Slic3rConfigWrapper(new ByteArrayInputStream(responseBody));
                                                            for (ConfigObject obj : cfg.printerModels) {
                                                                if (obj.get("thumbnail") != null) {
                                                                    obj.thumbnailUrl = baseUrl + "/" + obj.get("thumbnail");
                                                                }
                                                            }
                                                            vendorProfiles.add(cfg);
                                                        } catch (IOException e) {
                                                            onFailure(statusCode, headers, responseBody, e);
                                                            return;
                                                        }

                                                        if (loadedVendorsCount.get() < totalNeededVendors.get()) {
                                                            loadRunners.get(loadedVendorsCount.get()).run();
                                                        } else {
                                                            onVendorsLoaded.run();
                                                        }
                                                    }

                                                    @Override
                                                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                                                        Log.e(TAG, "Failed to load vendor file " + iniUrl, error);
                                                        isLoading = false;
                                                        ViewUtils.postOnMainThread(() -> {
                                                            Toast.makeText(Santoku.INSTANCE, R.string.IntroFailedToLoadRepos, Toast.LENGTH_SHORT).show();
                                                            fakeScroll(-1);
                                                            pager.setUserInputEnabled(true);
                                                        });
                                                    }
                                                }));

                                                zis.closeEntry();
                                            }
                                            zis.close();

                                            if (loadRunners.isEmpty()) {
                                                onVendorsLoaded.run();
                                            } else {
                                                loadRunners.get(0).run();
                                            }
                                        } catch (IOException e) {
                                            Log.e(TAG, "Failed to parse vendor indices", e);
                                            onFailure(statusCode, headers, responseBody, e);
                                        }
                                    }

                                    @Override
                                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                                        isLoading = false;
                                        Log.e(TAG, "Failed to load repo", error);
                                        ViewUtils.postOnMainThread(() -> {
                                            Toast.makeText(Santoku.INSTANCE, R.string.IntroFailedToLoadRepos, Toast.LENGTH_SHORT).show();
                                            fakeScroll(-1);
                                            pager.setUserInputEnabled(true);
                                        });
                                    }
                                });
                            }
                        }

                        pager.setUserInputEnabled(false);
                    }
                }

                invalidateTitleY();
                backgroundView.requestRender();
            }
        });
        pager.setAdapter(adapter);
        pager.getChildAt(0).setOverScrollMode(View.OVER_SCROLL_NEVER);

        FrameLayout fl = new FrameLayout(this) {
            @Override
            protected void onSizeChanged(int w, int h, int oldw, int oldh) {
                super.onSizeChanged(w, h, oldw, oldh);

                titleY = h / 4;
                invalidateTitleY();
            }
        };
        fl.setClipChildren(false);
        fl.setClipToPadding(false);
        backgroundView = new GLSurfaceView(this) {
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                super.surfaceDestroyed(holder);
                backgroundModel.release();
                backgroundModel = null;
                shadersManager.clearShaders();
                shadersManager = null;
            }
        };
        backgroundView.setEGLContextClientVersion(3);
        backgroundView.setRenderer(new GLSurfaceView.Renderer() {
            @Override
            public void onSurfaceCreated(GL10 gl, EGLConfig config) {}

            @Override
            public void onSurfaceChanged(GL10 gl, int width, int height) {
                glViewport(0, 0, width, height);
                if (backgroundModel == null) {
                    backgroundModel = new GLModel();
                    backgroundModel.initBackgroundTriangles();
                    shadersManager = new GLShadersManager();
                }
            }

            private float time;
            private long lastUpdate;
            @Override
            public void onDrawFrame(GL10 gl) {
                long dt = Math.min(System.currentTimeMillis() - lastUpdate, 16);
                lastUpdate = System.currentTimeMillis();
                time += dt / 1000f;

                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
                glClearColor(0, 0, 0, 0);

                if (backgroundModel != null) {
                    glDisable(GL_DEPTH_TEST);
                    GLShaderProgram shader = shadersManager.get(GLShadersManager.SHADER_BEAM_INTRO);
                    shader.startUsing();
                    int topColor = ThemesRepo.getColor(android.R.attr.colorAccent);
                    int bottomColor = ThemesRepo.getColor(android.R.attr.windowBackground);
                    if (cloudProfile) {
                        bottomColor = ColorUtils.blendARGB(bottomColor, topColor, 0.5f);
                    }

                    shader.setUniformColor("top_color", topColor);
                    shader.setUniformColor("bottom_color", bottomColor);
                    shader.setUniform("progress", backgroundProgress - (cloudProfile ? 1.4f : 0));
                    shader.setUniform("time", time);
                    backgroundModel.render();
                    shader.stopUsing();
                    glEnable(GL_DEPTH_TEST);
                }
            }
        });
        backgroundView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        fl.addView(backgroundView);

        title = new TextView(this);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setText(cloudProfile ? R.string.SettingsCloudManageTitle : R.string.AppName);
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 32);
        title.setTextColor(ThemesRepo.getColor(R.attr.textColorOnAccent));
        title.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL));
        fl.addView(title);

        fl.addView(pager, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        ViewCompat.setOnApplyWindowInsetsListener(fl, (v2, insets) -> {
            Insets systemBars = insets.getSystemWindowInsets();
            pager.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) title.getLayoutParams();
            params.leftMargin = systemBars.left;
            params.topMargin = systemBars.top;
            params.rightMargin = systemBars.right;
            params.bottomMargin = systemBars.bottom;
            return insets.consumeSystemWindowInsets();
        });
        setContentView(fl);

        if (!isLoading && !isReposLoaded) {
            // Pre-load repos silently
            loadRepos(false);
        }
    }

    private void invalidateTitleY() {
        float sc = ViewUtils.lerp(1, 22 / 32f, backgroundProgress);
        title.setPivotX(title.getWidth() / 2f);
        title.setPivotY(0);
        title.setScaleX(sc);
        title.setScaleY(sc);
        int color = ColorUtils.blendARGB(ThemesRepo.getColor(R.attr.textColorOnAccent), ThemesRepo.getColor(android.R.attr.colorAccent), cloudProfile ? 0f : backgroundProgress);
        title.setTextColor(color);
        title.setTranslationY(ViewUtils.lerp(titleY, (ViewUtils.dp(52) - title.getHeight() * title.getScaleY()) / 2f, backgroundProgress));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Santoku.EVENT_BUS.unregisterListener(this);
    }

    @SuppressLint("NotifyDataSetChanged")
    @EventHandler(runOnMainThread = true)
    public void onCloudSyncFinished(CloudSyncFinishedEvent e) {
        if (cloudProfile && Prefs.getCloudAPIToken() != null && cloudImport) {
            finish();
        }
        if (!about && !cloudProfile) {
            if (Prefs.getCloudAPIToken() != null) {
                limitRepoFragmentCount = false;
                limitProfileFragmentCount = false;
                pager.getAdapter().notifyDataSetChanged();
                pager.setCurrentItem(pager.getAdapter().getItemCount() - 1);
            }
        }
    }

    @EventHandler(runOnMainThread = true)
    public void onCloudAuthStateUpdated(CloudLoginStateUpdatedEvent e) {
        if (cloudProfile) {
            cloudItem.bindLoginButton(true);
        }
        if (!about && !cloudProfile && reposItem != null) {
            reposItem.onCloudInfoUpdated();
        }
    }

    private void setItems() {
        if (cloudProfile){
            adapter.setItems(Collections.singletonList(cloudItem = new CloudProfileItem()));
        } else if (about) {
            adapter.setItems(Collections.singletonList(new AboutItem()));
        } else {
            List<SimpleRecyclerItem> items = new ArrayList<>(Arrays.asList(
                    new IntroItem(),
                    reposItem = new ReposItem(),
                    profilesItem = new ProfilesItem()));

            items.add(new FinishItem());
            adapter.setItems(items);
        }
    }

    @Override
    public void onBackPressed() {
        if (pager.getCurrentItem() > 0) {
            pager.setCurrentItem(pager.getCurrentItem() - 1, true);
        } else {
            super.onBackPressed();
        }
    }

    private void loadRepos(boolean fromPage) {
        isLoading = true;
        repos.clear();
        List<String> finishedIndexes = new ArrayList<>();
        Map<String, List<ProfilesRepo>> reposMap = new HashMap<String, List<ProfilesRepo>>() {
            @Nullable
            @Override
            public List<ProfilesRepo> get(@Nullable Object key) {
                List<ProfilesRepo> list = super.get(key);
                if (list == null) put((String) key, list = new ArrayList<>());
                return list;
            }
        };
        for (String repo : REPOS_URLS) {
            client.get(repo, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    finishedIndexes.add(repo);
                    try {
                        JSONArray arr = new JSONArray(new String(responseBody));
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            if (obj.getString("id").endsWith("-fff")) {
                                ProfilesRepo r = new ProfilesRepo();
                                r.url = obj.getString("url");
                                r.name = obj.getString("name");
                                r.description = obj.getString("description");
                                r.indexUrl = obj.getString("index_url");
                                reposMap.get(repo).add(r);
                            }
                        }

                        if (finishedIndexes.size() == REPOS_URLS.size()) {
                            // Filter in the right way
                            for (String repo : REPOS_URLS) {
                                repos.addAll(reposMap.get(repo));
                            }

                            ViewUtils.postOnMainThread(() -> {
                                isLoading = false;
                                if (fromPage) {
                                    reposItem.onReposLoaded();
                                }
                                pager.setUserInputEnabled(true);
                                isReposLoaded = true;
                            });
                        }
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    isLoading = false;
                    Log.e(TAG, "Failed to load repos", error);
                    if (fromPage) {
                        ViewUtils.postOnMainThread(() -> {
                            Toast.makeText(Santoku.INSTANCE, R.string.IntroFailedToLoadRepos, Toast.LENGTH_SHORT).show();
                            fakeScroll(-1);
                            pager.setUserInputEnabled(true);
                        });
                    }
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        backgroundView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        backgroundView.onResume();
    }

    private void scrollToNext() {
        fakeScroll(1);
    }

    private void fakeScroll(float to) {
        if (fakeScroller != null) return;

        AtomicReference<Float> lastValue = new AtomicReference<>(0f);
        fakeScroller = new SpringAnimation(new FloatValueHolder(0))
                .setMinimumVisibleChange(1 / 256f)
                .setSpring(new SpringForce(1f)
                        .setStiffness(600f)
                        .setDampingRatio(1f))
                .addUpdateListener((animation, value, velocity) -> {
                    float delta = value - lastValue.getAndSet(value);
                    pager.fakeDragBy(delta * pager.getWidth() * -to);
                })
                .addEndListener((animation, canceled, value, velocity) -> {
                    pager.endFakeDrag();
                    fakeScroller = null;
                });
        pager.beginFakeDrag();
        fakeScroller.start();
    }

    private final class CloudProfileItem extends SimpleRecyclerItem<View> {
        private FrameLayout buttonView;
        private TextView buttonText;
        private ProgressBar buttonProgress;
        private TextView titleView;
        private TextView signUpButton;
        private boolean signUpInProgress;

        @Override
        public View onCreateView(Context ctx) {
            LinearLayout ll = new LinearLayout(ctx);
            ll.setOrientation(LinearLayout.VERTICAL);
            ll.setPadding(0, ViewUtils.dp(42), 0, 0);

            titleView = new TextView(ctx);
            titleView.setTextColor(ThemesRepo.getColor(R.attr.textColorOnAccent));
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            titleView.setGravity(Gravity.CENTER);
            titleView.setPadding(ViewUtils.dp(12), 0, ViewUtils.dp(12), 0);
            ll.addView(titleView);
            bindHeader();

            View spacer = new View(ctx);
            ll.addView(spacer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

            TextView tosButton = new TextView(ctx);
            SpannableStringBuilder sb = SpannableStringBuilder.valueOf(ctx.getString(R.string.SettingsCloudManageTermsOfService)).append(" ");
            Drawable dr = ContextCompat.getDrawable(ctx, R.drawable.external_link_outline_24);
            int size = ViewUtils.dp(16);
            dr.setBounds(0, 0, size, size);
            sb.append("d", new TextColorImageSpan(dr, ViewUtils.dp(2f)), SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
            tosButton.setText(sb);
            tosButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            tosButton.setTextColor(Color.WHITE);
            tosButton.setTypeface(ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM));
            tosButton.setGravity(Gravity.CENTER);
            tosButton.setPadding(ViewUtils.dp(12), ViewUtils.dp(8), ViewUtils.dp(12), ViewUtils.dp(8));
            tosButton.setBackground(ViewUtils.createRipple(ThemesRepo.getColor(android.R.attr.colorControlHighlight), 16));
            tosButton.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.BEAM_BASE_URL_PROD + "/tos"))));
            ll.addView(tosButton, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(52)) {{
                leftMargin = rightMargin = ViewUtils.dp(16);
                bottomMargin = ViewUtils.dp(8);
            }});

            buttonView = new FrameLayout(ctx);
            buttonView.setBackground(ViewUtils.createRipple(ThemesRepo.getColor(android.R.attr.colorControlHighlight), ThemesRepo.getColor(android.R.attr.colorAccent), 16));

            buttonText = new TextView(ctx);
            buttonText.setTextColor(ThemesRepo.getColor(R.attr.textColorOnAccent));
            buttonText.setTypeface(ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM));
            buttonText.setGravity(Gravity.CENTER);
            buttonText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            buttonView.addView(buttonText, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

            buttonProgress = new ProgressBar(ctx);
            buttonProgress.setIndeterminateTintList(ColorStateList.valueOf(ThemesRepo.getColor(R.attr.textColorOnAccent)));
            buttonView.addView(buttonProgress, new FrameLayout.LayoutParams(ViewUtils.dp(28), ViewUtils.dp(28), Gravity.CENTER));

            ll.addView(buttonView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(52)) {{
                leftMargin = rightMargin = ViewUtils.dp(16);
            }});

            signUpButton = new TextView(ctx);
            signUpButton.setText(R.string.SettingsCloudManageButtonSignUp);
            signUpButton.setTextColor(ThemesRepo.getColor(android.R.attr.colorAccent));
            signUpButton.setTypeface(ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM));
            signUpButton.setGravity(Gravity.CENTER);
            signUpButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            signUpButton.setBackground(ViewUtils.createRipple(ThemesRepo.getColor(android.R.attr.colorControlHighlight), 16));
            signUpButton.setOnClickListener(v -> showSignUpDialog(v.getContext()));
            ll.addView(signUpButton, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(52)) {{
                leftMargin = rightMargin = ViewUtils.dp(16);
                topMargin = ViewUtils.dp(8);
                bottomMargin = ViewUtils.dp(16);
            }});

            bindLoginButton(false);

            ll.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            return ll;
        }

        private void bindLoginButton(boolean animate) {
            boolean loggedIn = Prefs.getCloudAPIToken() != null;
            boolean loading = !loggedIn && CloudController.isLoggingIn();
            boolean wasLoading = buttonProgress.getTag() != null;
            if (animate) {
                if (wasLoading != loading) {
                    buttonProgress.setTag(loading ? 1 : null);

                    buttonProgress.animate().cancel();
                    buttonProgress.animate().scaleX(loading ? 1f : 0.4f).scaleY(loading ? 1f : 0.4f).alpha(loading ? 1f : 0f).setDuration(150).setInterpolator(ViewUtils.CUBIC_INTERPOLATOR).setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            if (loading) {
                                buttonProgress.setVisibility(View.VISIBLE);
                                buttonProgress.setAlpha(0f);
                                buttonProgress.setScaleX(0.4f);
                                buttonProgress.setScaleY(0.4f);
                            }
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            if (!loading) {
                                buttonProgress.setVisibility(View.GONE);
                            }
                        }
                    }).start();

                    buttonText.animate().cancel();
                    buttonText.animate().scaleX(!loading ? 1f : 0.4f).scaleY(!loading ? 1f : 0.4f).alpha(!loading ? 1f : 0f).setDuration(150).setInterpolator(ViewUtils.CUBIC_INTERPOLATOR).setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            if (!loading) {
                                buttonText.setVisibility(View.VISIBLE);
                                buttonText.setAlpha(0f);
                                buttonText.setScaleX(0.4f);
                                buttonText.setScaleY(0.4f);
                            }
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            if (loading) {
                                buttonText.setVisibility(View.GONE);
                            }
                        }
                    }).start();
                }
            } else {
                buttonProgress.setTag(loading ? 1 : null);
                buttonProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
                buttonText.setVisibility(loading ? View.GONE : View.VISIBLE);
            }
            buttonText.setText(loggedIn ? R.string.SettingsCloudManageButtonManage : R.string.SettingsCloudManageButtonLogIn);
            bindHeader();
            buttonView.setOnClickListener(v-> {
                if (loading) {
                    new BeamAlertDialogBuilder(v.getContext())
                            .setTitle(R.string.SettingsCloudManageButtonLogInCancelTitle)
                            .setMessage(R.string.SettingsCloudManageButtonLogInCancel)
                            .setNegativeButton(R.string.No, null)
                            .setPositiveButton(R.string.Yes, (dialog, which) -> CloudController.cancelLogin())
                            .show();
                } else if (Prefs.getCloudAPIToken() != null) {
                    new CloudManageBottomSheet(v.getContext()).show();
                } else {
                    CloudController.beginLogin();
                }
            });

            if (signUpButton != null) {
                boolean showSignUp = !loggedIn && !loading;
                signUpButton.setVisibility(showSignUp ? View.VISIBLE : View.GONE);
                signUpButton.setEnabled(showSignUp && !signUpInProgress);
                signUpButton.setAlpha(signUpButton.isEnabled() ? 1f : 0.6f);
            }
        }

        private void bindHeader() {
            if (titleView == null) return;
            CloudAPI.UserInfo info = CloudController.getUserInfo();
            if (Prefs.getCloudAPIToken() != null && info != null && info.displayName != null) {
                titleView.setText(titleView.getContext().getString(R.string.SettingsCloudManageLoggedInAs, info.displayName));
            } else {
                titleView.setText(R.string.SettingsCloudManageDescription);
            }
        }

        private void showSignUpDialog(Context ctx) {
            if (signUpInProgress) {
                return;
            }

            LinearLayout ll = new LinearLayout(ctx);
            ll.setOrientation(LinearLayout.VERTICAL);
            ll.setPadding(ViewUtils.dp(16), ViewUtils.dp(8), ViewUtils.dp(16), 0);

            TextView emailLabel = new TextView(ctx);
            emailLabel.setText(R.string.SettingsCloudManageSignUpEmail);
            emailLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            emailLabel.setTextColor(ThemesRepo.getColor(android.R.attr.textColorSecondary));
            ll.addView(emailLabel, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            EditText emailInput = new EditText(ctx);
            emailInput.setHint(R.string.SettingsCloudManageSignUpEmail);
            emailInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            emailInput.setTextColor(ThemesRepo.getColor(android.R.attr.textColorPrimary));
            emailInput.setHintTextColor(ThemesRepo.getColor(android.R.attr.textColorSecondary));
            ll.addView(emailInput, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView displayNameLabel = new TextView(ctx);
            displayNameLabel.setText(R.string.SettingsCloudManageSignUpDisplayName);
            displayNameLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            displayNameLabel.setTextColor(ThemesRepo.getColor(android.R.attr.textColorSecondary));
            ll.addView(displayNameLabel, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) {{
                topMargin = ViewUtils.dp(12);
            }});

            EditText displayNameInput = new EditText(ctx);
            displayNameInput.setHint(R.string.SettingsCloudManageSignUpDisplayName);
            displayNameInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
            displayNameInput.setTextColor(ThemesRepo.getColor(android.R.attr.textColorPrimary));
            displayNameInput.setHintTextColor(ThemesRepo.getColor(android.R.attr.textColorSecondary));
            ll.addView(displayNameInput, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) {{
                topMargin = ViewUtils.dp(8);
            }});

            TextView passwordLabel = new TextView(ctx);
            passwordLabel.setText(R.string.SettingsCloudManageSignUpPassword);
            passwordLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            passwordLabel.setTextColor(ThemesRepo.getColor(android.R.attr.textColorSecondary));
            ll.addView(passwordLabel, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) {{
                topMargin = ViewUtils.dp(12);
            }});

            EditText passwordInput = new EditText(ctx);
            passwordInput.setHint(R.string.SettingsCloudManageSignUpPassword);
            passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            passwordInput.setTextColor(ThemesRepo.getColor(android.R.attr.textColorPrimary));
            passwordInput.setHintTextColor(ThemesRepo.getColor(android.R.attr.textColorSecondary));
            ll.addView(passwordInput, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) {{
                topMargin = ViewUtils.dp(8);
            }});

            CheckBox showPassword = new CheckBox(ctx);
            showPassword.setText(R.string.SettingsCloudManageSignUpShowPassword);
            showPassword.setTextColor(ThemesRepo.getColor(android.R.attr.textColorPrimary));
            showPassword.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int selection = passwordInput.getSelectionEnd();
                if (isChecked) {
                    passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                } else {
                    passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
                passwordInput.setSelection(Math.max(selection, 0));
            });
            ll.addView(showPassword, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) {{
                topMargin = ViewUtils.dp(4);
            }});

            TextView dialogTitle = new TextView(ctx);
            dialogTitle.setText(R.string.SettingsCloudManageButtonSignUp);
            dialogTitle.setTextColor(ThemesRepo.getColor(android.R.attr.textColorPrimary));
            dialogTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 28);
            dialogTitle.setTypeface(ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM));
            dialogTitle.setGravity(Gravity.CENTER);
            dialogTitle.setPadding(0, ViewUtils.dp(4), 0, ViewUtils.dp(8));
            ll.addView(dialogTitle, 0, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            new BeamAlertDialogBuilder(ctx)
                    .setView(ll)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.SettingsCloudManageButtonSignUp, (dialog, which) -> {
                        String email = emailInput.getText().toString().trim();
                        String displayName = displayNameInput.getText().toString().trim();
                        String password = passwordInput.getText().toString();
                        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(displayName) || TextUtils.isEmpty(password)) {
                            Toast.makeText(ctx, R.string.SettingsCloudManageSignUpMissingFields, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        CloudAPI api = CloudController.getApiSafe();
                        if (api == null) {
                            Toast.makeText(ctx, R.string.SettingsCloudManageSignUpFailed, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        signUpInProgress = true;
                        bindLoginButton(true);
                        api.signup(email, password, displayName, new APICallback<CloudAPI.AuthToken>() {
                            @Override
                            public void onResponse(CloudAPI.AuthToken response) {
                                Prefs.setCloudAPIToken(response.bearer);
                                signUpInProgress = false;
                                CloudController.init();
                                Santoku.EVENT_BUS.fireEvent(new CloudLoginStateUpdatedEvent());
                            }

                            @Override
                            public void onException(Exception e) {
                                signUpInProgress = false;
                                ViewUtils.postOnMainThread(() -> {
                                    new BeamAlertDialogBuilder(ctx)
                                            .setTitle(R.string.SettingsCloudManageSignUpFailed)
                                            .setMessage(e.toString())
                                            .setPositiveButton(android.R.string.ok, null)
                                            .show();
                                    bindLoginButton(true);
                                });
                            }
                        });
                    })
                    .show();
        }

    }

    private final class AboutItem extends SimpleRecyclerItem<View> {

        @Override
        public View onCreateView(Context ctx) {
            LinearLayout ll = new LinearLayout(ctx);
            ll.setOrientation(LinearLayout.VERTICAL);
            ll.setGravity(Gravity.BOTTOM);

            String versionStr = null;
            PackageManager pm = ctx.getPackageManager();
            try {
                PackageInfo info = pm.getPackageInfo(ctx.getPackageName(), 0);
                versionStr = info.versionName;
            } catch (PackageManager.NameNotFoundException ignored) {}

            TextView subtitle = new TextView(ctx);
            subtitle.setText(ctx.getString(R.string.SettingsAboutVersion, versionStr));
            subtitle.setGravity(Gravity.CENTER);
            subtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            subtitle.setTextColor(ThemesRepo.getColor(android.R.attr.textColorSecondary));
            ll.addView(subtitle, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) {{
                bottomMargin = ViewUtils.dp(12);
            }});

            TextView buttonView = new TextView(ctx);
            buttonView.setText(android.R.string.ok);
            buttonView.setTextColor(ThemesRepo.getColor(R.attr.textColorOnAccent));
            buttonView.setTypeface(ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM));
            buttonView.setGravity(Gravity.CENTER);
            buttonView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            buttonView.setBackground(ViewUtils.createRipple(ThemesRepo.getColor(android.R.attr.colorControlHighlight), ThemesRepo.getColor(android.R.attr.colorAccent), 16));
            buttonView.setOnClickListener(v-> finish());
            ll.addView(buttonView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(52)) {{
                leftMargin = rightMargin = ViewUtils.dp(16);
                bottomMargin = ViewUtils.dp(16);
            }});
            ll.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            return ll;
        }
    }

    private final class IntroItem extends SimpleRecyclerItem<View> {
        private TextView buttonView;

        @Override
        public View onCreateView(Context ctx) {
            LinearLayout ll = new LinearLayout(ctx);
            ll.setOrientation(LinearLayout.VERTICAL);
            ll.setGravity(Gravity.BOTTOM);

            TextView favoriteHint = new TextView(ctx);
            favoriteHint.setText(R.string.IntroLetStartWithColor);
            favoriteHint.setGravity(Gravity.CENTER);
            favoriteHint.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            favoriteHint.setTextColor(ThemesRepo.getColor(android.R.attr.textColorSecondary));
            ll.addView(favoriteHint, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) {{
                bottomMargin = ViewUtils.dp(12);
            }});

            LinearLayout colors = new LinearLayout(ctx);
            colors.setOrientation(LinearLayout.HORIZONTAL);
            colors.setGravity(Gravity.CENTER);
            for (AccentColors color : AccentColors.values()) {
                MiniColorView view = new MiniColorView(ctx);
                view.setColor(color.color);
                int margin = colors.getChildCount() > 0 ? 10 : 0;
                view.setOnClickListener(v -> {
                    int from = ThemesRepo.getColor(android.R.attr.colorAccent);
                    if (from == color.color) return;
                    Prefs.setAccentColor(color.color);

                    int wasIndex = -1;
                    for (int i = 0; i < colors.getChildCount(); i++) {
                        AccentColors _c = AccentColors.values()[i];
                        if (_c.color == from) {
                            wasIndex = i;
                            break;
                        }
                    }

                    int finalWasIndex = wasIndex;
                    new SpringAnimation(new FloatValueHolder(0))
                            .setMinimumVisibleChange(1 / 256f)
                            .setSpring(new SpringForce(1f)
                                    .setStiffness(1000f)
                                    .setDampingRatio(1f))
                            .addUpdateListener((animation, value, velocity) -> {
                                BeamTheme.LIGHT.colors.put(android.R.attr.colorAccent, ColorUtils.blendARGB(from, color.color, value));
                                BeamTheme.DARK.colors.put(android.R.attr.colorAccent, ColorUtils.blendARGB(from, color.color, value));
                                buttonView.setBackground(ViewUtils.createRipple(ThemesRepo.getColor(android.R.attr.colorControlHighlight), ThemesRepo.getColor(android.R.attr.colorAccent), 16));
                                backgroundView.requestRender();
                                if (finalWasIndex != -1) {
                                    ((MiniColorView) colors.getChildAt(finalWasIndex)).setSelectionProgress(1f - value);
                                }
                                ((MiniColorView) v).setSelectionProgress(value);
                            })
                            .addEndListener((animation, canceled, value, velocity) -> adapter.notifyItemChanged(1))
                            .start();
                });

                colors.addView(view, new LinearLayout.LayoutParams(ViewUtils.dp(28), ViewUtils.dp(28)) {{
                    leftMargin = ViewUtils.dp(margin);
                }});
            }
            ll.addView(colors, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) {{
                bottomMargin = ViewUtils.dp(16);
            }});

            buttonView = new TextView(ctx);
            buttonView.setText(R.string.IntroStart);
            buttonView.setTextColor(ThemesRepo.getColor(R.attr.textColorOnAccent));
            buttonView.setTypeface(ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM));
            buttonView.setGravity(Gravity.CENTER);
            buttonView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            buttonView.setBackground(ViewUtils.createRipple(ThemesRepo.getColor(android.R.attr.colorControlHighlight), ThemesRepo.getColor(android.R.attr.colorAccent), 16));
            buttonView.setOnClickListener(v-> scrollToNext());
            ll.addView(buttonView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(52)) {{
                leftMargin = rightMargin = ViewUtils.dp(16);
                bottomMargin = ViewUtils.dp(16);
            }});
            ll.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            return ll;
        }
    }

    private final class ReposItem extends SimpleRecyclerItem<View> {
        private ProgressBar progressBar;
        private FrameLayout loadedLayout;
        private SimpleRecyclerAdapter adapter;
        private TextView cloudImportView;
        private TextView cloudOrView;
        private TextView customProfileView;
        private TextView buttonView;

        @Override
        public View onCreateView(Context ctx) {
            FrameLayout fl = new FrameLayout(ctx);
            progressBar = new ProgressBar(ctx);
            fl.addView(progressBar, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

            loadedLayout = new FrameLayout(ctx);
            RecyclerView recyclerView = new FadeRecyclerView(ctx);
            recyclerView.setAdapter(adapter = new SimpleRecyclerAdapter());
            loadedLayout.addView(recyclerView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

            LinearLayout ll = new LinearLayout(ctx);
            ll.setOrientation(LinearLayout.VERTICAL);
            cloudImportView = new TextView(ctx);
            cloudImportView.setVisibility(View.GONE);
            cloudImportView.setText(R.string.IntroImportFromCloud);
            cloudImportView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            cloudImportView.setGravity(Gravity.CENTER);
            cloudImportView.setPadding(ViewUtils.dp(12), ViewUtils.dp(8), ViewUtils.dp(12), ViewUtils.dp(8));
            cloudImportView.setOnClickListener(v -> startActivity(new Intent(v.getContext(), SetupActivity.class).putExtra(SetupActivity.EXTRA_CLOUD_PROFILE, true).putExtra(SetupActivity.EXTRA_CLOUD_IMPORT_FROM_SETUP, true)));
            ll.addView(cloudImportView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(48)) {{
                leftMargin = rightMargin = ViewUtils.dp(16);
            }});

            cloudOrView = new TextView(ctx);
            cloudOrView.setVisibility(View.GONE);
            cloudOrView.setText(R.string.IntroImportOr);
            cloudOrView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            cloudOrView.setGravity(Gravity.CENTER);
            ll.addView(cloudOrView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) {{
                leftMargin = rightMargin = ViewUtils.dp(16);
            }});

            customProfileView = new TextView(ctx);
            customProfileView.setText(R.string.IntroCustomProfile);
            customProfileView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            customProfileView.setGravity(Gravity.CENTER);
            customProfileView.setPadding(ViewUtils.dp(12), ViewUtils.dp(8), ViewUtils.dp(12), ViewUtils.dp(8));
            customProfileView.setOnClickListener(v -> {
                profilesItem.useCustomProfile = true;
                limitRepoFragmentCount = false;
                SetupActivity.this.adapter.notifyItemRangeInserted(REPOS_INDEX + 1, SetupActivity.this.adapter.getItemCount() - REPOS_INDEX - 1);
                scrollToNext();
            });
            ll.addView(customProfileView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(48)) {{
                leftMargin = rightMargin = ViewUtils.dp(16);
                bottomMargin = ViewUtils.dp(6);
            }});

            buttonView = new TextView(ctx);
            buttonView.setText(R.string.IntroNext);
            buttonView.setTextColor(ThemesRepo.getColor(R.attr.textColorOnAccent));
            buttonView.setTypeface(ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM));
            buttonView.setGravity(Gravity.CENTER);
            buttonView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            buttonView.setOnClickListener(v-> {
                boolean noChecked = true;
                for (ProfilesRepo repo : repos) {
                    if (repo.checked) {
                        noChecked = false;
                        break;
                    }
                }
                if (noChecked) {
                    new BeamAlertDialogBuilder(SetupActivity.this)
                            .setTitle(R.string.IntroNoRepos)
                            .setMessage(R.string.IntroNoReposDescription)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                } else {
                    profilesItem.useCustomProfile = false;
                    limitRepoFragmentCount = false;
                    SetupActivity.this.adapter.notifyItemRangeInserted(REPOS_INDEX + 1, SetupActivity.this.adapter.getItemCount() - REPOS_INDEX - 1);
                    scrollToNext();
                }
            });
            ll.addView(buttonView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(52)) {{
                leftMargin = rightMargin = ViewUtils.dp(16);
                bottomMargin = ViewUtils.dp(16);
            }});
            loadedLayout.addView(ll, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));

            loadedLayout.setAlpha(0f);
            fl.addView(loadedLayout, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            fl.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            return fl;
        }

        @SuppressLint("NotifyDataSetChanged")
        @Override
        public void onBindView(View view) {
            progressBar.setIndeterminateTintList(ColorStateList.valueOf(ThemesRepo.getColor(android.R.attr.colorAccent)));
            cloudImportView.setTextColor(ThemesRepo.getColor(android.R.attr.colorAccent));
            cloudImportView.setBackground(ViewUtils.createRipple(ThemesRepo.getColor(android.R.attr.colorControlHighlight), 16));
            cloudImportView.setVisibility(Prefs.getCloudAPIToken() != null ? View.VISIBLE : View.GONE);
            cloudOrView.setTextColor(ThemesRepo.getColor(android.R.attr.textColorSecondary));
            cloudOrView.setVisibility(Prefs.getCloudAPIToken() != null ? View.VISIBLE : View.GONE);
            customProfileView.setTextColor(ThemesRepo.getColor(android.R.attr.colorAccent));
            customProfileView.setBackground(ViewUtils.createRipple(ThemesRepo.getColor(android.R.attr.colorControlHighlight), 16));
            buttonView.setBackground(ViewUtils.createRipple(ThemesRepo.getColor(android.R.attr.colorControlHighlight), ThemesRepo.getColor(android.R.attr.colorAccent), 16));

            if (adapter.getItemCount() == 0 && isReposLoaded) {
                List<SimpleRecyclerItem> items = new ArrayList<>(repos);
                items.add(new TextHintRecyclerItem(Santoku.INSTANCE.getString(R.string.IntroSelectRepos)));
                adapter.setItems(items);
            } else {
                adapter.notifyDataSetChanged();
            }

            if (isReposLoaded) {
                progressBar.setVisibility(View.GONE);
                loadedLayout.setAlpha(1f);
            } else {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setAlpha(1f);
                progressBar.setScaleX(1f);
                progressBar.setScaleY(1f);
                loadedLayout.setAlpha(0f);
            }
        }

        public void onCloudInfoUpdated() {
            if (cloudImportView != null) {
                cloudImportView.setVisibility(Prefs.getCloudAPIToken() != null ? View.VISIBLE : View.GONE);
                cloudOrView.setVisibility(Prefs.getCloudAPIToken() != null ? View.VISIBLE : View.GONE);
            }
        }

        public void onReposLoaded() {
            List<SimpleRecyclerItem> items = new ArrayList<>(repos);
            items.add(new TextHintRecyclerItem(Santoku.INSTANCE.getString(R.string.IntroSelectRepos)));
            adapter.setItems(items);
            new SpringAnimation(new FloatValueHolder(0))
                    .setMinimumVisibleChange(1 / 256f)
                    .setSpring(new SpringForce(1f)
                            .setStiffness(1000f)
                            .setDampingRatio(1f))
                    .addUpdateListener((animation, value, velocity) -> {
                        progressBar.setAlpha(1f - value);
                        progressBar.setScaleX(1f - value * 0.5f);
                        progressBar.setScaleY(1f - value * 0.5f);

                        loadedLayout.setAlpha(value);
                        loadedLayout.setScaleX(0.5f + value * 0.5f);
                        loadedLayout.setScaleY(0.5f + value * 0.5f);
                    })
                    .addEndListener((animation, canceled, value, velocity) -> progressBar.setVisibility(View.GONE))
                    .start();
        }
    }

    private final class ProfilesItem extends SimpleRecyclerItem<View> {
        private ProgressBar progressBar;
        private FrameLayout loadedLayout;
        private FadeRecyclerView recyclerView;
        private SimpleRecyclerAdapter adapter;
        private TextView buttonView;

        private boolean useCustomProfile;

        @Override
        public View onCreateView(Context ctx) {
            FrameLayout fl = new FrameLayout(ctx);
            progressBar = new ProgressBar(ctx);
            fl.addView(progressBar, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

            loadedLayout = new FrameLayout(ctx);
            recyclerView = new FadeRecyclerView(ctx);
            recyclerView.setAdapter(adapter = new SimpleRecyclerAdapter());
            loadedLayout.addView(recyclerView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) {{
                topMargin = ViewUtils.dp(52);
                bottomMargin = ViewUtils.dp(72);
            }});

            buttonView = new TextView(ctx);
            buttonView.setText(R.string.IntroNext);
            buttonView.setTextColor(ThemesRepo.getColor(R.attr.textColorOnAccent));
            buttonView.setTypeface(ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM));
            buttonView.setGravity(Gravity.CENTER);
            buttonView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            buttonView.setOnClickListener(v-> {
                boolean noChecked = enabledPrinters.isEmpty();
                if (noChecked && !useCustomProfile) {
                    new BeamAlertDialogBuilder(SetupActivity.this)
                            .setTitle(R.string.IntroNoProfiles)
                            .setMessage(R.string.IntroNoProfilesDescription)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                } else {
                    limitProfileFragmentCount = false;
                    SetupActivity.this.adapter.notifyItemRangeInserted(PROFILES_INDEX + 1, SetupActivity.this.adapter.getItemCount() - PROFILES_INDEX - 1);
                    scrollToNext();
                }
            });
            loadedLayout.addView(buttonView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(52), Gravity.BOTTOM) {{
                leftMargin = rightMargin = ViewUtils.dp(16);
                bottomMargin = ViewUtils.dp(16);
            }});

            loadedLayout.setAlpha(0f);
            fl.addView(loadedLayout, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            fl.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            return fl;
        }

        @SuppressLint("NotifyDataSetChanged")
        @Override
        public void onBindView(View view) {
            progressBar.setIndeterminateTintList(ColorStateList.valueOf(ThemesRepo.getColor(android.R.attr.colorAccent)));
            buttonView.setBackground(ViewUtils.createRipple(ThemesRepo.getColor(android.R.attr.colorControlHighlight), ThemesRepo.getColor(android.R.attr.colorAccent), 16));
            if (adapter.getItemCount() == 0 && isReposLoaded) {
                adapter.setItems(getItems());
            } else if (useCustomProfile) {
                adapter.setItems(getItems());
            } else {
                adapter.notifyDataSetChanged();
            }

            if (useCustomProfile) {
                progressBar.setAlpha(0f);
                loadedLayout.setAlpha(1f);
            } else {
                if (isProfilesLoaded) {
                    progressBar.setVisibility(View.GONE);
                    loadedLayout.setAlpha(1f);
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setAlpha(1f);
                    progressBar.setScaleX(1f);
                    progressBar.setScaleY(1f);
                    loadedLayout.setAlpha(0f);
                }
            }
        }

        private List<SimpleRecyclerItem> getItems() {
            if (useCustomProfile) {
                List<SimpleRecyclerItem> items = new ArrayList<>();
                items.add(new BigHeaderItem(getString(R.string.IntroCustomProfileHeader)));
                items.add(new ProfileItem());
                return items;
            }

            List<Slic3rConfigWrapper> vendors = new ArrayList<>();
            for (List<Slic3rConfigWrapper> w : profilesMap.values()) {
                vendors.addAll(w);
            }
            Collections.sort(vendors, (o1, o2) -> o1.vendor.values.get("name").compareToIgnoreCase(o2.vendor.values.get("name")));

            List<SimpleRecyclerItem> items = new ArrayList<>();
            for (Slic3rConfigWrapper w : vendors) {
                items.add(new BigHeaderItem(w.vendor.values.get("name")));

                for (ConfigObject printer : w.printerModels) {
                    if (printer.getTitle().startsWith("*") && printer.getTitle().endsWith("*")) continue;

                    items.add(new ProfileItem(printer, TYPE_PRINTER));
                }
            }
            return items;
        }

        public void onProfilesLoaded() {
            adapter.setItems(getItems());
            new SpringAnimation(new FloatValueHolder(0))
                    .setMinimumVisibleChange(1 / 256f)
                    .setSpring(new SpringForce(1f)
                            .setStiffness(1000f)
                            .setDampingRatio(1f))
                    .addUpdateListener((animation, value, velocity) -> {
                        progressBar.setAlpha(1f - value);
                        progressBar.setScaleX(1f - value * 0.5f);
                        progressBar.setScaleY(1f - value * 0.5f);

                        loadedLayout.setAlpha(value);
                        loadedLayout.setScaleX(0.5f + value * 0.5f);
                        loadedLayout.setScaleY(0.5f + value * 0.5f);
                    })
                    .addEndListener((animation, canceled, value, velocity) -> progressBar.setVisibility(View.GONE))
                    .start();
        }
    }

    private final class FinishItem extends SimpleRecyclerItem<View> {
        private TextView buttonView;

        @Override
        public View onCreateView(Context ctx) {
            FrameLayout fl = new FrameLayout(ctx);
            TextView title = new TextView(ctx);
            title.setText(R.string.IntroConfigured);
            title.setTextColor(ThemesRepo.getColor(android.R.attr.textColorSecondary));
            title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            title.setGravity(Gravity.CENTER);
            fl.addView(title, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

            buttonView = new TextView(ctx);
            buttonView.setText(R.string.IntroFinish);
            buttonView.setTypeface(ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM));
            buttonView.setGravity(Gravity.CENTER);
            buttonView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            buttonView.setOnClickListener(v-> {
                Slic3rConfigWrapper cfg = new Slic3rConfigWrapper();
                if (profilesItem.useCustomProfile) {
                    ConfigObject custom = ConfigObject.createCustomPrinterProfile();
                    custom.profileListType = ConfigObject.PROFILE_LIST_PRINTER;
                    cfg.printerConfigs.add(custom);

                    ConfigObject genericFilament = ConfigObject.createCustomFilamentProfile();
                    cfg.filamentConfigs.add(genericFilament);

                    ConfigObject genericPrint = new ConfigObject(getString(R.string.IntroCustomProfileName));
                    genericPrint.profileListType = ConfigObject.PROFILE_LIST_PRINT;
                    cfg.printConfigs.add(genericPrint);

                    cfg.presets = new ConfigObject();
                    cfg.presets.put("printer", custom.getTitle());
                    cfg.presets.put("print", genericPrint.getTitle());
                    cfg.presets.put("filament", genericFilament.getTitle());
                } else {
                    for (ConfigObject printerModel : enabledPrinters) {
                        String model = printerModel.getTitle();
                        String[] variants = !TextUtils.isEmpty(printerModel.get("variants")) ? printerModel.get("variants").split(";") : new String[]{};
                        String[] materials = !TextUtils.isEmpty(printerModel.get("default_materials")) ? printerModel.get("default_materials").split(";") : new String[]{};

                        for (String variant : variants) {
                            variant = variant.trim();

                            for (List<Slic3rConfigWrapper> wrappers : profilesMap.values()) {
                                for (Slic3rConfigWrapper w : wrappers) {
                                    ConfigObject obj = w.findPrinterVariant(model, variant);
                                    if (obj != null) {
                                        cfg.printerConfigs.add(obj);

                                        Slic3rUtils.ConfigChecker checker = new Slic3rUtils.ConfigChecker(obj.serialize());
                                        for (ConfigObject printConfig : w.printConfigs) {
                                            if (!(printConfig.getTitle().startsWith("*") && printConfig.getTitle().endsWith("*")) && checker.checkCompatibility(printConfig.get("compatible_printers_condition"))) {
                                                cfg.printConfigs.add(printConfig);
                                            }
                                        }
                                        checker.release();
                                    }
                                }
                            }
                        }

                        for (String mat : materials) {
                            mat = mat.trim();

                            for (List<Slic3rConfigWrapper> wrappers : profilesMap.values()) {
                                for (Slic3rConfigWrapper w : wrappers) {
                                    ConfigObject obj = w.findFilament(mat);
                                    if (obj != null) cfg.filamentConfigs.add(obj);
                                }
                            }
                        }
                    }
                    cfg.presets = new ConfigObject();
                    if (!cfg.printerConfigs.isEmpty()) {
                        boolean foundDefault = false;
                        for (ConfigObject obj : cfg.printerConfigs) {
                            if (obj.getTitle().contains("0.4")) {
                                foundDefault = true;
                                cfg.presets.put("printer", obj.getTitle());
                                break;
                            }
                        }
                        if (!foundDefault && !cfg.printerConfigs.isEmpty()) {
                            cfg.presets.put("printer", cfg.printerConfigs.get(0).getTitle());
                        }
                    }

                    ConfigObject defPrinter = cfg.printerConfigs.isEmpty() ? null : cfg.findPrinter(cfg.presets.get("printer"));
                    if (defPrinter != null) {
                        Slic3rUtils.ConfigChecker checker = new Slic3rUtils.ConfigChecker(defPrinter.serialize());
                        if (defPrinter.get("default_print_profile") != null && cfg.findPrint(defPrinter.get("default_print_profile")) != null) {
                            cfg.presets.put("print", defPrinter.get("default_print_profile"));
                        } else {
                            if (!cfg.printConfigs.isEmpty()) {
                                boolean foundDefault = false;
                                for (ConfigObject obj : cfg.printConfigs) {
                                    if (obj.get("layer_height") != null && checker.checkCompatibility(obj.get("compatible_printers_condition")) && Float.parseFloat(obj.get("layer_height")) == 0.2f) {
                                        foundDefault = true;
                                        cfg.presets.put("print", obj.getTitle());
                                        break;
                                    }
                                }
                                if (!foundDefault && !cfg.printConfigs.isEmpty()) {
                                    cfg.presets.put("print", cfg.printConfigs.get(0).getTitle());
                                }
                            }
                        }
                        if (defPrinter.get("default_filament_profile") != null && cfg.findFilament(defPrinter.get("default_filament_profile")) != null) {
                            cfg.presets.put("filament", defPrinter.get("default_filament_profile"));
                        } else {
                            if (!cfg.filamentConfigs.isEmpty()) {
                                boolean foundDefault = false;
                                for (ConfigObject obj : cfg.filamentConfigs) {
                                    if (obj.getTitle().contains("Generic PLA") && checker.checkCompatibility(obj.get("compatible_printers_condition"))) { // TODO: Slic3rUtils.checkCompatibility(obj.get("compatible_prints_condition"), serialized)
                                        foundDefault = true;
                                        cfg.presets.put("filament", obj.getTitle());
                                        break;
                                    }
                                }
                                if (!foundDefault && !cfg.filamentConfigs.isEmpty()) {
                                    cfg.presets.put("filament", cfg.filamentConfigs.get(0).getTitle());
                                }
                            }
                        }
                        checker.release();
                    }
                }
                try {
                    if (Prefs.getCloudAPIToken() == null || Santoku.CONFIG == null) {
                        Santoku.getCurrentConfigFile().delete();
                        Santoku.CONFIG = cfg;
                        FileOutputStream fos = new FileOutputStream(Santoku.getConfigFile());
                        fos.write(cfg.serialize().getBytes(StandardCharsets.UTF_8));
                        fos.close();
                    }

                    startActivity(new Intent(SetupActivity.this, MainActivity.class));
                    finish();
                } catch (Exception e) {
                    Log.e(TAG, "Failed to save config", e);
                }
            });
            fl.addView(buttonView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(52), Gravity.BOTTOM) {{
                leftMargin = rightMargin = ViewUtils.dp(16);
                bottomMargin = ViewUtils.dp(16);
            }});
            fl.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            return fl;
        }

        @Override
        public void onBindView(View view) {
            buttonView.setTextColor(ThemesRepo.getColor(R.attr.textColorOnAccent));
            buttonView.setBackground(ViewUtils.createRipple(ThemesRepo.getColor(android.R.attr.colorControlHighlight), ThemesRepo.getColor(android.R.attr.colorAccent), 16));
        }
    }

    public enum AccentColors {
        DEFAULT(0xff38ef7d),
        BLUE(0xff5492f5),
        LIGHT_BLUE(0xff6dd5fa),
        RED(0xffe94056),
        ORANGE(0xffff4b2c),
        YELLOW(0xfffdc831),
        PINK(0xfff2709b),
        PURPLE(0xff6e74e1);

        public final int color;

        AccentColors(int color) {
            this.color = color;
        }
    }

    private static String parseVendorVersion(InputStream in) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(in));
        String line;
        while ((line = r.readLine()) != null) {
            if (line.contains(" = ")) continue;
            return line.substring(0, line.indexOf(' '));
        }
        return null;
    }

    private final static class ProfilesRepo extends SimpleRecyclerItem<ProfilesRepo.RepoHolderView> {
        private String url;
        private String name;
        private String description;
        private String indexUrl;
        private boolean checked;

        @Override
        public RepoHolderView onCreateView(Context ctx) {
            return new RepoHolderView(ctx);
        }

        @Override
        public void onBindView(RepoHolderView view) {
            view.bind(this);
        }

        public final static class RepoHolderView extends LinearLayout implements IThemeView {
            private TextView title;
            private TextView subtitle;
            private BeamSwitch mSwitch;

            public RepoHolderView(@NonNull Context context) {
                super(context);

                setOrientation(HORIZONTAL);
                setGravity(Gravity.CENTER_VERTICAL);

                LinearLayout inner = new LinearLayout(context);
                inner.setOrientation(VERTICAL);
                title = new TextView(context);
                title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
                title.setTypeface(ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM));
                inner.addView(title);

                subtitle = new TextView(context);
                subtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
                inner.addView(subtitle);

                addView(inner, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                mSwitch = new BeamSwitch(context);
                addView(mSwitch);

                setPadding(ViewUtils.dp(21), ViewUtils.dp(16), ViewUtils.dp(21), ViewUtils.dp(16));
                setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) {{
                    leftMargin = rightMargin = ViewUtils.dp(12);
                    topMargin = ViewUtils.dp(12);
                }});
                onApplyTheme();
            }

            public void bind(ProfilesRepo item) {
                title.setText(item.name);
                subtitle.setText(item.description);
                mSwitch.setChecked(item.checked);
                setOnClickListener(v -> {
                    item.checked = !item.checked;
                    mSwitch.setChecked(item.checked);
                });
                mSwitch.onApplyTheme();
                onApplyTheme();
            }

            @Override
            public void onApplyTheme() {
                title.setTextColor(ThemesRepo.getColor(android.R.attr.textColorPrimary));
                subtitle.setTextColor(ThemesRepo.getColor(android.R.attr.textColorSecondary));
                setBackground(ViewUtils.createRipple(ThemesRepo.getColor(android.R.attr.colorControlHighlight), ColorUtils.setAlphaComponent(ThemesRepo.getColor(android.R.attr.colorControlHighlight), 0x10), 32));
            }
        }
    }

    private final class ProfileItem extends SimpleRecyclerItem<ProfileItem.ProfileHolderView> {
        private ConfigObject object;
        private int type = TYPE_PRINTER;

        private ProfileItem(ConfigObject obj, int type) {
            this.object = obj;
            this.type = type;
        }

        private ProfileItem() {}

        @Override
        public ProfileHolderView onCreateView(Context ctx) {
            return new ProfileHolderView(ctx);
        }

        @Override
        public void onBindView(ProfileHolderView view) {
            view.bind(this);
        }

        public final class ProfileHolderView extends LinearLayout implements IThemeView {
            private ImageView icon;
            private TextView title;
            private BeamSwitch mSwitch;

            public ProfileHolderView(@NonNull Context context) {
                super(context);

                setOrientation(HORIZONTAL);
                setGravity(Gravity.CENTER_VERTICAL);

                icon = new ImageView(context);
                addView(icon, new LinearLayout.LayoutParams(ViewUtils.dp(36), ViewUtils.dp(36)) {{
                    rightMargin = ViewUtils.dp(16);
                }});

                LinearLayout inner = new LinearLayout(context);
                inner.setOrientation(VERTICAL);
                title = new TextView(context);
                title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
                title.setTypeface(ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM));
                inner.addView(title);

                addView(inner, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                mSwitch = new BeamSwitch(context);
                addView(mSwitch, new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT) {{
                    leftMargin = ViewUtils.dp(12);
                }});

                setPadding(ViewUtils.dp(16), ViewUtils.dp(12), ViewUtils.dp(21), ViewUtils.dp(12));
                setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) {{
                    leftMargin = rightMargin = ViewUtils.dp(12);
                    bottomMargin = ViewUtils.dp(8);
                }});
                onApplyTheme();
            }

            private List<ConfigObject> getList() {
                return enabledPrinters;
            }

            public void bind(ProfileItem item) {
                LayoutParams params = (LayoutParams) icon.getLayoutParams();
                if (item.object == null || item.object.thumbnailUrl == null) {
                    params.width = params.height = ViewUtils.dp(36);
                    icon.setColorFilter(ThemesRepo.getColor(android.R.attr.colorAccent));
                    switch (type) {
                        case TYPE_PRINTER:
                            icon.setImageResource(R.drawable.printer_outline_28);
                            break;
                        case TYPE_PRINT_CONFIG:
                            icon.setImageResource(R.drawable.wrench_outline_28);
                            break;
                        case TYPE_FILAMENT:
                            icon.setImageResource(R.drawable.slot_filament_28);
                            break;
                    }
                } else {
                    params.width = params.height = ViewUtils.dp(52);

                    icon.setColorFilter(null);
                    Glide.with(icon)
                            .load(item.object.thumbnailUrl)
                            .transform(new RoundedCorners(ViewUtils.dp(12)))
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .into(icon);
                }
                icon.requestLayout();
                mSwitch.onApplyTheme();

                if (item.object == null) {
                    title.setText(R.string.IntroCustomProfileName);
                    mSwitch.setChecked(true);
                    setOnClickListener(null);
                    setClickable(false);
                    return;
                }

                title.setText(item.object.get("name") != null ? item.object.get("name") : item.object.getTitle());
                boolean checked = getList().contains(item.object);

                mSwitch.setChecked(checked);
                setOnClickListener(v -> {
                    boolean _checked = getList().contains(item.object);
                    _checked = !_checked;
                    mSwitch.setChecked(_checked);

                    if (_checked) {
                        getList().add(item.object);
                    } else {
                        getList().remove(item.object);
                    }
                });
                onApplyTheme();
            }

            @Override
            public void onApplyTheme() {
                title.setTextColor(ThemesRepo.getColor(android.R.attr.textColorPrimary));
                setBackground(ViewUtils.createRipple(ThemesRepo.getColor(android.R.attr.colorControlHighlight), ColorUtils.setAlphaComponent(ThemesRepo.getColor(android.R.attr.colorControlHighlight), 0x10), 32));
            }
        }
    }
}
