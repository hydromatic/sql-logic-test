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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;

/**
 * Tests SqlLogicTest tests.
 */
public class SqlLogicTestTest {

  private static final String USAGE = ""
      + "slt [options] files_or_directories_with_tests\n"
      + "Executes the SQL Logic Tests using a SQL execution engine\n"
      + "See https://www.sqlite.org/sqllogictest/doc/trunk/about.wiki\n"
      + "Options:\n"
      + "-h            Show this help message and exit\n"
      + "-x            Stop at the first encountered query error\n"
      + "-n            Do not execute, just parse the test files\n"
      + "-e executor   Executor to use\n"
      + "-b filename   Load a list of buggy commands to skip from this file\n"
      + "-v            Increase verbosity (can be repeated)\n"
      + "-u username   Postgres user name\n"
      + "-p password   Postgres password\n"
      + "Registered executors:\n"
      + "\thsql\n"
      + "\tpsql\n"
      + "\tnone\n";

  private static final String UTF_8 = StandardCharsets.UTF_8.name();

  /** Test that prints usage information to stdout. */
  @Test void testHelp() throws IOException {
    Output res = launchSqlLogicTest("-h");
    assertThat(res.err, is(""));
    assertThat(res.out, is(USAGE));
  }

  /** Test with unknown option. */
  @Test void testUnknown() throws IOException {
    Output res = launchSqlLogicTest("--unknown");
    assertThat(res.err, is("Unknown option '--unknown'"
            + System.lineSeparator()));
    assertThat(res.out, is(USAGE));
    assertThat(res.statistics, nullValue());
  }

  /** Test that runs one script against HSQLDB. */
  @Test void testRunHsql() throws IOException {
    Output res = launchSqlLogicTest("-e", "hsql", "select1.test");
    assertThat(res.statistics, notNullValue());
    assertThat(res.statistics.getTestFileCount(), is(1));
    assertThat(res.statistics.getParseFailureCount(), is(0));
    assertThat(res.statistics.getFailedTestCount(), is(0));
    assertThat(res.statistics.getIgnoredTestCount(), is(0));
    assertThat(res.statistics.getPassedTestCount(), is(1000));
    assertThat(res.err, is(""));
  }

  /** Test that runs one script against Postgres. */
  @Test
  @Disabled("Running this test requires a Postgres database named SLT with "
      + "appropriate users and permissions")
  void runPsql() throws IOException {
    // Some queries use a unary + for a VARCHAR value
    // triggering errors from PSQL.
    Output res = launchSqlLogicTest("-e", "psql",
            "-u", "postgres", "-p", "password",
            "/index/random/1000/slt_good_0.test");
    assertThat(res.statistics, notNullValue());
    assertThat(res.statistics.getTestFileCount(), is(1));
    assertThat(res.statistics.getParseFailureCount(), is(0));
    assertThat(res.statistics.getFailedTestCount(), is(30));
    assertThat(res.statistics.getIgnoredTestCount(), is(0));
    assertThat(res.statistics.getPassedTestCount(), is(980));
  }

  /** Test that runs all scripts with no executor. */
  @Test void testRunNoExecutor() throws IOException {
    Output res = launchSqlLogicTest("-e", "none");
    assertThat(res.statistics, notNullValue());
    assertThat(res.statistics.getParseFailureCount(), is(0));
    assertThat(res.statistics.getFailedTestCount(), is(0));
    assertThat(res.statistics.getIgnoredTestCount(), is(5464410));
    assertThat(res.statistics.getPassedTestCount(), is(0));
    assertThat(res.statistics.getTestFileCount(), is(622));
  }

