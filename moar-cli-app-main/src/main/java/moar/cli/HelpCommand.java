package moar.cli;

import static moar.ansi.Ansi.cyanBold;
import static moar.ansi.Ansi.green;
import static moar.ansi.Ansi.purple;
import static moar.ansi.Ansi.purpleBold;
import static moar.ansi.Ansi.red;

public class HelpCommand
    extends
    BaseCommand {

  private String filter;

  @Override
  Boolean accept(String command) {
    return super.accept(command) || command.equals("--help") || command.isEmpty();
  }

  @SuppressWarnings("rawtypes")
  @Override
  void doRun(String[] args) {
    if (args.length > 2) {
      filter = "^" + args[2] + "$";
    } else {
      filter = "^--help$";
      outProgramHelp(args);
      outCommandList();
    }
    Class[] commands = getCommandClasses();
    for (var commandClz : commands) {
      var command = getCommand(commandClz);
      if (command.name.matches(filter)) {
        command.setStatus(status);
        command.outHelp();
      }
    }
  }

  @Override
  boolean includeInCommandNames() {
    return true;
  }

  private void outCommandList() {
    status.output(out -> {
      out.print(green("     /* Commands: "));
      var commandNames = getCommandNames();
      for (int i = 0; i < commandNames.size(); i++) {
        String commandName = commandNames.get(i);
        out.print(green(commandName));
        if (i < commandNames.size() - 1) {
          out.print(green(", "));
        }
        if (i == 4) {
          out.println();
          out.print(green("      * "));
        }
      }
      out.println(green(" */"));
      out.println();
    });
  }

  @Override
  void outHelp() {
    status.output(out -> {
      out.print(purpleBold(SCRIPT_NAME));
      out.print(" ");
      out.print(cyanBold(name));
      out.print(" ");
      out.println(purple("[<command>]"));
      out.println(green("     /* Shows this help or help for a specific command."));
      out.println(green("      * "));
      out.println(green("      * Example: moar --help status */"));
      out.println();
    });
  }

  private void outProgramHelp(String[] args) {
    status.output(out -> {
      out.print(purpleBold(args[0]) + " [");
      out.print(red("--version"));
      out.print("] [");
      out.print(red("--help"));
      out.print("] ");
      out.print(cyanBold("<command> "));
      out.print("[");
      out.print(purple("<args>"));
      out.print("]");
      out.println();
    });
  }

}
