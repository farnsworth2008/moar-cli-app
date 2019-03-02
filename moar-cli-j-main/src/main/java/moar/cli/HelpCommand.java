package moar.cli;

import static moar.ansi.Ansi.blue;
import static moar.ansi.Ansi.green;
import static moar.ansi.Ansi.purple;

public class HelpCommand
    extends
    BaseCommand {

  @Override
  Boolean accept(String command) {
    return super.accept(command) || command.equals("--help") || command.isEmpty();
  }

  @Override
  void doRun(String[] args) {
    StringBuilder b = new StringBuilder();
    b.append(args[0] + " [");
    b.append(green("--version"));
    b.append("] ");
    b.append(blue("<command> "));
    b.append("[");
    b.append(purple("<args>"));
    b.append("] [");
    b.append(green("--help"));
    b.append("]");
    out.println(b.toString());
    out.print("Valid commands: ");
    var commandNames = getCommandNames();
    for (int i = 0; i < commandNames.size(); i++) {
      String commandName = commandNames.get(i);
      out.print(blue(commandName));
      if (i < commandNames.size() - 1) {
        out.print(", ");
      }
    }
    out.println();
  }

}
