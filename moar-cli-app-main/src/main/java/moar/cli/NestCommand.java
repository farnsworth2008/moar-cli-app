package moar.cli;

import static java.lang.System.out;
import static moar.ansi.Ansi.cyanBold;
import static moar.ansi.Ansi.green;
import static moar.ansi.Ansi.purpleBold;

public class NestCommand
    extends
    InitBaseCommand {

  @Override
  void doModuleCommand(String[] args) {
    doInitCommand(null, true);
  }

  @Override
  boolean includeInCommandNames() {
    return false;
  }

  @Override
  void outHelp() {
    out.print(purpleBold(SCRIPT_NAME));
    out.print(" ");
    out.println(cyanBold(name));
    out.println(green("     /* Clone referenced modules in a \"nested\" structure"));
    out.println(green("      * without symbolic linking.  This supports accurate"));
    out.println(green("      * builds because the nested module version will match"));
    out.println(green("      * the state of the assoicated 'init-' file */"));
    out.println();
  }

}
