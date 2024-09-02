package com.nascorpent.googleplaygamesv2plugin;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;

import com.google.android.gms.games.Games;
import com.google.android.gms.games.PlayGames;
import com.google.android.gms.games.GamesSignInClient;
import com.google.android.gms.games.PlayGamesSdk;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;

import com.google.android.gms.games.SnapshotsClient;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotMetadata;
import com.google.android.gms.games.snapshot.SnapshotMetadataChange;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import org.json.JSONObject;
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
                    callbackContext.error("signInSilently:failed");
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
        PlayGames.signOut(cordova.getActivity()); // Correção: signOut agora é um método estático de PlayGames
        callbackContext.success(); // Chame success após signOut
    }

    private void saveGame(JSONArray args, CallbackContext callbackContext) throws JSONException {
        String snapshotName = args.getString(0);String data = args.getString(1);
        String coverImage = args.getString(2); // Base64 da imagem da capa
        String description = args.getString(3);
        long timestamp = args.getLong(4);
    
        SnapshotsClient snapshotsClient = PlayGames.getSnapshotsClient(cordova.getActivity());
    
        snapshotsClient.open(snapshotName, true, SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED)
                .continueWith(new Continuation<SnapshotsClient.DataOrConflict<Snapshot>, Task<SnapshotMetadata>>() {
                    @Override
                    public Task<SnapshotMetadata> then(@NonNull Task<SnapshotsClient.DataOrConflict<Snapshot>> task) throws Exception {
                        SnapshotsClient.DataOrConflict<Snapshot> result = task.getResult();
    
                        Snapshot snapshot = result.getData();
                        snapshot.getSnapshotContents().writeBytes(data.getBytes());
    
                        SnapshotMetadataChange metadataChange = new SnapshotMetadataChange.Builder()
                                .fromMetadata(snapshot.getMetadata())
                                .setDescription(description)
                                .setPlayedTimeMillis(timestamp)
                                .build();
    
                        if (!coverImage.isEmpty()) {
                            byte[] imageBytes = Base64.decode(coverImage, Base64.DEFAULT);
                            metadataChange = new SnapshotMetadataChange.Builder()
                                    .fromMetadata(snapshot.getMetadata())
                                    .setCoverImage(BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length))
                                    .build();
                        }
    
                        return snapshotsClient.commitAndClose(snapshot, metadataChange);
                    }
                })
                .addOnCompleteListener(new OnCompleteListener<Task<SnapshotMetadata>>() { // Alterar o tipo aqui
                    @Override
                    public void onComplete(@NonNull Task<SnapshotMetadata> task) {
                        if (task.isSuccessful()) {
                            callbackContext.success();
                        } else {
                            callbackContext.error("saveGame:failed");
                        }
                    }
                });
    }

    private void loadGame(JSONArray args, CallbackContext callbackContext) throws JSONException {
        String snapshotName = args.getString(0);
    
        SnapshotsClient snapshotsClient = PlayGames.getSnapshotsClient(cordova.getActivity());
    
        snapshotsClient.open(snapshotName, false, 0)
                .continueWith(new Continuation<SnapshotsClient.DataOrConflict<Snapshot>, Task<Snapshot>>() {
                    @Override
                    public Task<Snapshot> then(@NonNull Task<SnapshotsClient.DataOrConflict<Snapshot>> task) throws Exception {
                        Snapshot snapshot = task.getResult().getData();
                        return snapshotsClient.load(snapshot.getMetadata()); // Passar SnapshotMetadata para load
                    }
                })
                .continueWith(new Continuation<Snapshot, byte[]>() { // Adicionar continueWith para obter bytes
                    @Override
                    public byte[] then(@NonNull Task<Snapshot> task) throws Exception {
                        return task.getResult().getSnapshotContents().readFully();
                    }
                })
                .addOnCompleteListener(new OnCompleteListener<byte[]>() {
                    @Override
                    public void onComplete(@NonNull Task<byte[]> task) {
                        if (task.isSuccessful()) {
                            try {
                                byte[] data = task.getResult();
                                if (data != null) {
                                    String dataString = new String(data);
                                    JSONObject json = new JSONObject();
                                    json.put("data", dataString);
                                    callbackContext.success(json);
                                } else{
                                    callbackContext.error("loadGame:failed - no data found");
                                }
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