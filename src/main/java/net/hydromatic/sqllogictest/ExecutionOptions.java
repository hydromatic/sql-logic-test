/*
 * Copyright 2022 VMware, Inc.
 * SPDX-License-Identifier: MIT
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package net.hydromatic.sqllogictest;

import net.hydromatic.sqllogictest.executors.SqlSltTestExecutor;

import net.hydromatic.sqllogictest.util.Utilities;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Extensible command-line parsing.
 * New command-line options can be registered using 'registerOption'.
 */
public class ExecutionOptions {
  /**
   * All the known options.
   */
  final Map<String, OptionDescription> knownOptions;
  /**
   * Order in which the options were registered.
   */
  final List<String> optionOrder;
  public final PrintStream out;
  public final PrintStream err;
  /**
   * Name of the current binary.
   */
  String binaryName;
  /**
   * List of files/directories with tests.
   */
  final List<String> directories;
  /**
   * For each executor name a factory which knows how to produce the executor.
   */
  final Map<String, Supplier<SqlSltTestExecutor>> executorFactories;
  /**
   * If true call System.exit on error.
   */
  final boolean exit;

  /**
   * Read the list of statements and queries to skip from a file.
   * The file can contain comments on lines starting with //
   * Everything else is interpreted as a one-line statement (or query).
   */
  public Set<String> readBugsFile() throws IOException {
    Set<String> bugs = new HashSet<>();
    if (this.bugsFile.isEmpty()) {
      return bugs;
    }
    File file = new File(this.bugsFile);
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      while (true) {
        String line = reader.readLine();
        if (line == null) {
          break;
        }
        if (line.startsWith("//")) {
          continue;
        }
        bugs.add(line);
      }
    }
    return bugs;
  }

  void setBinaryName(String binaryName) {
    this.binaryName = binaryName;
  }

  int abort(boolean abort, @Nullable String message) {
    if (message != null) {
      err.println(message);
    }
    this.usage();
    if (abort) {
      System.exit(1);
    }
    return 1;
  }

  private int abort(@Nullable String message) {
    return this.abort(this.exit, message);
  }

