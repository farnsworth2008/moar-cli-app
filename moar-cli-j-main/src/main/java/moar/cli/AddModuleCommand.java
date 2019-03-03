package moar.cli;

public class AddModuleCommand
    extends
    InitBaseCommand {

  @Override
  void doModuleCommand(String[] args) {
    doInitCommand(args, false);
  }

}
