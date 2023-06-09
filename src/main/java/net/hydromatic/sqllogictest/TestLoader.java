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

import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

/**
 * A test loader is invoked for each test file.
 * It parses the file into a set of tests
 * and then executes the tests found in the file.
 */
public class TestLoader {
  /**
   * Statistics about the tests executed, including
   * all errors found.
   */
  public final TestStatistics statistics;
  /**
   * Options that guide the test execution.
   */
  public final OptionsParser.SuppliedOptions options;

  /**
   * Creates a new class that reads tests from a directory tree and
   * executes them.
   */
  public TestLoader(OptionsParser.SuppliedOptions options) {
    this.statistics = new TestStatistics(
            options.stopAtFirstError, options.verbosity);
    this.options = options;
  }

  /**
   * Function executed for each test file.
   * @param file   File to process.
   * @return       A decision whether the processing should continue or not.
   */
  public boolean visitFile(String file) {
    SqlSltTestExecutor executor = this.options.getExecutor();
    if (executor == null) {
      return false;
    }
    SltTestFile test = null;
    try {
      options.message("Running " + file, 1);
      test = new SltTestFile(file);
      test.parse(options);
    } catch (Exception ex) {
      options.err.println("Error while executing test " + file + ": "
          + ex.getMessage());
      this.statistics.incFilesNotParsed();
    }
    if (test != null) {
      try {
        TestStatistics stats = executor.execute(test, options);
        if (!stats.failures.isEmpty() && options.verbosity > 0) {
          options.out.println(stats.getFailedTestCount() + " failures");
        }
        this.statistics.add(stats);
        if (this.statistics.stopAtFirstErrror
            && !this.statistics.failures.isEmpty()) {
          return false;
        }
      } catch (SQLException | NoSuchAlgorithmException ex) {
        // Can't add exceptions to the overridden method visitFile
        throw new IllegalArgumentException(ex);
      }
    }
    return true;
  }
}
