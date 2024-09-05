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

  window.addEventListener('GPG_pluginError', handlePluginEvent);
  window.addEventListener('GPG_pluginSuccess', handlePluginEvent);

  function handlePluginEvent(event) {
    if (event.type === 'GPG_pluginError') {
      var error = event;
      console.error(error.message); // Exibe a mensagem de erro no console
      // Lógica para lidar com o erro
    } else if(event.type === 'GPG_pluginSuccess') {
      var success = event;
      console.log(success.message); // Exibe a mensagem de sucesso no console
      // Lógica para lidar com o sucesso
    }
  }

});
