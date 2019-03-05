package moar.cli;

import static java.lang.Boolean.FALSE;
import static moar.ansi.Ansi.cyanBold;
import static moar.ansi.Ansi.green;
import static moar.ansi.Ansi.purple;
import static moar.ansi.Ansi.purpleBold;
import moar.sugar.MoarException;

public class StatusCommand
    extends
    BaseStatusCommand {

  @Override
  void doRun(String[] args) {
    var filter = "";
    var argNum = 0;
    for (var i = 2; i < args.length; i++) {
      String arg = args[i];
      ++argNum;
      if (argNum == 1) {
        filter = arg;
      } else {
        throw new MoarException("Unexpected Arg");
      }
    }
    var defaultFilter = ".*";
    if (filter.isEmpty()) {
      filter = defaultFilter;
    }
    doStatus(filter, FALSE);
  }

  @Override
  void outHelp() {
    status.output(out -> {
      out.print(purpleBold(SCRIPT_NAME));
      out.print(" ");
      out.print(cyanBold(name));
      out.print(" ");
      out.println(purple("[<Module Filter RegEx>]"));
      out.println(green("     /* Show status."));
      out.println(green("      * "));
      out.println(green("      * Example: moar status 'group-.*' */"));
      out.println();
    });
  }

}
