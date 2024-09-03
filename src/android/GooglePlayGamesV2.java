package com.nascorpent.googleplaygamesv2plugin;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.games.PlayGames;
import com.google.android.gms.games.GamesSignInClient;
import com.google.android.gms.games.PlayGamesSdk;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;

import com.google.android.gms.games.SnapshotsClient;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotMetadata;
import com.google.android.gms.games.snapshot.SnapshotMetadataChange;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.io.IOException; // Importação necessária para IOException

import android.graphics.BitmapFactory;
import android.util.Base64;
import androidx.annotation.NonNull;


public class GooglePlayGamesV2 extends CordovaPlugin {

    private boolean playGamesInitialized = false;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView appView) {
        super.initialize(cordova, appView);
        if (!playGamesInitialized) {
            PlayGamesSdk.initialize(cordova.getActivity());
            playGamesInitialized = true;
        }
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        switch (action){
            case "signInSilently":
                this.signInSilently(callbackContext);
                return true;
            case "signIn":
                this.signIn(callbackContext);
                return true;
            case "signOut":
                this.signOut(callbackContext);
                return true;
            case "saveGame":
                this.saveGame(args, callbackContext);
                return true;
            case "loadGame":
                this.loadGame(args, callbackContext);
                return true;
            default:
                return false;
        }
    }

    private void signInSilently(CallbackContext callbackContext) {
        GamesSignInClient gamesSignInClient = PlayGames.getGamesSignInClient(cordova.getActivity());

        gamesSignInClient.isAuthenticated().addOnCompleteListener(isAuthenticatedTask -> {
            if (isAuthenticatedTask.isSuccessful()) {
                boolean isAuthenticated = isAuthenticatedTask.getResult().isAuthenticated();

                if (isAuthenticated) {
                    PlayGames.getPlayersClient(cordova.getActivity()).getCurrentPlayer()
                            .addOnCompleteListener(playerTask -> {
                                if (playerTask.isSuccessful()) {
                                    String playerId = playerTask.getResult().getPlayerId();
                                    callbackContext.success(playerId);
                                } else {
                                    callbackContext.error("signInSilently:failed to get player ID");
                                }
                            });
                } else {
                    callbackContext.error("signInSilently:failed to sign in silently");
                }
            } else {
                callbackContext.error("signInSilently:failed");
            }
        });
    }

    private void signIn(CallbackContext callbackContext) {
        GamesSignInClient gamesSignInClient = PlayGames.getGamesSignInClient(cordova.getActivity());
        gamesSignInClient.signIn().addOnCompleteListener(signInTask -> {
            if (signInTask.isSuccessful()) {
                PlayGames.getPlayersClient(cordova.getActivity()).getCurrentPlayer()
                        .addOnCompleteListener(playerTask -> {
                            if (playerTask.isSuccessful()) {
                                String playerId = playerTask.getResult().getPlayerId();
                                callbackContext.success(playerId);
                            } else {
                                callbackContext.error("signIn:failed to get player ID");
                            }
                        });
            } else {
                callbackContext.error("signIn:failed");
            }
        });
    }

    private void signOut(CallbackContext callbackContext) {
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(cordova.getActivity(),
                GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN);

        googleSignInClient.signOut()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callbackContext.success();
                    } else {
                        callbackContext.error("signOut:failed");
                    }
                });
    }

    private void saveGame(JSONArray args, CallbackContext callbackContext) throws JSONException {
        String snapshotName = args.getString(0);
        String data = args.getString(1);
        String coverImage = args.getString(2);
        String description = args.getString(3);
        long timestamp = args.getLong(4);

        SnapshotsClient snapshotsClient = PlayGames.getSnapshotsClient(cordova.getActivity());

        snapshotsClient.open(snapshotName, true, SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED)
                .continueWithTask(new Continuation<SnapshotsClient.DataOrConflict<Snapshot>, Task<SnapshotMetadata>>() {
                    @Override
                    public Task<SnapshotMetadata> then(@NonNull Task<SnapshotsClient.DataOrConflict<Snapshot>> task) throws Exception {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }

                        SnapshotsClient.DataOrConflict<Snapshot> result = task.getResult();
                        Snapshot snapshot = result.getData();

                        snapshot.getSnapshotContents().writeBytes(data.getBytes());

                        SnapshotMetadataChange.Builder metadataChangeBuilder = new SnapshotMetadataChange.Builder()
                                .fromMetadata(snapshot.getMetadata())
                                .setDescription(description)
                                .setPlayedTimeMillis(timestamp);

                        if (!coverImage.isEmpty()) {
                            try {
                                byte[] imageBytes = Base64.decode(coverImage, Base64.DEFAULT);
                                metadataChangeBuilder.setCoverImage(BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length));
                            } catch (IllegalArgumentException e) {
                                throw new RuntimeException("Failed to decode cover image", e);
                            }
                        }

                        SnapshotMetadataChange metadataChange = metadataChangeBuilder.build();
                        return snapshotsClient.commitAndClose(snapshot, metadataChange);
                    }
                })
                .addOnCompleteListener(new OnCompleteListener<SnapshotMetadata>() {
                    @Override
                    public void onComplete(@NonNull Task<SnapshotMetadata> task) {
                        if (task.isSuccessful()) {
                            callbackContext.success();
                        } else {
                            Exception e = task.getException();
                            String errorMessage = "saveGame:failed - " + e.getMessage();
                            callbackContext.error(errorMessage);
                        }
                    }
                });
    }


    private void loadGame(JSONArray args, CallbackContext callbackContext) throws JSONException {
        String snapshotName = args.getString(0);

        SnapshotsClient snapshotsClient = PlayGames.getSnapshotsClient(cordova.getActivity());

        snapshotsClient.open(snapshotName, false, 0)
                .continueWithTask(new Continuation<SnapshotsClient.DataOrConflict<Snapshot>, Task<Snapshot>>() {
                    @Override
                    public Task<Snapshot> then(@NonNull Task<SnapshotsClient.DataOrConflict<Snapshot>> task) throws Exception {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }

                        Snapshot snapshot = task.getResult().getData();
                        return Tasks.forResult(snapshot);
                    }
                })
                .addOnCompleteListener(new OnCompleteListener<Snapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<Snapshot> task) {
                        if (task.isSuccessful()) {
                            try {
                                Snapshot snapshot = task.getResult();
                                byte[] data;
                                try {
                                    data = snapshot.getSnapshotContents().readFully();
                                } catch (IOException e) {
                                    callbackContext.error("loadGame:failed to read snapshot data - " + e.getMessage());
                                    return;
                                }

                                JSONObject result = new JSONObject();
                                result.put("data", new String(data));
                                result.put("coverImage", snapshot.getMetadata().getCoverImageUri() != null ? snapshot.getMetadata().getCoverImageUri().toString() : "");
                                result.put("description", snapshot.getMetadata().getDescription());
                                result.put("timestamp", snapshot.getMetadata().getLastModifiedTimestamp());

                                callbackContext.success(result);
                            } catch (JSONException e) {
                                callbackContext.error("loadGame:failed to parse data - " + e.getMessage());
                            }
                        } else {
                            callbackContext.error("loadGame:failed");
                        }
                    }
                });
    }
}
