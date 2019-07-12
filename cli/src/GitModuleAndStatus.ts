import * as simpleGit from 'simple-git/promise';

export interface GitModuleAndStatus {
  gitModule: simpleGit.SimpleGit;
  status: simpleGit.StatusResult;
}
