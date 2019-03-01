package moar.cli;

import static moar.sugar.Sugar.nonNull;
import java.io.PrintStream;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MoarCliApp
    implements
    CommandLineRunner {
  public final PrintStream out;

  public MoarCliApp(PrintStream out) {
    this.out = out;
  }

  @Override
  public void run(String... args) throws Exception {
    for (String arg : args) {
      if (arg.matches("(--version|-v|version)")) {
        String specificationVersion = MoarCliApp.class.getPackage().getSpecificationVersion();
        out.println(nonNull(specificationVersion, "unknown"));
        return;
      }
    }
    if (args.length == 0 || args[0].equals("status")) {
      new StatusCommand(out).run(args);
      return;
    }
    if (args.length == 0 || args[0].equals("each")) {
      new EachCommand(out).run(args);
      return;
    }
  }

}
