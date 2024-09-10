const { exec } = require('child_process');

exec('npm install xml2js', (error, stdout, stderr) => {
    if (error) {
        console.error(`Erro ao instalar xml2js: ${error.message}`);
        return;
    }
    if (stderr) {
        console.error(`Erro: ${stderr}`);
        return;
    }
    console.log(`Dependência instalada: ${stdout}`);
});
