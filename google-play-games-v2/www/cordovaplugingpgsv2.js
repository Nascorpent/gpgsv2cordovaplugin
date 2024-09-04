cordova.define("cordovaplugingpgsv2.GPGSv2", function(require, exports, module) {var exec = require('cordova/exec');

  exports.signIn = function () {
    return new Promise(function (resolve, reject) {
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
});

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