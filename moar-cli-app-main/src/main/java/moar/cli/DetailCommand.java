package moar.cli;

import static java.lang.Boolean.TRUE;
import static java.lang.System.out;
import static moar.ansi.Ansi.cyanBold;
import static moar.ansi.Ansi.green;
import static moar.ansi.Ansi.purple;
import static moar.ansi.Ansi.purpleBold;
import static moar.sugar.Sugar.require;
import java.io.File;
import moar.sugar.MoarException;

public class DetailCommand
    extends
    BaseStatusCommand {

  @Override
  void doRun(String[] args) {
    var filter = "";
    var argNum = 0;
    for (int i = 2; i < args.length; i++) {
      String arg = args[i];
      argNum++;
      if (argNum != 1) {
        throw new MoarException("Expected only one argument");
      }
      filter = arg;
    }
    if (filter.isEmpty()) {
      String path = require(() -> dir.getCanonicalPath());
      String dirName = new File(path).getName();
      filter = String.format("^%s$", dirName);
    }
    doStatus(filter, TRUE);
  }

  @Override
  void outHelp() {
    out.print(purpleBold(SCRIPT_NAME));
    out.print(" ");
    out.print(cyanBold(name));
    out.print(" ");
    out.println(purple("[<Module Filter RegEx>]"));
    out.println(green("     /* Shows detail status. */"));
    out.println();
  }

}
