cordova.define("cordovaplugingpgsv2.cordovaplugingpgsv2", function(require, exports, module) {
  
  
  function callPlugin(name, params, onSuccess, onFailure) {
    cordova.exec(
      function callPluginSuccess(result) {
        if (typeof onSuccess === 'function') {
          onSuccess(result);
        }
      },function callPluginFailure(error) {
        if (typeof onFailure === 'function') {
          onFailure(error);
        }
      },
      'cordovaplugingpgsv2',
      name,
      params
    );
  }

  exports.signIn = function () {return new Promise(function (resolve, reject) {
      callPlugin('signIn', [], resolve, reject);
    });
  };

  exports.getPlayerId = function () {return new Promise(function (resolve, reject) {
        callPlugin('getPlayerId', [], resolve, reject);
      });
    };

  exports.saveGame = function (snapshotName, data, description, timestamp) {
    return new Promise(function (resolve, reject) {
      callPlugin('saveGame', [snapshotName, data, description, timestamp], resolve, reject);
    });
  };

  exports.loadGame = function (snapshotName) {
    return new Promise(function (resolve, reject) {
      callPlugin('loadGame', [snapshotName], resolve, reject);
    });
  };

//  window.addEventListener('GPG_pluginError', handlePluginEvent);
//  window.addEventListener('GPG_pluginSuccess', handlePluginEvent);
//  window.addEventListener('GPG_saveGameComplete', handlePluginEvent);
//  window.addEventListener('GPG_loadGameComplete', handlePluginEvent);
//  window.addEventListener('GPG_isAuthenticated', handlePluginEvent);

//  function handlePluginEvent(event) {
//    if (event.type === 'GPG_pluginError') {
//      console.error('Erro no plugin:', event.message);
//    } else if (event.type === 'GPG_pluginSuccess') {
//      console.log('Sucesso no plugin:', event.message);
//    } else if (event.type === 'GPG_saveGameComplete') {
//      console.log('Save game complete:', event.success, event.snapshotName);
//    } else if (event.type === 'GPG_loadGameComplete') {
//      console.log('Load game complete:', event.success, event.snapshotName);
//    } else if (event.type === 'GPG_isAuthenticated') {
//      console.log('Is authenticated:', event.isAuthenticated);
//    } else if (event.type === 'GPG_playerIdRetrieved') {
//        console.log('Player ID retrieved:', event.playerId, event.displayName);
//      }
//  }
});
