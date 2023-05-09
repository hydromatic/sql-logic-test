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

import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

/**
 * A test loader is invoked for each test file.
 * It parses the file into a set of tests
 * and then executes the tests found in the file.
 */
public class TestLoader extends SimpleFileVisitor<Path> {
  /**
   * Number of files that could not be parsed.
   */
  public int fileParseErrors = 0;
  /**
   * Statistics about the tests executed, including
   * all errors found.
   */
  public final TestStatistics statistics;
  /**
   * Options that guide the test execution.
   */
  public final ExecutionOptions options;

  /**
   * Creates a new class that reads tests from a directory tree and
   * executes them.
   */
  public TestLoader(ExecutionOptions options) {
    this.statistics = new TestStatistics(options.stopAtFirstError);
    this.options = options;
  }

  /**
   * Function executed for each test file.
   * @param file   File to process.
   * @param attrs  File attributes.
   * @return       A decision whether the processing should continue or not.
   */
  @Override public FileVisitResult visitFile(Path file,
      BasicFileAttributes attrs) {
    SqlSltTestExecutor executor = this.options.getExecutor();
    if (executor == null) {
      return FileVisitResult.TERMINATE;
    }
    String extension = Utilities.getFileExtension(file.toString());
    if (attrs.isRegularFile() && extension != null && extension.equals("test")) {
      SltTestFile test = null;
      try {
        options.message("Running " + file, 1);
        test = new SltTestFile(file);
        test.parse(options);
      } catch (Exception ex) {
        String errMsg = "Error while executing test " + file + ": "
                + ex.getMessage();
        options.err.println(errMsg);
        this.fileParseErrors++;
      }
      if (test != null) {
        try {
          TestStatistics stats = executor.execute(test, options);
          if (!stats.failures.isEmpty() && options.verbosity > 0) {
            options.out.println(stats.failed + " failures");
          }
          this.statistics.add(stats);
          if (this.statistics.stopAtFirstErrror
              && !this.statistics.failures.isEmpty()) {
            return FileVisitResult.TERMINATE;
          }
        } catch (SQLException | NoSuchAlgorithmException ex) {
          // Can't add exceptions to the overridden method visitFile
          throw new IllegalArgumentException(ex);
        }
      }
    }
    return FileVisitResult.CONTINUE;
  }
}
