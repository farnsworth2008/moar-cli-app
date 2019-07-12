import { Chalk } from 'chalk';

export interface WorkspaceTheme {
  behindChalk: Chalk;
  uncommitedChalk: Chalk;
  unmergedChalk: Chalk;
  aheadChalk: Chalk;
  signChalk: Chalk;
}
