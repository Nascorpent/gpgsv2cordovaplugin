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

  exports.saveGame = function (snapshotName, data, coverImage, description, timestamp) {
    return new Promise(function (resolve, reject) {
      callPlugin('saveGame', [snapshotName, data, coverImage, description, timestamp], resolve, reject);
    });
  };

  exports.loadGame = function (snapshotName) {
    return new Promise(function (resolve, reject) {
      callPlugin('loadGame', [snapshotName], resolve, reject);
    });
  };

  window.addEventListener(EVENT_PLUGIN_ERROR, function(event) {
    var error = event.detail;
    console.error(error.message); // Exibe a mensagem de erro no console
    // Lógica adicional paralidar com o erro, se necessário
  });

  window.addEventListener(EVENT_PLUGIN_SUCCESS, function(event) {
    var success = event.detail;
    console.log(success.message); // Exibe a mensagem de sucesso no console
    // Lógica adicional para lidar com o sucesso, se necessário
  });