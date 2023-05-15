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

  public static void main(String[] argv) throws IOException {
    ExecutionOptions options = new ExecutionOptions(
            true, System.out, System.err);
    execute(options, argv);
  }

  /** Execute the program using the specified command-line options. */
  public static int execute(
          ExecutionOptions options,
          String... argv) throws IOException {
    options.setBinaryName("slt");
    NoExecutor.register(options);
    HsqldbExecutor.register(options);
    PostgresExecutor.register(options);
    int parse = options.parse(argv);
    if (parse != 0) {
      return parse;
    }

    Set<String> allTests =
        new Reflections("test", Scanners.Resources).getResources(".*\\.test");
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
    options.out.println("Files that could not be not parsed: "
            + loader.fileParseErrors);
    loader.statistics.printStatistics(options.out);
    return 0;
  }
}

// End Main.java
