package moar.cli;

import static java.lang.String.format;
import static moar.ansi.Ansi.cyanBold;
import static moar.sugar.Sugar.require;
import static moar.sugar.thread.MoarThreadSugar.$;
import java.util.HashMap;
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

    var map = new HashMap<String, String>();

    var status = new StatusLine(out, format("%s %s", filter, "Scanning"));
    var after = new Vector<Runnable>();
    var futures = $();
    for (var module : modules) {
      $(async, futures, () -> {
        String name = module.getName();
        boolean filterMatches = name.matches(filter);
        boolean ignoreMatches = name.matches(ignore);
        if (filterMatches && !ignoreMatches) {
          SafeResult<ExecuteResult> result = module.execCommand(command);
          String output = result.threw() ? result.thrown().getMessage() : result.get().getOutput();
          after.add(() -> {
            map.put(module.getName(), output);
          });
          status.set(() -> (float) after.size() / modules.size(), name);
        }
      });
    }
    $(futures);
    for (var task : after) {
      task.run();
    }
    status.clear();

    for (var module : modules) {
      String name = module.getName();
      boolean filterMatches = name.matches(filter);
      boolean ignoreMatches = name.matches(ignore);
      if (filterMatches && !ignoreMatches) {
        String output = map.get(module.getName());
        if (!output.strip().isEmpty()) {
          out.println(cyanBold(module.getName()));
          out.println(output);
          out.println();
        }
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

}
