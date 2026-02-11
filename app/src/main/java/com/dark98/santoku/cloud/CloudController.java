package com.dark98.santoku.cloud;

import android.content.Intent;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ru.ytkab0bp.sapil.APICallback;
import ru.ytkab0bp.sapil.APIRequestHandle;
import com.dark98.santoku.R;
import com.dark98.santoku.Santoku;
import com.dark98.santoku.components.BeamAlertDialogBuilder;
import com.dark98.santoku.events.CloudLoginStateUpdatedEvent;
import com.dark98.santoku.events.CloudSyncFinishedEvent;
import com.dark98.santoku.events.CloudUserInfoUpdatedEvent;
import com.dark98.santoku.events.NeedDismissSnackbarEvent;
import com.dark98.santoku.events.NeedSnackbarEvent;
import com.dark98.santoku.slic3r.Slic3rConfigWrapper;
import com.dark98.santoku.utils.IOUtils;
import com.dark98.santoku.utils.Prefs;
import com.dark98.santoku.utils.ViewUtils;
import com.dark98.santoku.view.SnackbarsLayout;

public class CloudController {
    public final static String CLOUD_SYNC_TAG = "cloud_sync";

    private final static String TAG = "cloud";
    private final static long MIN_SYNC_DELTA = 5 * 60 * 1000L; // Once in 5 minutes
    private static boolean isSyncInProgress;
    private static CloudAPI.UserInfo userInfo;

    private static boolean isLoggingIn;
    private static APIRequestHandle beginLoginHandle;
    private static String loginSessionId;
    private static Runnable loginAutoCancel = () -> {
        loginSessionId = null;
        isLoggingIn = false;
        Santoku.EVENT_BUS.fireEvent(new CloudLoginStateUpdatedEvent());
    };
    private static Runnable loginCheck = new Runnable() {
        @Override
        public void run() {
            CloudAPI api = getApiSafe();
            if (api == null) {
                loginSessionId = null;
                isLoggingIn = false;
                Santoku.EVENT_BUS.fireEvent(new CloudLoginStateUpdatedEvent());
                return;
            }
            api.loginCheck(loginSessionId, new APICallback<CloudAPI.LoginState>() {
                @Override
                public void onResponse(CloudAPI.LoginState response) {
                    if (response.loggedIn) {
                        Prefs.setCloudAPIToken(response.bearer);
                        loadUserInfo();
                        ViewUtils.removeCallbacks(loginAutoCancel);
                    } else if (isLoggingIn) {
                        ViewUtils.postOnMainThread(loginCheck, 5000);
                    }
                }

                @Override
                public void onException(Exception e) {
                    Log.e(TAG, "Failed to check login state", e);

                    if (isLoggingIn) {
                        ViewUtils.postOnMainThread(loginCheck, 5000);
                    }
                }
            });
        }
    };

    private static Gson gson = new Gson();

    public static CloudAPI getApiSafe() {
        try {
            return CloudAPI.INSTANCE;
        } catch (Throwable t) {
            Log.e(TAG, "Cloud API unavailable", t);
            return null;
        }
    }

    public static void initCached() {
        if (Prefs.getCloudAPIToken() != null) {
            if (Prefs.getCloudCachedUserInfo() != null) {
                userInfo = gson.fromJson(Prefs.getCloudCachedUserInfo(), CloudAPI.UserInfo.class);
            }
        }
    }

    public static void init() {
        if (Prefs.getCloudAPIToken() != null) {
            long now = Santoku.TRUE_TIME.now().getTime();
            if (userInfo == null) {
                loadUserInfo();
            }

            if (userInfo != null && isSyncAvailable() && Prefs.isCloudProfileSyncEnabled()) {
                if (now - Prefs.getCloudLastSync() > MIN_SYNC_DELTA) {
                    syncData();
                }
            }
        }
    }

