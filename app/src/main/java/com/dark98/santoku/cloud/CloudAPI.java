package com.dark98.santoku.cloud;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

import ru.ytkab0bp.sapil.APICallback;
import ru.ytkab0bp.sapil.APILibrary;
import ru.ytkab0bp.sapil.APIRequestHandle;
import ru.ytkab0bp.sapil.APIRunner;
import ru.ytkab0bp.sapil.Arg;
import ru.ytkab0bp.sapil.Header;
import ru.ytkab0bp.sapil.Method;
import ru.ytkab0bp.sapil.RequestType;
import com.dark98.santoku.BuildConfig;
import com.dark98.santoku.utils.Prefs;

public interface CloudAPI extends APIRunner {
    CloudAPI INSTANCE = APILibrary.newRunner(CloudAPI.class, new RunnerConfig() {
        private final Map<String, String> headers = new HashMap<>();

        @Override
        public String getBaseURL() {
            return BuildConfig.CLOUD_BASE_URL_PROD;
        }

        @Override
        public String getDefaultUserAgent() {
            return "Santoku v" + BuildConfig.VERSION_NAME + "/" + BuildConfig.VERSION_CODE;
        }

        @Override
        public Map<String, String> getDefaultHeaders() {
            headers.clear();
            if (Prefs.getCloudAPIToken() != null) {
                headers.put("Authorization", "Bearer " + Prefs.getCloudAPIToken());
            }
            return headers;
        }

        @Override
        public ru.ytkab0bp.sapil.util.Pair<String, String> getNamingTransformPolicy() {
            return null;
        }
    });

    /**
     * Begins login flow, returns auth link
     */
    @Method("login/begin")
    APIRequestHandle loginBegin(APICallback<LoginData> callback);

    /**
     * Checks new login state by session id
     */
    @Method("login/check")
    void loginCheck(@Arg("sessionId") String sessionId, APICallback<LoginState> callback);

    /**
     * Cancels login flow
     */
    @Method("login/cancel")
    void loginCancel(@Arg("sessionId") String sessionId, APICallback<Boolean> callback);

    /**
     * Gets current user info
     * <p>
     * Requires authorization
     */
    @Method("user/getInfo")
    void userGetInfo(APICallback<UserInfo> callback);

    /**
     * Creates a new account (email/password)
     */
    @Method(requestType = RequestType.POST, value = "signup")
    void signup(@Arg("email") String email, @Arg("password") String password, @Arg("displayName") String displayName, APICallback<AuthToken> callback);

    /**
     * Login with email/password
     */
    @Method(requestType = RequestType.POST, value = "login")
    void login(@Arg("email") String email, @Arg("password") String password, APICallback<AuthToken> callback);

    /**
     * Fetches sync state
     * <p>
     * Requires authorization
     */
    @Method("sync/getState")
    void syncGetState(APICallback<SyncState> callback);

    /**
     * Uploads new data to the server
     * <p>
     * @param data New base64 encoded data
     * <p>
     * Requires authorization
     */
    @Method(requestType = RequestType.POST, value = "sync/upload")
    void syncUpload(@Arg("") String data, @Header("Content-Type") String type, APICallback<SyncState> callback);

    /**
     * Downloads base64 data
     * <p>
     * Requires authorization
     */
    @Method("sync/get")
    void syncGet(APICallback<String> callback);

    /**
     * Destroys token
     * <p>
     * Requires authorization
     */
    @Method("logout")
    void logout(APICallback<Boolean> callback);

    final class LoginData {
        /**
         * Url that should be clicked by the user to authorize
         */
        public String url;

        /**
         * Session identifier
         */
        public String sessionId;

        /**
         * Time at which session should be considered expired if not logged in
         */
        public long expiresAt;
    }

    final class LoginState {
        /**
         * If user is now logged in
         */
        public boolean loggedIn;

        /**
         * Bearer token if auth was successful
         */
        public String bearer;
    }

    final class AuthToken {
        /**
         * Bearer token
         */
        public String bearer;
    }

    final class UserInfo {
        /**
         * User's id
         */
        public String id;

        /**
         * User's display name
         */
        public String displayName;

        /**
         * User's avatar. Could be null
         */
        @Nullable
        public String avatarUrl;

    }


    final class SyncState {
        /**
         * Cloud data last updated time
         */
        public long lastUpdatedDate = 0;

        /**
         * Used size of cloud storage
         */
        public long usedSize;

        /**
         * Max storage size
         */
        public long maxSize;
    }

}
