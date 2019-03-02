package moar.cli;

import static java.lang.String.format;
import static java.nio.file.Files.createSymbolicLink;
import static java.nio.file.Files.isSymbolicLink;
import static moar.sugar.Sugar.exec;
import static moar.sugar.Sugar.require;
import static moar.sugar.thread.MoarThreadSugar.$;
import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;
import moar.ansi.StatusLine;
import moar.sugar.MoarException;
import moar.sugar.MoarStringUtil;

public class CloneModulesCommand
    extends
    ModuleCommand {

  void doCloneModules() {
    File workspaceDir = getWorkspaceDir();
    File moduleDir = getCurrentModuleDir();
    File moarModulesDir = new File(moduleDir, "moar-modules");
    String[] moarModuleList = moarModulesDir.list((dir, name) -> name.startsWith("git-"));
    var progress = new StatusLine(out, "Cloning moar modules");
    var count = moarModuleList.length;
    var completed = new AtomicInteger();
    var futures = $();
    for (var moarModule : moarModuleList) {
      $(async, futures, () -> {
        var moarModuleName = moarModule.replaceAll("^git-", "");
        File moduleRefFile = new File(moduleDir, moarModuleName);
        if (moduleRefFile.exists()) {
          if (isSymbolicLink(moduleRefFile.toPath())) {
            moduleRefFile.delete();
          } else {
            throw new MoarException(format("%s exists", moarModuleName));
          }
        }
        var refModuleCloneDir = new File(workspaceDir, moarModuleName);
        if (!refModuleCloneDir.exists()) {
          var moarModuleUrl = MoarStringUtil.fileContentsAsString(new File(moarModulesDir, moarModule));
          exec(format("git clone --recurse-submodules %s", moarModuleUrl), workspaceDir);
          exec("git checkout -b develop origin/develop", refModuleCloneDir);
          if (new File(refModuleCloneDir, "moar-setup.sh").canExecute()) {
            exec("moar-setup.sh", refModuleCloneDir);
          }
        }
        require(() -> createSymbolicLink(moduleRefFile.toPath(), refModuleCloneDir.toPath()));
        progress.set(() -> (float) completed.incrementAndGet() / count);
      });
    }
    $(futures);
    progress.clear();
  }

  @Override
  void doModuleCommand(String[] args) {
    doCloneModules();
  }

}
