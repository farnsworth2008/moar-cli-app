import * as fs from 'fs';
import * as simpleGit from 'simple-git/promise';
import { ModuleDir } from './ModuleDir';
import { Theme } from './Theme';
import { Command } from './Command';

/**
 * A command to show status for the Workspace.
 */
export class StatusCommand extends Command {
  constructor(theme: Theme) {
    super(theme);
  }

  private maxLen(prop: string) {
    let maxLen = 0;
    for (const moduleDir of this.moduleDirs) {
      let len = (<any>moduleDir)[prop];
      maxLen = len > maxLen ? len : maxLen;
    }
    return maxLen;
  }

  /**
   * Displays the Status for the Workspace
   */
  async run(errors: string[]): Promise<void> {
    this.checkModuleDir(errors);
    if(errors.length > 0) {
      return;
    }

    for (const workspaceModuleDir of this.workspaceDirs) {
      if (
        fs.existsSync(
          this.workspaceDir + '/' + workspaceModuleDir + '/.git/config'
        )
      ) {
        const gitModule = await simpleGit.default(
          this.workspaceDir + '/' + workspaceModuleDir
        );
        const moduleDir = new ModuleDir(
          this.moduleDir,
          workspaceModuleDir,
          gitModule,
          this.theme
        );
        await moduleDir.prepare();
        this.moduleDirs.push(moduleDir);
      }
    }
    let maxNameLen = this.maxLen('nameAreaLen');
    let maxTrackingLen = this.maxLen('trackingAreaLen');
    let maxDevelopLen = this.maxLen('developAreaLen');
    let maxMasterLen = this.maxLen('masterAreaLen');
    let maxUnmergedLen = this.maxLen('unmergedAreaLen');

    const labelConfig = { color: true, size: maxDevelopLen };
    for (const memberDir of this.moduleDirs) {
      const nameLen = memberDir.nameAreaLen;
      const trackingLen = memberDir.trackingAreaLen;
      const developLen = memberDir.developAreaLen;
      const masterLen = memberDir.masterAreaLen;
      const unmergedLen = memberDir.unmergedAreaLen;
      const trackingPushArrowSize = 1 + maxNameLen - nameLen;
      const developPushArrowSize = 1 + maxTrackingLen - trackingLen;
      const masterPushArrowSize = 1 + maxDevelopLen - developLen;
      const unmergedPushArrowSize =
        1 + (maxMasterLen - masterLen) + (maxUnmergedLen - unmergedLen);
      const label = memberDir.getStatusLabel({
        trackingPushArrowSize,
        developPushArrowSize,
        masterPushArrowSize,
        unmergedPushArrowSize,
        config: labelConfig
      });
      console.log(label.content);
    }
  }
}