/*
  JdbcExecutor jdbcExecutor(HashSet<String> sltBugs) {
    JdbcExecutor jdbc = new JdbcExecutor(this.jdbcConnectionString());
    jdbc.avoid(sltBugs);
    jdbc.setValidateStatus(this.validateStatus);
    return jdbc;
  }

  SqlSltTestExecutor getExecutor() throws IOException, SQLException {
    HashSet<String> sltBugs = new HashSet<>();
    if (this.bugsFile != null) {
      sltBugs = this.readBugsFile(this.bugsFile);
    }

    switch (this.executor) {
    case "none":
      return new NoExecutor();
    case "JDBC": {
      return this.jdbcExecutor(sltBugs);
    }
    default:
      // unreachable
      throw new RuntimeException("Unknown executor: " + this.executor);
    }
  }
*/

  public List<String> getDirectories() {
    if (this.directories.isEmpty()) {
      this.directories.add(".");  // This means "everything"
    }
    return this.directories;
  }

  public ExecutionOptions(boolean exit, PrintStream out, PrintStream err) {
    this.exit = exit;
    this.out = out;
    this.err = err;
    this.knownOptions = new HashMap<>();
    this.binaryName = "";
    this.optionOrder = new ArrayList<>();
    this.directories = new ArrayList<>();
    this.executorFactories = new HashMap<>();
    this.registerDefaultOptions();
  }

  public boolean stopAtFirstError = false;
  public boolean doNotExecute = false;
  public String executor = "";
  public String bugsFile = "";
  public int verbosity = 0;

  boolean setExecutor(String executor) {
    if (!this.executor.isEmpty()) {
      err.println("Executor already set to " + this.executor);
      return false;
    }
    this.executor = executor;
    return true;
  }

  boolean setBugsFile(String filename) {
    if (!this.bugsFile.isEmpty()) {
      err.println("Bugs file already set to " + this.bugsFile);
      return false;
    }
    this.bugsFile = filename;
    return true;
  }

  void registerDefaultOptions() {
    this.registerOption("-h", null, "Show this help message and exit",
        o -> false);
    this.registerOption("-x", null, "Stop at the first encountered query error",
        o -> {
          this.stopAtFirstError = true;
          return true;
        });
    this.registerOption("-n", null, "Do not execute, just parse the test files",
        o -> {
          this.doNotExecute = true;
          return true;
        });
    this.registerOption("-e", "executor", "Executor to use", this::setExecutor);
    this.registerOption("-b", "filename",
        "Load a list of buggy commands to skip from this file",
        this::setBugsFile);
    this.registerOption("-v", null, "Increase verbosity",
        o -> {
          this.verbosity++;
          return true;
        });
  }

  public void registerExecutor(String executorName,
        Supplier<SqlSltTestExecutor> executor) {
    if (this.executorFactories.containsKey(executorName)) {
      throw new RuntimeException("Executor for "
          + Utilities.singleQuote(executorName) + " already registered");
    }
    this.executorFactories.put(executorName, executor);
  }

  public @Nullable SqlSltTestExecutor getExecutor() {
    if (this.executor == null || this.executor.isEmpty()) {
      this.abort("Please supply an executor name using the -e flag");
      return null;
    }
    Supplier<SqlSltTestExecutor> supplier = executorFactories.get(this.executor);
    if (supplier == null) {
      err.println("Executor for " + Utilities.singleQuote(this.executor)
          + " not registered using 'registerExecutor");
      err.println("Registered executors:");
      for (String s: this.executorFactories.keySet()) {
        err.println("\t" + s);
      }
      this.abort(null);
      return null;
    }
    return supplier.get();
  }

  public void registerOption(String option, @Nullable String argName,
      String description, Function<String, Boolean> optionArgProcessor) {
    OptionDescription o =
        new OptionDescription(option, argName, description, optionArgProcessor);
    if (this.knownOptions.containsKey(option)) {
      this.error("Option " + Utilities.singleQuote(option)
          + " already registered");
      return;
    }
    this.optionOrder.add(option);
    this.knownOptions.put(option, o);
  }

  void error(String message) {
    err.println(message);
  }

  public void error(Throwable ex) {
    err.println("EXCEPTION: " + ex.getMessage());
  }

  /**
   * Report a message to the user.
   * @param message     Message to report.
   * @param importance  Importance.  Higher means less important.
   */
  public void message(String message, int importance) {
    if (this.verbosity >= importance) {
      out.println(message);
    }
  }

  /**
   * Parse command-line arguments.
   * Returns 0 on success.
   */
  int parse(String... argv) {
    if (argv.length == 0) {
      return this.abort("No arguments to process");
    }

    for (int i = 0; i < argv.length; i++) {
      String opt = argv[i];
      String arg = null;
      OptionDescription option = null;

      if (opt.startsWith("--")) {
        option = this.knownOptions.get(opt);
        if (option == null) {
          return this.abort("Unknown option " + Utilities.singleQuote(opt));
        }
      } else if (opt.startsWith("-")) {
        // Support GCC-style long options that begin with a single '-'.
        option = this.knownOptions.get(opt);
        // If there's no such option, try single-character options.
        if (option == null && opt.length() > 2) {
          arg = opt.substring(2);
          opt = opt.substring(0, 2);
          option = this.knownOptions.get(opt);
        }
        if (option == null) {
          return this.abort("Unknown option " + Utilities.singleQuote(opt));
        }
      }

      if (option == null) {
        this.directories.add(opt);
      } else {
        if (option.argName != null && arg == null) {
          if (i == argv.length - 1) {
            return this.abort("Option " + Utilities.singleQuote(opt)
                + " is missing required argument " + option.argName);
          }
          arg = argv[++i];
        }
        boolean success = option.optionArgProcessor.apply(arg);
        if (!success) {
          return this.abort(null);
        }
      }
    }
    return 0;
  }

  void usage() {
    out.println(this.binaryName + " [options] files_or_directories_with_tests");
    out.println("Executes the SQL Logic Tests using a SQL execution engine");
    out.println("See https://www.sqlite.org/sqllogictest/doc/trunk/about.wiki");
    out.println("Options:");

    int labelLen = 0;
    for (String o : this.optionOrder) {
      int len = o.length();
      OptionDescription option = this.knownOptions.get(o);
      if (option.argName != null) {
        len += 1 + option.argName.length();
      }
      if (labelLen < len) {
        labelLen = len;
      }
    }

    labelLen += 3;
    for (String o : this.optionOrder) {
      OptionDescription option = this.knownOptions.get(o);
      int len = o.length();
      out.print(option.option);
      if (option.argName != null) {
        out.print(" " + option.argName);
        len += 1 + option.argName.length();
      }

      for (String line : option.description.split("\n")) {
        for (int i = 0; i < labelLen - len; i++) {
          out.print(" ");
        }
        out.println(line);
        len = 0;
      }
    }

    out.println("Registered executors:");
    for (String e : this.executorFactories.keySet()) {
      out.println("\t" + e);
    }
  }

  @Override public String toString() {
    return "ExecutionOptions{"
        + "tests=" + this.directories
        + ", execute=" + !this.doNotExecute
        + ", executor=" + this.executor
        + ", stopAtFirstError=" + this.stopAtFirstError
        + '}';
  }

  /**
   * Description of a legal command-line option.
   */
  static class OptionDescription {
    /**
     * Option string.
     */
    final String option;
    /**
     * Name of the argument.
     * null if argument is not required.
     */
    final @Nullable String argName;
    /**
     * Human-readable description of the option.
     */
    final String description;
    /**
     * Function to execute when option is encountered.
     * Should return 'true' on success.
     */
    final Function<String, Boolean> optionArgProcessor;

    OptionDescription(String option, @Nullable String argName,
        String description, Function<String, Boolean> optionArgProcessor) {
      this.option = option;
      this.argName = argName;
      this.description = description;
      this.optionArgProcessor = optionArgProcessor;
    }
  }
}
