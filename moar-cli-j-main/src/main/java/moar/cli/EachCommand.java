package moar.cli;

import static java.lang.System.getProperty;
import static java.util.Collections.sort;
import static moar.ansi.Ansi.cyan;
import static moar.sugar.MoarStringUtil.fileContentsAsString;
import static moar.sugar.Sugar.nonNull;
import static moar.sugar.Sugar.require;
import static moar.sugar.thread.MoarThreadSugar.$;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import moar.ansi.Ansi;
import moar.sugar.ExecuteResult;
import moar.sugar.SafeResult;

public class EachCommand {

  private final PrintStream out;

  public EachCommand(PrintStream out) {
    this.out = out;
  }

  private ArrayList<MoarModule> getModules(File workspace) {
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

  private File getWorkspace() {
    var workspace = new File(getProperty("moar.workspace", getProperty("user.home") + "/moar-workspace"));
    workspace.mkdirs();
    return workspace;
  }

  void run(String... args) {
    String filter = args[1];
    String command = args[2];

    var workspace = getWorkspace();
    var modules = getModules(workspace);

    File ignoreFile = new File(workspace, ".ignore");
    String ignore = nonNull(fileContentsAsString(ignoreFile).strip(), "^$");

    var map = new HashMap<String, String>();

    require(() -> {
      var progress = Ansi.progress(out, "Processing");
      var after = new Vector<Runnable>();
      try (var async = $(4)) {
        var futures = $();
        for (var module : modules) {
          $(async, futures, () -> {
            String name = module.getName();
            boolean filterMatches = name.matches(filter);
            boolean ignoreMatches = name.matches(ignore);
            if (filterMatches && !ignoreMatches) {
              SafeResult<ExecuteResult> result = module.execCommand(command);
              String output = result.threw() ? result.thrown().getMessage() : result.get().getOutput();
              after.add(() -> {
                map.put(module.getName(), output);
              });
              synchronized (progress) {
                progress.set((float) after.size() / modules.size());
              }
            }
          });
        }
        $(futures);
        for (var task : after) {
          task.run();
        }
        progress.clear();
      }

      for (var module : modules) {
        String name = module.getName();
        boolean filterMatches = name.matches(filter);
        boolean ignoreMatches = name.matches(ignore);
        if (filterMatches && !ignoreMatches) {
          String output = map.get(module.getName());
          if (!output.strip().isEmpty()) {
            out.println(cyan(module.getName()));
            out.println(output);
            out.println();
          }
        }
      }
    });
  }

}
