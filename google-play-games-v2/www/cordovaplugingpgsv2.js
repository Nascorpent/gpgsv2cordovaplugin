 
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

//  window.addEventListener('GPG_pluginError', handlePluginEvent);
//  window.addEventListener('GPG_signInResult', handlePluginEvent);
//  window.addEventListener('GPG_saveGameComplete', handlePluginEvent);
//  window.addEventListener('GPG_loadGameComplete', handlePluginEvent);
//  window.addEventListener('GPG_isAuthenticated', handlePluginEvent);
//  window.addEventListener('GPG_playerIdRetrieved', handlePluginEvent);

//  function handlePluginEvent(event) {
//    if (event.type === 'GPG_pluginError') {
//    } else if (event.type === 'GPG_signInResult') {
//    } else if (event.type === 'GPG_saveGameComplete') {
//    } else if (event.type === 'GPG_loadGameComplete') {
//    } else if (event.type === 'GPG_isAuthenticated') {
//    } else if (event.type === 'GPG_playerIdRetrieved') {
//    }
//  }
