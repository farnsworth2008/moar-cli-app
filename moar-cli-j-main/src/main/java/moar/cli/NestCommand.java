package moar.cli;

public class NestCommand
    extends
    InitBaseCommand {

  @Override
  void doModuleCommand(String[] args) {
    doInitCommand(null, true);
  }

}
