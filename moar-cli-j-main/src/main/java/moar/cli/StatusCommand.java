package moar.cli;

import static java.lang.Boolean.FALSE;
import static java.lang.Math.max;
import static java.lang.System.getProperty;
import static java.lang.ThreadLocal.withInitial;
import static java.util.Collections.sort;
import static moar.sugar.Ansi.blue;
import static moar.sugar.Ansi.cyan;
import static moar.sugar.Ansi.green;
import static moar.sugar.Ansi.red;
import static moar.sugar.MoarStringUtil.fileContentsAsString;
import static moar.sugar.Sugar.nonNull;
import static moar.sugar.Sugar.require;
import static moar.sugar.thread.MoarThreadSugar.$;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import moar.sugar.Ansi;

public class StatusCommand {

  private final PrintStream out;
  private final ThreadLocal<Integer> behindMasterCount = withInitial(() -> null);
  private final ThreadLocal<Integer> behindOriginCount = withInitial(() -> null);
  private final ThreadLocal<Integer> aheadOriginCount = withInitial(() -> null);
  private final ThreadLocal<Integer> behindCount = withInitial(() -> null);
  private final ThreadLocal<Integer> aheadCount = withInitial(() -> null);
  private final ThreadLocal<Integer> uncommitedCount = withInitial(() -> null);
  private final ThreadLocal<String> upstreamBranch = withInitial(() -> null);
  private final ThreadLocal<String> moduleName = withInitial(() -> null);
  private final ThreadLocal<MoarModule> module = withInitial(() -> null);
  private final ThreadLocal<File> currentModuleDir = withInitial(() -> null);

  public StatusCommand(PrintStream out) {
    this.out = out;
  }

  private File getCurrentModule(File workspace) {
    var dir = new File(getProperty("user.dir"));
    while (dir.getParentFile() != null) {
      File parentFile = dir.getParentFile();
      if (parentFile.equals(workspace)) {
        if (new File(dir, ".git").isDirectory()) {
          return dir;
        }
      }
      dir = parentFile;
    }
    return null;
  }

  private String getFormattedLine(Boolean ansi, Integer padding) {
    Boolean priorEnabled = Ansi.enabled(ansi);
    try {
      var b = new StringBuilder();
      if (module.get().getDir().equals(currentModuleDir.get())) {
        b.append(cyan(moduleName.get()));
      } else {
        b.append(moduleName.get());
      }
      b.append(" ");

      if (uncommitedCount.get() != 0) {
        b.append(blue(uncommitedCount.get()));
        b.append(" ");
      }
      if (aheadCount.get() != 0 || behindCount.get() != 0) {
        b.append("(");
        b.append(green(aheadCount.get()));
        if (behindCount.get() != 0) {
          b.append("/");
          b.append(red(behindCount.get()));
        }
        b.append(") ");
      }
      b.append("-".repeat(padding) + "-> ");
      b.append(upstreamBranch.get());
      return b.toString();
    } finally {
      Ansi.enabled(priorEnabled);
    }
  }

  private Integer getLineLen() {
    return getFormattedLine(FALSE, 0).length();
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

  private void outputLine(Integer maxLineLen) {
    Integer lineLen = getLineLen();
    StringBuilder b = new StringBuilder();
    b.append(getFormattedLine(null, maxLineLen - lineLen));
    b.append(" ");
    if (aheadOriginCount.get() != 0 || behindOriginCount.get() != 0) {
      b.append("(");
      b.append(green(aheadOriginCount.get()));
      if (behindOriginCount.get() != 0) {
        b.append("/");
        b.append(red(behindOriginCount.get()));
      }
      b.append(") ");
    }
    if (behindMasterCount.get() != 0) {
      b.append(blue(behindMasterCount.get()));
    }
    out.println(b.toString());
  }

  private void processModule() {
    moduleName.set(module.get().getName());
    upstreamBranch.set(module.get().getUpstreamBranch());
    uncommitedCount.set(module.get().getUncommitedCount());
    aheadCount.set(module.get().getAheadCount());
    behindCount.set(module.get().getBehindCount());
    aheadOriginCount.set(module.get().getAheadOriginCount());
    behindOriginCount.set(module.get().getBehindOriginCount());
    behindMasterCount.set(module.get().getBehindMasterCount());
  }

  void run(String... args) {
    String filter = args.length > 1 ? args[1] : ".*";

    var workspace = getWorkspace();
    var modules = getModules(workspace);
    currentModuleDir.set(getCurrentModule(workspace));

    File ignoreFile = new File(workspace, ".ignore");
    String ignore = nonNull(fileContentsAsString(ignoreFile).strip(), "^$");
    AtomicInteger maxLineLen = new AtomicInteger();

    require(() -> {
      try (var async = $(100)) {
        var afterAll = new Vector<Runnable>();
        var futures = $();
        for (var module : modules) {
          $(async, futures, () -> {
            String name = module.getName();
            boolean filterMatches = name.matches(filter);
            boolean ignoreMatches = name.matches(ignore);
            if (filterMatches && !ignoreMatches) {
              this.module.set(module);
              processModule();
              Integer lineLen = getLineLen();
              afterAll.add(() -> {
                maxLineLen.set(max(maxLineLen.get(), lineLen));
              });
            }
          });
        }
        $(futures);
        for (var task : afterAll) {
          task.run();
        }
      }
    });

    for (var module : modules) {
      String name = module.getName();
      boolean filterMatches = name.matches(filter);
      boolean ignoreMatches = name.matches(ignore);
      if (filterMatches && !ignoreMatches) {
        this.module.set(module);
        processModule();
        outputLine(maxLineLen.get());
      }
    }
  }

}
