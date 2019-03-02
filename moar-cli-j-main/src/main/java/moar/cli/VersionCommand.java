package moar.cli;

import static moar.sugar.Sugar.nonNull;

public class VersionCommand
    extends
    BaseCommand {

  @Override
  Boolean accept(String command) {
    return command.equals("--version");
  }

  @Override
  void doRun(String[] args) {
    String specificationVersion = VersionCommand.class.getPackage().getSpecificationVersion();
    out.println(nonNull(specificationVersion, "unknown"));
  }

  @Override
  boolean includeInCommandNames() {
    return false;
  }

}
