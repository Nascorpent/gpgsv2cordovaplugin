package com.nascorpent;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.games.PlayGamesSdk;
import com.google.android.gms.games.PlayGames;
import com.google.android.gms.games.GamesSignInClient;
import com.google.android.gms.games.AuthenticationResult;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.SnapshotsClient;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotMetadata;
import com.google.android.gms.games.snapshot.SnapshotMetadataChange;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.games.GamesActivityResultCodes;
import java.util.Objects;


public class cordovaplugingpgsv2 extends CordovaPlugin {

    private static final String TAG = "GPGS_Plugin";
    public boolean pgsVerification;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        //this.cordova.getActivity().runOnUiThread(() -> PlayGamesSdk.initialize(cordova.getActivity()));

    }

    @Override
    protected void pluginInitialize() {
        Log.i(TAG, "Starting plugin");
        this.cordova.getActivity().runOnUiThread(() -> {
            PlayGamesSdk.initialize(cordova.getActivity());
            Log.i(TAG, "PlayGamesSdk initialized");
        });

    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(true);

        cordova.getActivity().runOnUiThread(() -> {
            SharedPreferences preferences = cordova.getActivity().getSharedPreferences("GamePrefs", Context.MODE_PRIVATE);
            pgsVerification = preferences.getBoolean("PGS_VERIFICATION", true);
            Log.i(TAG, "PGS_VERIFICATION: " + pgsVerification);
        });

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (pgsVerification) {
                if (isPlayGamesInstalled()) {
                    Log.i(TAG, "Google Play Games está instalado após a tentativa de instalação.");
                    sendPGSInstallReturnToJavascript(true, "installed");
                } else {
                    Log.i(TAG, "Google Play Games ainda não está instalado.");
                    sendPGSInstallReturnToJavascript(false, "notinstaled");
                }
            } else {
                Log.i(TAG, "pgsVerification FALSE");
            }
        }, 100);

    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        return switch (action) {
            case "signIn" -> {
                this.signIn();
                yield true;
            }
            case "isAuthenticated" -> {
                this.isAuthenticated();
                yield true;
            }
            case "getPlayerId" -> {
                this.getPlayerId();
                yield true;
            }
            case "saveGame" -> {
                this.saveGame(args);
                yield true;
            }
            case "loadGame" -> {
                this.loadGame(args);
                yield true;
            }
            case "playGames" -> {
                this.redirectToPlayGamesStore();
                yield true;
            }
            case "setPGSverification" -> {
                boolean enabled = (boolean) args.get(0);
                this.setPGSverification(enabled);
                yield true;
            }
            case "checkGooglePlayServicesAvailability" -> {
                this.checkGooglePlayServicesAvailability();
                yield true;
            }
            case "checkPGSVerification" -> {
                this.checkPGSVerification();
                yield true;
            }
            case "checkPGSVerificationWithoutPrefs" -> {
                this.checkPGSVerificationWithoutPrefs();
                yield true;
            }
            default -> false;
        };
    }

    private void checkGooglePlayServicesAvailability() {
        int googleApiAvailabilityCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(cordova.getActivity());

        if (googleApiAvailabilityCode == ConnectionResult.SUCCESS) {
            Log.d(TAG, "GooglePlayServices available");
            sendGSAvailableToJavascript(true);
        } else {
            Log.w(TAG, "GooglePlayServices not available. Error code: " + googleApiAvailabilityCode);
            sendGSAvailableToJavascript(false);
            sendErrorToJavascript("GPG_initialize_GoogleServicesNotAvailable", "GPG_initialize_GoogleServicesNotAvailable", new Exception("GooglePlayServices not available"));
        }
    }

    private void checkPGSVerificationWithoutPrefs() {
        new Handler(Looper.getMainLooper()).postDelayed(this::isPlayGamesInstalled, 500);
    }

    private void checkPGSVerification() {
        cordova.getActivity().runOnUiThread(() -> {
            SharedPreferences preferences = cordova.getActivity().getSharedPreferences("GamePrefs", Context.MODE_PRIVATE);
            pgsVerification = preferences.getBoolean("PGS_VERIFICATION", true);
            Log.i(TAG, "PGS_VERIFICATION: " + pgsVerification);
        });
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (pgsVerification) {
                Log.i(TAG, "pgsVerification TRUE");
                sendPGSVerificationPrefToJavascript(true);
                isPlayGamesInstalled();
            } else {
                Log.i(TAG, "pgsVerification FALSE");
                sendPGSVerificationPrefToJavascript(false);
            }
        }, 100);
    }


    public boolean isPlayGamesInstalled() {
        Context context = cordova.getActivity();
        if (context == null) {
            Log.i(TAG, "Context is null, cannot check if Play Games is installed");
            sendPGSAvailableToJavascript(false);
            sendErrorToJavascript("GPG_initialize_ContextNull", "Context is null", new Exception("Context is null"));
            return false;
        }

        try {
            context.getPackageManager().getPackageInfo("com.google.android.play.games", 0);
            Log.d(TAG, "Google Play Games is installed");
            sendPGSAvailableToJavascript(true);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Log.i(TAG, "Google Play Games not installed");
            sendPGSAvailableToJavascript(false);
            sendErrorToJavascript("GPG_initialize_PlayGamesNotAvailable", "Google Play Games not available", e);
            return false;
        }
    }

    public void setPGSverification(boolean enabled) {
        cordova.getActivity().runOnUiThread(() -> {
            SharedPreferences preferences = cordova.getActivity().getSharedPreferences("GamePrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("PGS_VERIFICATION", enabled);
            boolean commitSuccess = editor.commit();
            Log.i(TAG, "Commit success: " + commitSuccess);
        });
    }

    public void redirectToPlayGamesStore() {
        cordova.getThreadPool().execute(() -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.play.games"));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                cordova.getActivity().startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.play.games"));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                cordova.getActivity().startActivity(intent);
            }
        });
    }

    private void isAuthenticated() {
        cordova.getActivity().runOnUiThread(() -> {
            GamesSignInClient gamesSignInClient = PlayGames.getGamesSignInClient(cordova.getActivity());
            gamesSignInClient.isAuthenticated().addOnCompleteListener(new AuthenticationCompleteListener());
        });
    }

    private class AuthenticationCompleteListener implements OnCompleteListener<AuthenticationResult> {
        @Override
        public void onComplete(@NonNull Task<AuthenticationResult> isAuthenticatedTask) {
            if (isAuthenticatedTask.isSuccessful()) {
                AuthenticationResult authResult = isAuthenticatedTask.getResult();
                boolean isAuthenticated = authResult.isAuthenticated();
                sendIsAuthenticatedToJavascript(isAuthenticated);
            } else {
                // Tratamento de erro diretamente aqui
                try {
                    Exception exception = isAuthenticatedTask.getException();
                    if (exception instanceof ApiException apiException) {
                        int statusCode = apiException.getStatusCode();
                        String errorMessage = getErrorMessageForStatusCode(statusCode, "authentication");
                        sendErrorToJavascript("GPG_isAuthenticated", null , new Exception(errorMessage));
                        Log.i(TAG, "PlayGamesSdk initialization failed", exception);
                    } else {
                        sendErrorToJavascript("GPG_isAuthenticated", null , new Exception("An unexpected error occurred during authentication"));
                    }
                } catch (Exception e) {
                    // Falha ao enviar erro para o JavaScript
                }
                AuthenticationResult authResult = isAuthenticatedTask.getResult();
                boolean isAuthenticated = authResult.isAuthenticated();
                sendIsAuthenticatedToJavascript(isAuthenticated);
            }
        }
    }

    private void signIn() {
        cordova.getActivity().runOnUiThread(() -> {
            GamesSignInClient gamesSignInClient = PlayGames.getGamesSignInClient(cordova.getActivity());
            gamesSignInClient.signIn().addOnCompleteListener(new SignInCompleteListener());
        });
    }

    private class SignInCompleteListener implements OnCompleteListener<AuthenticationResult> {
        @Override
        public void onComplete(@NonNull Task<AuthenticationResult> signInTask) {
            if (signInTask.isSuccessful()) {
                cordova.getActivity().runOnUiThread(() -> {
                    GamesSignInClient gamesSignInClient = PlayGames.getGamesSignInClient(cordova.getActivity());
                    gamesSignInClient.isAuthenticated().addOnCompleteListener(new SignInAuthenticationCompleteListener());
                });
            } else {
                try {
                    Exception exception = signInTask.getException();
                    if (exception instanceof ApiException apiException) {
                        int statusCode = apiException.getStatusCode();
                        String errorMessage = getErrorMessageForStatusCode(statusCode, "signingIn");
                        sendErrorToJavascript("GPG_signIn", null , new Exception(errorMessage));
                    } else {
                        sendErrorToJavascript("GPG_signIn", null , new Exception("An unexpected error occurred during sign-in"));
                    }
                } catch (Exception e) {
                    // Falha ao enviar erro para o JavaScript
                }
                    sendSignInResultToJavascript(signInTask.isSuccessful());
            }
        }
    }

    private class SignInAuthenticationCompleteListener implements OnCompleteListener<AuthenticationResult> {
        @Override
        public void onComplete(@NonNull Task<AuthenticationResult> isAuthenticatedTask) {
            if (isAuthenticatedTask.isSuccessful()) {
                AuthenticationResult authResult = isAuthenticatedTask.getResult();
                boolean isAuthenticated = authResult.isAuthenticated();
                sendSignInResultToJavascript(isAuthenticated);
            } else {
                try {
                    Exception exception = isAuthenticatedTask.getException();
                    if (exception instanceof ApiException apiException) {
                        int statusCode = apiException.getStatusCode();
                        String errorMessage = getErrorMessageForStatusCode(statusCode, "authentication");
                        sendErrorToJavascript("GPG_signIn", null , new Exception(errorMessage));
                        Log.i(TAG, "PlayGamesSdk initialization failed", exception);
                    } else {
                        sendErrorToJavascript("GPG_signIn", null , new Exception("An unexpected error occurred during authentication"));
                    }
                } catch (Exception e) {
                    // Falha ao enviar erro para o JavaScript
                }
                sendSignInResultToJavascript(false);
            }
        }
    }

    private void getPlayerId() {
        cordova.getThreadPool().execute(() -> PlayGames.getPlayersClient(cordova.getActivity()).getCurrentPlayer()
                .addOnCompleteListener(new PlayerIdCompleteListener()));
    }

    private class PlayerIdCompleteListener implements OnCompleteListener<Player> {
        @Override
        public void onComplete(@NonNull Task<Player> playerTask) {
            if (playerTask.isSuccessful()) {
                String playerId = playerTask.getResult().getPlayerId();
                String displayName = playerTask.getResult().getDisplayName();
                boolean result = true;
                try {
                    JSONObject playerData = new JSONObject();
                    playerData.put("playerId", playerId);
                    playerData.put("displayName", displayName);
                    playerData.put("result", result);
                    sendPlayerIdRetrievedToJavascript(playerId, displayName, result);
                } catch (JSONException e) {
                    sendErrorToJavascript("GPG_getPlayerId", null , e);
                }
            } else {
                String playerId = "NotValid";
                String displayName = "NotValid";
                boolean result = false;
                sendPlayerIdRetrievedToJavascript(playerId, displayName, result);
                try {
                    Exception exception = playerTask.getException();
                    if (exception instanceof ApiException apiException) {
                        int statusCode = apiException.getStatusCode();
                        String errorMessage = getErrorMessageForStatusCode(statusCode, "gettingPlayerId");
                        sendErrorToJavascript("GPG_getPlayerId", null , new Exception(errorMessage));
                    } else {
                        sendErrorToJavascript("GPG_getPlayerId", null , new Exception("An unexpected error occurred while retrieving player ID"));
                    }
                } catch (Exception e) {
                    // Falha ao enviar erro para o JavaScript
                }
            }
        }
    }

    private void saveGame(JSONArray args) throws JSONException {
        String snapshotName = args.getString(0);
        String data = args.getString(1);
        String description = args.getString(2);
        long timestamp = args.getLong(3);

        cordova.getThreadPool().execute(() -> {
            SnapshotsClient snapshotsClient = PlayGames.getSnapshotsClient(cordova.getActivity());
            snapshotsClient.open(snapshotName, true)
                    .continueWithTask(new SnapshotSaveOpenTask(data, description, timestamp, snapshotsClient))
                    .addOnCompleteListener(new SnapshotSaveCompleteListener(snapshotName));
        });
    }

    private record SnapshotSaveOpenTask(String data, String description, long timestamp,SnapshotsClient snapshotsClient)
            implements Continuation<SnapshotsClient.DataOrConflict<Snapshot>, Task<SnapshotMetadata>> {

        @Override
        public Task<SnapshotMetadata> then(@NonNull Task<SnapshotsClient.DataOrConflict<Snapshot>> task) throws Exception {
            if (!task.isSuccessful()) {
                throw Objects.requireNonNull(task.getException());
            }

            Snapshot snapshot = task.getResult().getData();
            if (snapshot == null) {
                throw new Exception("Snapshot data is null");
            }

            snapshot.getSnapshotContents().writeBytes(data.getBytes());
            SnapshotMetadataChange metadataChange = new SnapshotMetadataChange.Builder()
                    .fromMetadata(snapshot.getMetadata())
                    .setDescription(description)
                    .setPlayedTimeMillis(timestamp)
                    .build();

            return snapshotsClient.commitAndClose(snapshot, metadataChange);
        }
    }

    private class SnapshotSaveCompleteListener implements OnCompleteListener<SnapshotMetadata> {
        private final String snapshotName;

        SnapshotSaveCompleteListener(String snapshotName) {
            this.snapshotName = snapshotName;
        }

        @Override
        public void onComplete(@NonNull Task<SnapshotMetadata> task) {
            if (task.isSuccessful()) {
                sendSaveGameCompleteToJavascript(true, snapshotName);
            } else {
                try {
                    Exception exception = task.getException();
                    if (exception instanceof ApiException apiException) {
                        int statusCode = apiException.getStatusCode();
                        String errorMessage = getErrorMessageForStatusCode(statusCode, "savingGame");
                        sendErrorToJavascript("GPG_saveGame", null , new Exception(errorMessage));
                    } else {
                        sendErrorToJavascript("GPG_saveGame", null , new Exception("An unexpected error occurred while saving game"));
                    }
                } catch (Exception e) {
                    // Falha ao enviar erro para o JavaScript
                }
                sendSaveGameCompleteToJavascript(false, snapshotName);
            }
        }
    }

    private void loadGame(JSONArray args) throws JSONException {
        String snapshotName = args.getString(0);
        cordova.getThreadPool().execute(() -> {
            SnapshotsClient snapshotsClient = PlayGames.getSnapshotsClient(cordova.getActivity());
            snapshotsClient.open(snapshotName, true, SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED)
                    .continueWithTask(new SnapshotLoadOpenTask(snapshotName, snapshotsClient))
                    .addOnCompleteListener(new SnapshotLoadCompleteListener(snapshotName));
        });
    }

    private record SnapshotLoadOpenTask(String snapshotName, SnapshotsClient snapshotsClient)
            implements Continuation<SnapshotsClient.DataOrConflict<Snapshot>, Task<String>> {

        @Override
        public Task<String> then(@NonNull Task<SnapshotsClient.DataOrConflict<Snapshot>> task) throws Exception {
            if (!task.isSuccessful()) {
                throw Objects.requireNonNull(task.getException());
            }
            Snapshot snapshot = task.getResult().getData();
            if (snapshot == null) {
                throw new Exception("Snapshot is null");
            } else {
                byte[] data = snapshot.getSnapshotContents().readFully();
                String resultData = new String(data);
                return Tasks.forResult(resultData);
            }
        }
    }

    private class SnapshotLoadCompleteListener implements OnCompleteListener<String> {
        private final String snapshotName;

        public SnapshotLoadCompleteListener(String snapshotName) {
            this.snapshotName = snapshotName;
        }

        @Override
        public void onComplete(@NonNull Task<String> task) {
            if (task.isSuccessful()) {
                String resultData = task.getResult();
                sendLoadGameCompleteToJavascript(true, snapshotName, resultData);
            } else {
                try {
                    Exception exception = task.getException();
                    if (exception instanceof ApiException apiException) {
                        int statusCode = apiException.getStatusCode();
                        String errorMessage = getErrorMessageForStatusCode(statusCode, "loadingGame");
                        sendErrorToJavascript("GPG_loadGame", null , new Exception(errorMessage));
                    } else {
                        sendErrorToJavascript("GPG_loadGame", null , new Exception("An unexpected error occurred while loading game"));
                    }
                } catch (Exception e) {
                    // Falha ao enviar erro para o JavaScript
                }
                sendLoadGameCompleteToJavascript(false, snapshotName, null);
            }
        }
    }

    private String getErrorMessageForStatusCode(int statusCode, String context) {
        return switch (statusCode) {
            case GamesActivityResultCodes.RESULT_SIGN_IN_FAILED -> "Sign-in failed";
            case CommonStatusCodes.SIGN_IN_REQUIRED -> "Sign-in required";
            case CommonStatusCodes.NETWORK_ERROR -> "Network error";
            case CommonStatusCodes.TIMEOUT -> "Timeout";
            case CommonStatusCodes.API_NOT_CONNECTED -> "API not connected";
            default -> "An unknown error occurred during " + context;
        };
    }

    private void sendErrorToJavascript(String methodName, String initializeError, @NonNull Exception exception) {
        try {
            JSONObject error = new JSONObject();
            error.put("message", methodName + ": " + exception.getMessage());
            error.put("code", methodName);
            if (initializeError != null) {
                error.put("initializeError", initializeError);
            } else {
                error.put("initializeError", false);
            }
            emitWindowEvent("GPG_pluginError", error);
            Log.w(TAG,methodName, exception);
        } catch (JSONException e) {
            Log.w(TAG,methodName + ": Failed to create error message", e);
        }
    }

    private void sendSaveGameCompleteToJavascript(boolean success, String snapshotName) {
        try {
            JSONObject eventData = new JSONObject();
            eventData.put("success", success);
            eventData.put("snapshotName", snapshotName);
            emitWindowEvent("GPG_saveGameComplete", eventData);
        } catch (JSONException e) {
            Log.w(TAG,"saveGame: Failed to create event message", e);
        }
    }

    private void sendLoadGameCompleteToJavascript(boolean success, String snapshotName, String savedGame) {
        try {
            JSONObject eventData = new JSONObject();
            eventData.put("success", success);
            eventData.put("snapshotName", snapshotName);
            eventData.put("savedGame", savedGame);
            emitWindowEvent("GPG_loadGameComplete", eventData);
        } catch (JSONException e) {
            Log.w(TAG,"loadGame: Failed to create event message", e);
        }
    }

    private void sendIsAuthenticatedToJavascript(boolean isAuthenticated) {
        try {
            JSONObject eventData = new JSONObject();
            eventData.put("isAuthenticated", isAuthenticated);
            emitWindowEvent("GPG_isAuthenticated", eventData);
        } catch (JSONException e) {
            Log.w(TAG,"isAuthenticated: Failed to create event message", e);
        }
    }

    private void sendGSAvailableToJavascript(boolean isGSAvailable) {
        try {
            JSONObject eventData = new JSONObject();
            eventData.put("isAvailable", isGSAvailable);
            emitWindowEvent("GPG_isGSAvailable", eventData);
        } catch (JSONException e) {
            Log.w(TAG,"isAvailable: Failed to create event message", e);
        }
    }

    private void sendPGSAvailableToJavascript(boolean isPGSAvailable) {
        try {
            JSONObject eventData = new JSONObject();
            eventData.put("isAvailable", isPGSAvailable);
            emitWindowEvent("GPG_isPGSAvailable", eventData);
        } catch (JSONException e) {
            Log.w(TAG,"isAvailable: Failed to create event message", e);
        }
    }

    private void sendPGSInstallReturnToJavascript(boolean PGSInstallReturn, String result) {
        try {
            JSONObject eventData = new JSONObject();
            eventData.put("PGSInstallReturn", PGSInstallReturn);
            eventData.put("Result", result);
            emitWindowEvent("GPG_PGSInstallReturn", eventData);
        } catch (JSONException e) {
            Log.w(TAG,"PGSInstallReturn: Failed to create event message", e);
        }
    }

    private void sendPGSVerificationPrefToJavascript(boolean PGSPref) {
        try {
            JSONObject eventData = new JSONObject();
            eventData.put("PGSPref", PGSPref);
            emitWindowEvent("GPG_PGSPref", eventData);
        } catch (JSONException e) {
            Log.w(TAG,"PGSPref: Failed to create event message", e);
        }
    }

    private void sendSignInResultToJavascript(boolean result) {
        try {
            JSONObject eventData = new JSONObject();
            eventData.put("signInResult", result);
            emitWindowEvent("GPG_signInResult", eventData);
        } catch (JSONException e) {
            Log.w(TAG,"isSignedIn: Failed to create event message", e);
        }
    }

    private void sendPlayerIdRetrievedToJavascript(String playerId, String displayName, boolean result) {
        try {
            JSONObject eventData = new JSONObject();
            eventData.put("playerId", playerId);
            eventData.put("displayName", displayName);
            eventData.put("playerIdResult", result);
            emitWindowEvent("GPG_playerIdRetrieved", eventData);
        } catch (JSONException e) {
            Log.w(TAG,"getPlayerId: Failed to create event message", e);
        }
    }

    private void emitWindowEvent(final String event, final JSONObject data) {
        final CordovaWebView view = this.webView;
        this.cordova.getActivity().runOnUiThread(() -> view.loadUrl(String.format("javascript:cordova.fireWindowEvent('%s', %s);", event, data.toString())));
    }
}
