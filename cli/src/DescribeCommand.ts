import program from 'commander';
import * as fs from 'fs';
import * as simpleGit from 'simple-git/promise';
import * as path from 'path';
import { ModuleDir } from './WorkspaceMemberDir';
import { WorkspaceTheme } from './WorkspaceTheme';

/**
 * A command to show status for the Workspace.
 */
export class StatusCommand {
  private moduleDir: string = <string>process.env.MOAR_MODULE_DIR;

  constructor(private theme: WorkspaceTheme) {}

  /**
   * Calculate the length of the longest name.
   */
  private calcMaxNameLen(moduleDirs: ModuleDir[]) {
    let maxLen = 0;
    for (const moduleDir of moduleDirs) {
      let len = moduleDir.nameAreaLen;
      maxLen = len > maxLen ? len : maxLen;
    }
    return maxLen;
  }

  /**
   * Calculate the length of the longest tracking label.
   */
  private calcMaxTrackingLen(moduleDirs: ModuleDir[], maxNameLen: number) {
    let maxLen = 0;
    for (const moduleDir of moduleDirs) {
      moduleDir.getStatusLabel(maxNameLen);
      let len = moduleDir.trackingAreaLen;
      maxLen = len > maxLen ? len : maxLen;
    }
    return maxLen;
  }

  /**
   * Calculate the length of the longest status label.
   */
  private calcMaxDevelopLen(moduleDirs: ModuleDir[], maxNameLen: number, maxTrackingLen: number) {
    let maxLen = 0;
    for (const moduleDir of moduleDirs) {
      const trackingPushArrowSize = 1 + maxNameLen - moduleDir.nameAreaLen;
      moduleDir.getStatusLabel(maxNameLen, trackingPushArrowSize);
      const developPushArrowSize = 1 + maxTrackingLen - moduleDir.trackingAreaLen;
      moduleDir.getStatusLabel(trackingPushArrowSize, developPushArrowSize);
      const len = moduleDir.developAreaLen;
      maxLen = len > maxLen ? len : maxLen;
    }
    return maxLen;
  }

  /**
   * Calculate the length of the longest status label.
   */
  private calcMaxMasterLen(moduleDirs: ModuleDir[], maxNameLen: number, maxTrackingLen: number) {
    let maxLen = 0;
    for (const moduleDir of moduleDirs) {
      const trackingPushArrowSize = 1 + maxNameLen - moduleDir.nameAreaLen;
      moduleDir.getStatusLabel(maxNameLen, trackingPushArrowSize);
      const developPushArrowSize = 1 + maxTrackingLen - moduleDir.trackingAreaLen;
      moduleDir.getStatusLabel(trackingPushArrowSize, developPushArrowSize);
      const len = 1 + moduleDir.masterAreaLen;
      maxLen = len > maxLen ? len : maxLen;
    }
    return maxLen;
  }

  /**
   * Calculate the length of the longest status label.
   */
  private calcMaxUnmergedLen(moduleDirs: ModuleDir[], maxNameLen: number, maxTrackingLen: number) {
    let maxLen = 0;
    for (const moduleDir of moduleDirs) {
      const len = moduleDir.unmergedAreaLen;
      maxLen = len > maxLen ? len : maxLen;
    }
    return maxLen;
  }

  /**
   * Displays the Status for the Workspace
   */
  async run(errors: string[]): Promise<void> {
    if (!fs.existsSync(this.moduleDir + '/.git')) {
      errors.push(
        'The status command must be run from the root of a GIT Module directory.'
      );
      return;
    }
    const workspaceDir = this.moduleDir.substring(
      0,
      this.moduleDir.lastIndexOf('/')
    );
    const moduleDirs: ModuleDir[] = [];
    const workspaceDirs = fs.readdirSync(workspaceDir);
    for (const workspaceModuleDir of workspaceDirs) {
      if (
        fs.existsSync(workspaceDir + '/' + workspaceModuleDir + '/.git/config')
      ) {
        const gitModule = await simpleGit.default(workspaceDir + '/' + workspaceModuleDir);
        const moduleDir = new ModuleDir(
          this.moduleDir,
          workspaceModuleDir,
          gitModule,
          this.theme
        );
        await moduleDir.prepare();
        moduleDirs.push(moduleDir);
      }
    }
    let maxNameLen = this.calcMaxNameLen(moduleDirs);
    let maxTrackingLen = this.calcMaxTrackingLen(moduleDirs, maxNameLen);
    let maxDevelopLen = this.calcMaxDevelopLen(moduleDirs, maxNameLen, maxTrackingLen);
    let maxMasterLen = this.calcMaxMasterLen(moduleDirs, maxNameLen, maxTrackingLen);
    let maxUnmergedLen = this.calcMaxUnmergedLen(moduleDirs, maxNameLen, maxTrackingLen);

    const labelConfig = { color: true, size: maxDevelopLen };
    for (const memberDir of moduleDirs) {
      const nameLen = memberDir.nameAreaLen;
      const trackingLen = memberDir.trackingAreaLen;
      const developLen = memberDir.developAreaLen;
      const masterLen = memberDir.masterAreaLen;
      const unmergedLen = memberDir.unmergedAreaLen;
      const trackingPushArrowSize = 1 + maxNameLen - nameLen;
      const developPushArrowSize = 1 + maxTrackingLen - trackingLen;
      const masterPushArrowSize = 1 + maxDevelopLen - developLen;
      const unmergedPushArrowSize = (maxMasterLen - masterLen) + (maxUnmergedLen - unmergedLen);
      const label = memberDir.getStatusLabel(
        trackingPushArrowSize,
        developPushArrowSize,
        masterPushArrowSize,
        unmergedPushArrowSize,
        labelConfig
      );
      console.log(label.content());
    }
  }
}
