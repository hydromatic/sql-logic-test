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
package net.hydromatic.sqllogictest.executors;

import net.hydromatic.sqllogictest.OptionsParser;
import net.hydromatic.sqllogictest.SltTestFile;
import net.hydromatic.sqllogictest.TestStatistics;

/**
 * This executor does not execute the tests at all.
 * It is still useful to validate that the test parsing works.
 */
public class NoExecutor extends SqlSltTestExecutor {
  NoExecutor(OptionsParser.SuppliedOptions options) {
    super(options);
  }

  /**
   * Register the NoExecutor with the execution options.
   * It can be specified using the "-e none" option.
   */
  public static void register(OptionsParser execOptions) {
    execOptions.registerExecutor("none",
        () -> new NoExecutor(execOptions.getOptions()));
  }

  @Override public TestStatistics execute(SltTestFile testFile,
      OptionsParser.SuppliedOptions options) {
    TestStatistics result = new TestStatistics(options.stopAtFirstError);
    result.incFiles();
    this.startTest();
    result.setFailedTestCount(0);
    result.setIgnoredTestCount(testFile.getTestCount());
    result.setPassedTestCount(0);
    options.message(this.elapsedTime(testFile.getTestCount()), 1);
    return result;
  }
}
