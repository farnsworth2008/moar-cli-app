package moar.cli;

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
    status.set(command);
    status.output(out -> {
      out.println(exec(command, dir).getOutput());
    });
  }

  @Override
  void outHelp() {
    status.output(out -> {
      out.print(purpleBold(SCRIPT_NAME));
      out.print(" ");
      out.println(cyanBold(name));
      out.print(" ");
      out.println(green("     /* Run './gradlew cleanEclipse eclipse'. */"));
      out.println();
    });
  }

}
