module.exports = function(ctx) {
  var fs = require('fs');
  var path = require('path');

  // Caminho do config.xml
  var configXmlPath = path.join(ctx.opts.projectRoot, 'config.xml');
  var xml2js = require('xml2js');
  var parser = new xml2js.Parser();

  // Lendo o config.xml para obter o valor da variável
  fs.readFile(configXmlPath, 'utf8', function(err, data) {
      if (err) {
          throw new Error('Erro ao ler config.xml: ' + err);
      }

      // Parse do XML para capturar o valor da variável
      parser.parseString(data, function(err, result) {
          if (err) {
              throw new Error('Erro ao fazer parse do config.xml: ' + err);
          }

          // Acessando a variável gpgs_project_id dentro do plugin
          var plugins = result.widget.plugin;
          var gpgsProjectId = null;

          plugins.forEach(function(plugin) {
              if (plugin.$.name === 'cordovaplugingpgsv2') {
                  plugin.variable.forEach(function(variable) {
                      if (variable.$.name === 'gpgs_project_id') {
                          gpgsProjectId = variable.$.value;
                      }
                  });
              }
          });

          if (!gpgsProjectId) {
              throw new Error('gpgs_project_id não encontrado no config.xml');
          }

          console.log('gpgs_project_id encontrado: ' + gpgsProjectId);

          // Caminho do games-ids.xml
          var gamesIdsPath = path.join(ctx.opts.projectRoot, 'platforms/android/app/src/main/res/values/games-ids.xml');

          // Substituir o valor no arquivo games-ids.xml
          fs.readFile(gamesIdsPath, 'utf8', function(err, fileData) {
              if (err) {
                  throw new Error('Erro ao ler games-ids.xml: ' + err);
              }

              var result = fileData.replace(/YOUR_PROJECT_ID/g, gpgsProjectId);

              fs.writeFile(gamesIdsPath, result, 'utf8', function(err) {
                  if (err) {
                      throw new Error('Erro ao escrever no games-ids.xml: ' + err);
                  }

                  console.log('games-ids.xml atualizado com sucesso!');
              });
          });
      });
  });
};
