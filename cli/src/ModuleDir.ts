import { default as chalk, Chalk } from 'chalk';
import * as simpleGit from 'simple-git/promise';
import { Theme } from './Theme';
import { Indicator } from './Indicator';
import { IndicatorConfig } from './IndicatorConfig';
import { StatusResult } from 'simple-git/typings/response';

export class ModuleDir {
  static domain?: string;
  readonly name: string;
  uncommited = 0;
  ahead = 0;
  behind = 0;
  tracking?: string;
  trackingToDevelop = 0;
  masterToDevelop = 0;
  developToMaster = 0;
  developToTracking = 0;
  unmergedBranchCount = 0;
  status?: StatusResult;
  goodHead?: boolean;
  goodTracking?: boolean;
  goodMaster?: boolean;
  goodDevelop?: boolean;
  trackingLabel = '';
  headAuthor = '';
  headRelative = '';
  trackingAuthor = '';
  trackingRelative = '';
  developAuthor = '';
  developRelative = '';
  masterAuthor = '';
  masterRelative = '';

  constructor(
    private workspaceModuleDir: string,
    readonly dir: string,
    readonly gitModule: simpleGit.SimpleGit,
    readonly theme: Theme
  ) {
    const workspaceModuleDirSplit = this.workspaceModuleDir.split('/');
    const name = dir
      .substring(dir.lastIndexOf('/') + 1)
      .replace(workspaceModuleDirSplit[workspaceModuleDirSplit.length - 2], '');
    this.name = name;
  }

