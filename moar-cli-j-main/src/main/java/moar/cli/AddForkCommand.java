package moar.cli;

import static java.lang.String.format;
import static moar.sugar.MoarStringUtil.readStringFromFile;
import static moar.sugar.MoarStringUtil.writeStringToFile;
import static moar.sugar.Sugar.exec;
import static moar.sugar.Sugar.nonNull;
import java.io.File;
import moar.ansi.StatusLine;
import moar.sugar.MoarException;

public class AddForkCommand
    extends
    ModuleCommand {

  @Override
  void doModuleCommand(String[] args) {
    var fork = "";
    var argNum = 0;
    for (int i = 2; i < args.length; i++) {
      String arg = args[i];
      argNum++;
      if (argNum != 1) {
        throw new MoarException("Expected only one argument");
      }
      fork = arg;
    }
    File forkConfigFile = new File(getWorkspaceDir(), ".fork");
    if (fork.isEmpty()) {
      fork = nonNull(readStringFromFile(forkConfigFile), "");
    }
    if (fork.isEmpty()) {
      throw new MoarException("You must supply the GitHub account name for the fork.");
    }
    if (!forkConfigFile.exists()) {
      writeStringToFile(forkConfigFile, fork);
    }
    var dir = getCurrentModuleDir();
    String remoteUpdateCommand = "git remote update";
    var command = format("git remote add fork git@github.com:%s/%s", fork, dir.getName());
    exec(command, dir);
    var status = new StatusLine(out, remoteUpdateCommand);
    exec(remoteUpdateCommand, dir);
    status.clear();
    exec("git checkout -b develop fork/develop", dir);
  }

}