  /** Test that runs hsqldb on a file which produces errors and stops
   * at the first error. */
  @Test void testRunWithErrorsStop() throws IOException {
    // Triggers https://sourceforge.net/p/hsqldb/bugs/1680/
    // Will need to be updated when this bug is closed
    Output res =
        launchSqlLogicTest("-e", "hsql", "-x",
            "random/select/slt_good_12.test");
    String[] outLines = res.out.split(System.lineSeparator());
    assertThat(res.err, is(""));
    assertThat(outLines.length, is(9));
    assertThat(res.out, outLines[1], is("Files not parsed: 0"));
    assertThat(res.out, outLines[2], is("Passed: 4"));
    assertThat(res.out, outLines[3], is("Failed: 1"));
    assertThat(res.out, outLines[4], is("Ignored: 0"));
    assertThat(res.out, outLines[5], is("1 failures:"));
    assertThat(res.out, outLines[6], is("ERROR: unexpected token: -"));

    assertThat(res.statistics, notNullValue());
    assertThat(res.statistics.getParseFailureCount(), is(0));
    assertThat(res.statistics.getFailedTestCount(), is(1));
    assertThat(res.statistics.getIgnoredTestCount(), is(0));
    assertThat(res.statistics.getPassedTestCount(), is(4));
    assertThat(res.statistics.getTestFileCount(), is(1));
  }

  /** Test that runs hsqldb on a file which produces many errors. */
  @Test void testRunWithErrors() throws IOException {
    Output res =
        launchSqlLogicTest("-e", "hsql", "random/select/slt_good_12.test");
    assertThat(res.err, is(""));
    assertThat(res.statistics, notNullValue());
    assertThat(res.statistics.getTestFileCount(), is(1));
    assertThat(res.statistics.getParseFailureCount(), is(0));
    assertThat(res.statistics.getFailedTestCount(), is(1656));
    assertThat(res.statistics.getIgnoredTestCount(), is(0));
    assertThat(res.statistics.getPassedTestCount(), is(7404));
  }

  @Test void testRunSingleTestFile() throws IOException {
    Output res = launchSqlLogicTest("-e", "hsql", "select1.test");
    assertThat(res.err, is(""));
    assertThat(res.statistics, notNullValue());
    assertThat(res.statistics.getTestFileCount(), is(1));
    assertThat(res.statistics.getParseFailureCount(), is(0));
    assertThat(res.statistics.getFailedTestCount(), is(0));
    assertThat(res.statistics.getIgnoredTestCount(), is(0));
    assertThat(res.statistics.getPassedTestCount(), is(1000));
  }

  @Test void testRunSingleTestFileNFlag() throws IOException {
    Output res = launchSqlLogicTest("-e", "hsql", "-n", "select1.test");
    assertThat(res.err, is(""));
    assertThat(res.statistics, notNullValue());
    assertThat(res.statistics.getTestFileCount(), is(1));
    assertThat(res.statistics.getParseFailureCount(), is(0));
    assertThat(res.statistics.getFailedTestCount(), is(0));
    // Because of the -n flag
    assertThat(res.statistics.getIgnoredTestCount(), is(1000));
    assertThat(res.statistics.getPassedTestCount(), is(0));
  }

  @Test void testRunMultipleTestFiles() throws IOException {
    Output res =
        launchSqlLogicTest("-e", "hsql", "select1.test", "select2.test");
    assertThat(res.err, is(""));
    assertThat(res.statistics, notNullValue());
    assertThat(res.statistics.getTestFileCount(), is(2));
    assertThat(res.statistics.getParseFailureCount(), is(0));
    assertThat(res.statistics.getFailedTestCount(), is(0));
    assertThat(res.statistics.getIgnoredTestCount(), is(0));
    assertThat(res.statistics.getPassedTestCount(), is(2000));
  }

  private static Output launchSqlLogicTest(String... args) throws IOException {
    try (ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ByteArrayOutputStream berr = new ByteArrayOutputStream()) {
      final PrintStream out = new PrintStream(bout);
      final PrintStream err = new PrintStream(berr);
      OptionsParser optionParser = new OptionsParser(false, out, err);
      TestStatistics statistics = Main.execute(optionParser, args);
      if (statistics != null) {
        statistics.printStatistics(out);
      }
      out.flush();
      err.flush();
      return new Output(bout.toString(UTF_8), berr.toString(UTF_8), statistics);
    }
  }

  private static class Output {
    final String out;
    final String err;
    final TestStatistics statistics;

    Output(String out, String err, TestStatistics statistics) {
      this.out = out;
      this.err = err;
      this.statistics = statistics;
    }
  }
}

// End SqlLogicTestTest.java
