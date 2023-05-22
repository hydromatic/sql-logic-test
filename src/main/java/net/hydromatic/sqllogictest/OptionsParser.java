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
 *
 * <p>New command-line options can be registered using
 * {@link #registerOption(String, String, String, Function)}.
 */
public class OptionsParser {
  /**
   * All the known options.
   */
  final Map<String, OptionDescription> knownOptions;
  /**
   * Order in which the options were registered.
   */
  final List<String> optionOrder;
  /**
   * Get the options produced after parsing the command-line options.
   */
  public SuppliedOptions getOptions() {
    return this.options;
  }

  /**
   * Description of the command-line options supplied.
   */
  public final class SuppliedOptions {
    /**
     * Zero if no error occurred, 1 otherwise.
     */
    public int exitCode = 0;
    /**
     * Name of the current binary.
     */
    String binaryName;
    /**
     * List of files/directories with tests.
     */
    final List<String> directories;
    /**
     * Stream used to display messages.
     */
    public final PrintStream out;
    /**
     * Stream used to display errors.
     */
    public final PrintStream err;

    SuppliedOptions(boolean exit, PrintStream out, PrintStream err) {
      this.exit = exit;
      this.executorFactories = new HashMap<>();
      this.directories = new ArrayList<>();
      this.out = out;
      this.err = err;
    }

    SuppliedOptions abort(boolean abort, @Nullable String message) {
      this.exitCode = 1;
      if (message != null) {
        err.println(message);
      }
      OptionsParser.this.usage();
      if (abort) {
        System.exit(1);
      }
      return this;
    }

    /**
     * Get the list of directories or files that contain tests to be executed.
     */
    public List<String> getDirectories() {
      if (this.directories.isEmpty()) {
        this.directories.add("test");  // This means "everything"
      }
      return this.directories;
    }
    /**
     * If true execution stops at the first encountered error.
     */
    public boolean stopAtFirstError = false;
    /**
     * If true tests are not executed.
     */
    public boolean doNotExecute = false;
    /**
     * Name of the test executor to invoke.
     */
    public String executor = "";
    /**
     * Optional name of the file that contains statements and queries that are
     * expected to fail.
     */
    public String bugsFile = "";
    /**
     * A higher value causes execution to display more information.
     */
    public int verbosity = 0;
    /**
     * For each executor name a factory which knows how to produce the executor.
     */
    final Map<String, Supplier<SqlSltTestExecutor>> executorFactories;
    /**
     * If true call System.exit on error.
     */
    final boolean exit;

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
     * Register a new executor.
     *
     * @param executorName  Name that identifies the executor as a
     *                      command-line option.
     * @param executor      Executor that can run tests.
     */
    public void registerExecutor(String executorName,
        Supplier<SqlSltTestExecutor> executor) {
      if (this.executorFactories.containsKey(executorName)) {
        throw new RuntimeException("Executor for "
            + Utilities.singleQuote(executorName) + " already registered");
      }
      this.executorFactories.put(executorName, executor);
    }

    private SuppliedOptions abort(@Nullable String message) {
      return this.abort(this.exit, message);
    }

    /**
     * @return The executor associated with the specified name
     * or null if there isn't any such executor.
     */
    public @Nullable SqlSltTestExecutor getExecutorByName(String executor) {
      Supplier<SqlSltTestExecutor> supplier =
              this.executorFactories.get(executor);
      if (supplier == null) {
        err.println("Executor for " + Utilities.singleQuote(executor)
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

    /**
     * Get the executor indicated by the command-line options.
     */
    public @Nullable SqlSltTestExecutor getExecutor() {
      if (this.executor == null || this.executor.isEmpty()) {
        this.abort("Please supply an executor name using the -e flag");
        return null;
      }
      return this.getExecutorByName(this.executor);
    }

    void error(String message) {
      this.err.println(message);
    }

    /**
     * Function used to display an error message on the screen.
     */
    public void error(Throwable ex) {
      this.err.println("EXCEPTION: " + ex.getMessage());
    }

    @Override public String toString() {
      return "Options{"
          + "tests=" + this.directories
          + ", execute=" + !this.doNotExecute
          + ", executor=" + this.executor
          + ", stopAtFirstError=" + this.stopAtFirstError
          + '}';
    }
  }

  /**
   * Set the name displayed for the current program when printing usage.
   */
  public void setBinaryName(String binaryName) {
    this.options.binaryName = binaryName;
  }

  /**
   * The result produced by parsing the command-line options.
   * Constructed beforehand and filled as options are parsed.
   */
  final SuppliedOptions options;

  /**
   * Create a fresh parser for command-line options.
   * Calling 'parse' will actually perform the parsing of the options.
   * @param exit  If true call System.exit on error,
   *              otherwise just return a non-zero value.
   * @param out   Stream used for output.
   * @param err   Stream used for errors.
   */
  public OptionsParser(boolean exit, PrintStream out, PrintStream err) {
    this.knownOptions = new HashMap<>();
    this.options = new SuppliedOptions(exit, out, err);
    this.optionOrder = new ArrayList<>();
    this.registerDefaultOptions();
  }

  void registerDefaultOptions() {
    this.registerOption("-h", null, "Show this help message and exit",
        o -> false);
    this.registerOption("-x", null, "Stop at the first encountered query error",
        o -> {
          this.options.stopAtFirstError = true;
          return true;
        });
    this.registerOption("-n", null, "Do not execute, just parse the test files",
        o -> {
          this.options.doNotExecute = true;
          return true;
        });
    this.registerOption("-e", "executor",
        "Executor to use", this.options::setExecutor);
    this.registerOption("-b", "filename",
        "Load a list of buggy commands to skip from this file",
        this.options::setBugsFile);
    this.registerOption("-v", null, "Increase verbosity",
        o -> {
          this.options.verbosity++;
          return true;
        });
  }

  /**
   * Register a new command-line options.
   *
   * @param option              String used to indicate option on the
   *                            command-line.
   * @param argName             Name of option argument; null if no argument.
   * @param description         Description of this option.
   * @param optionArgProcessor  Function invoked when option is supplied.  The
   *                            function should return 'true' for successful
   *                            option processing, and 'false' on error.
   */
  public void registerOption(String option, @Nullable String argName,
      String description, Function<String, Boolean> optionArgProcessor) {
    OptionDescription o =
        new OptionDescription(option, argName, description, optionArgProcessor);
    if (this.knownOptions.containsKey(option)) {
      this.options.error("Option " + Utilities.singleQuote(option)
          + " already registered");
      return;
    }
    this.optionOrder.add(option);
    this.knownOptions.put(option, o);
  }

  /**
   * Parse command-line arguments.
   *
   * <p>If parsing fails the result has an {@link SuppliedOptions#exitCode}
   * field that is non-zero.
   *
   * @return A structure summarizing the result of parsing.
   */
  public SuppliedOptions parse(String... argv) {
    if (argv.length == 0) {
      return this.options.abort("No arguments to process");
    }

    for (int i = 0; i < argv.length; i++) {
      String opt = argv[i];
      String arg = null;
      OptionDescription option = null;

      if (opt.startsWith("--")) {
        option = this.knownOptions.get(opt);
        if (option == null) {
          return this.options.abort("Unknown option "
              + Utilities.singleQuote(opt));
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
          return this.options.abort("Unknown option "
              + Utilities.singleQuote(opt));
        }
      }

      if (option == null) {
        this.options.directories.add(opt);
      } else {
        if (option.argName != null && arg == null) {
          if (i == argv.length - 1) {
            return this.options.abort("Option " + Utilities.singleQuote(opt)
                + " is missing required argument " + option.argName);
          }
          arg = argv[++i];
        }
        boolean success = option.optionArgProcessor.apply(arg);
        if (!success) {
          return this.options.abort(null);
        }
      }
    }
    return this.options;
  }

  public void registerExecutor(String executorName,
      Supplier<SqlSltTestExecutor> executor) {
    this.options.registerExecutor(executorName, executor);
  }

  void usage() {
    this.options.out.println(this.options.binaryName
        + " [options] files_or_directories_with_tests");
    this.options.out.println(
        "Executes the SQL Logic Tests using a SQL execution engine");
    this.options.out.println(
        "See https://www.sqlite.org/sqllogictest/doc/trunk/about.wiki");
    this.options.out.println("Options:");

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
      this.options.out.print(option.option);
      if (option.argName != null) {
        this.options.out.print(" " + option.argName);
        len += 1 + option.argName.length();
      }

      for (String line : option.description.split("\n")) {
        for (int i = 0; i < labelLen - len; i++) {
          this.options.out.print(" ");
        }
        this.options.out.println(line);
        len = 0;
      }
    }

    this.options.out.println("Registered executors:");
    for (String e : this.options.executorFactories.keySet()) {
      this.options.out.println("\t" + e);
    }
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
