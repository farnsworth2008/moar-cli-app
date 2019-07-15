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
  prepareError?: any;
  headDate = '';
  current = '';

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
    try {
      await this.doPrepare();
    } catch (e) {
      this.prepareError = e;
    }
  }

  private async doPrepare() {
    this.gitModule.silent(true);
    await this.init();
    await this.prepareStatus();
    await this.prepareTracking();
    await this.prepareMasterToDevelop();

    if (this.tracking !== 'origin/master') {
      await this.prepareDevelopToMaster();
      await this.prepareDevelopToTracking();
    }

    const result = await this.gitModule.show(['--date=iso', '--name-only']);
    const lines = result.split('\n');
    for (const line of lines) {
      if (line.startsWith('Date: ')) {
        this.headDate = line.replace(/^Date: /, '').trim();
      }
    }

    const headShowRelative = await this.showRelative('HEAD');
    const trackingShowRelative = await this.showRelative(this.tracking);
    const developShowRelative = await this.showRelative('origin/develop');
    const masterShowRelative = await this.showRelative('origin/master');

    const headRelative = this.parseAuthorAndRelative(headShowRelative);
    this.headAuthor = headRelative.author;
    this.headRelative = headRelative.relative;

    const trackingRelative = this.parseAuthorAndRelative(trackingShowRelative);
    this.trackingAuthor = trackingRelative.author;
    this.trackingRelative = trackingRelative.relative;

    const developRelative = this.parseAuthorAndRelative(developShowRelative);
    this.developAuthor = developRelative.author;
    this.developRelative = developRelative.relative;

    const masterRelative = this.parseAuthorAndRelative(masterShowRelative);
    this.masterAuthor = masterRelative.author;
    this.masterRelative = masterRelative.relative;

    this.goodHead = headShowRelative.good;
    this.goodTracking = trackingShowRelative.good;
    this.goodMaster = masterShowRelative.good;
    this.goodDevelop = developShowRelative.good;

    const branchSummary = await this.gitModule.branch(['-a', '--no-merged']);
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

  private async prepareDevelopToTracking() {
    try {
      const developToTracking = await this.gitModule.log({
        symmetric: false,
        from: 'origin/develop',
        to: this.tracking ? this.tracking : 'HEAD'
      });
      this.developToTracking = developToTracking ? developToTracking.total : 0;
    } catch (e) {}
  }

  private async prepareDevelopToMaster() {
    try {
      const developToMaster = await this.gitModule.log({
        symmetric: false,
        from: 'origin/develop',
        to: 'origin/master'
      });
      this.developToMaster = developToMaster ? developToMaster.total : 0;
    } catch (e) {}
  }

  private async prepareMasterToDevelop() {
    try {
      const masterToDevelop = await this.gitModule.log({
        symmetric: false,
        from: 'origin/master',
        to: 'origin/develop'
      });
      this.masterToDevelop = masterToDevelop ? masterToDevelop.total : 0;
    } catch (e) {}
  }

  private async prepareTracking() {
    this.tracking = this.status ? this.status.tracking : undefined;
    try {
      const trackingToDevelop = await this.gitModule.log({
        symmetric: false,
        from: this.tracking ? this.tracking : 'HEAD',
        to: 'origin/develop'
      });
      this.trackingToDevelop = trackingToDevelop ? trackingToDevelop.total : 0;
    } catch (e) {}
  }

  private async prepareStatus() {
    try {
      this.status = await this.gitModule.status();
      this.current = this.status.current.replace(/.*\//, '');
    } catch (e) {}
  }

  private async init() {
    if (ModuleDir.domain == undefined) {
      try {
        const result = await this.gitModule.raw(['config', 'user.email']);
        ModuleDir.domain = result
          .replace(/.*\@/, '')
          .trim()
          .toLowerCase();
      } catch (e) {}
    }
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
        const shortendAuthor = author.replace(/[\.-\@\+].*/, '');
        if (author !== shortendAuthor) {
          author = shortendAuthor + '...';
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

  private async showRelative(
    id?: string
  ): Promise<{ good?: boolean; result?: string }> {
    if (id === undefined) {
      return { good: undefined, result: undefined };
    }
    try {
      const result = await this.gitModule.show([
        id,
        '--show-signature',
        '--name-only',
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
    if (this.prepareError) {
      result.pushText('ERROR', chalk.redBright);
    }
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
      .pushText(this.current, textualChalk)
      .pushText(this.sign(this.goodHead), this.theme.signChalk)
      .pushText(' [', textualChalk)
      .pushText(this.name, textualChalk)
      .pushText(']', textualChalk)
      .push('▶', this.uncommited, this.theme.uncommitedChalk)
      .push('▲', this.ahead, this.theme.aheadChalk)
      .push('▼', this.behind, this.theme.behindChalk);
  }

  pushTrackingArea(indicator: Indicator, textualChalk?: Chalk) {
    return indicator
      .pushText(
        this.trackingLabel === this.current ? '' : this.trackingLabel,
        textualChalk
      )
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
