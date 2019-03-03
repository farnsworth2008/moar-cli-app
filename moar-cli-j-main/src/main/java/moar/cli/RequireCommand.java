package moar.cli;

public class RequireCommand
    extends
    InitBaseCommand {

  @Override
  void doModuleCommand(String[] args) {
    doInitCommand(null, true);
  }

}
