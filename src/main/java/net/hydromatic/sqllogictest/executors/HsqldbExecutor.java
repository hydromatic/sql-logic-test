package net.hydromatic.sqllogictest.executors;

import net.hydromatic.sqllogictest.ExecutionOptions;

import java.io.IOException;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A test executor that uses HSQLDB through JDBC.
 */
public class HsqldbExecutor extends JdbcExecutor {
  public static class Factory extends ExecutorFactory {
    public static final HsqldbExecutor.Factory INSTANCE =
        new HsqldbExecutor.Factory();

    private Factory() {}

    @Override public void register(ExecutionOptions options) {
      options.registerExecutor("hsql", () -> {
        HsqldbExecutor result = new HsqldbExecutor(options);
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

  public HsqldbExecutor(ExecutionOptions options) {
    super(options, "jdbc:hsqldb:mem:db", "", "");
  }

  @Override List<String> getTableList() throws SQLException {
    List<String> result = new ArrayList<>();
    assert this.connection != null;
    DatabaseMetaData md = this.connection.getMetaData();
    ResultSet rs = md.getTables(null, null, "%", new String[]{"TABLE"});
    while (rs.next()) {
      String tableName = rs.getString(3);
      if (tableName.equals("PUBLIC")) {
        // The catalog table in HSQLDB
        continue;
      }
      result.add(tableName);
    }
    rs.close();
    return result;
  }

  @Override List<String> getViewList() throws SQLException {
    List<String> result = new ArrayList<>();
    assert this.connection != null;
    DatabaseMetaData md = this.connection.getMetaData();
    ResultSet rs = md.getTables(null, null, "%", new String[]{"VIEW"});
    while (rs.next()) {
      String tableName = rs.getString(3);
      result.add(tableName);
    }
    rs.close();
    return result;
  }

  @Override public void establishConnection() throws SQLException {
    super.establishConnection();
    assert this.connection != null;
    try (Statement statement = this.connection.createStatement()) {
      // Enable postgres compatibility
      statement.execute("SET DATABASE SQL SYNTAX PGS TRUE");
    }
  }
}
