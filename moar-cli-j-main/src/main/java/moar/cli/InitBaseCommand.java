package moar.cli;

import static java.lang.String.format;
import static moar.sugar.MoarStringUtil.readStringFromFile;
import static moar.sugar.MoarStringUtil.writeStringToFile;
import static moar.sugar.Sugar.exec;
import static moar.sugar.Sugar.nonNull;
import static moar.sugar.thread.MoarThreadSugar.$;
import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;
import moar.ansi.StatusLine;
import moar.sugar.MoarException;

public abstract class InitBaseCommand
    extends
    ModuleCommand {

  void doCloneModules(File moarModulesDir, boolean nest) {
    if (!moarModulesDir.exists()) {
      moarModulesDir.mkdir();
    }
    var workspaceDir = getWorkspaceDir();
    var moduleDir = getCurrentModuleDir();
    var moarModuleList = moarModulesDir.list((dir, name) -> name.startsWith("git-"));
    var progress = new StatusLine(out, "Cloning moar modules");
    var count = moarModuleList.length;
    var completed = new AtomicInteger();
    var futures = $();
    for (var moarModule : moarModuleList) {
      $(async, futures, () -> {
        var moarModuleName = moarModule.replaceAll("^git-", "");
        File moduleRefFile = new File(moduleDir, moarModuleName);
        if (moduleRefFile.exists()) {
          exec(format("rm -rf %s", moarModuleName), moduleDir);
          if (moduleRefFile.exists()) {
            throw new MoarException("Unable to remove module");
          }
        }
        File initFile = new File(moarModulesDir, "init-" + moarModuleName);
        var moarModuleUrl = readStringFromFile(new File(moarModulesDir, moarModule)).strip();
        var init = nonNull(readStringFromFile(initFile), "").strip();
        StringBuilder builder = new StringBuilder();
        builder.append("git clone --recurse-submodules %s;");
        builder.append("cd %s;");
        builder.append("git reset --hard %s");
        String command = format(builder.toString(), moarModuleUrl, moarModuleName, init);
        if (nest) {
          /* If we "nest", clone into sub-dir for a nested structure. */
          exec(command, moduleDir);
        } else {
          /* Otherwise we setup a symlink version */
          var refModuleCloneDir = new File(workspaceDir, moarModuleName);
          if (!refModuleCloneDir.exists()) {
            exec(command, workspaceDir);
          }
          var currentDir = getCurrentModuleDir();
          var refModuleName = refModuleCloneDir.getName();
          var refModulePath = refModuleCloneDir.getAbsolutePath();
          exec(format("ln -s %s %s", refModulePath, refModuleName), currentDir);
          init = exec("git rev-parse HEAD", refModuleCloneDir).getOutput();
          writeStringToFile(initFile, init);
          progress.set(() -> (float) completed.incrementAndGet() / count);
        }
      });
    }
    $(futures);
    progress.clear();
  }

  protected void doInitCommand(String[] args, boolean nested) {
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
      var refModuleName = url.replaceAll("^.*/", "").replaceAll("\\..*", "");
      File refFile = new File(moarModulesDir, "git-" + refModuleName);
      writeStringToFile(refFile, url);
    }
    doCloneModules(moarModulesDir, nested);

    var hasMoarSugar = new File(dir, "moar-sugar").exists();
    var hasGradleWrapper = new File(dir, "gradlew").exists();
    var hasBuildGradle = new File(dir, "build.gradle").exists();

    if (hasMoarSugar && !hasGradleWrapper) {
      String moduleName = getCurrentModuleDir().getName();
      String mkdirMainCmd = "mkdir -p %s-main/src/main/java;";
      String settingsGradleCmd = "cat moar-sugar/template-settings.gradle | sed 's/MODULE/%s/g' > settings.gradle;";
      String buildMainGradleCmd = "cp moar-sugar/template-main-build.gradle %s-main/build.gradle;";
      String buildGradleCmd = "cp moar-sugar/template-build.gradle build.gradle;";
      String buildMainJavaCmd = "cp moar-sugar/template-Main.java %s-main/src/main/java/Main.java;";
      StringBuilder builder = new StringBuilder();
      if (!hasBuildGradle) {
        builder.append(buildGradleCmd);
        builder.append(format(settingsGradleCmd, moduleName));
      }
      if (!new File(dir.getAbsolutePath() + format("/%s-main/src/main/java", moduleName)).exists()) {
        builder.append(format(mkdirMainCmd, moduleName));
        builder.append(format(buildMainGradleCmd, moduleName));
        builder.append(format(buildMainJavaCmd, moduleName));
      }
      builder.append("cp moar-sugar/template.gitignore .gitignore;");
      builder.append("cp moar-sugar/template-build.sh build.sh;");
      builder.append("ln -s moar-sugar/gradle gradle;");
      builder.append("ln -s moar-sugar/gradlew gradlew");
      exec(builder.toString(), dir);
      hasGradleWrapper = true;
    }
    if (hasGradleWrapper) {
      var progress = new StatusLine(out, "./gradlew cleanEclipse eclipse");
      exec("./gradlew cleanEclipse eclipse", dir);
      progress.clear();
    }
  }

  @Override
  protected boolean includeInCommandNames() {
    return true;
  }

}