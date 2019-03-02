package moar.cli;

import static java.lang.Boolean.FALSE;
import static java.lang.Math.max;
import static java.lang.String.format;
import static java.lang.ThreadLocal.withInitial;
import static moar.ansi.Ansi.BLUE;
import static moar.ansi.Ansi.GREEN;
import static moar.ansi.Ansi.PURPLE;
import static moar.ansi.Ansi.RED;
import static moar.ansi.Ansi.blue;
import static moar.ansi.Ansi.cyan;
import static moar.ansi.Ansi.green;
import static moar.ansi.Ansi.purple;
import static moar.ansi.Ansi.red;
import static moar.sugar.MoarStringUtil.truncate;
import static moar.sugar.Sugar.require;
import static moar.sugar.thread.MoarThreadSugar.$;
import java.io.File;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import moar.ansi.Ansi;
import moar.ansi.StatusLine;

public abstract class BaseStatusCommand
    extends
    BaseCommand {

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

  protected void doStatus(String filter, Boolean detail) {
    currentModuleDir.set(getCurrentModuleDir());

    var modules = getModules();

    String ignore = getIgnoreRegEx();
    AtomicInteger maxLineLen = new AtomicInteger();

    var progress = new StatusLine(out, "Scanning");
    require(() -> {
      var after = new Vector<Runnable>();
      var futures = $();
      try (var scanAsync = $(4)) {
        for (var module : modules) {
          $(scanAsync, futures, () -> {
            String name = module.getName();
            boolean filterMatches = name.matches(filter);
            boolean ignoreMatches = name.matches(ignore);
            if (filterMatches && !ignoreMatches) {
              this.module.set(module);
              processModule();
              Integer lineLen = getLineLen();
              after.add(() -> {
                maxLineLen.set(max(maxLineLen.get(), lineLen));
              });
              progress.set(() -> (float) after.size() / modules.size());
            }
          });
        }
        $(futures);
      }
      for (var task : after) {
        task.run();
      }
    });
    progress.clear();

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
    MoarModule module = this.module.get();
    moduleName.set(module.getName());
    upstreamBranch.set(module.getUpstreamBranch());
    require(() -> {
      var futures = $(Integer.class);
      var uncommitedFuture = $(async, futures, () -> module.getUncommitedCount());
      var aheadFuture = $(async, futures, () -> module.getAheadCount());
      var behindFuture = $(async, futures, () -> module.getBehindCount());
      var aheadOriginFuture = $(async, futures, () -> module.getAheadOriginCount());
      var behindOriginFuture = $(async, futures, () -> module.getBehindOriginCount());
      var behindMasterFuture = $(async, futures, () -> module.getBehindMasterCount());
      uncommitedCount.set(uncommitedFuture.get());
      aheadCount.set(aheadFuture.get());
      behindCount.set(behindFuture.get());
      aheadOriginCount.set(aheadOriginFuture.get());
      behindOriginCount.set(behindOriginFuture.get());
      behindMasterCount.set(behindMasterFuture.get());
    });
  }

}