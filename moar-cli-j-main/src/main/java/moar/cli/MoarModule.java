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
  final File dir;
  private String upstreamBranch;
  private List<String> uncommitedFiles;
  private List<String> aheadCommits;
  private List<String> behindCommits;
  private List<String> aheadOriginCommits;
  private List<String> behindOriginCommits;
  private List<String> behindMasterCommits;

  public MoarModule(File dir) {
    this.dir = dir;
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
    return aheadCommits;
  }

  public Integer getAheadCount() {
    if (aheadCommits == null) {
      var command = format("git log --oneline %s.. 2> /dev/null", getUpstreamBranch());
      aheadCommits = listCommandResults(command);
    }
    return aheadCommits.size();
  }

  public List<String> getAheadOriginCommits() {
    return aheadOriginCommits;
  }

  public Integer getAheadOriginCount() {
    if (aheadOriginCommits == null) {
      var command = format("git log --oneline origin/develop..%s 2> /dev/null", getUpstreamBranch());
      aheadOriginCommits = listCommandResults(command);
    }
    return aheadOriginCommits.size();
  }

  public List<String> getBehindCommits() {
    return behindCommits;
  }

  public Integer getBehindCount() {
    if (behindCommits == null) {
      var command = format("git log --oneline ..%s 2> /dev/null", getUpstreamBranch());
      behindCommits = listCommandResults(command);
    }
    return behindCommits.size();
  }

  public List<String> getBehindMasterCommits() {
    return behindMasterCommits;
  }

  public Integer getBehindMasterCount() {
    if (behindMasterCommits == null) {
      var command = format("git log --oneline origin/develop..origin/master 2> /dev/null", getUpstreamBranch());
      behindMasterCommits = listCommandResults(command);
    }
    return behindMasterCommits.size();
  }

  public List<String> getBehindOriginCommits() {
    return behindOriginCommits;
  }

  public Integer getBehindOriginCount() {
    if (behindOriginCommits == null) {
      var command = format("git log --oneline %s..origin/develop 2> /dev/null", getUpstreamBranch());
      behindOriginCommits = listCommandResults(command);
    }
    return behindOriginCommits.size();
  }

  public File getDir() {
    return dir;
  }

  public String getName() {
    return dir.getName();
  }

  public Integer getUncommitedCount() {
    if (uncommitedFiles == null) {
      var command = "git status --short 2> /dev/null";
      uncommitedFiles = listCommandResults(command);
    }
    return uncommitedFiles.size();
  }

  public List<String> getUncommitedFiles() {
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
}
