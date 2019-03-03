package moar.cli;

import static java.lang.String.format;
import static moar.sugar.Sugar.exec;
import static moar.sugar.thread.MoarThreadSugar.$;
import moar.ansi.StatusLine;

public class PushCommand
    extends
    ModuleCommand {

  private void doCommand(StatusLine status, String command) {
    exec(command, getCurrentModuleDir());
    status.completeOne();
  }

  @Override
  void doModuleCommand(String[] args) {
    var status = new StatusLine(out, "Push");
    var futures = $();
    $(async, futures, () -> doCommand(status, "git push"));
    for (var i = 2; i < args.length; i++) {
      String arg = args[i];
      if (arg.equals("master")) {
        $(async, futures, () -> doCommand(status, "git push origin"));
        $(async, futures, () -> doCommand(status, "git push fork HEAD:master"));
        $(async, futures, () -> doCommand(status, "git push origin HEAD:master"));
      } else {
        $(async, futures, () -> doCommand(status, format("git push %s", arg)));
      }
    }
    status.setCount(futures.size());
    status.clear();
  }

}
