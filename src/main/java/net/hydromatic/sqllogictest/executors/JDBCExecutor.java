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

import net.hydromatic.sqllogictest.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings({"SqlDialectInspection", "SqlNoDataSourceInspection"})
public abstract class JDBCExecutor extends SqlSLTTestExecutor {
  protected final String username;
  protected final String password;

  public final String dbUrl;
  @Nullable
  protected Connection connection;

  // In SLT all data received from the database is converted to strings.
  static class Row {
    public final List<String> values;
    Row() {
      this.values = new ArrayList<>();
    }
    void add(String v) {
      this.values.add(v);
    }
    @Override
    public String toString() {
      return String.join("\n", this.values);
    }
  }

  /**
   * A set of rows produced as a result of a query execution.
   */
  static class Rows {
    List<Row> allRows;

    Rows() {
      this.allRows = new ArrayList<>();
    }

    void add(Row row) {
      this.allRows.add(row);
    }

    @Override
    public String toString() {
      return String.join("\n", Utilities.map(this.allRows, Row::toString));
    }

    public int size() {
      return this.allRows.size();
    }

    public void sort(SqlTestQueryOutputDescription.SortOrder order) {
      switch (order) {
      case NONE:
        break;
      case ROW:
        this.allRows.sort(new RowComparator());
        break;
      case VALUE:
        this.allRows = Utilities.flatMap(this.allRows,
            r -> Utilities.map(r.values,
                r0 -> {
                  Row res = new Row();
                  res.add(r0);
                  return res;
                }));
        this.allRows.sort(new RowComparator());
        break;
      }
    }
  }

  public JDBCExecutor(ExecutionOptions options, String dbUrl, String username, String password) {
    super(options);
    this.dbUrl = dbUrl;
    this.username = username;
    this.password = password;
    this.connection = null;
  }

  void statement(SLTSqlStatement statement) throws SQLException {
    String stat = statement.statement;
    // Some tests drop views in the wrong order, e.g., sqllogictest/test/index/view/1000/slt_good_0.test
    if (stat.toLowerCase().startsWith("drop view")) {
      if (!stat.toLowerCase().contains("if exists"))
        stat = stat.substring(0, 9) + " IF EXISTS " + stat.substring(10);
      if (!stat.toLowerCase().contains("cascade"))
        stat += " CASCADE";
    }
    this.options.message(this.statementsExecuted + ": " + stat, 2);
    assert this.connection != null;
    if (this.buggyOperations.contains(statement.statement) || this.options.doNotExecute) {
      options.message("Skipping " + statement.statement, 2);
      return;
    }
    try (Statement stmt = this.connection.createStatement()) {
      stmt.execute(stat);
    } catch (SQLException ex) {
      options.error(ex);
      // Failures during the execution of statements are fatal.
      // Only failures in queries are handled.
      throw ex;
    }
    this.statementsExecuted++;
  }

  void query(SqlTestQuery query, TestStatistics statistics) throws SQLException, NoSuchAlgorithmException {
    assert this.connection != null;
    if (this.buggyOperations.contains(query.getQuery()) || this.options.doNotExecute) {
      statistics.incIgnored();
      options.message("Skipping " + query.getQuery(), 2);
      return;
    }
    try (Statement stmt = this.connection.createStatement()) {
      try (ResultSet resultSet = stmt.executeQuery(query.getQuery())) {
        this.validate(query, resultSet, query.outputDescription, statistics);
      }
    }
    options.message(statistics.testsRun() + ": " + query.getQuery(), 2);
  }

  /**
   * This is the SLT logic which decodes the data received from a Query.
   * Sometimes the result type does not match the actual query type,
   * and a conversion is performed.  This function specifies this logic.
   */
  Row getValue(ResultSet rs, String columnTypes) throws SQLException {
    Row row = new Row();
    // Column numbers start from 1
    for (int i = 1; i <= columnTypes.length(); i++) {
      char c = columnTypes.charAt(i - 1);
      switch (c) {
      case 'R':
        double d = rs.getDouble(i);
        if (rs.wasNull())
          row.add("NULL");
        else
          row.add(String.format("%.3f", d));
        break;
      case 'I':
        try {
          long integer = rs.getLong(i);
          if (rs.wasNull())
            row.add("NULL");
          else
            row.add(String.format("%d", integer));
        } catch (SQLDataException | NumberFormatException ignore) {
          // This probably indicates a bug in the query, since
          // the query expects an integer, but the result cannot
          // be interpreted as such.
          // unparsable string: replace with 0
          row.add("0");
        }
        break;
      case 'T':
        String s = rs.getString(i);
        if (s == null)
          row.add("NULL");
        else {
          StringBuilder result = new StringBuilder();
          for (int j = 0; j < s.length(); j++) {
            char sc = s.charAt(j);
            if (sc < ' ' || sc > '~')
              sc = '@';
            result.append(sc);
          }
          row.add(result.toString());
        }
        break;
      default:
        throw new RuntimeException("Unexpected column type " + c);
      }
    }
    return row;
  }

