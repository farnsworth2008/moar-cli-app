package moar.cli;

import static java.lang.System.out;
import static moar.ansi.Ansi.cyanBold;
import static moar.ansi.Ansi.green;
import static moar.ansi.Ansi.purpleBold;
import static moar.sugar.Sugar.exec;

public class EclipseCommand
    extends
    ModuleCommand {

  @Override
  void doModuleCommand(String[] args) {
    var command = "./gradlew cleanEclipse eclipse";
    out.println(exec(command, dir).getOutput());
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
    out.print(" ");
    out.println(green("     /* Run './gradlew cleanEclipse eclipse'. */"));
    out.println();
  }

}
