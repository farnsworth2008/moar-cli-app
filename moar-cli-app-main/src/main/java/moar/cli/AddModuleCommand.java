package moar.cli;

import static moar.ansi.Ansi.cyanBold;
import static moar.ansi.Ansi.green;
import static moar.ansi.Ansi.purple;
import static moar.ansi.Ansi.purpleBold;

public class AddModuleCommand
    extends
    InitBaseCommand {

  @Override
  void doModuleCommand(String[] args) {
    doInitCommand(args, false);
  }

  @Override
  void outHelp() {
    status.output(out -> {
      out.print(purpleBold(SCRIPT_NAME));
      out.print(" ");
      out.print(cyanBold(name));
      out.print(" ");
      out.println(purple("<Git URL>"));
      out.println(green("     /* Add a moar-module reference. */"));
      out.println();
    });
  }

}
