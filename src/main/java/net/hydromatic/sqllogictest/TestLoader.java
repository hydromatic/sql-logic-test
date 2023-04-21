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

package net.hydromatic.sqllogictest;

import net.hydromatic.sqllogictest.executors.SqlSLTTestExecutor;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

class TestLoader extends SimpleFileVisitor<Path> {
    int errors = 0;
    final TestStatistics statistics;
    public final ExecutionOptions options;

    /**
     * Creates a new class that reads tests from a directory tree and executes them.
     */
    TestLoader(ExecutionOptions options) {
        this.statistics = new TestStatistics(options.stopAtFirstError);
        this.options = options;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        SqlSLTTestExecutor executor = this.options.getExecutor();
        if (executor == null)
            return FileVisitResult.TERMINATE;
        String extension = Utilities.getFileExtension(file.toString());
        if (attrs.isRegularFile() && extension != null && extension.equals("test")) {
            SLTTestFile test = null;
            try {
                options.message("Running " + file, 1);
                test = new SLTTestFile(file.toString());
                test.parse(options);
            } catch (Exception ex) {
                System.err.println("Error while executing test " + file + ": " + ex.getMessage());
                this.errors++;
            }
            if (test != null) {
                try {
                    TestStatistics stats = executor.execute(test, options);
                    this.statistics.add(stats);
                } catch (IOException | SQLException | NoSuchAlgorithmException ex) {
                    // Can't add exceptions to the overridden method visitFile
                    throw new IllegalArgumentException(ex);
                }
            }
        }
        return FileVisitResult.CONTINUE;
    }
}
