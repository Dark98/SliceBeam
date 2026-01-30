package com.dark98.santoku.cloud;

import androidx.annotation.Nullable;

import com.google.gson.Gson;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
            return "https://api.beam3d.ru/v1/";
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
     * Gets user features
     */
    @Method("user/getFeatures")
    void userGetFeatures(APICallback<UserFeatures> callback);

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
     * Generates 3D model from image
     * <p>
     * @param image Base64 encoded image
     * <p>
     * Requires authorization
     */
    @Method(requestType = RequestType.POST, value = "models/generate")
    void modelsGenerate(@Arg("") String image, @Header("Content-Type") String type, APICallback<InputStream> callback);

    /**
     * Gets remaining model generations count
     * <p>
     * Requires authorization
     */
    @Method("models/getRemainingCount")
    void modelsGetRemainingCount(APICallback<ModelsRemainingCount> callback);

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

    final class UserFeatures {
        /**
         * Which level is required for early access
         */
        public int earlyAccessLevel;

        /**
         * Which level is required for data sync
         */
        public int syncRequiredLevel;

        /**
         * Which level is required for AI model generator
         */
        public int aiGeneratorRequiredLevel;

        /**
         * Models per month max
         */
        public int aiGeneratorModelsPerMonth;

        /**
         * Url at which user should be redirected for info about how to restore a subscription
         */
        public String alreadySubscribedInfoUrl;

        /**
         * List of subscription levels
         */
        public List<SubscriptionLevel> levels = new ArrayList<>();
    }

    final class SubscriptionLevel {
        /**
         * Int representation
         */
        public int level;

        /**
         * Title of this level
         */
        public String title;

        /**
         * Price of this level
         */
        public String price;

        /**
         * Url at which user should be redirected for purchase
         */
        public String subscribeOrUpgradeUrl;

        /**
         * Url at which user should be redirected for managing the subscription
         */
        public String manageUrl;
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

        /**
         * Current subscription level
         */
        public int currentLevel;
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

    final class ModelsRemainingCount {
        /**
         * Used generations
         */
        public int used;

        /**
         * Max available generations
         */
        public int max;
    }
}
