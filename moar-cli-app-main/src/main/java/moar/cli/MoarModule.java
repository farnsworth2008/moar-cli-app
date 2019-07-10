package moar.cli;

import static java.lang.String.format;
import static moar.sugar.MoarStringUtil.toLineList;
import static moar.sugar.Sugar.exec;
import static moar.sugar.Sugar.safely;
import static moar.sugar.Sugar.swallow;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import moar.sugar.ExecuteResult;
import moar.sugar.SafeResult;

public class MoarModule {
  private static final String GIT_LOG = "git log --reverse --no-color '--pretty=%h %aN %s' ";
  final File dir;
  private String upstreamBranch;
  private List<String> uncommitedFiles;
  private List<String> aheadCommits;
  private List<String> behindCommits;
  private List<String> aheadOriginCommits;
  private List<String> behindOriginCommits;
  private List<String> behindMasterCommits;
  private String branch;
  private int moduleNumber;

  public MoarModule(File dir, int moduleNumber) {
    this.dir = dir;
    this.moduleNumber = moduleNumber;
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof MoarModule)) {
      return false;
    }

    MoarModule otherModule = (MoarModule) other;
    return dir.equals(otherModule.dir);
  }

  public SafeResult<ExecuteResult> execCommand(String command) {
    return safely(() -> exec(command, dir));
  }

  public List<String> getAheadCommits() {
    if (aheadCommits == null) {
      var command = GIT_LOG + format("%s.. 2> /dev/null", getUpstreamBranch());
      aheadCommits = listCommandResults(command);
    }
    return aheadCommits;
  }

  public List<String> getAheadOriginCommits() {
    if (aheadOriginCommits == null) {
      var command = GIT_LOG + format("origin/develop..%s 2> /dev/null", getUpstreamBranch());
      aheadOriginCommits = listCommandResults(command);
    }
    return aheadOriginCommits;
  }

  public List<String> getBehindCommits() {
    if (behindCommits == null) {
      var command = GIT_LOG + format("..%s 2> /dev/null", getUpstreamBranch());
      behindCommits = listCommandResults(command);
    }
    return behindCommits;
  }

  public List<String> getBehindMasterCommits() {
    if (behindMasterCommits == null) {
      var command = GIT_LOG + format("origin/develop..origin/master 2> /dev/null", getUpstreamBranch());
      behindMasterCommits = listCommandResults(command);
    }
    return behindMasterCommits;
  }

  public List<String> getBehindOriginCommits() {
    if (behindOriginCommits == null) {
      var command = GIT_LOG + format("%s..origin/develop 2> /dev/null", getUpstreamBranch());
      behindOriginCommits = listCommandResults(command);
    }
    return behindOriginCommits;
  }

  public String getBranch() {
    if (branch == null) {
      ExecuteResult result = swallow(() -> exec("git rev-parse --abbrev-ref HEAD", dir));
      branch = result == null ? "" : result.getOutput().strip();
    }
    return branch;
  }

  public File getDir() {
    return dir;
  }

  public String getName() {
    return dir.getName();
  }

  public List<String> getUncommitedFiles() {
    if (uncommitedFiles == null) {
      var command = "git status --short 2> /dev/null";
      uncommitedFiles = listCommandResults(command);
    }
    return uncommitedFiles;
  }

  public String getUpstreamBranch() {
    if (upstreamBranch == null) {
      ExecuteResult result = swallow(() -> exec("git rev-parse --abbrev-ref @{upstream}", dir));
      upstreamBranch = result == null ? "" : result.getOutput().strip();
    }
    return upstreamBranch;
  }

  @Override
  public int hashCode() {
    return dir.hashCode();
  }

  private ArrayList<String> listCommandResults(String command) {
    var result = swallow(() -> exec(command, dir));
    var statusOutput = result == null ? "" : result.getOutput().strip();
    var list = new ArrayList<String>();
    var lineList = toLineList(statusOutput);
    for (String line : lineList) {
      line = line.strip();
      if (!line.isBlank()) {
        list.add(line);
      }
    }
    return list;
  }

  @Override
  public String toString() {
    return dir.toString();
  }

  public int getModuleNumber() {
    return moduleNumber;
  }
}
