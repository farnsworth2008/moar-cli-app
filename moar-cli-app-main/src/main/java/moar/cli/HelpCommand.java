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

  @Override
  void doRun(String[] args) {
    if (args.length > 2) {
      filter = "^" + args[2] + "$";
    } else {
      filter = "^--help$";
      outProgramHelp(args);
      outCommandList();
    }
    outAddForkHelp(args);
    outAddModulesHelp(args);
    outDetailHelp(args);
    outEachHelp(args);
    outEclipseHelp(args);
    outHelpHelp(args);
    outInitHelp(args);
    outNestHelp(args);
    outPushHelp(args);
    outStatusHelp(args);
  }

  @Override
  boolean includeInCommandNames() {
    return true;
  }

  private void outAddForkHelp(String[] args) {
    String command = "add-fork";
    if (!command.matches(filter)) {
      return;
    }
    status.output(out -> {
      out.print(purpleBold(args[0]));
      out.print(" ");
      out.print(cyanBold(command));
      out.print(" ");
      out.println(purple("<Git Hub Account>"));
      out.println(green("     /* Add a fork remote. */"));
      out.println();
    });
  }

  private void outAddModulesHelp(String[] args) {
    String command = "add-module";
    if (!command.matches(filter)) {
      return;
    }
    status.output(out -> {
      out.print(purpleBold(args[0]));
      out.print(" ");
      out.print(cyanBold(command));
      out.print(" ");
      out.println(purple("<Git URL>"));
      out.println(green("     /* Add a moar-module reference. */"));
      out.println();
    });
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

  private void outDetailHelp(String[] args) {
    String command = "detail";
    if (!command.matches(filter)) {
      return;
    }
    status.output(out -> {
      out.print(purpleBold(args[0]));
      out.print(" ");
      out.print(cyanBold(command));
      out.print(" ");
      out.println(purple("[<Module Filter RegEx>]"));
      out.println(green("     /* Shows detail status. */"));
      out.println();
    });
  }

  private void outEachHelp(String[] args) {
    String command = "each";
    if (!command.matches(filter)) {
      return;
    }
    status.output(out -> {
      out.print(purpleBold(args[0]));
      out.print(" ");
      out.print(cyanBold(command));
      out.print(" ");
      out.println(purple("<Bash Command> <Module Filter RegEx>"));
      out.println(green("     /* Run <Bash Command> in filtered module directories."));
      out.println(green("      * "));
      out.println(green("      * Example: moar each 'git remote update'"));
      out.println(green("      * "));
      out.println(green("      * Example: moar each 'git remote update' 'group-.*' */"));
      out.println();
    });
  }

  private void outEclipseHelp(String[] args) {
    String command = "eclipse";
    if (!command.matches(filter)) {
      return;
    }
    status.output(out -> {
      out.print(purpleBold(args[0]));
      out.print(" ");
      out.println(cyanBold(command));
      out.print(" ");
      out.println(green("     /* Run './gradlew cleanEclipse eclipse'. */"));
      out.println();
    });
  }

  private void outHelpHelp(String[] args) {
    String command = "--help";
    if (!command.matches(filter)) {
      return;
    }
    status.output(out -> {
      out.print(purpleBold(args[0]));
      out.print(" ");
      out.print(cyanBold(command));
      out.print(" ");
      out.println(purple("[<command>]"));
      out.println(green("     /* Shows this help or help for a specific command."));
      out.println(green("      * "));
      out.println(green("      * Example: moar --help status */"));
      out.println();
    });
  }

  private void outInitHelp(String[] args) {
    String command = "init";
    if (!command.matches(filter)) {
      return;
    }
    status.output(out -> {
      out.print(purpleBold(args[0]));
      out.print(" ");
      out.println(cyanBold(command));
      out.println(green("     /* Initalize will clone referenced modules and setup"));
      out.println(green("      * symbolic linking. This supports development"));
      out.println(green("      * environments where the shared module version is"));
      out.println(green("      * used.  The assoicated 'init-' file is updated if"));
      out.println(green("      * the shared module differs from the specified "));
      out.println(green("      * version. */"));
      out.println();
    });
  }

  private void outNestHelp(String[] args) {
    String command = "nest";
    if (!command.matches(filter)) {
      return;
    }
    status.output(out -> {
      out.print(purpleBold(args[0]));
      out.print(" ");
      out.println(cyanBold(command));
      out.println(green("     /* Clone referenced modules in a \"nested\" structure"));
      out.println(green("      * without symbolic linking.  This supports accurate"));
      out.println(green("      * builds because the nested module version will match"));
      out.println(green("      * the state of the assoicated 'init-' file */"));
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

  private void outPushHelp(String[] args) {
    String command = "push";
    if (!command.matches(filter)) {
      return;
    }
    status.output(out -> {
      out.print(purpleBold(args[0]));
      out.print(" ");
      out.print(cyanBold(command));
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

  private void outStatusHelp(String[] args) {
    String command = "status";
    if (!command.matches(filter)) {
      return;
    }
    status.output(out -> {
      out.print(purpleBold(args[0]));
      out.print(" ");
      out.print(cyanBold(command));
      out.print(" ");
      out.println(purple("[<Module Filter RegEx>]"));
      out.println(green("     /* Show status."));
      out.println(green("      * "));
      out.println(green("      * Example: moar status 'group-.*' */"));
      out.println();
    });
  }

}
