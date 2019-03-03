package moar.cli;

import static java.lang.String.format;
import static java.nio.file.Files.createSymbolicLink;
import static java.nio.file.Files.isSymbolicLink;
import static moar.sugar.MoarStringUtil.readStringFromFile;
import static moar.sugar.MoarStringUtil.writeStringToFile;
import static moar.sugar.Sugar.exec;
import static moar.sugar.Sugar.require;
import static moar.sugar.thread.MoarThreadSugar.$;
import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;
import moar.ansi.StatusLine;
import moar.sugar.MoarException;
import moar.sugar.MoarStringUtil;

public abstract class InitBaseCommand
    extends
    ModuleCommand {

  void doCloneModules(File moarModulesDir, boolean require) {
    File workspaceDir = getWorkspaceDir();
    File moduleDir = getCurrentModuleDir();
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
        File initFile = new File(moarModulesDir, "init-" + moarModuleName);
        var init = readStringFromFile(initFile);
        if (!refModuleCloneDir.exists()) {
          var moarModuleUrl = readStringFromFile(new File(moarModulesDir, moarModule));
          exec(format("git clone --recurse-submodules %s", moarModuleUrl), workspaceDir);
          exec(format("git reset --hard %s", init), refModuleCloneDir);
          if (new File(refModuleCloneDir, "moar-setup.sh").canExecute()) {
            exec("moar-setup.sh", refModuleCloneDir);
          }
        }
        if (require) {
          exec("git checkout -b moar-init", refModuleCloneDir);
          exec(format("git reset --hard %s", init), refModuleCloneDir);
          exec("git branch -D init-" + moduleDir.getName(), refModuleCloneDir);
          exec("git branch -m init-" + moduleDir.getName(), refModuleCloneDir);
        } else {
          require(() -> createSymbolicLink(moduleRefFile.toPath(), refModuleCloneDir.toPath()));
          init = exec("git rev-parse HEAD", refModuleCloneDir).getOutput();
          writeStringToFile(initFile, init);
          progress.set(() -> (float) completed.incrementAndGet() / count);
        }
      });
    }
    $(futures);
    progress.clear();
  }

  protected void doInitCommand(String[] args, boolean require) {
    var dir = getCurrentModuleDir();
    if (new File(dir, "moar-setup.sh").exists()) {
      exec("moar-setup.sh", dir);
    }
    var url = "";
    if (args != null) {
      var argNum = 0;
      for (int i = 2; i < args.length; i++) {
        ++argNum;
        if (argNum == 1) {
          url = args[i];
        } else {
          throw new MoarException("Unexpected Arg");
        }
      }
      if (url.equals("sugar")) {
        url = "git@github.com:moar-stuff/moar-sugar.git";
      }
    }
    File moarModulesDir = new File(getCurrentModuleDir(), "moar-modules");
    if (!url.isEmpty()) {
      var refModuleName = url.replaceAll("^.*/", "").replaceAll("\\.*", "");
      File refFile = new File(moarModulesDir, "git-" + refModuleName);
      MoarStringUtil.writeStringToFile(refFile, "");
    }
    doCloneModules(moarModulesDir, require);
    var hasMoarSugar = new File(dir, "moar-sugar").exists();
    var hasGradleBuild = new File(dir, "build.gradle").exists();
    var hasGradleWrapper = new File(dir, "gradlew").exists();
    if (hasGradleBuild) {
      if (hasMoarSugar && !hasGradleWrapper) {
        exec("ln -s moar-sugar/gradle gradle && ln -s moar-sugar/gradlew gradlew", dir);
        hasGradleWrapper = true;
      }
      if (hasGradleWrapper) {
        var progress = new StatusLine(out, "./gradlew cleanEclipse eclipse");
        exec("./gradlew cleanEclipse eclipse", dir);
        progress.clear();
      }
    }
  }

  @Override
  protected boolean includeInCommandNames() {
    return true;
  }

}