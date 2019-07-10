package moar.cli;

import static java.lang.String.format;
import static moar.sugar.MoarStringUtil.appendLinesToFile;
import static moar.sugar.MoarStringUtil.readStringFromFile;
import static moar.sugar.MoarStringUtil.writeStringToFile;
import static moar.sugar.Sugar.exec;
import static moar.sugar.Sugar.nonNull;
import static moar.sugar.Sugar.require;
import static moar.sugar.thread.MoarThreadSugar.$;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import moar.ansi.StatusLine;
import moar.sugar.MoarException;

public abstract class InitBaseCommand
    extends
    ModuleCommand {

  void doCloneModules(File moarModulesDir, boolean nest) {
    if (!moarModulesDir.exists()) {
      moarModulesDir.mkdir();
    }
    var moduleDir = dir;
    var modules = moarModulesDir.list((dir, name) -> name.startsWith("git-"));
    var status = new StatusLine();
    status.setCount(modules.length, format("%d modules", modules.length));
    for (var module : modules) {
      $(async, futures, () -> {
        var moarModuleName = module.replaceAll("^git-", "");
        File moduleRefFile = new File(moduleDir, moarModuleName);
        if (moduleRefFile.exists()) {
          exec(format("rm -rf %s", moarModuleName), moduleDir);
          if (moduleRefFile.exists()) {
            throw new MoarException("Unable to remove module");
          }
        }
        File initFile = new File(moarModulesDir, "init-" + moarModuleName);
        var moarModuleUrl = readStringFromFile(new File(moarModulesDir, module)).strip();
        var init = nonNull(readStringFromFile(initFile), "").strip();
        StringBuilder builder = new StringBuilder();
        String cloneCommand = "git clone --recurse-submodules %s;";
        builder.append(cloneCommand);
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
          var currentDir = dir;
          var refModuleName = refModuleCloneDir.getName();
          var refModulePath = refModuleCloneDir.getAbsolutePath();
          exec(format("ln -s %s %s", refModulePath, refModuleName), currentDir);
          init = exec("git rev-parse HEAD", refModuleCloneDir).getOutput();
          writeStringToFile(initFile, init);
        }
        status.completeOne();
      });
    }
    $(futures);
    status.remove();
  }

  protected void doInitCommand(String[] args, boolean nested) {
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
    File moarModulesDir = new File(dir, "moar-modules");
    if (!url.isEmpty()) {
      var refModuleName = url.replaceAll("^.*/", "").replaceAll("\\..*", "");
      File refFile = new File(moarModulesDir, "git-" + refModuleName);
      writeStringToFile(refFile, url);
      updateIgnoreFile(format("/%s", refModuleName));
    }
    doCloneModules(moarModulesDir, nested);

    var hasMoarSugar = new File(dir, "moar-sugar").exists();
    var hasGradleWrapper = new File(dir, "gradlew").exists();
    var hasBuildGradle = new File(dir, "build.gradle").exists();
    var hasLicense = new File(dir, "LICENSE").exists();

    if (hasMoarSugar && !hasGradleWrapper) {
      String moduleName = dir.getName();
      String mkdirMainCmd = "mkdir -p %s-main/src/main/java;";
      String buildMainGradleCmd = "cp moar-sugar/template-main-build.gradle %s-main/build.gradle;";
      String buildMainJavaCmd = "cp moar-sugar/template-Main.java %s-main/src/main/java/Main.java;";
      if (!hasBuildGradle) {
        String settingsGradleCmd = "cat moar-sugar/template-settings.gradle | sed 's/MODULE/%s/g' > settings.gradle";
        doAsyncExec("cp moar-sugar/template-build.sh build.sh");
        doAsyncExec("cp moar-sugar/template-run.sh run.sh");
        doAsyncExec("cp moar-sugar/template-build.gradle build.gradle");
        doAsyncExec(format(settingsGradleCmd, moduleName));
        if (!new File(dir.getAbsolutePath() + format("/%s-main/src/main/java", moduleName)).exists()) {
          require(() -> doAsyncExec(format(mkdirMainCmd, moduleName)).get());
          doAsyncExec(format(buildMainGradleCmd, moduleName));
          doAsyncExec(format(buildMainJavaCmd, moduleName));
        }
        doAsyncExec("cp moar-sugar/template.gitignore .gitignore;");
        if (!hasLicense) {
          doAsyncExec("cp moar-sugar/LICENSE LICENSE;");
        }
      }
      doAsyncExec("ln -s moar-sugar/gradle gradle;");
      doAsyncExec("ln -s moar-sugar/gradlew gradlew");
      $(futures);
      hasGradleWrapper = true;
    }
    if (hasGradleWrapper) {
      doAsyncExec("./gradlew cleanEclipse eclipse");
    }
    $(futures);
    if (new File(dir, "moar-setup.sh").exists()) {
      doAsyncExec("./moar-setup.sh");
    }
    $(futures);

  }

  @Override
  boolean includeInCommandNames() {
    return true;
  }

  private boolean searchIgnoreFile(File file, String ignoreSpec) {
    return require(() -> {
      var found = false;
      if (file.exists()) {
        try (var fr = new FileReader(file)) {
          try (var br = new BufferedReader(fr)) {
            String line;
            while ((line = br.readLine()) != null) {
              if (line.equals(ignoreSpec)) {
                found = true;
                break;
              }
            }
          }
        }
      }
      return found;
    });
  }

  private void updateIgnoreFile(String ignoreSpec) {
    File gitIgnoreFile = new File(dir, ".gitignore");
    var found = searchIgnoreFile(gitIgnoreFile, ignoreSpec);
    if (!found) {
      appendLinesToFile(gitIgnoreFile, ignoreSpec);
    }
  }

}