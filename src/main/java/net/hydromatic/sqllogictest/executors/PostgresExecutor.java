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

import net.hydromatic.sqllogictest.ExecutionOptions;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A test executor that uses Postgres through JDBC.
 */
public class PostgresExecutor extends JdbcExecutor {
  public static class Factory extends ExecutorFactory {
    String username = "";
    String password = "";

    public static final PostgresExecutor.Factory INSTANCE =
        new PostgresExecutor.Factory();

    private Factory() {}

    /**
     * Register the postgres-specific command-line options with the
     * execution options.
     * @param options  Options that parse the command-line and
     *                 guide the execution.
     */
    @Override public void register(ExecutionOptions options) {
      options.registerOption("-u", "username", "Postgres user name", o -> {
        this.username = o;
        return true;
      });
      options.registerOption("-p", "password", "Postgres password", o -> {
        this.password = o;
        return true;
      });
      options.registerExecutor("psql", () -> {
        PostgresExecutor result = new PostgresExecutor(options,
            this.username, this.password);
        try {
          Set<String> bugs = options.readBugsFile();
          result.avoid(bugs);
          return result;
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
    }
  }

  /**
   * Note: this implementation requires the existence of a database named SLT
   */
  public PostgresExecutor(ExecutionOptions options, String username,
      String password) {
    super(options, "jdbc:postgresql://localhost/slt", username, password);
  }

  List<String> getStringResults(String query) throws SQLException {
    List<String> result = new ArrayList<>();
    assert this.connection != null;
    try (Statement stmt = this.connection.createStatement()) {
      try (ResultSet rs = stmt.executeQuery(query)) {
        while (rs.next()) {
          String tableName = rs.getString(1);
          result.add(tableName);
        }
      }
    }
    return result;
  }

  @Override List<String> getTableList() throws SQLException {
    return this.getStringResults("SELECT tableName FROM pg_catalog.pg_tables\n"
        + "    WHERE schemaname != 'information_schema' AND\n"
        + "    schemaname != 'pg_catalog'");
  }

  @Override List<String> getViewList() throws SQLException {
    return this.getStringResults("SELECT table_name\n"
        + "FROM information_schema.views\n"
        + "WHERE table_schema NOT IN ('information_schema', 'pg_catalog')\n");
  }
}
