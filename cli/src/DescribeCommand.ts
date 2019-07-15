import chalk from 'chalk';
import * as simpleGit from 'simple-git/promise';

import { Theme } from './Theme';
import { Command } from './Command';
import { ModuleDir } from './ModuleDir';
import { Indicator } from './Indicator';

/**
 * A command to show a description for the module directory.
 */
export class DescribeCommand extends Command {
  constructor(theme: Theme) {
    super(theme);
  }

  async run(errors: string[]) {
    this.checkModuleDir(errors);
    if(errors.length > 0) {
      return;
    }

    const gitModule = await simpleGit.default(this.moduleDir);
    const dir = this.moduleDir.substring(this.moduleDir.lastIndexOf('/') + 1);
    const moduleDir = new ModuleDir(this.moduleDir, dir, gitModule, this.theme);
    await moduleDir.prepare();
    const len = moduleDir.getStatusLabel().content.length;
    const config = { color: true, size: len };
    const label = moduleDir.getStatusLabel({ config });
    console.log(label.content);
    console.log();
    console.log(
      chalk.bold(
        `Module Status (HEAD): ${
          moduleDir.pushHeadArea(new Indicator(config)).content
        }`
      )
    );
    console.log(`  * ${moduleDir.headRelativeArea}`);
    if (moduleDir.uncommited > 0) {
      console.log(
        `  * ${this.theme.uncommitedChalk('' + moduleDir.uncommited)} file ${
          moduleDir.uncommited === 1 ? 'change' : 'changes'
        }`
      );
    }
    this.showSignatureLine(moduleDir.goodHead);
    this.showAheadBehind({
      ahead: moduleDir.ahead,
      behind: moduleDir.behind,
      trackingLabel: moduleDir.trackingLabel
    });
    if (moduleDir.trackingLabel !== '') {
      console.log(
        chalk.bold(
          `Tracking Status.....: ${
            moduleDir.pushTrackingArea(new Indicator(config)).content
          }`
        )
      );
      console.log(`  * ${moduleDir.trackingRelativeArea}`);
    }
    this.showSignatureLine(moduleDir.goodTracking);
    this.showAheadBehind({
      ahead: moduleDir.developToTracking,
      behind: moduleDir.trackingToDevelop,
      trackingLabel: 'develop'
    });
    console.log(
      chalk.bold(
        `Develop Status......: ${
          moduleDir.pushDevelopArea(new Indicator(config)).content
        }`
      )
    );
    console.log(`  * ${moduleDir.developRelativeArea}`);
    this.showSignatureLine(moduleDir.goodDevelop);
    this.showAheadBehind({
      ahead: moduleDir.masterToDevelop,
      behind: moduleDir.developToMaster,
      trackingLabel: 'master'
    });
    console.log(
      chalk.bold(
        `Master Status.......: ${
          moduleDir.pushMasterArea(new Indicator(config)).content
        }`
      )
    );
    console.log(`  * ${moduleDir.masterRelativeArea}`);
    this.showSignatureLine(moduleDir.goodMaster);
    console.log(
      chalk.bold(
        `Unmerged Branches...: ${moduleDir
          .pushUnmergedArea(new Indicator(config))
          .content.trim()}`
      )
    );
  }

  private showAheadBehind({
    ahead,
    behind,
    trackingLabel
  }: {
    ahead: number;
    behind: number;
    trackingLabel: string;
  }) {
    if (ahead) {
      console.log(
        `  * Ahead of '${chalk.bold(trackingLabel)}' by ${this.theme.aheadChalk(
          '' + ahead
        )} commits`
      );
    }
    if (behind) {
      console.log(
        `  * Behind of '${chalk.bold(trackingLabel)}' by ${this.theme.behindChalk(
          '' + behind
        )} commits`
      );
    }
  }

  private showSignatureLine(good?: boolean) {
    if (good === undefined) {
      console.log(`  * ${this.theme.signChalk('No signature')}.`);
    } else {
      console.log(
        `  * ${
          good ? this.theme.signChalk('Good') : this.theme.signChalk('Unknown')
        } signature.`
      );
    }
  }
}
