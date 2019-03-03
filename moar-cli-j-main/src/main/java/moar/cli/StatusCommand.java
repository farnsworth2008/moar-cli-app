package moar.cli;

import static java.lang.Boolean.FALSE;
import moar.sugar.MoarException;

public class StatusCommand
    extends
    BaseStatusCommand {

  @Override
  void doRun(String[] args) {
    var filter = "";
    var argNum = 0;
    for (var i = 2; i < args.length; i++) {
      String arg = args[i];
      ++argNum;
      if (argNum == 1) {
        filter = arg;
      } else {
        throw new MoarException("Unexpected Arg");
      }
    }
    var defaultFilter = ".*";
    if (filter.isEmpty()) {
      filter = defaultFilter;
    }
    doStatus(filter, FALSE);
  }

}
