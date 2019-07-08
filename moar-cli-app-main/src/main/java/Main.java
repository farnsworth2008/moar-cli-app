
import static java.lang.String.format;
import static java.lang.System.out;
import static moar.ansi.Ansi.cyanBold;
import static moar.ansi.Ansi.red;
import static moar.cli.BaseCommand.getCommand;
import static moar.cli.BaseCommand.getCommandClasses;
import static moar.sugar.thread.MoarThreadSugar.$;
import moar.cli.HelpCommand;

@SuppressWarnings({ "rawtypes" })
public class Main {

  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      String[] defaultArgs = { "help" };
      args = defaultArgs;
    }
    try (var async = $(100)) {
      Class[] commands = getCommandClasses();
      for (var commandClz : commands) {
        var command = getCommand(commandClz);
        if (command.run(async, out, args)) {
          return;
        }
      }
      out.println(format("Command \"%s\" %s", cyanBold(args[1]), red("not found!")));
      out.println();
      args[1] = "--help";
      new HelpCommand().run(async, out, args);
    }
  }
}
