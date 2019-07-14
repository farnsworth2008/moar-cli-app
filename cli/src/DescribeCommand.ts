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
    const gitModule = await simpleGit.default(this.moduleDir);
    const dir = this.moduleDir.substring(this.moduleDir.lastIndexOf('/') + 1);
    const moduleDir = new ModuleDir(
      this.moduleDir,
      dir,
      gitModule,
      this.theme
    );
    await moduleDir.prepare();
    const len = moduleDir.getStatusLabel().content.length;
    const config = { color: true, size: len };
    const label = moduleDir.getStatusLabel({config});
    console.log(label.content);
    console.log();
    console.log(`Module Status (HEAD): ${moduleDir.pushHeadArea(new Indicator(config)).content}`);
    if(moduleDir.trackingLabel !== '') {
      console.log(`Tracking Status.....: ${moduleDir.pushTrackingArea(new Indicator(config)).content}`);
    }
    console.log(`Develop Status......: ${moduleDir.pushDevelopArea(new Indicator(config)).content}`);
    console.log(`Master Status.......: ${moduleDir.pushMasterArea(new Indicator(config)).content}`);
    console.log(`Unmerged Branches...: ${moduleDir.pushUnmergedArea(new Indicator(config)).content.trim()}`);
    console.log();
  }
}
