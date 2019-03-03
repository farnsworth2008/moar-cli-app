package moar.cli;

public abstract class ModuleCommand
    extends
    BaseCommand {

  public ModuleCommand() {
    super();
  }

  abstract void doModuleCommand(String[] args);

  @Override
  protected final void doRun(String[] args) {
    verifyCurrentModuleExists();
    doModuleCommand(args);
  }

}