    private static void loadUserInfo() {
        CloudAPI api = getApiSafe();
        if (api == null) {
            return;
        }
        api.userGetInfo(new APICallback<CloudAPI.UserInfo>() {
            @Override
            public void onResponse(CloudAPI.UserInfo response) {
                userInfo = response;

                if (userInfo.id.equals("null")) {
                    userInfo = null;
                    Prefs.setCloudAPIToken(null);
                    Prefs.setCloudCachedUserInfo(null);
                    Santoku.EVENT_BUS.fireEvent(new CloudUserInfoUpdatedEvent());

                    if (isLoggingIn) {
                        isLoggingIn = false;
                        Santoku.EVENT_BUS.fireEvent(new CloudLoginStateUpdatedEvent());
                    }
                } else {
                    Prefs.setCloudCachedUserInfo(gson.toJson(userInfo));

                    Santoku.EVENT_BUS.fireEvent(new CloudUserInfoUpdatedEvent());

                    if (isLoggingIn) {
                        isLoggingIn = false;
                        Santoku.EVENT_BUS.fireEvent(new CloudLoginStateUpdatedEvent());
                    }

                    if (isSyncAvailable() && Prefs.isCloudProfileSyncEnabled()) {
                        syncData();
                    }
                }
            }

            @Override
            public void onException(Exception e) {
                Log.e(TAG, "Failed to get user info", e);
                ViewUtils.postOnMainThread(CloudController::init, 15000);
            }
        });
    }

    public static boolean isLoggingIn() {
        return isLoggingIn;
    }

