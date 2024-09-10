package com.nascorpent;

import android.util.Log;

import androidx.annotation.NonNull;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.games.PlayGamesSdk;
import com.google.android.gms.games.PlayGames;
import com.google.android.gms.games.GamesSignInClient;
import com.google.android.gms.games.SnapshotsClient;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotMetadataChange;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.games.GamesActivityResultCodes;
import java.lang.Runnable;

public class cordovaplugingpgsv2 extends CordovaPlugin {

    private CordovaWebView cordovaWebView;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        cordovaWebView = webView;
        super.initialize(cordova, webView);
        PlayGamesSdk.initialize(cordova.getActivity());
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        switch (action) {
            case "signIn":
                this.signIn();
                return true;
            case "isAuthenticated":
                this.isAuthenticated();
                return true;
            case "getPlayerId":
                this.getPlayerId();
                return true;
            case "saveGame":
                this.saveGame(args);
                return true;
            case "loadGame":
                this.loadGame(args);
                return true;
            default:
                return false;
        }
    }

    private void isAuthenticated() {
        cordova.getActivity().runOnUiThread(() -> {
            GamesSignInClient gamesSignInClient = PlayGames.getGamesSignInClient(cordova.getActivity());

            gamesSignInClient.isAuthenticated().addOnCompleteListener(isAuthenticatedTask -> {
                if (isAuthenticatedTask.isSuccessful()) {
                    boolean isAuthenticated = isAuthenticatedTask.getResult().isAuthenticated();
                    sendIsAuthenticatedToJavascript(isAuthenticated);
                } else {
                    sendErrorToJavascript("isAuthenticated", isAuthenticatedTask.getException());
                    sendIsAuthenticatedToJavascript(false);
                }
            });
        });
    }

    private void signIn() {
        cordova.getActivity().runOnUiThread(() -> {
        GamesSignInClient gamesSignInClient = PlayGames.getGamesSignInClient(cordova.getActivity());

        gamesSignInClient.signIn().addOnCompleteListener(signInTask -> {
            if (signInTask.isSuccessful()) {
                sendSignInResultToJavascript(true);
            } else {
                getSignInError(signInTask.getException());
                sendSignInResultToJavascript(false);
            }
        });
    });
    }

    private void getPlayerId() {
        cordova.getActivity().runOnUiThread(() -> {
            PlayGames.getPlayersClient(cordova.getActivity()).getCurrentPlayer()
                    .addOnCompleteListener(playerTask -> {
                        if (playerTask.isSuccessful()) {
                            String playerId= playerTask.getResult().getPlayerId();
                            String displayName = playerTask.getResult().getDisplayName();
                            try {
                                JSONObject playerData = new JSONObject();
                                playerData.put("playerId", playerId);
                                playerData.put("displayName", displayName);
                                sendPlayerIdRetrievedToJavascript(playerId, displayName);
                            } catch (JSONException e) {
                                sendErrorToJavascript("getPlayerId", e);
                            }
                        } else {
                            sendErrorToJavascript("getPlayerId", playerTask.getException());
                        }
                    });
        });
    }

    private void getSignInError(Exception exception) {
        try {
            if (exception instanceof ApiException) {
                ApiException apiException = (ApiException) exception;
                int statusCode = apiException.getStatusCode();
                String errorMessage;
                switch (statusCode) {
                    case GamesActivityResultCodes.RESULT_SIGN_IN_FAILED:
                        errorMessage = "Sign-in failed";
                        break;
                    case CommonStatusCodes.SIGN_IN_REQUIRED:
                        errorMessage = "Sign-in required";
                        break;
                    default:
                        errorMessage = "An unknown error occurred during sign-in";
                        break;
                }
                sendErrorToJavascript("getSignInError", new Exception(errorMessage));
            } else {
                sendErrorToJavascript("getSignInError", new Exception("An unexpected error occurred during sign-in"));
            }
        } catch (Exception e) {
            // Falha ao enviar erro para o JavaScript
        }
    }

    private void saveGame(JSONArray args) throws JSONException {
        String snapshotName = args.getString(0);
        String data = args.getString(1);
        String description = args.getString(2);
        long timestamp = args.getLong(3);

        cordova.getActivity().runOnUiThread(() -> {
            SnapshotsClient snapshotsClient = PlayGames.getSnapshotsClient(cordova.getActivity());
            snapshotsClient.open(snapshotName, true)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) {
                            throw task.getException();
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
                    })
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            sendSaveGameCompleteToJavascript(true, snapshotName);
                        } else {
                            sendErrorToJavascript("saveGame", task.getException());
                            sendSaveGameCompleteToJavascript(false, snapshotName);
                        }
                    });
        });
    }

    private void loadGame(JSONArray args) throws JSONException {
        String snapshotName = args.getString(0);
        cordova.getActivity().runOnUiThread(() -> {
            SnapshotsClient snapshotsClient = PlayGames.getSnapshotsClient(cordova.getActivity());
            snapshotsClient.open(snapshotName, true, SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }
                        Snapshot snapshot = task.getResult().getData();
                        if (snapshot == null || snapshot.getSnapshotContents() == null) {
                            throw new Exception("Snapshot is null");
                        }
                        byte[] data = snapshot.getSnapshotContents().readFully();
                        String resultData = new String(data);
                        return Tasks.forResult(resultData);
                    })
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String resultData = task.getResult();
                            sendLoadGameCompleteToJavascript(true, snapshotName, resultData);
                        } else {
                            sendErrorToJavascript("loadGame", task.getException());
                            sendLoadGameCompleteToJavascript(false, snapshotName, null);
                        }
                    });
        });
    }

    private void sendErrorToJavascript(String methodName, @NonNull Exception exception) {
        try {
            JSONObject error = new JSONObject();
            error.put("message", methodName + ": " + exception.getMessage());
            emitWindowEvent("GPG_pluginError", error);
        } catch (JSONException e) {
            Log.e("ERROR",methodName + ": Failed to create error message", e);
        }
    }

    private void sendSaveGameCompleteToJavascript(boolean success, String snapshotName) {
        try {
            JSONObject eventData = new JSONObject();
            eventData.put("success", success);
            eventData.put("snapshotName", snapshotName);
            emitWindowEvent("GPG_saveGameComplete", eventData);
        } catch (JSONException e) {
            Log.e("ERROR","saveGame: Failed to create event message", e);
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
            Log.e("ERROR","loadGame: Failed to create event message", e);
        }
    }

    private void sendIsAuthenticatedToJavascript(boolean isAuthenticated) {
        try {
            JSONObject eventData = new JSONObject();
            eventData.put("isAuthenticated", isAuthenticated);
            emitWindowEvent("GPG_isAuthenticated", eventData);
        } catch (JSONException e) {
            Log.e("ERROR","isAuthenticated: Failed to create event message", e);
        }
    }

    private void sendSignInResultToJavascript(boolean result) {
        try {
            JSONObject eventData = new JSONObject();
            eventData.put("signInResult", result);
            emitWindowEvent("GPG_signInResult", eventData);
        } catch (JSONException e) {
            Log.e("ERROR","isSignedIn: Failed to create event message", e);
        }
    }

    private void sendPlayerIdRetrievedToJavascript(String playerId, String displayName) {
        try {
            JSONObject eventData = new JSONObject();
            eventData.put("playerId", playerId);
            eventData.put("displayName", displayName);
            emitWindowEvent("GPG_playerIdRetrieved", eventData);
        } catch (JSONException e) {
            Log.e("ERROR","getPlayerId: Failed to create event message", e);
        }
    }

    private void emitWindowEvent(final String event, final JSONObject data) {
        final CordovaWebView view = this.webView;
        this.cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                view.loadUrl(String.format("javascript:cordova.fireWindowEvent('%s', %s);", event, data.toString()));
            }
        });
    }
}
