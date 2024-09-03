cordova.define("cordova-plugin-google-play-games-v2.google-play-games-v2", function(require, exports, module) {
  var exec = require('cordova/exec');

  // Faz login silencioso no Play Games Services.
  exports.signInSilently = function () {
    return new Promise(function (resolve, reject) {
      exec(resolve, reject, 'GooglePlayGamesV2', 'signInSilently', []);
    });
  };

  // Faz login interativo no Play Games Services.
  exports.signIn = function () {
    return new Promise(function (resolve, reject) {
      exec(resolve, reject, 'GooglePlayGamesV2', 'signIn', []);
    });
  };

  // Faz logout do Play Games Services.
  exports.signOut = function () {
    return new Promise(function (resolve, reject) {
      exec(resolve, reject, 'GooglePlayGamesV2', 'signOut', []);
    });
  };

  // Salva o jogo no Play Games Services.
  exports.saveGame = function (snapshotName, data, coverImage, description, timestamp) {
    return new Promise(function (resolve, reject) {
      exec(resolve, reject, 'GooglePlayGamesV2', 'saveGame', [snapshotName, data, coverImage, description, timestamp]);
    });
  };

  // Carrega o jogo do Play Games Services.
  exports.loadGame = function (snapshotName) {
    return new Promise(function (resolve, reject) {
      exec(resolve, reject, 'GooglePlayGamesV2', 'loadGame', [snapshotName]);
    });
  };
});