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

import net.hydromatic.sqllogictest.executors.HsqldbExecutor;
import net.hydromatic.sqllogictest.executors.NoExecutor;
import net.hydromatic.sqllogictest.executors.PostgresExecutor;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.io.IOException;
import java.util.Set;

/**
 * Execute all SqlLogicTest tests.
 */
public class Main {
  private Main() {}

  /** Command-line entry point. */
  public static void main(String[] args) throws IOException {
    OptionsParser optionParser =
        new OptionsParser(true, System.out, System.err);
    execute(optionParser, args);
  }

  /**
   * Get the list of all test files.
   */
  public static Set<String> getTestList() {
    return new Reflections("test", Scanners.Resources).getResources(".*\\.test");
  }

  /**
   * Execute the program using the specified command-line options.
   * @param optionParser  Parser that will be used to parse the command-line
   *                      options.
   * @param args          Command-line options.
   * @return              A description of the outcome of the tests.  null when
   *                      tests cannot even be started.
   */
  public static TestStatistics execute(OptionsParser optionParser,
      String... args) throws IOException {
    optionParser.setBinaryName("slt");
    NoExecutor.register(optionParser);
    HsqldbExecutor.register(optionParser);
    PostgresExecutor.register(optionParser);
    OptionsParser.SuppliedOptions options = optionParser.parse(args);
    if (options.exitCode != 0) {
      return null;
    }

    Set<String> allTests = getTestList();
    TestLoader loader = new TestLoader(options);
    for (String testPath : allTests) {
      boolean runTest =
          options.getDirectories().stream().anyMatch(testPath::contains);
      if (!runTest) {
        continue;
      }
      if (!loader.visitFile(testPath)) {
        break;
      }
    }
    return loader.statistics;
  }
}

// End Main.java
