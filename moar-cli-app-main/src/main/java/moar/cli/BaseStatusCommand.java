package moar.cli;

import static java.lang.Boolean.FALSE;
import static java.lang.Math.max;
import static java.lang.String.format;
import static moar.ansi.Ansi.GREEN;
import static moar.ansi.Ansi.GREEN_UNDERLINED;
import static moar.ansi.Ansi.PURPLE;
import static moar.ansi.Ansi.PURPLE_UNDERLINED;
import static moar.ansi.Ansi.RED;
import static moar.ansi.Ansi.RED_UNDERLINED;
import static moar.ansi.Ansi.cyanBold;
import static moar.ansi.Ansi.green;
import static moar.ansi.Ansi.purple;
import static moar.ansi.Ansi.red;
import static moar.sugar.MoarStringUtil.middleTruncate;
import static moar.sugar.thread.MoarThreadSugar.$;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import moar.ansi.Ansi;

public abstract class BaseStatusCommand
    extends
    BaseCommand {

  protected void doStatus(String filter, Boolean detail) {
    var a = async;
    var f = futures;
    var s = status;

    String ignore = getIgnoreRegEx();
    AtomicInteger maxLineLen = new AtomicInteger();

    var step1 = new Vector<Runnable>();
    var step2 = new Vector<Runnable>();
    var modules = getModules();
    for (var module : modules) {
      String name = module.getName();
      boolean filterMatches = name.matches(filter);
      boolean ignoreMatches = name.matches(ignore);
      if (filterMatches && !ignoreMatches) {
        var m = module;
        $(a, f, () -> s.completeOne(() -> m.getBranch()));
        $(a, f, () -> s.completeOne(() -> m.getUncommitedFiles().size()));
        $(a, f, () -> s.completeOne(() -> m.getAheadCommits()));
        $(a, f, () -> s.completeOne(() -> m.getBehindCommits()));
        $(a, f, () -> s.completeOne(() -> m.getAheadOriginCommits()));
        $(a, f, () -> s.completeOne(() -> m.getBehindOriginCommits()));
        $(a, f, () -> s.completeOne(() -> m.getBehindMasterCommits()));
        step1.add(() -> {
          Integer lineLen = getLineLen(module);
          maxLineLen.set(max(maxLineLen.get(), lineLen));
          step2.add(() -> {
            outLine(module, maxLineLen.get());
            if (detail) {
              outDetail(module);
            }
          });
        });
      }
    }
    completeAsyncTasks(format("Scan: %s", filter));
    s.setCount(step1.size(), "Sizing");
    for (var task : step1) {
      task.run();
      status.completeOne();
    }
    status.setCount(step2.size(), "Rendering");
    for (var task : step2) {
      task.run();
      status.completeOne();
    }
  }

  private String getFormattedLine(MoarModule module, Boolean ansi, Integer padding) {
    Boolean priorEnabled = Ansi.enabled(ansi);
    try {
      var b = new StringBuilder();
      if (module.getDir().equals(dir)) {
        b.append(cyanBold(module.getName()));
      } else {
        b.append(module.getName());
      }
      b.append(" ");

      Integer uncommitedCount = module.getUncommitedFiles().size();
      if (uncommitedCount != 0) {
        b.append(purple(uncommitedCount));
        b.append(" ");
      }
      Integer aheadCount = module.getAheadCommits().size();
      Integer behindCount = module.getBehindCommits().size();
      if (aheadCount != 0 || behindCount != 0) {
        b.append("(");
        b.append(green(aheadCount));
        if (behindCount != 0) {
          b.append("/");
          b.append(red(behindCount));
        }
        b.append(") ");
      }
      b.append("-".repeat(padding) + "-> ");
      String upstream = module.getUpstreamBranch();
      upstream = upstream.replaceAll("^fork/", "");
      upstream = upstream.replaceAll("^feature/", "");
      b.append(upstream);
      return b.toString();
    } finally {
      Ansi.enabled(priorEnabled);
    }
  }

  private Integer getLineLen(MoarModule module) {
    return getFormattedLine(module, FALSE, 0).length();
  }

  private void outAheadDetail(String branch, MoarModule module) {
    status.output(out -> {
      out.print("     ");
      var command = format("%s..", branch);
      out.println(GREEN_UNDERLINED.apply(command));
      if (outDetailLines(GREEN, module.getAheadCommits())) {
        outShortStat(module, GREEN, command);
      }
    });
  }

  private void outAheadOriginDetail(String branch, MoarModule module) {
    status.output(out -> {
      out.print("     ");
      var command = format("origin/develop..%s", branch);
      out.println(GREEN_UNDERLINED.apply(command));
      if (outDetailLines(GREEN, module.getAheadOriginCommits())) {
        outShortStat(module, GREEN, command);
      }
    });
  }

  private void outBehindDetail(String branch, MoarModule module) {
    status.output(out -> {
      out.print("     ");
      var command = format("..%s", branch);
      out.println(RED_UNDERLINED.apply(command));
      if (outDetailLines(RED, module.getBehindCommits())) {
        outShortStat(module, RED, command);
      }
    });
  }

  private void outBehindMasterDetail(String branch, MoarModule module) {
    status.output(out -> {
      out.print("     ");
      var command = format("origin/develop..origin/master", branch);
      out.println(PURPLE_UNDERLINED.apply(command));
      if (outDetailLines(PURPLE, module.getBehindMasterCommits())) {
        outShortStat(module, PURPLE, command);
      }
    });
  }

  private void outBehindOriginDetail(String branch, MoarModule module) {
    status.output(out -> {
      out.print("     ");
      var command = format("%s..origin/develop", branch);
      out.println(RED_UNDERLINED.apply(command));
      if (outDetailLines(RED, module.getBehindOriginCommits())) {
        outShortStat(module, RED, command);
      }
    });
  }

  private void outDetail(MoarModule module) {
    status.output(out -> {
      String branch = module.getUpstreamBranch();
      if (module.getUncommitedFiles().size() > 0) {
        outUncommitedDetail(module);
      }
      if (module.getAheadCommits().size() > 0) {
        outAheadDetail(branch, module);
      }
      if (module.getBehindCommits().size() > 0) {
        outBehindDetail(branch, module);
      }
      if (module.getAheadOriginCommits().size() > 0) {
        outAheadOriginDetail(branch, module);
      }
      if (module.getBehindOriginCommits().size() > 0) {
        outBehindOriginDetail(branch, module);
      }
      if (module.getBehindMasterCommits().size() > 0) {
        outBehindMasterDetail(branch, module);
      }
      out.println();
    });
  }

  private Boolean outDetailLines(Ansi color, List<String> lines) {
    return status.output(out -> {
      var i = 0;
      for (var line : lines) {
        var lineNumber = ++i;
        line = middleTruncate(line, 6, 80, "...");
        var formattedLine = format("%s - %s", format("%6d", lineNumber), line);
        out.println(color.apply(formattedLine));
      }
      return !lines.isEmpty();
    });
  }

  private void outLine(MoarModule module, Integer maxLineLen) {
    status.output(out -> {
      Integer lineLen = getLineLen(module);
      StringBuilder b = new StringBuilder();
      b.append(getFormattedLine(module, null, maxLineLen - lineLen));
      b.append(" ");
      Integer aheadOriginCount = module.getAheadOriginCommits().size();
      Integer behindOriginCount = module.getBehindOriginCommits().size();
      Integer behindMasterCount = module.getBehindMasterCommits().size();
      if (aheadOriginCount != 0 || behindOriginCount != 0) {
        b.append("(");
        b.append(green(aheadOriginCount));
        if (behindOriginCount != 0) {
          b.append("/");
          b.append(red(behindOriginCount));
        }
        b.append(") ");
      }
      if (behindMasterCount != 0) {
        b.append(purple(behindMasterCount));
      }
      out.println(b.toString());
    });
  }

  private void outShortStat(MoarModule module, Ansi color, String command) {
    status.output(out -> {
      var pad = "       === ";
      String output = module.execCommand("git diff --shortstat " + command).get().getOutput();
      var summary = output.strip().replaceAll("( changed| insertions| deletions)", "");
      out.println(color.apply(pad + summary));
    });
  }

  private void outUncommitedDetail(MoarModule module) {
    status.output(out -> {
      out.print("     ");
      var command = "..HEAD";
      out.println(PURPLE_UNDERLINED.apply(command));
      if (outDetailLines(PURPLE, module.getUncommitedFiles())) {
        outShortStat(module, PURPLE, "HEAD");
      }
    });
  }
}