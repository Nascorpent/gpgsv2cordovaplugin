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
                String playerId = Games.getPlayersClient(cordova.getActivity()).getCurrentPlayerId();
                callbackContext.success(playerId);
            } else {
                callbackContext.error("signInSilently:failed");
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

    private void saveGame(JSONArray args, CallbackContext callbackContext) {
        // TODO: Implementar a lógica de salvar jogo aqui
        callbackContext.success(); // ou callbackContext.error("Mensagem de erro");
    }

    private void loadGame(JSONArray args, CallbackContext callbackContext) {
        // TODO: Implementar a lógica de carregar jogo aqui
        callbackContext.success(); // ou callbackContext.error("Mensagem de erro");
    }
}