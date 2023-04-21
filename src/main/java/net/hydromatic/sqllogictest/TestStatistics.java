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

import org.checkerframework.checker.nullness.qual.Nullable;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class TestStatistics {
  static final DecimalFormat df = new DecimalFormat("#,###");

  public static class FailedTestDescription {
    public final SqlTestQuery query;
    public final String error;
    @Nullable
    public final Throwable exception;
    public final boolean verbose;

    public FailedTestDescription(SqlTestQuery query, String error, @Nullable Throwable exception, boolean verbose) {
      this.query = query;
      this.error = error;
      this.exception = exception;
      this.verbose = verbose;
    }

    @Override
    public String toString() {
      String extra = "";
      if (this.exception != null && this.verbose) {
        StringPrintStream str = new StringPrintStream();
        this.exception.printStackTrace(str.getPrintStream());
        extra = str + "\n";
      }
      return "ERROR: " + this.error + "\n\t" + this.query.file + ":" + this.query.line +
          "\n\t" + this.query + "\n" + extra;
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

  List<FailedTestDescription> failures = new ArrayList<>();
  final boolean stopAtFirstErrror;

  public TestStatistics(boolean stopAtFirstError) {
    this.stopAtFirstErrror = stopAtFirstError;
  }

  public void addFailure(FailedTestDescription failure) {
    this.failures.add(failure);
    this.failed++;
  }

  public int testsRun() {
    return this.passed + this.ignored + this.failed;
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();
    if (!this.failures.isEmpty())
      result.append(this.failures.size())
          .append(" failures:\n");
    for (FailedTestDescription failure : this.failures)
      result.append(failure.toString());
    return "Passed: " + TestStatistics.df.format(this.passed) +
        "\nFailed: " + TestStatistics.df.format(this.failed) +
        "\nIgnored: " + TestStatistics.df.format(this.ignored) +
        "\n" +
        result;
  }

  public int totalTests() {
    return this.failed + this.passed + this.ignored;
  }
}
