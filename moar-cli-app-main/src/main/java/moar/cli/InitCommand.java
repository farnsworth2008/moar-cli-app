package moar.cli;

import static java.lang.System.out;
import static moar.ansi.Ansi.cyanBold;
import static moar.ansi.Ansi.green;
import static moar.ansi.Ansi.purpleBold;

public class InitCommand
    extends
    InitBaseCommand {

  @Override
  void doModuleCommand(String[] args) {
    doInitCommand(null, false);
  }

  @Override
  void outHelp() {
    out.print(purpleBold(SCRIPT_NAME));
    out.print(" ");
    out.println(cyanBold(name));
    out.println(green("     /* Initalize will clone referenced modules and setup"));
    out.println(green("      * symbolic linking. This supports development"));
    out.println(green("      * environments where the shared module version is"));
    out.println(green("      * used.  The assoicated 'init-' file is updated if"));
    out.println(green("      * the shared module differs from the specified "));
    out.println(green("      * version. */"));
    out.println();
  }

}
