var exec = require('cordova/exec');

exports.signInSilently = function (success, error) {
    exec(success, error, 'GooglePlayGamesV2', 'signInSilently', []);
};

exports.signOut = function (success, error) {
    exec(success, error, 'GooglePlayGamesV2', 'signOut', []);
};

exports.saveGame = function (data, success, error) {
    exec(success, error, 'GooglePlayGamesV2', 'saveGame', [data]);
};

exports.loadGame = function (success, error) {
    exec(success, error, 'GooglePlayGamesV2', 'loadGame',[]);
};