    private static void beginLogin0() {
        CloudAPI api = getApiSafe();
        if (api == null) {
            isLoggingIn = false;
            Santoku.EVENT_BUS.fireEvent(new CloudLoginStateUpdatedEvent());
            return;
        }
        beginLoginHandle = api.loginBegin(new APICallback<CloudAPI.LoginData>() {
            @Override
            public void onResponse(CloudAPI.LoginData response) {
                loginSessionId = response.sessionId;

                ViewUtils.postOnMainThread(loginAutoCancel, response.expiresAt * 1000L - Santoku.TRUE_TIME.now().getTime());
                ViewUtils.postOnMainThread(loginCheck, 5000);
                ViewUtils.postOnMainThread(() -> Santoku.INSTANCE.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(response.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
            }

            @Override
            public void onException(Exception e) {
                ViewUtils.postOnMainThread(CloudController::beginLogin0, 15000);
            }
        });
    }

    public static void beginLogin() {
        isLoggingIn = true;
        Santoku.EVENT_BUS.fireEvent(new CloudLoginStateUpdatedEvent());
        beginLogin0();
    }

    public static void cancelLogin() {
        isLoggingIn = false;
        Santoku.EVENT_BUS.fireEvent(new CloudLoginStateUpdatedEvent());
        if (loginSessionId != null) {
            CloudAPI api = getApiSafe();
            if (api != null) {
                api.loginCancel(loginSessionId, response -> {});
            }
        }
        if (beginLoginHandle != null && beginLoginHandle.isRunning()) {
            beginLoginHandle.cancel();
            beginLoginHandle = null;
        }
        ViewUtils.removeCallbacks(loginCheck);
        ViewUtils.removeCallbacks(loginAutoCancel);
        loginSessionId = null;
    }

    public static void logout() {
        CloudAPI api = getApiSafe();
        if (api != null) {
            api.logout(response -> {});
        }
        Prefs.setCloudAPIToken(null);
        userInfo = null;
        Santoku.EVENT_BUS.fireEvent(new CloudLoginStateUpdatedEvent());
        Santoku.EVENT_BUS.fireEvent(new CloudUserInfoUpdatedEvent());
    }

    public static CloudAPI.UserInfo getUserInfo() {
        return userInfo;
    }

    public static boolean hasAccountFeatures() {
        return true;
    }

    public static boolean isSyncAvailable() {
        return Prefs.getCloudAPIToken() != null && userInfo != null;
    }

    private static void downloadData(long lastModified) {
        CloudAPI api = getApiSafe();
        if (api == null) {
            isSyncInProgress = false;
            return;
        }
        api.syncGet(new APICallback<String>() {
            @Override
            public void onResponse(String response) {
                IOUtils.IO_POOL.submit(() -> {
                    try {
                        File f = Santoku.getConfigFile();
                        byte[] data = Base64.decode(response, 0);
                        FileOutputStream fos = new FileOutputStream(f);
                        fos.write(data);
                        fos.close();

                        Santoku.CONFIG = new Slic3rConfigWrapper(f);

                        Prefs.setCloudLocalLastModified(lastModified);
                        Prefs.setCloudLocalLastSentModified(lastModified);
                        Prefs.setCloudRemoteLastModified(lastModified);
                        Prefs.setCloudLastSync(Santoku.TRUE_TIME.now().getTime());
                        Santoku.EVENT_BUS.fireEvent(new NeedDismissSnackbarEvent(CLOUD_SYNC_TAG));
                        Santoku.EVENT_BUS.fireEvent(new NeedSnackbarEvent(R.string.CloudSyncSuccess));
                        Santoku.EVENT_BUS.fireEvent(new CloudSyncFinishedEvent());
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to write data", e);
                        isSyncInProgress = false;

                        Santoku.EVENT_BUS.fireEvent(new NeedDismissSnackbarEvent(CLOUD_SYNC_TAG));
                        Santoku.EVENT_BUS.fireEvent(new NeedSnackbarEvent(SnackbarsLayout.Type.ERROR, R.string.CloudSyncError));
                        Santoku.EVENT_BUS.fireEvent(new CloudSyncFinishedEvent());
                    }
                });
            }

            @Override
            public void onException(Exception e) {
                Log.e(TAG, "Failed to download data", e);
                isSyncInProgress = false;

                Santoku.EVENT_BUS.fireEvent(new NeedDismissSnackbarEvent(CLOUD_SYNC_TAG));
                Santoku.EVENT_BUS.fireEvent(new NeedSnackbarEvent(SnackbarsLayout.Type.ERROR, R.string.CloudSyncError));
                Santoku.EVENT_BUS.fireEvent(new CloudSyncFinishedEvent());
            }
        });
    }

    private static void syncData() {
        if (isSyncInProgress) {
            return;
        }
        CloudAPI api = getApiSafe();
        if (api == null) {
            return;
        }
        long modified = Prefs.getCloudLocalLastModified();
        isSyncInProgress = true;
        Santoku.EVENT_BUS.fireEvent(new NeedSnackbarEvent(SnackbarsLayout.Type.LOADING, R.string.CloudSyncInProgress).tag(CLOUD_SYNC_TAG));

        api.syncGetState(new APICallback<CloudAPI.SyncState>() {
            @Override
            public void onResponse(CloudAPI.SyncState response) {
                if (Santoku.CONFIG == null && response.usedSize != 0) {
                    // Setup screen, no config yet
                    downloadData(response.lastUpdatedDate);
                } else if (response.usedSize == 0) {
                    if (Santoku.CONFIG == null) {
                        Santoku.EVENT_BUS.fireEvent(new NeedDismissSnackbarEvent(CLOUD_SYNC_TAG));
                        Santoku.EVENT_BUS.fireEvent(new CloudSyncFinishedEvent());
                        return;
                    }

                    // No data on server yet, send anyway
                    uploadData(modified);
                } else if (response.lastUpdatedDate != Prefs.getCloudRemoteLastModified()) {
                    if (Prefs.getCloudLocalLastSentModified() == modified) {
                        // Modified only on server
                        downloadData(response.lastUpdatedDate);
                    } else {
                        // Modified on client and on server
                        Santoku.EVENT_BUS.fireEvent(new NeedDismissSnackbarEvent(CLOUD_SYNC_TAG));
                        Santoku.EVENT_BUS.fireEvent(new NeedSnackbarEvent(SnackbarsLayout.Type.WARNING, R.string.CloudSyncConflict).button(R.string.CloudSyncConflictResolve, v -> {
                            SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
                            new BeamAlertDialogBuilder(v.getContext())
                                    .setTitle(R.string.CloudSyncConflict)
                                    .setMessage(v.getContext().getString(R.string.CloudSyncConflictResolveMessage, format.format(new Date(response.lastUpdatedDate)), format.format(new Date(Prefs.getCloudLocalLastModified()))))
                                    .setPositiveButton(R.string.CloudSyncConflictChooseRemote, (dialog, which) -> downloadData(response.lastUpdatedDate))
                                    .setNegativeButton(R.string.CloudSyncConflictChooseLocal, (dialog, which) -> uploadData(modified))
                                    .show();
                        }).tag(CLOUD_SYNC_TAG));
                    }
                } else {
                    if (Prefs.getCloudLocalLastSentModified() != modified) {
                        // Modified only on client
                        uploadData(modified);
                    } else {
                        // Not modified on server and on client
                        Santoku.EVENT_BUS.fireEvent(new NeedDismissSnackbarEvent(CLOUD_SYNC_TAG));
                        Santoku.EVENT_BUS.fireEvent(new CloudSyncFinishedEvent());
                    }
                }
            }

            @Override
            public void onException(Exception e) {
                Log.e(TAG, "Failed to get sync state", e);
                isSyncInProgress = false;

                Santoku.EVENT_BUS.fireEvent(new NeedDismissSnackbarEvent(CLOUD_SYNC_TAG));
                Santoku.EVENT_BUS.fireEvent(new NeedSnackbarEvent(SnackbarsLayout.Type.ERROR, R.string.CloudSyncError));
            }
        });
    }

    private static void uploadData(long modified) {
        IOUtils.IO_POOL.submit(() -> {
            try {
                File f = Santoku.getConfigFile();
                FileInputStream fis = new FileInputStream(f);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buffer = new byte[10240];
                int c;
                while ((c = fis.read(buffer)) != -1) {
                    bos.write(buffer, 0, c);
                }
                bos.close();
                fis.close();

                CloudAPI api = getApiSafe();
                if (api == null) {
                    isSyncInProgress = false;
                    return;
                }
                api.syncUpload(Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP), "application/ini", new APICallback<CloudAPI.SyncState>() {
                    @Override
                    public void onResponse(CloudAPI.SyncState response) {
                        isSyncInProgress = false;
                        if (Prefs.getCloudLocalLastModified() != modified) { // Re-send otherwise
                            syncData();
                            return;
                        }
                        Prefs.setCloudRemoteLastModified(response.lastUpdatedDate);
                        Prefs.setCloudLocalLastSentModified(modified);
                        Prefs.setCloudLastSync(Santoku.TRUE_TIME.now().getTime());
                        Santoku.EVENT_BUS.fireEvent(new NeedDismissSnackbarEvent(CLOUD_SYNC_TAG));
                        Santoku.EVENT_BUS.fireEvent(new NeedSnackbarEvent(R.string.CloudSyncSuccess));
                        Santoku.EVENT_BUS.fireEvent(new CloudSyncFinishedEvent());
                    }

                    @Override
                    public void onException(Exception e) {
                        Log.e(TAG, "Failed to upload sync data", e);
                        isSyncInProgress = false;

                        Santoku.EVENT_BUS.fireEvent(new NeedDismissSnackbarEvent(CLOUD_SYNC_TAG));
                        Santoku.EVENT_BUS.fireEvent(new NeedSnackbarEvent(SnackbarsLayout.Type.ERROR, R.string.CloudSyncError));
                        Santoku.EVENT_BUS.fireEvent(new CloudSyncFinishedEvent());
                    }
                });
            } catch (IOException e) {
                Log.e(TAG, "Failed to read sync data", e);
                isSyncInProgress = false;

                Santoku.EVENT_BUS.fireEvent(new NeedDismissSnackbarEvent(CLOUD_SYNC_TAG));
                Santoku.EVENT_BUS.fireEvent(new NeedSnackbarEvent(SnackbarsLayout.Type.ERROR, R.string.CloudSyncError));
                Santoku.EVENT_BUS.fireEvent(new CloudSyncFinishedEvent());
            }
        });
    }

    public static void notifyDataChanged() {
        long now = Santoku.TRUE_TIME.now().getTime();
        Prefs.setCloudLocalLastModified(now);
        if (!isSyncAvailable() || !Prefs.isCloudProfileSyncEnabled()) {
            return;
        }
        if (now - Prefs.getCloudLastSync() > MIN_SYNC_DELTA) {
            syncData();
        }
    }
}
