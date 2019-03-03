package moar.cli;

import static moar.ansi.Ansi.cyan;
import static moar.ansi.Ansi.green;
import static moar.ansi.Ansi.purple;
import static moar.ansi.Ansi.purpleBold;

public class HelpCommand
    extends
    BaseCommand {

  @Override
  Boolean accept(String command) {
    return super.accept(command) || command.equals("--help") || command.isEmpty();
  }

  @Override
  void doRun(String[] args) {
    out.print(purpleBold(args[0]) + " [");
    out.print(green("--version"));
    out.print("] [");
    out.print(green("--help"));
    out.print("] ");
    out.print(cyan("<command> "));
    out.print("[");
    out.print(purple("<args>"));
    out.print("]");
    out.println();
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
    out.print(purpleBold(args[0]));
    out.print(" ");
    out.print(cyan("add-module "));
    out.println(purple("<Git URL>"));
    out.println(green("     /* Add a moar-module reference. */"));
    out.println();
    out.print(purpleBold(args[0]));
    out.print(" ");
    out.print(cyan("add-fork"));
    out.print(" ");
    out.println(purple("<Git Hub Account>"));
    out.println(green("     /* Add a fork remote. */"));
    out.println();
    out.print(purpleBold(args[0]));
    out.print(" ");
    out.print(cyan("detail"));
    out.print(" ");
    out.println(purple("[<Module Filter RegEx>]"));
    out.println(green("     /* Shows detail status. */"));
    out.println();
    out.print(purpleBold(args[0]));
    out.print(" ");
    out.print(cyan("each"));
    out.print(" ");
    out.println(purple("<Module Filter RegEx> <Bash Command>"));
    out.println(green("     /* Run bash command in each matching module directory. */"));
    out.println();
    out.print(purpleBold(args[0]));
    out.print(" ");
    out.println(cyan("help"));
    out.println(green("     /* Shows this help. */"));
    out.println();
    out.print(purpleBold(args[0]));
    out.print(" ");
    out.println(cyan("require"));
    out.println(green("     /* Require will clone referenced modules and/or force */"));
    out.println(green("      * them to refer to the exact init commit. */"));
    out.println();
    out.print(purpleBold(args[0]));
    out.print(" ");
    out.println(cyan("init"));
    out.println(green("     /* Initalize will clone referenced modules and/or may */"));
    out.println(green("      * update the init files. */"));
    out.println();
    out.print(purpleBold(args[0]));
    out.print(" ");
    out.print(cyan("status"));
    out.print(" ");
    out.println(purple("[<Module Filter RegEx>]"));
    out.println(green("     /* Show status. */"));
    out.println();
  }

}
