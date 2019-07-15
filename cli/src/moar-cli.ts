import chalk, { Chalk } from 'chalk';
import { StatusCommand } from './StatusCommand';
import { DescribeCommand } from './DescribeCommand';

import * as modulePackage from './package.json';

const commandLineArgs = require('command-line-args');
const commandLineUsage = require('command-line-usage');

const aliases: any = {
  s: 'status',
  d: 'describe',
  h: 'help'
};

run();

async function run() {
  const theme = {
    aheadChalk: chalk.green.bold,
    behindChalk: chalk.red.bold,
    uncommitedChalk: chalk.blue.bold,
    unmergedChalk: chalk.blue.bold,
    signChalk: chalk.magenta.bold
  };

  const mainOptions = commandLineArgs(
    [{ name: 'command', defaultOption: true }, { name: 'version', alias: 'v' }],
    { stopAtFirstUnknown: true }
  );

  const errors: string[] = [];

  if (!process.env.MOAR_MODULE_DIR) {
    errors.push('MOAR_MODULE_DIR must be defined');
  } else {
    let command: string = mainOptions.command;
    let alias = aliases[command];
    if (alias) {
      command = alias;
    }
    if (mainOptions.version !== undefined) {
      console.log(modulePackage.version);
      return;
    } else if (command === undefined || command === 'help') {
      showHelp();
    } else if (command === 'status') {
      await new StatusCommand(theme).run(errors);
    } else if (command === 'describe') {
      await new DescribeCommand(theme).run(errors);
    } else {
      errors.push(`Invalid Command: ${command}`);
    }
  }

  if (errors.length) {
    showHelp();
    console.log();
    for (const error of errors) {
      console.log(chalk.red(`  * ${error}`));
    }
    console.log();
  }
}

/**
 * Show Help
 */
function showHelp() {
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
        { name: '--version', alias: '-v', summary: 'Display help' },
        { name: 'help', alias: 'h', summary: 'Display help' },
        { name: 'status', alias: 's', summary: 'Show status for all modules' },
        { name: 'describe', alias: 'd', summary: 'Describe the current module' }
      ]
    }
  ];
  const usage = commandLineUsage(sections);
  console.log(usage);
}
