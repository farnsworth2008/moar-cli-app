package moar.cli;

import static java.lang.String.format;
import static java.lang.System.out;
import static moar.ansi.Ansi.cyanBold;
import static moar.ansi.Ansi.green;
import static moar.ansi.Ansi.purple;
import static moar.ansi.Ansi.purpleBold;
import static moar.sugar.Sugar.require;
import static moar.sugar.thread.MoarThreadSugar.$;
import java.util.Vector;
import moar.ansi.StatusLine;
import moar.sugar.ExecuteResult;
import moar.sugar.MoarException;
import moar.sugar.SafeResult;

public class EachCommand
    extends
    BaseCommand {

  private void doEach(String filter, String command) throws Exception {
    var modules = getModules();

    String ignore = getIgnoreRegEx();

    var after = new Vector<Runnable>();
    var status = new StatusLine();
    status.setCount(modules.size(), format("Each: '%s' '%s'", command, filter));
    for (var module : modules) {
      $(async, futures, () -> {
        String name = module.getName();
        boolean filterMatches = name.matches(filter);
        boolean ignoreMatches = name.matches(ignore);
        if (filterMatches && !ignoreMatches) {
          SafeResult<ExecuteResult> result = module.execCommand(command);
          String output = result.threw() ? result.thrown().getMessage() : result.get().getOutput();
          after.add(() -> {
            if (filterMatches && !ignoreMatches) {
              if (!output.strip().isEmpty()) {
                status.output(() -> {
                  out.println(cyanBold(module.getName()));
                  out.println(output);
                  out.println();
                });
              }
            }
          });
          status.completeOne();
        }
      });
    }
    $(futures);
    status.remove();
    if (command.equals("git remote update")) {
      StatusCommand statusCommand = new StatusCommand();
      statusCommand.setAsync(async);
      statusCommand.doStatus(filter, false);
    } else {
      for (var task : after) {
        task.run();
      }
    }
  }

  @Override
  void doRun(String[] args) {
    require(() -> {
      var filter = ".*";
      var command = "git remote update";
      var argNum = 0;
      for (var i = 2; i < args.length; i++) {
        String arg = args[i];
        ++argNum;
        if (argNum == 1) {
          command = arg;
        } else if (argNum == 2) {
          filter = arg;
        } else {
          throw new MoarException("Unexpected Arg");
        }
      }

      doEach(filter, command);
    });
  }

  @Override
  void outHelp() {
    out.print(purpleBold(SCRIPT_NAME));
    out.print(" ");
    out.print(cyanBold(name));
    out.print(" ");
    out.println(purple("<Bash Command> <Module Filter RegEx>"));
    out.println(green(format("     /* Run <Bash Command> in filtered module directories.")));
    out.println(green(format("      * ")));
    out.println(green(format("      * Example: moar %s 'git remote update'", name)));
    out.println(green(format("      * ")));
    out.println(green(format("      * Example: moar %s 'git remote update' 'group-.*' */", name)));
    out.println();
  }

}
