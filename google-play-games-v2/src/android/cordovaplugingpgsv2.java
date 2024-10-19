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

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        PlayGamesSdk.initialize(cordova.getActivity());
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
            default -> false;
        };
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
                sendErrorToJavascript("isAuthenticated", Objects.requireNonNull(isAuthenticatedTask.getException()));
                sendIsAuthenticatedToJavascript(false);
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
                // Login bem-sucedido
                sendSignInResultToJavascript(true);
            } else {
                // Tratamento de erro
                getSignInError(signInTask.getException());
                sendSignInResultToJavascript(false);
            }
        }
    }

    private void getSignInError(Exception exception) {
        try {
            if (exception instanceof ApiException apiException) {
                int statusCode = apiException.getStatusCode();
                String errorMessage = switch (statusCode) {
                    case GamesActivityResultCodes.RESULT_SIGN_IN_FAILED -> "Sign-in failed";
                    case CommonStatusCodes.SIGN_IN_REQUIRED -> "Sign-in required";
                    default -> "An unknown error occurred during sign-in";
                };
                sendErrorToJavascript("getSignInError", new Exception(errorMessage));
            } else {
                sendErrorToJavascript("getSignInError", new Exception("An unexpected error occurred during sign-in"));
            }
        } catch (Exception e) {
            // Falha ao enviar erro para o JavaScript
        }
    }

    private void getPlayerId() {
        cordova.getActivity().runOnUiThread(() -> PlayGames.getPlayersClient(cordova.getActivity()).getCurrentPlayer()
                .addOnCompleteListener(new PlayerIdCompleteListener()));
    }

    private class PlayerIdCompleteListener implements OnCompleteListener<Player> {
        @Override
        public void onComplete(@NonNull Task<Player> playerTask) {
            if (playerTask.isSuccessful()) {
                String playerId = playerTask.getResult().getPlayerId();
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
                sendErrorToJavascript("getPlayerId", Objects.requireNonNull(playerTask.getException()));
            }
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
                sendErrorToJavascript("saveGame", Objects.requireNonNull(task.getException()));
                sendSaveGameCompleteToJavascript(false, snapshotName);
            }
        }
    }

    private void loadGame(JSONArray args) throws JSONException {
        String snapshotName = args.getString(0);
        cordova.getActivity().runOnUiThread(() -> {
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
                sendErrorToJavascript("loadGame", Objects.requireNonNull(task.getException()));
                sendLoadGameCompleteToJavascript(false, snapshotName, null);
            }
        }
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
        this.cordova.getActivity().runOnUiThread(() -> view.loadUrl(String.format("javascript:cordova.fireWindowEvent('%s', %s);", event, data.toString())));
    }
}
