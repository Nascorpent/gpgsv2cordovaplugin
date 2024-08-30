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

import android.content.Intent;

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

public class GooglePlayGamesV2 extends CordovaPlugin {

    private boolean playGamesInitialized = false;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView appView) {
        super.initialize(cordova, appView);if (!playGamesInitialized) {

            PlayGamesSdk.initialize(cordova.getActivity());
            playGamesInitialized = true;
        }
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("signInSilently")) {
            this.signInSilently(callbackContext);
            return true;
        } else if (action.equals("signIn")) {
            this.signIn(callbackContext);
            return true;
        } else if (action.equals("signOut")) {
            this.signOut(callbackContext);
            return true;
        } else if (action.equals("saveGame")) {
            this.saveGame(args, callbackContext);
            return true;
        } else if (action.equals("loadGame")) {
            this.loadGame(args, callbackContext);
            return true;
        }
        return false;
    }

    private void signInSilently(CallbackContext callbackContext) {
        GamesSignInClient gamesSignInClient = PlayGames.getGamesSignInClient(cordova.getActivity());
    
        gamesSignInClient.isAuthenticated().addOnCompleteListener(isAuthenticatedTask -> {
            boolean isAuthenticated = (isAuthenticatedTask.isSuccessful() &&
                    isAuthenticatedTask.getResult().isAuthenticated());
    
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
            } else{
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
                      } else {callbackContext.error("signIn:failed to get player ID");
                      }
                    });
          } else {
            callbackContext.error("signIn:failed");
          }
        });
      }

    private void signOut(CallbackContext callbackContext) {
        GamesSignInClient gamesSignInClient = PlayGames.getGamesSignInClient(cordova.getActivity());
        gamesSignInClient.signOut().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                callbackContext.success();
            } else {
                callbackContext.error("signOut:failed");
            }
        });
    }

    private void saveGame(JSONArray args, CallbackContext callbackContext) throws JSONException {
        String snapshotName = args.getString(0);String data = args.getString(1);
        String coverImage = args.getString(2); // Base64 da imagem da capa
        String description = args.getString(3);
        long timestamp = args.getLong(4);
    
        SnapshotsClient snapshotsClient = Games.getSnapshotsClient(cordova.getActivity());
    
        snapshotsClient.open(snapshotName, true).continueWith(new Continuation<SnapshotsClient.DataOrConflict<Snapshot>, Task<SnapshotMetadata>>() {
            @Override
            public Task<SnapshotMetadata> then(@NonNull Task<SnapshotsClient.DataOrConflict<Snapshot>> task) throws Exception {
                SnapshotsClient.DataOrConflict<Snapshot> result = task.getResult();
    
                if (result.isConflict()) {
                    // Descartar o snapshot conflitante
                    Snapshot conflictSnapshot = result.getConflict();
                    snapshotsClient.discardAndClose(conflictSnapshot);
    
                    // Abrir o snapshot novamente (sem criar um novo)
                    return snapshotsClient.open(snapshotName, false).continueWith(new Continuation<SnapshotsClient.DataOrConflict<Snapshot>, Task<SnapshotMetadata>>() {
                        @Override
                        public Task<SnapshotMetadata> then(@NonNull Task<SnapshotsClient.DataOrConflict<Snapshot>> task) throws Exception {
                            Snapshot snapshot = task.getResult().getData();
                            snapshot.getSnapshotContents().writeBytes(data.getBytes());
    
                            SnapshotMetadataChange metadataChange = new SnapshotMetadataChange.Builder()
                                    .fromMetadata(snapshot.getMetadata())
                                    .setDescription(description)
                                    .setPlayedTimeMillis(timestamp)
                                    .build();
    
                            if (!coverImage.isEmpty()) {
                                byte[] imageBytes = Base64.decode(coverImage, Base64.DEFAULT);
                                metadataChange = new SnapshotMetadataChange.Builder()
                                        .fromMetadataChange(metadataChange)
                                        .setCoverImage(BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length))
                                        .build();
                            }
    
                            return snapshotsClient.commitAndClose(snapshot, metadataChange);
                        }
                    });
                } else {
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
                                .fromMetadataChange(metadataChange)
                                .setCoverImage(BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length))
                                .build();
                    }
    
                    return snapshotsClient.commitAndClose(snapshot, metadataChange);
                }
            }
        }).addOnCompleteListener(new OnCompleteListener<SnapshotMetadata>() {
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
    
        SnapshotsClient snapshotsClient = Games.getSnapshotsClient(cordova.getActivity());
    
        snapshotsClient.open(snapshotName, false).continueWith(new Continuation<SnapshotsClient.DataOrConflict<Snapshot>, Task<byte[]>>() {
            @Override
            public Task<byte[]> then(@NonNull Task<SnapshotsClient.DataOrConflict<Snapshot>> task) throws Exception {
                Snapshot snapshot = task.getResult().getData();
                return snapshotsClient.loadSnapshotContents(snapshot);
            }
        }).addOnCompleteListener(new OnCompleteListener<byte[]>() {
            @Override
            public void onComplete(@NonNull Task<byte[]> task) {
                if (task.isSuccessful()) {
                    String data = new String(task.getResult());
                    try {
                        JSONObject json = new JSONObject();
                        json.put("data", data);
                        // Inclua outros metadados, se necessário
                        // json.put("timestamp", snapshot.getMetadata().getLastModifiedTimestamp()); 
                        callbackContext.success(json);
                    } catch (JSONException e) {
                        callbackContext.error("loadGame:failed to parse data");
                    }
                } else {
                    callbackContext.error("loadGame:failed");
                }
            }
        });
    }
}