  /**
   * Prepare the module
   */
  async prepare() {
    const gitModule = this.gitModule;
    gitModule.silent(true);
    if (ModuleDir.domain == undefined) {
      const result = await gitModule.raw(['config', 'user.email']);
      ModuleDir.domain = result
        .replace(/.*\@/, '')
        .trim()
        .toLowerCase();
    }
    this.status = await gitModule.status();
    this.tracking = this.status.tracking;
    try {
      const trackingToDevelop = await gitModule.log({
        symmetric: false,
        from: this.tracking ? this.tracking : 'HEAD',
        to: 'origin/develop'
      });
      this.trackingToDevelop = trackingToDevelop ? trackingToDevelop.total : 0;
    } catch (e) {}

    try {
      const masterToDevelop = await gitModule.log({
        symmetric: false,
        from: 'origin/master',
        to: 'origin/develop'
      });
      this.masterToDevelop = masterToDevelop ? masterToDevelop.total : 0;
    } catch (e) {}

    if (this.tracking !== 'origin/master') {
      try {
        const developToMaster = await gitModule.log({
          symmetric: false,
          from: 'origin/develop',
          to: 'origin/master'
        });
        this.developToMaster = developToMaster ? developToMaster.total : 0;
      } catch (e) {}
      try {
        const developToTracking = await gitModule.log({
          symmetric: false,
          from: 'origin/develop',
          to: this.tracking ? this.tracking : 'HEAD'
        });
        this.developToTracking = developToTracking
          ? developToTracking.total
          : 0;
      } catch (e) {}
    }

    const headShow = await this.show('HEAD');
    const trackingShow = await this.show(this.tracking);
    const masterShow = await this.show('origin/master');
    const developShow = await this.show('origin/develop');

    const headRelative = this.parseAuthorAndRelative(headShow);
    this.headAuthor = headRelative.author;
    this.headRelative = headRelative.relative;

    const trackingRelative = this.parseAuthorAndRelative(trackingShow);
    this.trackingAuthor = trackingRelative.author;
    this.trackingRelative = trackingRelative.relative;

    const developRelative = this.parseAuthorAndRelative(developShow);
    this.developAuthor = developRelative.author;
    this.developRelative = developRelative.relative;

    const masterRelative = this.parseAuthorAndRelative(masterShow);
    this.masterAuthor = masterRelative.author;
    this.masterRelative = masterRelative.relative;

    this.goodHead = headShow.good;
    this.goodTracking = trackingShow.good;
    this.goodMaster = masterShow.good;
    this.goodDevelop = developShow.good;

    const branchSummary = await gitModule.branch(['-a', '--no-merged']);
    let count = 0;
    for (const branch of branchSummary.all) {
      count += branch.startsWith('remotes/origin/') ? 1 : 0;
    }
    this.unmergedBranchCount = count;
    let status = this.status;
    this.uncommited = status ? status.files.length : 0;
    if (this.tracking) {
      this.ahead = status ? status.ahead : 0;
      this.behind = status ? status.behind : 0;
    } else {
      this.ahead = this.developToTracking;
      this.behind = this.trackingToDevelop;
      this.trackingToDevelop = 0;
      this.developToTracking = 0;
    }
    let trackingLabel = this.tracking ? this.tracking.replace(/HEAD/, '') : '';
    if (trackingLabel.match(/(develop|master)/)) {
      trackingLabel = '';
    }
    trackingLabel = trackingLabel.replace(/.*\//, '');
    this.trackingLabel = trackingLabel;
  }

  private parseAuthorAndRelative(headShow: {
    good?: boolean | undefined;
    result?: string | undefined;
  }): { author: string; relative: string } {
    let author = '';
    let relative = '';
    const lines = headShow.result ? headShow.result.split('\n') : [];
    for (const line of lines) {
      if (line.startsWith('Author:')) {
        author = line.replace(/.*</, '').replace(/>.*/, '');
        if (ModuleDir.domain) {
          if (author.toLowerCase().endsWith(ModuleDir.domain)) {
            author = author.substring(
              0,
              author.length - ModuleDir.domain.length - 1
            );
          }
        }
        break;
      }
    }
    for (const line of lines) {
      if (line.startsWith('Date:')) {
        relative = line.replace('Date: ', '').trim();
        break;
      }
    }
    return { author, relative };
  }

  get trackingAreaLen(): number {
    let len = this.trackingLabel.length;
    if (this.trackingToDevelop) {
      len += ` ▲${this.trackingToDevelop}`.length;
    }
    if (this.developToTracking) {
      len += ` ▼${this.developToTracking}`.length;
    }
    if (this.goodTracking !== true) {
      len += this.sign(this.goodTracking).length;
    }
    return len;
  }

  get nameAreaLen(): number {
    let len = this.name.length;
    if (this.uncommited) {
      len += ` ▶${this.uncommited}`.length;
    }
    if (this.ahead) {
      len += ` ▲${this.ahead}`.length;
    }
    if (this.behind) {
      len += ` ▼${this.behind}`.length;
    }
    if (this.goodHead !== true) {
      len += this.sign(this.goodHead).length;
    }
    return len;
  }

  get developAreaLen(): number {
    let len = 'develop'.length;
    if (this.masterToDevelop) {
      len += ' ▲'.length + `${this.masterToDevelop}`.length;
    }
    if (this.developToMaster) {
      len += ' ▼'.length + `${this.developToMaster}`.length;
    }
    if (this.goodDevelop !== true) {
      len += this.sign(this.goodDevelop).length;
    }
    return len;
  }

  get masterAreaLen(): number {
    let len = ' master'.length;
    len += this.sign(this.goodMaster).length;
    return len;
  }

  get unmergedAreaLen(): number {
    let len = ` ${this.unmergedBranchCount}`.length;
    return len;
  }

  private async show(id: string): Promise<{ good?: boolean; result?: string }> {
    try {
      const result = await this.gitModule.show([
        id,
        '--show-signature',
        '--date=relative'
      ]);
      if (result.indexOf('gpg: Good signature from') >= 0) {
        return { good: true, result };
      } else {
        if (result.indexOf("gpg: Can't check signature") >= 0) {
          return { good: false, result };
        }
      }
      return { good: undefined, result };
    } catch {}
    return { good: undefined, result: undefined };
  }

  /**
   * Output the status label for a workspace member directory
   *
   * The following are examples:
   * 1. `one ▶1 ▲2 ▼3 ─> XX-004 ▲5 ▼6 ─> develop ▲7 ▼8 ─> master~ <── 9`
   * 2. `two ▶10 ──────> XX-011 ───────> develop? ──────> master <── 12`
   * 3. `three ────────> XX-013~ ──────> develop ───────> master <─ ▲14`
   *
   * In the example, the `one` module shows `▶1` uncommited change, `▲2`
   * commits ahead and `▼3` behind the `XX-004`  tracking branch is
   * `(▲5 ▼6)` relative `origin/develop` with `origin/develop` `[▼8]`
   * relative to `origin/master`.  There are `9` branches with unmerged
   * changes.
   *
   * The n
   *
   * The `->` section expands as required to reach the targeted size.
   * Indicators with zero value are suppressed.
   *
   * The result is plain or formatted to the specified size with the option of
   * color coding based on a theme to help users quickly comprehend the
   * information.  Colors used by default represet **uncommited** as **blue**,
   * **ahead** using **green**, and **behind** with **red**.
   */
  getStatusLabel({
    trackingPushArrowSize,
    developPushArrowSize,
    masterPushArrowSize,
    unmergedPushArrowSize,
    config
  }: {
    trackingPushArrowSize?: number;
    developPushArrowSize?: number;
    masterPushArrowSize?: number;
    unmergedPushArrowSize?: number;
    config?: IndicatorConfig;
  } = {}) {
    const theme = this.theme;

    let textualChalk: Chalk | undefined;

    if (config) {
      textualChalk = this.workspaceModuleDir.endsWith(`/${this.dir}`)
        ? chalk.bold
        : chalk.reset;
    }

    trackingPushArrowSize = trackingPushArrowSize ? trackingPushArrowSize : 1;
    developPushArrowSize = developPushArrowSize ? developPushArrowSize : 1;
    masterPushArrowSize = masterPushArrowSize ? masterPushArrowSize : 1;
    unmergedPushArrowSize = unmergedPushArrowSize ? unmergedPushArrowSize : 1;

    const unmergedBranchCount = this.unmergedBranchCount;

    const result = this.pushHeadArea(new Indicator(config), textualChalk);
    result.pushArrowLine(trackingPushArrowSize);
    this.pushTrackingArea(result, textualChalk);
    result.pushArrowLine(developPushArrowSize);
    this.pushDevelopArea(result, textualChalk);
    result.pushArrowLine(masterPushArrowSize);
    this.pushMasterArea(result, textualChalk);
    result.pushLeftArrowLine(unmergedPushArrowSize);
    this.pushUnmergedArea(result, textualChalk);
    result.pushText(
      ` ◎ ${
        textualChalk
          ? textualChalk(this.headRelativeArea)
          : this.headRelativeArea
      }`
    );
    return result;
  }

  pushUnmergedArea(indicator: Indicator, _textualChalk?: Chalk) {
    return indicator.push(
      'ᚿ',
      this.unmergedBranchCount,
      this.theme.unmergedChalk,
      true
    );
  }

  pushMasterArea(indicator: Indicator, textualChalk?: Chalk) {
    return indicator
      .pushText('master', textualChalk)
      .pushText(this.sign(this.goodMaster), this.theme.signChalk);
  }

  pushDevelopArea(indicator: Indicator, textualChalk?: Chalk) {
    return indicator
      .pushText('develop', textualChalk)
      .pushText(this.sign(this.goodDevelop), this.theme.signChalk)
      .push('▲', this.masterToDevelop, this.theme.aheadChalk)
      .push('▼', this.developToMaster, this.theme.behindChalk);
  }

  pushHeadArea(indicator: Indicator, textualChalk?: Chalk) {
    return indicator
      .pushText(this.name, textualChalk)
      .pushText(this.sign(this.goodHead), this.theme.signChalk)
      .push('▶', this.uncommited, this.theme.uncommitedChalk)
      .push('▲', this.ahead, this.theme.aheadChalk)
      .push('▼', this.behind, this.theme.behindChalk);
  }

  pushTrackingArea(indicator: Indicator, textualChalk?: Chalk) {
    return indicator
      .pushText(this.trackingLabel, textualChalk)
      .pushText(this.sign(this.goodTracking), this.theme.signChalk)
      .push('▲', this.developToTracking, this.theme.aheadChalk)
      .push('▼', this.trackingToDevelop, this.theme.behindChalk);
  }

  private sign(good: boolean | undefined) {
    if (good === true) {
      return '';
    }
    return good === undefined ? '!' : '?';
  }

  get headRelativeArea() {
    return `${this.headRelative} by ${this.headAuthor}`;
  }

  get trackingRelativeArea() {
    return `${this.trackingRelative} by ${this.trackingAuthor}`;
  }

  get developRelativeArea() {
    return `${this.developRelative} by ${this.developAuthor}`;
  }

  get masterRelativeArea() {
    return `${this.masterRelative} by ${this.masterAuthor}`;
  }
}
