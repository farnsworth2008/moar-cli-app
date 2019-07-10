import program = require('commander');
import _package = require('../package.json');
import { StatusCommand } from './StatusCommand';

const commands = {
  status: new StatusCommand()
};

program
  .version(_package.version)
  .option('-m, --module-dir [dir]', 'Module Directory');

program.command('status').action(commands.status.handler);

let found = false;
const args: string[] = process.argv;
for (let i = 2; i < args.length; i++) {
  const arg = args[i];
  if (arg.startsWith('-') && arg !== '-V' && arg !== '--version') {
    i++;
  } else {
    found = true;
  }
}

if (!found) {
  program.outputHelp();
} else {
  program.parse(args);
}