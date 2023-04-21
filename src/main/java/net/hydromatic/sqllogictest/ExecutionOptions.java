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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import net.hydromatic.sqllogictest.executors.SqlSLTTestExecutor;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Extensible command-line parsing.
 * New command-line options can be registered using 'registerOption'.
 */
public class ExecutionOptions {
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
     * True if the argument can be missing.
     */
    final boolean optionalArgument;
    /**
     * Function to execute when option is encountered.
     * Should return 'true' on success.
     */
    final Function<String, Boolean> optionArgProcessor;

    OptionDescription(String option, @Nullable String argName, String description, boolean optionalArgument,
                      Function<String, Boolean> optionArgProcessor) {
      this.option = option;
      this.argName = argName;
      this.description = description;
      this.optionalArgument = optionalArgument;
      this.optionArgProcessor = optionArgProcessor;
    }
  }

  /**
   * All the known options.
   */
  final Map<String, OptionDescription> knownOptions;
  /**
   * Additional information to be printed as help.
   */
  final List<String> additionalUsage;
  /**
   * Order in which the options were registered.
   */
  final List<String> optionOrder;
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
  final Map<String, Supplier<SqlSLTTestExecutor>> executorFactories;
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
    HashSet<String> bugs = new HashSet<>();
    if (this.bugsFile.isEmpty())
      return bugs;
    File file = new File(this.bugsFile);
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      while (true) {
        String line = reader.readLine();
        if (line == null) break;
        if (line.startsWith("//"))
          continue;
        bugs.add(line);
      }
    }
    return bugs;
  }

  void setBinaryName(String binaryName) {
    this.binaryName = binaryName;
  }

  int abort(boolean abort, @Nullable String message) {
    if (message != null)
      System.err.println(message);
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
  JDBCExecutor jdbcExecutor(HashSet<String> sltBugs) {
    JDBCExecutor jdbc = new JDBCExecutor(this.jdbcConnectionString());
    jdbc.avoid(sltBugs);
    jdbc.setValidateStatus(this.validateStatus);
    return jdbc;
  }

  SqlSLTTestExecutor getExecutor() throws IOException, SQLException {
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
      break;
    }
    throw new RuntimeException("Unknown executor: " + this.executor);  // unreachable
  }
   */

  public List<String> getDirectories() {
    if (this.directories.isEmpty())
      this.directories.add(".");  // This means "everything"
    return this.directories;
  }

  public ExecutionOptions(boolean exit) {
    this.knownOptions = new HashMap<>();
    this.additionalUsage = new ArrayList<>();
    this.binaryName = "";
    this.optionOrder = new ArrayList<>();
    this.directories = new ArrayList<>();
    this.executorFactories = new HashMap<>();
    this.exit = exit;
    this.registerDefaultOptions();
  }

  /**
   * Install tests if they don't exist.
   */
  public boolean install = false;
  /**
   * Directory with tests.
   */
  public String directory = "";
  public boolean stopAtFirstError = false;
  public boolean doNotExecute = false;
  public String executor = "";
  public boolean ignoreSqlStatus = false;
  public String bugsFile = "";
  public int verbosity = 0;

  boolean setExecutor(String executor) {
    if (!this.executor.isEmpty()) {
      System.err.println("Executor already set to " + this.executor);
      return false;
    }
    this.executor = executor;
    return true;
  }

  boolean setBugsFile(String filename) {
    if (!this.bugsFile.isEmpty()) {
      System.err.println("Bugs file already set to " + this.bugsFile);
      return false;
    }
    this.bugsFile = filename;
    return true;
  }

  void registerDefaultOptions() {
    this.registerOption("-h", null, "Show this help message and exit",
            false, (o) -> false);
    this.registerOption("-d", "directory", "Directory with SLT tests", false,
            (o) -> { this.directory = o; return true; });
    this.registerOption("-i", null, "Install the SLT tests if the directory does not exist",
            false, (o) -> { this.install = true; return true; });
    this.registerOption("-x", null, "Stop at the first encountered query error",
            false, o -> { this.stopAtFirstError = true; return true; });
    this.registerOption("-n", null, "Do not execute, just parse the test files",
            false, o -> { this.doNotExecute = true; return true; });
    this.registerOption("-e", "executor", "Executor to use",
            false, this::setExecutor);
    this.registerOption("-s", null, "Ignore the status of SQL commands executed",
            false, o -> { this.ignoreSqlStatus = true; return true; });
    this.registerOption("-b", "filename", "Load a list of buggy commands to skip from this file",
            false, this::setBugsFile);
    this.registerOption("-v", null, "Increase verbosity",
            false, o -> { this.verbosity++; return true; });
  }

  public void registerExecutor(String executorName, Supplier<SqlSLTTestExecutor> executor) {
    if (this.executorFactories.containsKey(executorName)) {
      throw new RuntimeException("Executor for " + Utilities.singleQuote(executorName) + " already registered");
    }
    this.executorFactories.put(executorName, executor);
  }

  @Nullable
  public SqlSLTTestExecutor getExecutor() {
    if (this.executor == null || this.executor.isEmpty()) {
      this.abort("Please supply an executor name using the -e flag");
      return null;
    }
    Supplier<SqlSLTTestExecutor> supplier = this.executorFactories.get(this.executor);
    if (supplier == null) {
      System.err.println("Executor for " + Utilities.singleQuote(this.executor)
              + " not registered using 'registerExecutor");
      System.err.println("Registered executors:");
      for (String s: this.executorFactories.keySet()) {
        System.err.println("\t" + s);
      }
      this.abort(null);
      return null;
    }
    return supplier.get();
  }

  public void registerOption(String option, @Nullable String argName, String description,
                      boolean optionalArgument, Function<String, Boolean> optionArgProcessor) {
    OptionDescription o = new OptionDescription(option, argName, description, optionalArgument, optionArgProcessor);
    if (this.knownOptions.containsKey(option)) {
      this.error("Option " + Utilities.singleQuote(option) + " already registered");
      return;
    }
    this.optionOrder.add(option);
    this.knownOptions.put(option, o);
  }

  void registerUsage(String usage) {
    this.additionalUsage.add(usage);
  }
  
  void error(String message) {
    System.err.println(message);
  }

  public void error(Throwable ex) {
    System.err.println("EXCEPTION: " + ex.getMessage());
  }

  /**
   * Report a message to the user.
   * @param message     Message to report.
   * @param importance  Importance.  Higher means less important.
   */
  public void message(String message, int importance) {
    if (this.verbosity >= importance)
      System.out.println(message);
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
        if ((option.optionalArgument) && (arg == null || arg.isEmpty()))
          arg = null;
      }

      if (option == null) {
        this.directories.add(opt);
      } else {
        if (option.argName != null && arg == null && !(option.optionalArgument)) {
          if (i == argv.length - 1) {
            return this.abort("Option " + Utilities.singleQuote(opt) +
                    " is missing required argument " + option.argName);
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
    System.out.println(this.binaryName + " [options] files_or_directories_with_tests");
    System.out.println("Executes the SQL Logic Tests using a SQL execution engine");
    System.out.println("See https://www.sqlite.org/sqllogictest/doc/trunk/about.wiki");
    System.out.println("Options:");

    int labelLen = 0;
    for (String o : this.optionOrder) {
      int len = o.length();
      OptionDescription option = this.knownOptions.get(o);
      if (option.argName != null) len += 1 + option.argName.length();
      if (labelLen < len) labelLen = len;
    }

    labelLen += 3;
    for (String o : this.optionOrder) {
      OptionDescription option = this.knownOptions.get(o);
      int len = o.length();
      System.out.print(option.option);
      if (option.argName != null) {
        if (option.optionalArgument) {
          System.out.print("[=" + option.argName + "]");
          len += 3 + option.argName.length();
        } else {
          System.out.print(" " + option.argName);
          len += 1 + option.argName.length();
        }
      }

      for (String line: option.description.split("\n")) {
        for (int i = 0; i < labelLen - len; i++)
          System.out.print(" ");
        System.out.println(line);
        len = 0;
      }
    }

    System.out.println("Registered executors:");
    for (String e: this.executorFactories.keySet()) {
      System.out.println("\t" + e);
    }

    for (String m : this.additionalUsage)
      System.out.println(m);
  }
}
