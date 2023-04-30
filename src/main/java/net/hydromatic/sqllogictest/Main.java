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

import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Execute all SqlLogicTest tests.
 */
public class Main {
  private Main() {}

  public static void main(String[] argv) throws IOException {
    execute(true, System.out, System.err, argv);
  }

  /** As {@link #main} but does not call {@link System#exit} if {@code exit}
   * is false. */
  public static int execute(boolean exit, PrintStream out, PrintStream err,
      String... argv) throws IOException {
    ExecutionOptions options = new ExecutionOptions(exit, out, err);
    options.setBinaryName("slt");
    NoExecutor.register(options);
    HsqldbExecutor.register(options);
    PostgresExecutor.register(options);
    int parse = options.parse(argv);
    if (parse != 0) {
      return parse;
    }

    URL r = Thread.currentThread().getContextClassLoader().getResource("test");
    if (r == null) {
      out.println("Cannot find resources");
      return 1;
    }

    TestLoader loader = new TestLoader(options);
    for (String file : options.getDirectories()) {
      Path path = Paths.get(r.getPath(), file);
      Files.walkFileTree(path, loader);
    }
    out.println("Files that could not be not parsed: " + loader.fileParseErrors);
    loader.statistics.printStatistics(out);
    return 0;
  }
}

// End Main.java
