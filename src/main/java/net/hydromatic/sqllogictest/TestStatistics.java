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
     * If the test caused an exception it is stored here.
     */
    public final @Nullable Throwable exception;
    /**
     * If true then store a verbose description of the exceptions.
     */
    public final boolean verbose;

    public FailedTestDescription(SqlTestQuery query, @Nullable String error,
        @Nullable Throwable exception, boolean verbose) {
      this.query = query;
      this.error = error;
      this.exception = exception;
      this.verbose = verbose;
    }

    @Override public String toString() {
      String result = "ERROR: " + (this.error != null ? this.error : "")
              + System.lineSeparator() + "\t" + this.query.file
              + ":" + this.query.line
              + System.lineSeparator()  + "\t" + this.query;
      if (this.exception != null && this.verbose) {
        StringPrintStream str = new StringPrintStream();
        this.exception.printStackTrace(str.getPrintStream());
        result += System.lineSeparator() + str;
      }
      return result;
    }
  }

  private int failedTestCount;
  private int passedTestCount;
  private int ignoredTestCount;
  private int filesNotParsed;
  private int testFiles;

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

  public TestStatistics(boolean stopAtFirstError) {
    this.stopAtFirstErrror = stopAtFirstError;
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
      out.println(failure.toString());
    }
  }
}
