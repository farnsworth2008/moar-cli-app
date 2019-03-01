package moar.cli;

import static java.lang.Boolean.FALSE;
import static java.lang.Math.max;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.lang.ThreadLocal.withInitial;
import static java.util.Collections.sort;
import static moar.sugar.Ansi.BLUE;
import static moar.sugar.Ansi.GREEN;
import static moar.sugar.Ansi.PURPLE;
import static moar.sugar.Ansi.RED;
import static moar.sugar.Ansi.blue;
import static moar.sugar.Ansi.cyan;
import static moar.sugar.Ansi.green;
import static moar.sugar.Ansi.purple;
import static moar.sugar.Ansi.red;
import static moar.sugar.MoarStringUtil.fileContentsAsString;
import static moar.sugar.MoarStringUtil.truncate;
import static moar.sugar.Sugar.nonNull;
import static moar.sugar.Sugar.require;
import static moar.sugar.thread.MoarThreadSugar.$;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
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
      b.append(upstreamBranch.get().replaceAll("^fork/", "").replaceAll("^feature/", ""));
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

  private void outputDetail() {
    MoarModule module = this.module.get();
    if (outputDetailLines(BLUE, module.getUncommitedFiles())) {
      outputShortStat(BLUE, "git diff --shortstat HEAD");
    }
    module.getUpstreamBranch();
    if (outputDetailLines(GREEN, module.getAheadCommits())) {
      outputShortStat(GREEN, "git diff --shortstat %s..");
    }
    if (outputDetailLines(RED, module.getBehindCommits())) {
      outputShortStat(RED, "git diff --shortstat ..%s");
    }
    if (outputDetailLines(GREEN, module.getAheadOriginCommits())) {
      outputShortStat(GREEN, "git diff --shortstat origin/develop..%s");
    }
    if (outputDetailLines(RED, module.getBehindOriginCommits())) {
      outputShortStat(RED, "git diff --shortstat %s..origin/develop");
    }
    if (outputDetailLines(PURPLE, module.getBehindMasterCommits())) {
      outputShortStat(PURPLE, "git diff --shortstat origin/develop..origin/master");
    }
    out.println();
  }

  private Boolean outputDetailLines(Ansi color, List<String> lines) {
    var i = 0;
    for (var line : lines) {
      var lineNumber = ++i;
      var formattedLine = format("%s - %s", color.apply(format("%6d", lineNumber)), color.apply(line));
      formattedLine = truncate(formattedLine, 100, "...");
      out.println(color.apply(formattedLine));
    }
    return !lines.isEmpty();
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
      b.append(purple(behindMasterCount.get()));
    }
    out.println(b.toString());
  }

  private void outputShortStat(Ansi color, String command) {
    var pad = "       === ";
    MoarModule module = this.module.get();
    String branch = module.getUpstreamBranch();
    String output = module.execCommand(format(command, branch)).get().getOutput();
    var summary = output.strip().replaceAll("( changed| insertions| deletions)", "");
    out.println(color.apply(pad + summary));
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
    String filterArg = args.length > 1 ? args[1] : "";
    String filter = filterArg.isEmpty() ? ".*" : filterArg;
    Boolean detail = args.length > 2 ? args[2].equals("--detail") : FALSE;

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
        if (detail) {
          outputDetail();
        }
      }
    }
  }

}
