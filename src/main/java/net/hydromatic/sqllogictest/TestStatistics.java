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

public class TestStatistics {
  static final DecimalFormat DF = new DecimalFormat("#,###");

  public static class FailedTestDescription {
    public final SqlTestQuery query;
    public final String error;
    public final @Nullable Throwable exception;
    public final boolean verbose;

    public FailedTestDescription(SqlTestQuery query, String error,
        @Nullable Throwable exception, boolean verbose) {
      this.query = query;
      this.error = error;
      this.exception = exception;
      this.verbose = verbose;
    }

    @Override public String toString() {
      String result = "ERROR: " + this.error
              + System.lineSeparator() + "\t" + this.query.file + ":" + this.query.line
              + System.lineSeparator()  + "\t" + this.query;
      if (this.exception != null && this.verbose) {
        StringPrintStream str = new StringPrintStream();
        this.exception.printStackTrace(str.getPrintStream());
        result += System.lineSeparator() + str;
      }
      return result;
    }
  }

  int failed;
  int passed;
  int ignored;

  public void incPassed() {
    this.passed++;
  }

  public void incIgnored() {
    this.ignored++;
  }

  public void setPassed(int n) {
    this.passed = n;
  }

  public void setFailed(int n) {
    this.failed = n;
  }

  public void setIgnored(int n) {
    this.ignored = n;
  }

  public void add(TestStatistics stats) {
    this.failed += stats.failed;
    this.passed += stats.passed;
    this.ignored += stats.ignored;
    this.failures.addAll(stats.failures);
  }

  public int getFailed() {
    return this.failed;
  }

  public int getPassed() {
    return this.passed;
  }

  public int getIgnored() {
    return this.ignored;
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
    this.failed++;
    return this.stopAtFirstErrror;
  }

  public int testsRun() {
    return this.passed + this.ignored + this.failed;
  }

  public void printStatistics(PrintStream out) {
    out.println("Passed: " + TestStatistics.DF.format(this.passed));
    out.println("Failed: " + TestStatistics.DF.format(this.failed));
    out.println("Ignored: " + TestStatistics.DF.format(this.ignored));
    if (!this.failures.isEmpty()) {
      out.print(this.failures.size());
      out.println(" failures:");
    }
    for (FailedTestDescription failure : this.failures) {
      out.println(failure.toString());
    }
  }

  public int totalTests() {
    return this.failed + this.passed + this.ignored;
  }
}
