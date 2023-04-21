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

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SqlLogicTestTest {
  @Test void testHelp() throws IOException {
    int result = Main.execute(false, "-h");
    assertEquals(1, result);
  }

  @Test void runHsql() throws IOException {
    Main.execute(false, "-d", "sqllogictest", "-i", "-e", "hsql", "select1.test");
  }

  //@Test
  // Running this test requires a Postgres database named SLT with appropriate users and permissions
  void runPsql() throws IOException {
    Main.execute(false, "-d", "sqllogictest", "-i", "-e", "psql", "-v",
            "-u", "postgres", "-p", "password", "/index/random/1000/slt_good_0.test");
  }

  @Test void runNoExecutor() throws IOException {
    Main.execute(false, "-d", "sqllogictest", "-i", "-e", "none");
  }
}

// End SqlLogicTestTest.java
