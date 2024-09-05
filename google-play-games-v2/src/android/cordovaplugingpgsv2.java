package com.nascorpent;

import android.util.Log;

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

    private static final String EVENT_PLUGIN_ERROR = "GPG_pluginError";
    private static final String EVENT_PLUGIN_SUCCESS = "GPG_pluginSuccess";
    private CordovaWebView cordovaWebView;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        cordovaWebView = webView;
        super.initialize(cordova, webView);
        // Inicializando o PlayGamesSdk
        PlayGamesSdk.initialize(cordova.getActivity());
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        // Ação sendo executada
        switch (action) {
            case "signIn":
                this.signIn(callbackContext);
                return true;
            case "getPlayerId":
                this.getPlayerId(callbackContext);
                return true;
            case "saveGame":
                this.saveGame(args, callbackContext);
                return true;
            case "loadGame":
                this.loadGame(args, callbackContext);
                return true;
            default:
                // Ação desconhecida
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
                        // Usuário já autenticado
                        callbackContext.success("1");
                        sendSuccessToJavascript(callbackContext, "signIn", "User already authenticated");
                    } else {
                        // Usuário não autenticado, iniciando o login interativo
                        gamesSignInClient.signIn().addOnCompleteListener(signInTask -> {
                            if (signInTask.isSuccessful()) {
                                callbackContext.success("2");
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
                        String displayName = playerTask.getResult().getDisplayName();
                        // ID do jogador obtido com sucesso
                        try {
                            JSONObject playerData = new JSONObject();
                            playerData.put("playerId", playerId);
                            playerData.put("displayName", displayName);
                            callbackContext.success(playerData);
                            sendSuccessToJavascript(callbackContext, "getPlayerId", "Player ID retrieved successfully");
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
                        errorMessage = "Sign-in failed";
                        break;
                    case CommonStatusCodes.SIGN_IN_REQUIRED:
                        errorMessage = "Sign-in required";
                        break;
                    default:
                        errorMessage = "An unknown error occurred during sign-in";
                        break;
                }
                sendErrorToJavascript(callbackContext, "getSignInError", new Exception(errorMessage));

            } else {
                sendErrorToJavascript(callbackContext, "getSignInError", new Exception("An unexpected error occurred during sign-in"));
            }
        } catch (Exception e) {
            // Falha ao enviar erro para o JavaScript
        }
    }

    private void saveGame(JSONArray args, CallbackContext callbackContext) throws JSONException {
        String snapshotName = args.getString(0);
        String data = args.getString(1);
        String description = args.getString(2);
        long timestamp = args.getLong(3);

        cordova.getActivity().runOnUiThread(() -> {
            // Salvando o jogo com o nome do snapshot
            SnapshotsClient snapshotsClient = PlayGames.getSnapshotsClient(cordova.getActivity());

            snapshotsClient.open(snapshotName, true)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) {
                            // Falha ao abrir o snapshot
                            throw task.getException();
                        }

                        Snapshot snapshot = task.getResult().getData();
                        if (snapshot == null) {
                            // Dados do snapshot são nulos
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
                            // Jogo salvo com sucesso
                            callbackContext.success();
                            sendSuccessToJavascript(callbackContext, "saveGame", "Game saved successfully");
                        } else {
                            sendErrorToJavascript(callbackContext, "saveGame", task.getException());
                        }
                    });
        });
    }

    private void loadGame(JSONArray args, CallbackContext callbackContext) throws JSONException {
        String snapshotName = args.getString(0);

        cordova.getActivity().runOnUiThread(() -> {
            // Carregando o jogo com o nome do snapshot
            SnapshotsClient snapshotsClient = PlayGames.getSnapshotsClient(cordova.getActivity());

            snapshotsClient.open(snapshotName, true, SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) {
                            // Falha ao abrir o snapshot
                            throw task.getException();
                        }

                        Snapshot snapshot = task.getResult().getData();
                        if (snapshot == null || snapshot.getSnapshotContents() == null) {
                            // Snapshot é nulo
                            throw new Exception("Snapshot is null");
                        }

                        byte[] data = snapshot.getSnapshotContents().readFully();
                        String resultData = new String(data);
                        // Dados do jogo carregados com sucesso
                        return Tasks.forResult(resultData);
                    })
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String resultData = task.getResult();
                            callbackContext.success(resultData);
                            sendSuccessToJavascript(callbackContext, "loadGame", "Game loaded successfully");
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
            emitWindowEvent(EVENT_PLUGIN_ERROR, error); // Envia o evento de erro
//            callbackContext.error(error); // Mantém o callbackContext.error para compatibilidade
        } catch (JSONException e) {
            // Falha ao criar objeto JSON para erro
            callbackContext.error(methodName + ": Failed to create error message");
        }
    }

    private void sendSuccessToJavascript(CallbackContext callbackContext, String methodName, String message) {
        try {
            JSONObject success = new JSONObject();
            success.put("message", methodName + ": " + message);
            emitWindowEvent(EVENT_PLUGIN_SUCCESS, success); // Envia o evento de sucesso
//            callbackContext.success(success);
        } catch (JSONException e) {
            // Falha ao criar objeto JSON para sucesso
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
