/*
 * Copyright 2023 VMware, Inc.
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

import net.hydromatic.sqllogictest.util.StringPrintStream;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that keeps track of the tests executed, including
 * failures encountered.
 */
public class TestStatistics {
  static final DecimalFormat DF = new DecimalFormat("#,###");

  /**
   * Description of a test that failed.
   * Only query tests are tracked; statements are not supposed to fail.
   */
  public static class FailedTestDescription {
    /**
     * Query that caused the failure.
     */
    public final SqlTestQuery query;
    /**
     * Description of the error encountered.
     */
    public final @Nullable String error;
    /**
     * Detailed information about the error.
     * Printed by toString() when verbosity is higher than 1.
     */
    public final String details;
    /**
     * If the test caused an exception it is stored here.
     */
    public final @Nullable Throwable exception;

    /**
     * Create a description of a failed test.
     * @param query     Query executed that failed.
     * @param error     Error encountered.
     * @param details   Details about error; output by toString()
     *                  when verbosity is greater than 1.
     * @param exception Exception encountered while processing
     *                  query, if any.  Null otherwise.
     */
    public FailedTestDescription(SqlTestQuery query, @Nullable String error,
        String details,
        @Nullable Throwable exception) {
      this.query = query;
      if (error == null && exception != null) {
        error = exception.getMessage();
        if (error == null) {
          error = "Exception " + exception.getClass().getSimpleName();
        }
      }
      this.error = error;
      this.details = details;
      this.exception = exception;
    }

    public String toString(int verbosity) {
      String result = "ERROR: " + (this.error != null ? this.error : "")
              + System.lineSeparator() + "\ttest: " + this.query.file
              + ":" + this.query.line
              + System.lineSeparator()  + "\t" + this.query;
      if (verbosity > 0) {
        if (this.exception != null) {
          StackTraceElement[] stackTrace = this.exception.getStackTrace();
          if (stackTrace.length > 0) {
            StackTraceElement el = stackTrace[0];
            result += System.lineSeparator()
                    + el.getFileName() + ":" + el.getLineNumber() + " "
                    + el.getClassName() + "." + el.getMethodName();
          }
        }
      }
      if (this.exception != null && verbosity > 1) {
        if (!this.details.isEmpty()) {
          result += System.lineSeparator() + this.details;
        }
        StringPrintStream str = new StringPrintStream();
        this.exception.printStackTrace(str.getPrintStream());
        result += System.lineSeparator() + str;
      }
      return result;
    }

    @Override public String toString() {
      return this.toString(1);
    }
  }

  private int failedTestCount;
  private int passedTestCount;
  private int ignoredTestCount;
  /**
   * Test files that could not be parsed.
   */
  private int filesNotParsed;
  /**
   * Files that were processed.
   */
  private int testFiles;
  private int verbosity;

  public int getTestFileCount() {
    return this.testFiles;
  }

  public int getParseFailureCount() {
    return this.filesNotParsed;
  }

  public void incFilesNotParsed() {
    this.filesNotParsed++;
  }

  /**
   * Increment the number of files processed.
   */
  public void incFiles() {
    this.testFiles++;
  }

  /**
   * Increment the number of tests that have passed.
   */
  public void incPassed() {
    this.passedTestCount++;
  }

  /**
   * Increment the number of tests that were ignored.
   */
  public void incIgnored() {
    this.ignoredTestCount++;
  }

  /**
   * Set the number of tests that have passed.
   */
  public void setPassedTestCount(int n) {
    this.passedTestCount = n;
  }

  /**
   * Set the number of tests that have failed.
   */
  public void setFailedTestCount(int n) {
    this.failedTestCount = n;
  }

  /**
   * Set the number of tests that were ignored.
   */
  public void setIgnoredTestCount(int n) {
    this.ignoredTestCount = n;
  }

  /**
   * Add the other statistics to this.
   */
  public void add(TestStatistics stats) {
    this.failedTestCount += stats.failedTestCount;
    this.passedTestCount += stats.passedTestCount;
    this.ignoredTestCount += stats.ignoredTestCount;
    this.filesNotParsed += stats.filesNotParsed;
    this.testFiles += stats.testFiles;
    this.verbosity = Math.max(this.verbosity, stats.verbosity);
    this.failures.addAll(stats.failures);
  }

  /**
   * @return The number of failed tests.
   */
  public int getFailedTestCount() {
    return this.failedTestCount;
  }

  /**
   * @return The number of passed tests.
   */
  public int getPassedTestCount() {
    return this.passedTestCount;
  }

  /**
   * @return The number of ignored tests.
   */
  public int getIgnoredTestCount() {
    return this.ignoredTestCount;
  }

  final List<FailedTestDescription> failures = new ArrayList<>();
  final boolean stopAtFirstErrror;

  public TestStatistics(boolean stopAtFirstError, int verbosity) {
    this.stopAtFirstErrror = stopAtFirstError;
    this.verbosity = verbosity;
    this.failedTestCount = 0;
    this.passedTestCount = 0;
    this.ignoredTestCount = 0;
    this.filesNotParsed = 0;
    this.testFiles = 0;
  }

  /**
   * Add a new failure; return 'true' if we need to stop executing.
   */
  public boolean addFailure(FailedTestDescription failure) {
    this.failures.add(failure);
    this.failedTestCount++;
    return this.stopAtFirstErrror;
  }

  /**
   * @return Total number of tests that were considered.
   */
  public int totalTests() {
    return this.passedTestCount + this.ignoredTestCount + this.failedTestCount;
  }

  /**
   * Print the statistics to the specified stream.
   */
  public void printStatistics(PrintStream out) {
    out.println("Total files processed: "
            + TestStatistics.DF.format(this.testFiles));
    out.println("Files not parsed: "
            + TestStatistics.DF.format(this.filesNotParsed));
    out.println("Passed: " + TestStatistics.DF.format(this.passedTestCount));
    out.println("Failed: " + TestStatistics.DF.format(this.failedTestCount));
    out.println("Ignored: " + TestStatistics.DF.format(this.ignoredTestCount));
    if (!this.failures.isEmpty()) {
      out.print(this.failures.size());
      out.println(" failures:");
    }

    for (FailedTestDescription failure : this.failures) {
      out.println(failure.toString(this.verbosity));
    }
  }
}
