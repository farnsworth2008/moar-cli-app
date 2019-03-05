package moar.cli;

import static moar.sugar.Sugar.exec;

public class EclipseCommand
    extends
    ModuleCommand {

  @Override
  void doModuleCommand(String[] args) {
    var command = "./gradlew cleanEclipse eclipse";
    status.set(command);
    exec(command, dir);

  }

}
