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

import net.hydromatic.sqllogictest.ISqlTestOperation;
import net.hydromatic.sqllogictest.OptionsParser;
import net.hydromatic.sqllogictest.SltSqlStatement;
import net.hydromatic.sqllogictest.SltTestFile;
import net.hydromatic.sqllogictest.SqlTestQuery;
import net.hydromatic.sqllogictest.SqlTestQueryOutputDescription;
import net.hydromatic.sqllogictest.TestStatistics;
import net.hydromatic.sqllogictest.util.Utilities;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Base class for tests executors that use a JDBC connection
 * to execute tests.
 */
@SuppressWarnings({"SqlDialectInspection", "SqlNoDataSourceInspection"})
public abstract class JdbcExecutor extends SqlSltTestExecutor {
  protected final String username;
  protected final String password;

  /**
   * URL for connecting to the databse.
   */
  public final String dbUrl;
  protected @Nullable Connection connection;

  /**
   * A row produced by a query execution.
   */
  static class Row {
    /**
     * In SLT all data received from the database is converted to strings.
     */
    public final List<String> values;

    Row() {
      this.values = new ArrayList<>();
    }

    void add(String v) {
      this.values.add(v);
    }

    @Override public String toString() {
      return String.join(System.lineSeparator(), this.values);
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

    @Override public String toString() {
      return String.join(System.lineSeparator(),
              Utilities.map(this.allRows, Row::toString));
    }

    /**
     * @return Number of rows.
     */
    public int size() {
      return this.allRows.size();
    }

    /**
     * Sort the rows using the specified sort order.
     */
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

  /**
   * Create an executor that uses JDBC to run tests.
   *
   * @param options  Execution options.
   * @param dbUrl    URL for the database.
   * @param username Name of database user.
   * @param password Password of database user.
   */
  public JdbcExecutor(OptionsParser.SuppliedOptions options, String dbUrl,
      String username, String password) {
    super(options);
    this.dbUrl = dbUrl;
    this.username = username;
    this.password = password;
    this.connection = null;
  }

  /**
   * Get the connection.  Throws if the connection has not been established.
   */
  public Connection getConnection() {
    assert this.connection != null;
    return this.connection;
  }

  /**
   * Execute the specified statement.
   * @param statement  SQL statement to execute.
   *                   Throws when a statement fails although it should pass.
   */
  public void statement(SltSqlStatement statement) throws SQLException {
    String stat = statement.statement;
    // This "hack" should not be necessary, but some tests are wrong
    // and fail on some databases.
    // Some tests drop views in the wrong order, e.g.,
    // index/view/1000/slt_good_0.test
    if (stat.toLowerCase().startsWith("drop view")) {
      if (!stat.toLowerCase().contains("if exists")) {
        stat = stat.substring(0, 9) + " IF EXISTS " + stat.substring(10);
      }
      if (!stat.toLowerCase().contains("cascade")) {
        stat += " CASCADE";
      }
    }
    this.options.message(this.statementsExecuted + ": " + stat, 2);
    if (this.buggyOperations.contains(statement.statement)
        || this.options.doNotExecute) {
      options.message("Skipping " + statement.statement, 2);
    }
    try (Statement stmt = this.getConnection().createStatement()) {
      stmt.execute(stat);
    } catch (SQLException ex) {
      if (statement.shouldPass) {
        options.error(ex);
        throw ex;
      } // otherwise we can just ignore the exception
    }
    this.statementsExecuted++;
  }

  /**
   * Run a query.
   *
   * @param query       Query to execute.
   * @param statistics  Execution statistics recording the result of
   *                    the query execution.
   * @return            True if we need to stop executing.
   */
  boolean query(SqlTestQuery query, TestStatistics statistics)
      throws SQLException, NoSuchAlgorithmException {
    if (this.buggyOperations.contains(query.getQuery())
        || this.options.doNotExecute) {
      statistics.incIgnored();
      options.message("Skipping " + query.getQuery(), 2);
      return false;
    }
    try (Statement stmt = this.getConnection().createStatement()) {
      try (ResultSet resultSet = stmt.executeQuery(query.getQuery())) {
        boolean result =
            this.validate(query, resultSet, query.outputDescription,
                statistics);
        options.message(statistics.totalTests() + ": " + query.getQuery(), 2);
        return result;
      }
    }
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
        if (rs.wasNull()) {
          row.add("NULL");
        } else {
          row.add(String.format("%.3f", d));
        }
        break;
      case 'I':
        try {
          long integer = rs.getLong(i);
          if (rs.wasNull()) {
            row.add("NULL");
          } else {
            row.add(String.format("%d", integer));
          }
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
        if (s == null) {
          row.add("NULL");
        } else {
          StringBuilder result = new StringBuilder();
          for (int j = 0; j < s.length(); j++) {
            char sc = s.charAt(j);
            if (sc < ' ' || sc > '~') {
              sc = '@';
            }
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
    @Override public int compare(Row o1, Row o2) {
      if (o1.values.size() != o2.values.size()) {
        throw new RuntimeException("Comparing rows of different lengths");
      }
      for (int i = 0; i < o1.values.size(); i++) {
        int r = o1.values.get(i).compareTo(o2.values.get(i));
        if (r != 0) {
          return r;
        }
      }
      return 0;
    }
  }

  /**
   * Validate the result of the execution of a query.
   * Returns 'true' if execution has to stop due to a validation failure.
   */
  @SuppressWarnings("java:S4790")  // MD5 checksum
  public boolean validate(SqlTestQuery query, ResultSet rs,
      SqlTestQueryOutputDescription description,
      TestStatistics statistics)
      throws SQLException, NoSuchAlgorithmException {
    assert description.columnTypes != null;
    Rows rows = new Rows();
    while (rs.next()) {
      Row row = this.getValue(rs, description.columnTypes);
      rows.add(row);
    }
    if (description.getValueCount()
        != rows.size() * description.columnTypes.length()) {
      return statistics.addFailure(
          new TestStatistics.FailedTestDescription(query,
              "Expected " + description.getValueCount() + " rows, got "
                  + rows.size() * description.columnTypes.length(),
              "",
              null));
    }
    rows.sort(description.getOrder());
    if (description.getQueryResults() != null) {
      String r = rows.toString();
      String q = String.join(System.lineSeparator(),
              description.getQueryResults());
      if (!r.equals(q)) {
        return statistics.addFailure(
            new TestStatistics.FailedTestDescription(query,
                "Output differs from expected value",
          "computed" + System.lineSeparator()
                    + r + System.lineSeparator()
                    + "Expected:" + System.lineSeparator()
                    + q + System.lineSeparator(),
                null));
      }
    }
    if (description.hash != null) {
      // MD5 is considered insecure, but we have no choice because this is
      // the algorithm used to compute the checksums by SLT.
      MessageDigest md = MessageDigest.getInstance("MD5");
      String repr = rows + System.lineSeparator();
      md.update(repr.getBytes(StandardCharsets.UTF_8));
      byte[] digest = md.digest();
      String hash = Utilities.toHex(digest);
      if (!description.hash.equals(hash)) {
        return statistics.addFailure(
            new TestStatistics.FailedTestDescription(query,
                "Hash of data does not match expected value",
                    "expected:" + description.hash + " "
                    + "computed: " + hash + System.lineSeparator(), null));
      }
    }
    statistics.incPassed();
    return false;
  }

  /**
   * Returns a list of all tables in the database.
   */
  List<String> getTableList() throws SQLException {
    List<String> result = new ArrayList<>();
    DatabaseMetaData md = this.getConnection().getMetaData();
    ResultSet rs = md.getTables(null, null, "%", new String[]{"TABLE"});
    while (rs.next()) {
      String tableName = rs.getString(3);
      result.add(tableName);
    }
    rs.close();
    return result;
  }

  /**
   * Returns a list of all views in the database.
   */
  List<String> getViewList() throws SQLException {
    List<String> result = new ArrayList<>();
    DatabaseMetaData md = this.getConnection().getMetaData();
    ResultSet rs = md.getTables(null, null, "%", new String[]{"VIEW"});
    while (rs.next()) {
      String tableName = rs.getString(3);
      result.add(tableName);
    }
    rs.close();
    return result;
  }

  public void dropAllTables() throws SQLException {
    List<String> tables = this.getTableList();
    for (String tableName : tables) {
      // Unfortunately prepare statements cannot be parameterized in
      // table names.  Sonar complains about this, but there is
      // nothing we can do but suppress the warning.
      String del = "DROP TABLE " + tableName + " CASCADE";
      options.message(del, 2);
      try (Statement drop = this.getConnection().createStatement()) {
        drop.execute(del);  // NOSONAR
      }
    }
  }

  public void dropAllViews() throws SQLException {
    List<String> tables = this.getViewList();
    for (String tableName : tables) {
      // Unfortunately prepare statements cannot be parameterized in
      // table names.  Sonar complains about this, but there is
      // nothing we can do but suppress the warning.
      String del = "DROP VIEW IF EXISTS " + tableName + " CASCADE";
      options.message(del, 2);
      try (Statement drop = this.getConnection().createStatement()) {
        drop.execute(del);  // NOSONAR
      }
    }
  }

  /**
   * Establish a JDBC connection to the database.
   */
  public void establishConnection() throws SQLException {
    this.connection = DriverManager.getConnection(
        this.dbUrl, this.username, this.password);
    assert this.connection != null;
  }

  /**
   * Close the connection to the databse.
   */
  public void closeConnection() throws SQLException {
    this.getConnection().close();
  }

  /**
   * Execute all the test in the specified file using this database.
   * @param file    File with tests.
   * @param options Options guiding the execution.
   * @return        The statistics describing the tests executed.
   */
  @Override public TestStatistics execute(SltTestFile file,
      OptionsParser.SuppliedOptions options)
      throws SQLException {
    this.startTest();
    this.establishConnection();
    this.dropAllTables();
    TestStatistics result = new TestStatistics(
            options.stopAtFirstError, options.verbosity);
    result.incFiles();
    for (ISqlTestOperation operation : file.fileContents) {
      SltSqlStatement stat = operation.as(SltSqlStatement.class);
      if (stat != null) {
        try {
          this.statement(stat);
          if (!stat.shouldPass) {
            options.err.println("Statement should have failed: " + operation);
          }
        } catch (SQLException ex) {
          // errors in statements cannot be recovered.
          if (stat.shouldPass) {
            // shouldPass should always be true, otherwise
            // the exception should not be thrown.
            options.err.println("Error '" + ex.getMessage()
                    + "' in SQL statement " + operation);
            result.incFilesNotParsed();
            return result;
          }
        }
      } else {
        SqlTestQuery query = operation.to(options.err, SqlTestQuery.class);
        boolean stop;
        try {
          stop = this.query(query, result);
        } catch (Throwable ex) {
          // Need to catch Throwable to handle assertion failures too
          options.message("Exception during query: " + ex.getMessage(), 1);
          stop = result.addFailure(
              new TestStatistics.FailedTestDescription(query,
                  null, "", ex));
        }
        if (stop) {
          break;
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
