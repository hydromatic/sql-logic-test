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

import net.hydromatic.sqllogictest.executors.ISqlTestOperation;

/**
 * Represents a query in a test.
 */
public class SqlTestQuery implements ISqlTestOperation {
  /**
   * Query that is executed.
   */
  String query;
  /**
   * Some queries have optional names.
   */
  String name;
  /**
   * The name of the file containing the query.
   */
  public final String file;
  /**
   * The line number in the file where the query starts.
   */
  int line;
  /**
   * Description of the output expected from the query.
   */
  public final SqlTestQueryOutputDescription outputDescription;

  /**
   * In test files some queries are named.
   * This helps e.g., identify multiple queries in different dialects
   * that are mutually exclusive.
   */
  public void setName(String name) {
    this.name = name;
  }

  SqlTestQuery(String file) {
    this.query = "";
    this.file = file;
    this.outputDescription = new SqlTestQueryOutputDescription();
  }

  void setQuery(String query, int line) {
    this.query = query;
    this.line = line;
  }

  /**
   * @return The SQL query to execute.
   */
  public String getQuery() {
    return this.query;
  }

  @Override public String toString() {
    return this.query;
  }
}
