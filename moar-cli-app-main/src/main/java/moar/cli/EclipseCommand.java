package moar.cli;

import static moar.sugar.Sugar.exec;
import moar.ansi.StatusLine;

public class EclipseCommand
    extends
    ModuleCommand {

  @Override
  void doModuleCommand(String[] args) {
    var command = "./gradlew cleanEclipse eclipse";
    var status = new StatusLine(out, command);
    exec(command, getCurrentModuleDir());
    status.clear();
  }

}
