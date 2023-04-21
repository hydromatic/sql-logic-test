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

package net.hydromatic.sqllogictest.executors;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Set;

/**
 * Base class for classes that can run tests.
 */
public class SqlTestExecutor implements ICastable {
  static final DecimalFormat DF = new DecimalFormat("#,###");

  protected final Set<String> buggyOperations;

  private static long startTime = -1;
  private static int totalTests = 0;
  private long lastTestStartTime;
  protected long statementsExecuted = 0;

  protected SqlTestExecutor() {
    this.buggyOperations = new HashSet<>();
  }

  static long seconds(long end, long start) {
    return (end - start) / 1000000000;
  }

  public void avoid(Set<String> statementsToSkip) {
    this.buggyOperations.addAll(statementsToSkip);
  }

  @SuppressWarnings("java:S2696")  // static variable accessed
  protected String elapsedTime(int tests) {
    long end = System.nanoTime();
    totalTests += tests;
    return DF.format(tests) + " tests took "
        + DF.format(seconds(end, this.lastTestStartTime)) + "s, "
        + DF.format(totalTests) + " took "
        + DF.format(seconds(end, startTime)) + "s";
  }

  @SuppressWarnings("java:S2696")  // static variable accessed
  protected void startTest() {
    this.lastTestStartTime = System.nanoTime();
    if (startTime == -1) {
      startTime = lastTestStartTime;
    }
  }
}
