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

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Tests {@link SqlLogicTest}.
 */
public class SqlLogicTestTest {
  private static final String UTF_8 = StandardCharsets.UTF_8.name();

  /** Test that prints usage information to stdout.
   *
   * <p>There is no way to redirect the usage output; JCommander writes directly
   * to {@link System#console()}.
   */
  @Test void testMain() throws IOException {
    Output res = launchSqlLogicTest("-h");
    assertThat(res.err, is(""));
    assertThat(res.out,
        is("ExecutionOptions{tests=[], execute=true, "
            + "executor=calcite, stopAtFirstError=false}\n"));
  }

  @Test void testRunSingleTestFile() throws IOException {
    Output res = launchSqlLogicTest("-e", "JDBC", "select1.test");
    String[] outlines = res.out.split("\n");
    assertThat(res.err, is(""));
    assertThat(outlines[3], is("Passed: 1,000"));
    assertThat(outlines[4], is("Failed: 0"));
    assertThat(outlines[5], is("Ignored: 0"));
  }

  @Test void testRunMultipleTestFiles() throws IOException {
    Output res =
        launchSqlLogicTest("-e", "JDBC", "select1.test", "select2.test");
    String[] outlines = res.out.split("\n");
    assertThat(res.err, is(""));
    assertThat(outlines[4], is("Passed: 2,000"));
    assertThat(outlines[5], is("Failed: 0"));
    assertThat(outlines[6], is("Ignored: 0"));
  }

  private static Output launchSqlLogicTest(String... args) throws IOException {
    try (ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ByteArrayOutputStream berr = new ByteArrayOutputStream()) {
      final PrintStream out = new PrintStream(bout);
      final PrintStream err = new PrintStream(berr);
      Main.main2(false, out, err, args);
      out.flush();
      err.flush();
      return new Output(bout.toString(UTF_8), berr.toString(UTF_8));
    }
  }

  private static class Output {
    final String out;
    final String err;

    Output(String out, String err) {
      this.out = out;
      this.err = err;
    }
  }
}

// End SqlLogicTestTest.java
