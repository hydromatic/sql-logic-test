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

import static org.hamcrest.MatcherAssert.assertThat;
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
      + "-d directory  Directory with SLT tests\n"
      + "-i            Install the SLT tests if the directory does not exist\n"
      + "-x            Stop at the first encountered query error\n"
      + "-n            Do not execute, just parse the test files\n"
      + "-e executor   Executor to use\n"
      + "-b filename   Load a list of buggy commands to skip from this file\n"
      + "-v            Increase verbosity\n"
      + "-u username   Postgres user name\n"
      + "-p password   Postgres password\n"
      + "Registered executors:\n"
      + "\thsql\n"
      + "\tpsql\n"
      + "\tnone\n";

  @Test void testHelp() throws IOException {
    final ByteArrayOutputStream baosOut = new ByteArrayOutputStream();
    final PrintStream out = new PrintStream(baosOut);
    final ByteArrayOutputStream baosErr = new ByteArrayOutputStream();
    final PrintStream err = new PrintStream(baosErr);
    int result = Main.execute(false, out, err, "-h");
    out.flush();
    err.flush();
    assertThat(result, is(1));
    assertThat(baosErr.size(), is(0));
    assertThat(baosOut.toString("utf-8"),
        is(USAGE));
  }

  @Test void runHsql() throws IOException {
    final PrintStream out = new PrintStream(new ByteArrayOutputStream());
    final PrintStream err = new PrintStream(new ByteArrayOutputStream());
    Main.execute(false, out, err,
        "-d", "sqllogictest", "-i", "-e", "hsql", "select1.test");
  }

  @Test
  @Disabled("Running this test requires a Postgres database named SLT with "
      + "appropriate users and permissions")
  void runPsql() throws IOException {
    final PrintStream out = new PrintStream(new ByteArrayOutputStream());
    final PrintStream err = new PrintStream(new ByteArrayOutputStream());
    Main.execute(false, out, err,
        "-d", "sqllogictest", "-i", "-e", "psql", "-v",
        "-u", "postgres", "-p", "password",
        "/index/random/1000/slt_good_0.test");
  }

  @Test void runNoExecutor() throws IOException {
    final PrintStream out = new PrintStream(new ByteArrayOutputStream());
    final PrintStream err = new PrintStream(new ByteArrayOutputStream());
    Main.execute(false, out, err,
        "-d", "sqllogictest", "-i", "-e", "none");
  }
}

// End SqlLogicTestTest.java
