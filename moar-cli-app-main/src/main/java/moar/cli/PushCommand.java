package moar.cli;

import static java.lang.String.format;
import static moar.ansi.Ansi.cyanBold;
import static moar.ansi.Ansi.green;
import static moar.ansi.Ansi.purple;
import static moar.ansi.Ansi.purpleBold;
import static moar.sugar.Sugar.exec;
import static moar.sugar.thread.MoarThreadSugar.$;
import moar.ansi.StatusLine;

public class PushCommand
    extends
    ModuleCommand {

  private void doCommand(StatusLine status, String command) {
    status.set(command);
    exec(command, dir);
    status.completeOne();
  }

  @Override
  void doModuleCommand(String[] args) {
    status.set("Push");
    $(async, futures, () -> doCommand(status, "git push"));
    for (var i = 2; i < args.length; i++) {
      String arg = args[i];
      if (arg.equals("master")) {
        $(async, futures, () -> doCommand(status, "git push origin"));
        $(async, futures, () -> doCommand(status, "git push fork HEAD:master"));
        $(async, futures, () -> doCommand(status, "git push origin HEAD:master"));
      } else {
        $(async, futures, () -> doCommand(status, format("git push %s", arg)));
      }
    }
    completeAsyncTasks("push");

  }

  @Override
  void outHelp() {
    status.output(out -> {
      out.print(purpleBold(SCRIPT_NAME));
      out.print(" ");
      out.print(cyanBold(name));
      out.print(" ");
      out.println(purple("[<origin> | <master>]"));
      out.println(green("     /* Push to upstream and optionally other remotes."));
      out.println(green("      * "));
      out.println(green("      * Push to the current upstream."));
      out.println(green("      * Example: moar push"));
      out.println(green("      * "));
      out.println(green("      * Push to the current upstream, and origin."));
      out.println(green("      * Example: moar push origin"));
      out.println(green("      * "));
      out.println(green("      * Push to the current upstream, origin, and origin"));
      out.println(green("      * master."));
      out.println(green("      * Example: moar push master */"));
      out.println();
    });
  }

}
