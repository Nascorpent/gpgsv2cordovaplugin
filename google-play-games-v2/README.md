*************************** Salvando Jogo *********************************

        var snapshotName = "meu_snapshot";
        var data = runtime.globalVars.ValorTextInput;
        var description= "Teste Save Game";
        var timestamp = Date.now(); // Timestamp atual


		GPGSv2.saveGame(snapshotName, data, description, timestamp)
            .then(function(result) {
                // Lidar com o sucesso
                console.log("Jogo salvo com sucesso:", result);
            })
            .catch(function(error) {
                // Lidar com o erro
                console.log("Erro ao salvar o jogo:", error);
            });

***************************************************************************
************************** Carregando Jogo ********************************

        GPGSv2.loadGame("meu_snapshot")
            .then(function (result) {
                // Lidar com o sucesso - result contém os dados do jogo
                console.log("Jogo carregado com sucesso: " + result);

            })
            .catch(function (error) {
                // Lidar com o erro
                console.log("Erro ao carregar o jogo: " + error);
            });

***************************************************************************
************************ Obter Dados Jogador ******************************


        GPGSv2.getPlayerId()
            .then(function(playerData) {
                if (playerData) {
                    console.log("ID do jogador:", playerData.playerId);
                    console.log("Nome de exibição:", playerData.displayName);
                    // ... acessar outros dados do jogador ...
            } else {
                console.log("Login bem-sucedido, mas os dados do jogador não foram retornados.");
            }
            }).catch(function(error) {
                console.error("Erro ao receber dados:", error);
            }); 

***************************************************************************
******************** Verificar estado de signIn****************************

        GPGSv2.signIn().then(function(result) {
            if (result === "1") {
                console.log("User already authenticated");
            } else if (result === "2") {
                console.log("Sign-in successful");
            } else {
                console.log("Sign-in failed or user is not authenticated.");
            }
            }).catch(function(error) {
                console.error("Error during sign-in:", error);
            });