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

import net.hydromatic.sqllogictest.executors.HSQLExecutor;
import net.hydromatic.sqllogictest.executors.NoExecutor;
import net.hydromatic.sqllogictest.executors.PostgresExecutor;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

/**
 * Execute all SqlLogicTest tests.
 */
public class Main {
  public static void main(String[] argv) throws IOException {
    execute(true, argv);
  }

  /** As {@link #main} but does not call {@link System#exit} if {@code exit}
   * is false. */
  public static int execute(boolean exit, String... argv) throws IOException {
    ExecutionOptions options = new ExecutionOptions(exit);
    options.setBinaryName("slt");
    NoExecutor.Factory.INSTANCE.register(options);
    HSQLExecutor.Factory.INSTANCE.register(options);
    PostgresExecutor.Factory.INSTANCE.register(options);
    int parse = options.parse(argv);
    if (parse != 0)
      return parse;
    if (options.directory == null || options.directory.isEmpty())
      return options.abort(exit,
              "Please specify the directory with the SqlLogicTest suite using the -d flag");

    File dir = new File(options.directory);
    if (dir.exists()) {
      if (!dir.isDirectory())
        return options.abort(exit, options.directory + " is not a directory");
      if (options.install)
        System.err.println("Directory " + options.directory + " exists; skipping download");
    } else {
      if (options.install) {
        SLTInstaller installer = new SLTInstaller(dir);
        installer.install();
      } else {
        return options.abort(exit, options.directory + " does not exist and no installation was specified");
      }
    }

    TestLoader loader = new TestLoader(options);
    for (String file : options.getDirectories()) {
      Path path = Paths.get(options.directory + "/test/" + file);
      Files.walkFileTree(path, loader);
    }
    System.out.println("Files that could not be not parsed: " + loader.errors);
    System.out.println(loader.statistics);
    return 0;
  }
}
