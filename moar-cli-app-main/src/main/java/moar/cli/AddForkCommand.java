package moar.cli;

import static java.lang.String.format;
import static java.lang.System.out;
import static moar.ansi.Ansi.cyanBold;
import static moar.ansi.Ansi.green;
import static moar.ansi.Ansi.purple;
import static moar.ansi.Ansi.purpleBold;
import static moar.sugar.Sugar.exec;
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
      if (argNum == 1) {
        fork = arg;
      } else {
        throw new MoarException("Unexpected Arg");
      }
    }
    if (fork.isEmpty()) {
      throw new MoarException("You must supply the GitHub account name for the fork.");
    }
    var forkParts = fork.split("/");
    var lastForkPart = forkParts[forkParts.length - 1];
    if (lastForkPart.startsWith("~")) {
      fork += "/" + getModuleName() + ".git";
    }
    var command = format("git remote add fork %s", fork);
    var remoteUpdateCommand = "git remote update";
    exec(command, dir);
    var status = new StatusLine();
    status.set(remoteUpdateCommand);
    exec(remoteUpdateCommand, dir);
    status.remove();
  }

  @Override
  boolean includeInCommandNames() {
    return false;
  }

  @Override
  void outHelp() {
    out.print(purpleBold(SCRIPT_NAME));
    out.print(" ");
    out.print(cyanBold(getName()));
    out.print(" ");
    out.println(purple("<Git Hub Account>"));
    out.println(green("     /* Add a fork remote. */"));
    out.println();
  }

}
