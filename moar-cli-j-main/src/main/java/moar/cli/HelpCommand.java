package moar.cli;

import static moar.ansi.Ansi.cyanBold;
import static moar.ansi.Ansi.green;
import static moar.ansi.Ansi.purple;
import static moar.ansi.Ansi.purpleBold;
import static moar.ansi.Ansi.red;

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
    out.print(red("--version"));
    out.print("] [");
    out.print(red("--help"));
    out.print("] ");
    out.print(cyanBold("<command> "));
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
    out.print(cyanBold("add-module "));
    out.println(purple("<Git URL>"));
    out.println(green("     /* Add a moar-module reference. */"));
    out.println();
    out.print(purpleBold(args[0]));
    out.print(" ");
    out.print(cyanBold("add-fork"));
    out.print(" ");
    out.println(purple("<Git Hub Account>"));
    out.println(green("     /* Add a fork remote. */"));
    out.println();
    out.print(purpleBold(args[0]));
    out.print(" ");
    out.print(cyanBold("detail"));
    out.print(" ");
    out.println(purple("[<Module Filter RegEx>]"));
    out.println(green("     /* Shows detail status. */"));
    out.println();
    out.print(purpleBold(args[0]));
    out.print(" ");
    out.print(cyanBold("each"));
    out.print(" ");
    out.println(purple("<Module Filter RegEx> <Bash Command>"));
    out.println(green("     /* Run <Bash Command> in filtered module directories. */"));
    out.println();
    out.print(purpleBold(args[0]));
    out.print(" ");
    out.println(cyanBold("eclipse"));
    out.print(" ");
    out.println(green("     /* Run './gradlew cleanEclipse eclipse'. */"));
    out.println();
    out.print(purpleBold(args[0]));
    out.print(" ");
    out.println(cyanBold("help"));
    out.println(green("     /* Shows this help. */"));
    out.println();
    out.print(purpleBold(args[0]));
    out.print(" ");
    out.println(cyanBold("init"));
    out.println(green("     /* Initalize will clone referenced modules and setup"));
    out.println(green("      * symbolic linking. */"));
    out.println();
    out.print(purpleBold(args[0]));
    out.print(" ");
    out.println(cyanBold("nest"));
    out.println(green("     /* Require will clone referenced modules in a \"nested\""));
    out.println(green("      * structure without symbolic linking. */"));
    out.println();
    out.print(purpleBold(args[0]));
    out.print(" ");
    out.print(cyanBold("push"));
    out.print(" ");
    out.println(purple("[<origin>, <master>]"));
    out.println(green("     /* Push to upstream and optionally other remotes */"));
    out.println();
    out.print(purpleBold(args[0]));
    out.print(" ");
    out.print(cyanBold("status"));
    out.print(" ");
    out.println(purple("[<Module Filter RegEx>]"));
    out.println(green("     /* Show status. */"));
    out.println();
  }

}
