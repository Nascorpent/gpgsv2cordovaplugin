var exec = require('cordova/exec');

exports.signInSilently = function (success, error) {
    exec(success, error, 'GooglePlayGamesV2', 'signInSilently', []);
};

exports.signIn = function (success, error) {
    exec(success, error, 'GooglePlayGamesV2', 'signIn', []);
  };

exports.signOut = function (success, error) {
    exec(success, error, 'GooglePlayGamesV2', 'signOut', []);
};

exports.saveGame = function (snapshotName, data, coverImage, description, timestamp, success, error) {
    exec(success, error, 'GooglePlayGamesV2', 'saveGame', [snapshotName, data, coverImage, description, timestamp]);
  };

  exports.loadGame = function (snapshotName, success, error) {
    exec(success, error, 'GooglePlayGamesV2', 'loadGame', [snapshotName]);
};