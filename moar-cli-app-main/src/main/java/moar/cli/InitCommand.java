package moar.cli;

public class InitCommand
    extends
    InitBaseCommand {

  @Override
  void doModuleCommand(String[] args) {
    doInitCommand(null, false);
  }

}
