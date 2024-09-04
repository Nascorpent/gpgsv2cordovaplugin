package com.nascorpent.cordovaplugingpgsv2;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log; 
import androidx.annotation.NonNull;

import com.google.android.gms.games.PlayGamesSdk;
import com.google.android.gms.games.PlayGames;
import com.google.android.gms.games.GamesSignInClient;
import com.google.android.gms.games.SnapshotsClient;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotMetadata;
import com.google.android.gms.games.snapshot.SnapshotMetadataChange;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

public class cordovaplugingpgsv2 extends CordovaPlugin {

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

        switch (action){
            case "signIn":
                this.signIn(callbackContext);
                return true;
            case "saveGame":
                this.saveGame(args, callbackContext);
                return true;
            case "loadGame":
                this.loadGame(args, callbackContext);
                return true;
            default:
                Log.w(TAG, "Unknown action: " + action);
                return false;
        }
    }

    private void signIn(CallbackContext callbackContext) {
        Log.d(TAG, "signIn: Starting interactive sign-in");

        GamesSignInClient gamesSignInClient = PlayGames.getGamesSignInClient(cordova.getActivity());
        gamesSignInClient.signIn().addOnCompleteListener(signInTask -> {
            if (signInTask.isSuccessful()) {
                Log.d(TAG, "signIn: Sign-in successful, getting current player ID");
                PlayGames.getPlayersClient(cordova.getActivity()).getCurrentPlayer()
                        .addOnCompleteListener(playerTask -> {
                            if (playerTask.isSuccessful()) {
                                String playerId = playerTask.getResult().getPlayerId();
                                Log.d(TAG, "signIn: Success, playerId = " + playerId);
                                callbackContext.success(playerId);
                            } else {
                                Log.e(TAG, "signIn: Failed to get player ID", playerTask.getException());
                                callbackContext.error("signIn:failed to get player ID");
                            }
                        });
            } else {
                Log.e(TAG, "signIn: Sign-in failed", signInTask.getException());
                callbackContext.error("signIn:failed");
            }
        });
    }

    private void saveGame(JSONArray args, CallbackContext callbackContext) throws JSONException {
        String snapshotName = args.getString(0);
        String data = args.getString(1);
        String coverImage = args.getString(2);
        String description = args.getString(3);
        long timestamp = args.getLong(4);

        Log.d(TAG, "saveGame: Saving game with snapshotName = " + snapshotName);

        SnapshotsClient snapshotsClient = PlayGames.getSnapshotsClient(cordova.getActivity());

        snapshotsClient.open(snapshotName, true, SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED)
                .continueWithTask(new Continuation<SnapshotsClient.DataOrConflict<Snapshot>, Task<Void>>() {
                    @Override
                    public Task<Void> then(@NonNull Task<SnapshotsClient.DataOrConflict<Snapshot>> task) throws Exception {
                        if (!task.isSuccessful()) {
                            Log.e(TAG, "saveGame: Failed to open snapshot", task.getException());
                            throw task.getException();
                        }

                        SnapshotsClient.DataOrConflict<Snapshot> result = task.getResult();
                        Snapshot snapshot = result.getData();

                        if (snapshot == null) {
                            Log.e(TAG, "saveGame: Snapshot data is null");
                            throw new Exception("Snapshot data is null");
                        }

                        snapshot.getSnapshotContents().writeBytes(data.getBytes());
                        Log.d(TAG, "saveGame: Snapshot data written");

                        SnapshotMetadataChange.Builder metadataChangeBuilder = new SnapshotMetadataChange.Builder()
                                .fromMetadata(snapshot.getMetadata())
                                .setDescription(description)
                                .setPlayedTimeMillis(timestamp);

                        if (!coverImage.isEmpty()) {
                            try {
                                byte[] imageBytes = Base64.decode(coverImage, Base64.DEFAULT);
                                metadataChangeBuilder.setCoverImage(BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length));
                                Log.d(TAG, "saveGame: Cover image set");
                            } catch (IllegalArgumentException e) {
                                Log.e(TAG, "saveGame: Failed to decode cover image", e);
                                throw new RuntimeException("Failed to decode cover image", e);
                            }
                        }

                        SnapshotMetadataChange metadataChange = metadataChangeBuilder.build();
                        return snapshotsClient.commitAndClose(snapshot, metadataChange).continueWithTask(new Continuation<SnapshotMetadata, Task<Void>>() {
                            @Override
                            public Task<Void> then(@NonNull Task<SnapshotMetadata> task) throws Exception {
                                if (!task.isSuccessful()) {
                                    Log.e(TAG, "saveGame: Failed to commit snapshot", task.getException());
                                    throw task.getException();
                                }
                                Log.d(TAG, "saveGame: Game saved successfully");
                                return null;
                            }
                        });
                    }
                })
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "saveGame: Game saved successfully");
                            callbackContext.success();
                        } else {
                            Exception e = task.getException();
                            String errorMessage = "saveGame:failed - " + e.getMessage();
                            Log.e(TAG, errorMessage, e);
                            callbackContext.error(errorMessage);
                        }
                    }
                });
    }

    private void loadGame(JSONArray args, CallbackContext callbackContext) throws JSONException {
        String snapshotName = args.getString(0);

        Log.d(TAG, "loadGame: Loading game with snapshotName = " + snapshotName);

        SnapshotsClient snapshotsClient = PlayGames.getSnapshotsClient(cordova.getActivity());

        snapshotsClient.open(snapshotName, false, 0)
                .continueWithTask(new Continuation<SnapshotsClient.DataOrConflict<Snapshot>, Task<String>>() {
                    @Override
                    public Task<String> then(@NonNull Task<SnapshotsClient.DataOrConflict<Snapshot>> task) throws Exception {
                        if (!task.isSuccessful()) {
                            Log.e(TAG, "loadGame: Failed to open snapshot", task.getException());
                            throw task.getException();
                        }

                        SnapshotsClient.DataOrConflict<Snapshot> result = task.getResult();
                        Snapshot snapshot = result.getData();

                        if (snapshot == null || snapshot.getSnapshotContents() == null) {
                            Log.e(TAG, "loadGame: Snapshot is null");
                            throw new Exception("Snapshot is null");
                        }

                        byte[] data = snapshot.getSnapshotContents().readFully();
                        String resultData = new String(data);
                        Log.d(TAG, "loadGame: Game data loaded successfully");
                        return Tasks.forResult(resultData);
                    }
                })
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (task.isSuccessful()) {
                            String resultData = task.getResult();
                            callbackContext.success(resultData);
                        } else {
                            Exception e = task.getException();
                            String errorMessage = "loadGame:failed - " + e.getMessage();
                            Log.e(TAG, errorMessage, e);
                            callbackContext.error(errorMessage);
                        }
                    }
                });
    }
}
