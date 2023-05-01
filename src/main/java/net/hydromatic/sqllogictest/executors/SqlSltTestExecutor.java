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

import net.hydromatic.sqllogictest.ExecutionOptions;
import net.hydromatic.sqllogictest.SltTestFile;
import net.hydromatic.sqllogictest.TestStatistics;

import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

/**
 * Base class that must be derived from to implement new test executors
 * for SQL Logic Test.
 *
 * An Executor should implement a static method with signature
 * public static void register(ExecutionOptions options)
 * which can be used to register new command-line options
 * (using ExecutionOptions.registerOption)
 * and to create an executor at runtime
 * (using ExecutionOptions.registerExecutor).
 */
public abstract class SqlSltTestExecutor extends SqlTestExecutor {
  protected final ExecutionOptions options;

  /**
   * Create a new test executor.
   * @param options  Options that will guide the execution.
   */
  public SqlSltTestExecutor(ExecutionOptions options) {
    this.options = options;
  }

  /**
   * Executes the specified test file.
   */
  public abstract TestStatistics execute(SltTestFile testFile,
      ExecutionOptions options)
      throws SQLException, NoSuchAlgorithmException;
}
