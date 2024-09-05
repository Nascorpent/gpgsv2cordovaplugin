package com.nascorpent;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log; 

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

    private static final String EVENT_PLUGIN_ERROR = "pluginError";
    private static final String EVENT_PLUGIN_SUCCESS = "pluginSuccess";
    private CordovaWebView cordovaWebView;

    private static final String TAG = "cordovaplugingpgsv2"; // Definindo a TAG para logs

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        cordovaWebView = webView;
        super.initialize(cordova, webView);
        Log.d(TAG, "Initializing PlayGamesSdk");
        PlayGamesSdk.initialize(cordova.getActivity());
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        Log.d(TAG, "execute: action = " + action);

        switch (action) {
            case "signIn":
                this.signIn(callbackContext);
                return true;
            case "saveGame":
                this.saveGame(args, callbackContext);
                return true;
            case "loadGame":
                this.loadGame(args,callbackContext);
                return true;
            default:
                Log.w(TAG, "Unknown action: " + action);
                return false;
        }
    }

    private void signIn(final CallbackContext callbackContext) {
        cordova.getActivity().runOnUiThread(() -> {
            GamesSignInClient gamesSignInClient = PlayGames.getGamesSignInClient(cordova.getActivity());

            gamesSignInClient.isAuthenticated().addOnCompleteListener(isAuthenticatedTask -> {
                if (isAuthenticatedTask.isSuccessful()) {
                    boolean isAuthenticated = isAuthenticatedTask.getResult().isAuthenticated();

                    if (isAuthenticated) {
                        Log.d(TAG, "login: User is already authenticated, getting player ID");
                        getPlayerId(callbackContext);
                        sendSuccessToJavascript(callbackContext, "signIn", "Sign-in successful");
                    } else {
                        Log.d(TAG, "login: User is not authenticated, starting interactive sign-in");
                        gamesSignInClient.signIn().addOnCompleteListener(signInTask -> {if (signInTask.isSuccessful()) {
                                Log.d(TAG, "login: Sign-in successful, getting player ID");
                                getPlayerId(callbackContext);
                                sendSuccessToJavascript(callbackContext, "signIn", "Sign-in successful");
                            } else {
                                getSignInError(callbackContext, signInTask.getException());
                            }
                        });
                    }
                } else {
                    sendErrorToJavascript(callbackContext, "signIn", isAuthenticatedTask.getException());
                }
            });
        });
    }

    private void getPlayerId(CallbackContext callbackContext) {
        PlayGames.getPlayersClient(cordova.getActivity()).getCurrentPlayer()
                .addOnCompleteListener(playerTask -> {
                    if (playerTask.isSuccessful()) {
                        String playerId = playerTask.getResult().getPlayerId();
                        Log.d(TAG, "login: Success, playerId = " + playerId);
                        try {
                            JSONObject result = new JSONObject();
                            result.put("id", playerId);
                            callbackContext.success(result);sendSuccessToJavascript(callbackContext, "getPlayerId", "Player ID retrieved successfully");
                        } catch (JSONException e) {
                            sendErrorToJavascript(callbackContext, "getPlayerId", e);
                        }
                    } else {
                        sendErrorToJavascript(callbackContext, "getPlayerId", playerTask.getException());
                    }
                });
    }

    private void getSignInError(CallbackContext callbackContext, Exception exception) {
        try {
            if (exception instanceof ApiException) {
                ApiException apiException = (ApiException) exception;
                int statusCode = apiException.getStatusCode();
                String errorMessage = "";

                switch (statusCode) {
                    case GamesActivityResultCodes.RESULT_SIGN_IN_FAILED:
                        errorMessage = "login: Sign-in failed";
                        break;
                    case CommonStatusCodes.SIGN_IN_REQUIRED:
                        errorMessage = "login: Sign-in required";
                        break;
                    default:
                        errorMessage = "login: An unknown error occurred during sign-in";
                        break;
                }
                sendErrorToJavascript(callbackContext, "getSignInError", new Exception(errorMessage));

            } else {
                sendErrorToJavascript(callbackContext, "getSignInError", new Exception("login: An unexpected error occurred during sign-in"));
            }
        } catch (Exception e) {
            Log.e(TAG, "getSignInError: Error sending error to JavaScript", e);
        }
    }

    private void saveGame(JSONArray args, CallbackContext callbackContext) throws JSONException {
        String snapshotName = args.getString(0);
        String data = args.getString(1);
        String description = args.getString(2);
        long timestamp = args.getLong(3);

        cordova.getActivity().runOnUiThread(() -> {
            Log.d(TAG, "saveGame: Saving game with snapshotName = " + snapshotName);

            SnapshotsClient snapshotsClient = PlayGames.getSnapshotsClient(cordova.getActivity());

            snapshotsClient.open(snapshotName, true)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) {
                            Log.e(TAG, "saveGame: Failed to open snapshot", task.getException());
                            throw task.getException();
                        }

                        Snapshot snapshot = task.getResult().getData();
                        if (snapshot == null) {
                            Log.e(TAG, "saveGame: Snapshot data is null");
                            throw new Exception("Snapshot data is null");
                        }

                        snapshot.getSnapshotContents().writeBytes(data.getBytes());
                        Log.d(TAG, "saveGame: Snapshot data written");

                        snapshot.getSnapshotContents().writeBytes(data.getBytes());
                        Log.d(TAG, "saveGame: Snapshot data written");

                        SnapshotMetadataChange metadataChange = new SnapshotMetadataChange.Builder()
                                .fromMetadata(snapshot.getMetadata())
                                .setDescription(description)
                                .setPlayedTimeMillis(timestamp)
                                .build(); // Removido o código relacionado à coverImage

                        return snapshotsClient.commitAndClose(snapshot, metadataChange);
                    })
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "saveGame: Game saved successfully");
                            callbackContext.success();
                            sendSuccessToJavascript(callbackContext, "saveGame", "Game saved successfully"); // Mensagem de sucesso
                        } else {
                            sendErrorToJavascript(callbackContext, "saveGame", task.getException());
                        }
                    });
        });
    }

    private void loadGame(JSONArray args, CallbackContext callbackContext) throws JSONException {
        String snapshotName = args.getString(0);

        cordova.getActivity().runOnUiThread(() -> {
            Log.d(TAG, "loadGame: Loading game with snapshotName = " + snapshotName);

            SnapshotsClient snapshotsClient = PlayGames.getSnapshotsClient(cordova.getActivity());

            snapshotsClient.open(snapshotName, true, SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) {
                            Log.e(TAG, "loadGame: Failed to open snapshot", task.getException());
                            throw task.getException();
                        }

                        Snapshot snapshot = task.getResult().getData();
                        if (snapshot == null || snapshot.getSnapshotContents() == null) {
                            Log.e(TAG, "loadGame: Snapshot is null");
                            throw new Exception("Snapshot is null");
                        }

                        byte[] data = snapshot.getSnapshotContents().readFully();
                        String resultData = new String(data);
                        Log.d(TAG, "loadGame: Game data loaded successfully");
                        return Tasks.forResult(resultData);
                    })
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String resultData = task.getResult();
                            callbackContext.success(resultData);
                            sendSuccessToJavascript(callbackContext, "loadGame", "Game loaded successfully"); // Mensagem de sucesso
                        } else {
                            sendErrorToJavascript(callbackContext, "loadGame", task.getException());
                        }
                    });
        });
    }

    private void sendErrorToJavascript(CallbackContext callbackContext, String methodName, Exception exception) {
        try {
            JSONObject error = new JSONObject();
            error.put("message", methodName + ": " + exception.getMessage());
            emitWindowEvent(EVENT_PLUGIN_ERROR, error); // Envia o evento
            callbackContext.error(error); // Mantém o callbackContext.error para compatibilidade
        } catch (JSONException e) {
            Log.e(TAG, "sendErrorToJavascript: Failed to create JSON object", e);
            callbackContext.error(methodName + ": Failed to create error message");
        }
    }

    private void sendSuccessToJavascript(CallbackContext callbackContext, String methodName, String message) {
        try {
            JSONObject success = new JSONObject();
            success.put("message", methodName + ": " + message);
            emitWindowEvent(EVENT_PLUGIN_SUCCESS, success);
            callbackContext.success(success);
        } catch (JSONException e) {
            Log.e(TAG, "sendSuccessToJavascript: Failed to create JSON object", e);
            callbackContext.error(methodName + ": Failed to create success message");
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
