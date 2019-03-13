package moar.cli;

import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.System.getProperty;
import static java.util.Collections.sort;
import static moar.sugar.MoarStringUtil.readStringFromFile;
import static moar.sugar.Sugar.exec;
import static moar.sugar.Sugar.nonNull;
import static moar.sugar.Sugar.require;
import static moar.sugar.thread.MoarThreadSugar.$;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.Future;
import com.google.common.base.CaseFormat;
import moar.ansi.StatusLine;
import moar.sugar.MoarException;
import moar.sugar.thread.MoarAsyncProvider;

public abstract class BaseCommand {
  protected static String SCRIPT_NAME = "MOAR";
  private static final File findModuleDir() {
    var dir = new File(getProperty("user.dir"));
    do {
      if (new File(dir, ".git").isDirectory()) {
        return dir;
      }
      File parentFile = dir.getParentFile();
      dir = parentFile;
    } while (dir.getParentFile() != null);
    return null;
  }

  @SuppressWarnings({ "javadoc", "unchecked", "rawtypes" })
  public static BaseCommand getCommand(Class clz) {
    return require(() -> {
      Constructor constructor = clz.getDeclaredConstructor(new Class<?>[] {});
      Object newInstance = constructor.newInstance(new Object[] {});
      return (BaseCommand) newInstance;
    });
  }
  @SuppressWarnings("rawtypes")
  public static Class[] getCommandClasses() {
    //@formatter:off
    Class[] clzList = {
      AddForkCommand.class,
      AddModuleCommand.class,
      DetailCommand.class,
      EachCommand.class,
      EclipseCommand.class,
      HelpCommand.class,
      NestCommand.class,
      PushCommand.class,
      InitCommand.class,
      StatusCommand.class,
      VersionCommand.class,
    };
    return clzList;
    //@formatter:on
  }

  @SuppressWarnings("rawtypes")
  public static ArrayList<String> getCommandNames() {
    var commandNames = new ArrayList<String>();
    {
      Class[] commands = getCommandClasses();
      for (var commandClz : commands) {
        BaseCommand command = getCommand(commandClz);
        if (command.includeInCommandNames()) {
          commandNames.add(command.getName());
        }
      }
    }
    return commandNames;
  }

  final File dir = findModuleDir();

  StatusLine status;

  final Vector<Future<Object>> futures = $();

  MoarAsyncProvider async;

  final File workspaceDir = findWorkspaceDir();

  final String name;

  public BaseCommand() {
    name = UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, this.getClass().getSimpleName().replaceAll("Command$", ""));
  }

  Boolean accept(String commandName) {
    return getName().startsWith(commandName);
  }

  void completeAsyncTasks(String string) {
    $(futures);
    status.setCount(0, "");
  }

  void doAsyncExec(String command) {
    status.set(command);
    $(async, futures, () -> {
      exec(command, findModuleDir());
      status.completeOne();
    });
  }

  abstract void doRun(String[] args);

  private File findWorkspaceDir() {
    var workspace = new File(getProperty("moar.workspace", getProperty("user.home") + "/moar-workspace"));
    workspace.mkdirs();
    return workspace;
  }

  final String getIgnoreRegEx() {
    var workspace = workspaceDir;
    File ignoreFile = new File(workspace, ".ignore");
    String ignore = nonNull(nonNull(readStringFromFile(ignoreFile), "").strip(), "^$");
    return ignore;
  }

  final ArrayList<MoarModule> getModules() {
    File workspace = workspaceDir;
    var modules = new ArrayList<MoarModule>();
    var dirs = workspace.listFiles(filter -> filter.isDirectory());
    for (var dir : dirs) {
      if (new File(dir, ".git").exists()) {
        modules.add(new MoarModule(dir));
      }
    }
    sort(modules, (o1, o2) -> o1.dir.compareTo(o2.dir));
    return modules;
  }

  public String getName() {
    return name;
  }
  boolean includeInCommandNames() {
    return true;
  }
  abstract void outHelp();

  public final Boolean run(MoarAsyncProvider async, PrintStream out, String[] args) {
    setAsync(async);
    String command = args.length > 1 ? args[1] : "";
    if (accept(command)) {
      setStatus(new StatusLine());
      doRun(args);
      status.clear();
      return TRUE;
    }
    return FALSE;
  }

  public void setAsync(MoarAsyncProvider async) {
    this.async = async;
  }

  public void setStatus(StatusLine status) {
    this.status = status;
  }

  final void verifyCurrentModuleExists() {
    File currentModule = findModuleDir();
    if (currentModule == null) {
      throw new MoarException("Could not find a \".git\" directory");
    }
  }
}
