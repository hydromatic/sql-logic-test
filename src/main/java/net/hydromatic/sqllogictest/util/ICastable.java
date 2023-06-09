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

import java.io.PrintStream;

/**
 * Utility interface providing some useful casting methods.
 */
public interface ICastable {
  /**
   * Cast this object ot the specified type.
   * @param clazz   Type to cast to.
   * @return        An object of type T or null if the cast cannot be performed.
   * @param <T>     Type of expected object.
   */
  default <T> @Nullable T as(Class<T> clazz) {
    return ICastable.as(this, clazz, (String) null);
  }

  static <T> @Nullable T as(Object obj, Class<T> clazz, String s) {
    try {
      return clazz.cast(obj);
    } catch (ClassCastException e) {
      return null;
    }
  }

  /**
   * Print an error when a cast fails.
   * @param err      Stream where error is printed.
   * @param message  Message to print.
   */
  default void error(PrintStream err, String message) {
    err.println(message);
  }

  /**
   * Cast this object ot the specified type.
   * @param clazz   Type to cast to.
   * @return        An object of type T or null if the cast cannot be performed.
   * @param <T>     Type of expected object.* @param err
   * @param failureMessage  Message to display if the cast fails.
   */
  default <T> T as(PrintStream err, Class<T> clazz,
      @Nullable String failureMessage) {
    T result = this.as(clazz);
    if (result == null) {
      if (failureMessage == null) {
        failureMessage = this + "(" + this.getClass().getName()
            + ") is not an instance of " + clazz;
      }
      this.error(err, failureMessage);
    }
    assert result != null;
    return result;
  }

  /**
   * Cast this object to the specified type.
   * @param err    Stream where errors are displayed.
   * @param clazz  Type to cast to.
   * @return       A non-null object of the specified type.
   * @param <T>    Type expected.
   *           Throws when the cast cannot be performed.
   */
  default <T> T to(PrintStream err, Class<T> clazz) {
    return this.as(err, clazz, (String) null);
  }

  /**
   * Check whether this object can be cast to the specified type.
   * @param clazz  Type to cast to.
   * @return       True if the cast can be performed.
   * @param <T>    Type expected.
   */
  default <T> boolean is(Class<T> clazz) {
    return this.as(clazz) != null;
  }
}
