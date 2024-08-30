var exec = require('cordova/exec');

exports.loginSilently = function (success, error) {
    exec(success, error, 'GooglePlayGamesV2', 'loginSilently', []);
};

exports.logout = function (success, error) {
    exec(success, error, 'GooglePlayGamesV2', 'logout', []);
};

exports.saveGame = function (data, success, error) {
    exec(success, error, 'GooglePlayGamesV2', 'saveGame', [data]);
};

exports.loadGame = function (success, error) {
    exec(success, error, 'GooglePlayGamesV2', 'loadGame',[]);
};