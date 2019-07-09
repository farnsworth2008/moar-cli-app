import fs = require('fs');
import program = require('commander');
import _package = require('../package.json');

program
  .version(_package.version)
  .option('-m, --module-dir [dir]', 'Module Directory');

program.command('status').action(statusCommand);

let found = false;
const args: string[] = process.argv.slice(2);
for (let i = 2; i < args.length; i++) {
  const arg = args[i];
  if (arg.startsWith('-')) {
    i++;
  } else {
    found = true;
  }
}

if (!found) {
  program.help();
} else {
  program.parse(process.argv);
}

function statusCommand() {
  const moduleDir: string = program.opts().moduleDir;
  const workspaceDir = moduleDir.substring(0, moduleDir.lastIndexOf('/'));
  console.log(`status: ${workspaceDir}`);
  const dir = fs.readdirSync(workspaceDir);
  console.dir(dir);
}