  static class RowComparator implements Comparator<Row> {
    @Override
    public int compare(Row o1, Row o2) {
      if (o1.values.size() != o2.values.size())
        throw new RuntimeException("Comparing rows of different lengths");
      for (int i = 0; i < o1.values.size(); i++) {
        int r = o1.values.get(i).compareTo(o2.values.get(i));
        if (r != 0)
          return r;
      }
      return 0;
    }
  }

  @SuppressWarnings("java:S4790")  // MD5 checksum
  void validate(SqlTestQuery query, ResultSet rs,
      SqlTestQueryOutputDescription description,
      TestStatistics statistics)
      throws SQLException, NoSuchAlgorithmException {
    assert description.columnTypes != null;
    Rows rows = new Rows();
    while (rs.next()) {
      Row row = this.getValue(rs, description.columnTypes);
      rows.add(row);
    }
    if (description.getValueCount() != rows.size() * description.columnTypes.length()) {
      statistics.addFailure(new TestStatistics.FailedTestDescription(
          query, "Expected " + description.getValueCount() + " rows, got " +
          rows.size() * description.columnTypes.length(), null, options.verbosity > 0));
      return;
    }
    rows.sort(description.getOrder());
    if (description.getQueryResults() != null) {
      String r = rows.toString();
      String q = String.join("\n", description.getQueryResults());
      if (!r.equals(q)) {
        statistics.addFailure(new TestStatistics.FailedTestDescription(
            query, "Output differs: computed\n" + r + "\nExpected:\n" + q,
                null, options.verbosity > 0));
        return;
      }
    }
    if (description.hash != null) {
      // MD5 is considered insecure, but we have no choice because this is
      // the algorithm used to compute the checksums by SLT.
      MessageDigest md = MessageDigest.getInstance("MD5");
      String repr = rows + "\n";
      md.update(repr.getBytes(StandardCharsets.UTF_8));
      byte[] digest = md.digest();
      String hash = Utilities.toHex(digest);
      if (!description.hash.equals(hash)) {
        statistics.addFailure(new TestStatistics.FailedTestDescription(
            query, "Hash of data does not match expected value", null,
                options.verbosity > 0));
        return;
      }
    }
    statistics.incPassed();
  }

  /**
   * Returns a list of all tables in the database.
   */
  abstract List<String> getTableList() throws SQLException;

  /**
   * Returns a list of all views in the database.
   */
  abstract List<String> getViewList() throws SQLException;

  void dropAllTables() throws SQLException {
    assert this.connection != null;
    List<String> tables = this.getTableList();
    for (String tableName : tables) {
      // Unfortunately prepare statements cannot be parameterized in table names.
      // Sonar complains about this, but there is nothing we can do but suppress the warning.
      String del = "DROP TABLE " + tableName + " CASCADE";
      options.message(del, 2);
      try (Statement drop = this.connection.createStatement()) {
        drop.execute(del);  // NOSONAR
      }
    }
  }

  void dropAllViews() throws SQLException {
    assert this.connection != null;
    List<String> tables = this.getViewList();
    for (String tableName : tables) {
      // Unfortunately prepare statements cannot be parameterized in table names.
      // Sonar complains about this, but there is nothing we can do but suppress the warning.
      String del = "DROP VIEW IF EXISTS " + tableName + " CASCADE";
      options.message(del, 2);
      try (Statement drop = this.connection.createStatement()) {
        drop.execute(del);  // NOSONAR
      }
    }
  }

  public void establishConnection() throws SQLException {
    this.connection = DriverManager.getConnection(
        this.dbUrl, this.username, this.password);
    assert this.connection != null;
  }

  public void closeConnection() throws SQLException {
    assert this.connection != null;
    this.connection.close();
  }

  @Override
  public TestStatistics execute(SLTTestFile file, ExecutionOptions options)
      throws SQLException, NoSuchAlgorithmException {
    this.startTest();
    this.establishConnection();
    this.dropAllTables();
    TestStatistics result = new TestStatistics(options.stopAtFirstError);
    for (ISqlTestOperation operation : file.fileContents) {
        SLTSqlStatement stat = operation.as(SLTSqlStatement.class);
        if (stat != null) {
          try {
            this.statement(stat);
          } catch (SQLException ex) {
            System.err.println("Error while processing #" +  (result.testsRun()+1) + " " + operation);
            throw ex;
          }
        } else {
          SqlTestQuery query = operation.to(SqlTestQuery.class);
          try {
            this.query(query, result);
          } catch (SQLException ex) {
            result.addFailure(new TestStatistics.FailedTestDescription(
                    query, ex.getMessage(), ex, options.verbosity > 0));
          }
        }
    }
    this.dropAllViews();
    this.dropAllTables();
    this.closeConnection();
    options.message(this.elapsedTime(file.getTestCount()), 1);
    return result;
  }
}
