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
package net.hydromatic.sqllogictest.util;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class Utilities {
  private Utilities() {}

  /**
   * Just adds single quotes around a string.  No escaping is performed.
   */
  public static String singleQuote(String other) {
    return "'" + other + "'";
  }

  public static @Nullable String getFileExtension(String filename) {
    int i = filename.lastIndexOf('.');
    if (i > 0) {
      return filename.substring(i + 1);
    }
    return null;
  }

  private static final char[] HEX_CODES = "0123456789abcdef".toCharArray();

  public static String toHex(byte[] data) {
    StringBuilder r = new StringBuilder(data.length * 2);
    for (byte b : data) {
      r.append(HEX_CODES[(b >> 4) & 0xF]);
      r.append(HEX_CODES[b & 0xF]);
    }
    return r.toString();
  }

  public static <T, S> List<S> map(List<T> data, Function<T, S> function) {
    List<S> result = new ArrayList<>(data.size());
    for (T datum : data) {
      result.add(function.apply(datum));
    }
    return result;
  }

  public static <T, S> List<S> flatMap(List<T> data,
      Function<T, List<S>> function) {
    List<S> result = new ArrayList<>(data.size());
    for (T datum : data) {
      result.addAll(function.apply(datum));
    }
    return result;
  }
}
