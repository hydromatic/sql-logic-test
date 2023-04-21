/*
 * Licensed to Julian Hyde under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
