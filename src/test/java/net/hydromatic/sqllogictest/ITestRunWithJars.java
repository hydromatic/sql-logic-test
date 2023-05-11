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

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Integration test for verifying scenarios using the produced jars.
 *
 * <p>The test is skipped when run from IDE cause the required System properties
 * are only set in maven.</p>
 */
public class ITestRunWithJars {
  private static final String BUILD_DIR = System.getProperty("buildDirectory");
  private static final String ARTIFACT = System.getProperty("artifactName");
  private static final String CLASSPATH = System.getProperty("buildClasspath");

  @Test void testRunSingleTestNoException()
      throws IOException, InterruptedException {
    Output r = runMain("-e", "none", "select1.test");
    assertThat(r.err, not(containsString("Exception")));
  }

  private static Output runMain(String... args)
      throws IOException, InterruptedException {
    Assumptions.assumeFalse(BUILD_DIR == null);
    Assumptions.assumeFalse(ARTIFACT == null);
    Assumptions.assumeFalse(CLASSPATH == null);
    String jarPath = Paths.get(BUILD_DIR, ARTIFACT).toString();
    String classPath = jarPath + ":" + CLASSPATH;
    List<String> cmd = new ArrayList<>();
    cmd.add("java");
    cmd.add("-cp");
    cmd.add(classPath);
    cmd.add("net.hydromatic.sqllogictest.Main");
    cmd.addAll(Arrays.asList(args));
    Process p = new ProcessBuilder(cmd).start();
    p.waitFor();
    return new Output(readAllLines(p.getInputStream()),
        readAllLines(p.getErrorStream()));
  }

  private static String readAllLines(InputStream is) {
    return new BufferedReader(new InputStreamReader(is)).lines()
        .collect(Collectors.joining(System.lineSeparator()));
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
