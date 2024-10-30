 
  function callPlugin(name, params) {
    cordova.exec(
      null,
      null,
      'cordovaplugingpgsv2',
      name,
      params
    );
  }

  exports.isAuthenticated = function () {
      callPlugin('isAuthenticated', []);
  };

  exports.signIn = function () {
      callPlugin('signIn', []);
  };

  exports.getPlayerId = function () {
      callPlugin('getPlayerId', []);
  };

  exports.saveGame = function (snapshotName, data, description, timestamp) {
      callPlugin('saveGame', [snapshotName, data, description, timestamp]);
  };

  exports.loadGame = function (snapshotName) {
      callPlugin('loadGame', [snapshotName]);
  };

  
