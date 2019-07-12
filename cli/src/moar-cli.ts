import chalk, { Chalk } from 'chalk';
import { StatusCommand } from './StatusCommand';

const commandLineArgs = require('command-line-args');
const commandLineUsage = require('command-line-usage');

run();

async function run() {
  const theme = {
    aheadChalk: chalk.green,
    behindChalk: chalk.red,
    uncommitedChalk: chalk.blue,
    unmergedChalk: chalk.blue,
    signChalk: chalk.magenta
  };

  const mainOptions = commandLineArgs(
    [{ name: 'command', defaultOption: true }],
    { stopAtFirstUnknown: true }
  );
  const argv = mainOptions._unknown || [];

  const errors: string[] = [];

  const command = mainOptions.command;
  if (command === undefined || command === 'help') {
    showHelp();
  } else if (command === 'status') {
    if (!process.env.MOAR_MODULE_DIR) {
      errors.push('MOAR_MODULE_DIR must be defined');
    } else {
      await new StatusCommand(theme).run(errors);
    }
  } else {
    errors.push(`Invalid Command: ${command}`);
  }

  if (errors.length) {
    showHelp();
    console.log();
    for(const error of errors) {
      console.log(chalk.red(`  * ${error}`));
    }
    console.log();
  }
}

/**
 * Show Help
 */
function showHelp() {
  const commandLineUsage = require('command-line-usage');
  const sections = [
    {
      header: 'Moar CLI',
      content: 'A tool for managing more then one GIT module.'
    },
    {
      header: 'Synopsis',
      content: '$ moar <options> <command>'
    },
    {
      header: 'Commands',
      content: [
        { name: 'help', summary: 'Display help' },
        { name: 'status', summary: 'Show status for all modules' },
        { name: 'describe', summary: 'Describe the current module' }
      ]
    }
  ];
  const usage = commandLineUsage(sections);
  console.log(usage);
}
