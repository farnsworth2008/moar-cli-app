package moar.cli;

import static moar.sugar.Sugar.exec;
import java.io.File;
import moar.ansi.StatusLine;

public class InitCommand
    extends
    InitModulesCommand {

  @Override
  void doModuleCommand(String[] args) {
    File dir = getCurrentModuleDir();
    if (new File(dir, "moar-setup.sh").exists()) {
      exec("moar-setup.sh", dir);
    }
    doCloneModules();
    boolean hasMoarSugar = new File(dir, "moar-sugar").exists();
    boolean hasGradleBuild = new File(dir, "build.gradle").exists();
    boolean hasGradleWrapper = new File(dir, "gradlew").exists();
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
  boolean includeInCommandNames() {
    return true;
  }

}
