<!--
{% comment %}
Copyright 2022 VMware, Inc.
SPDX-License-Identifier: MIT

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
{% endcomment %}
-->
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/net.hydromatic/sql-logic-test/badge.svg)](https://maven-badges.herokuapp.com/maven-central/net.hydromatic/sql-logic-test)
[![Build Status](https://github.com/hydromatic/sql-logic-test/actions/workflows/main.yml/badge.svg?branch=main)](https://github.com/hydromatic/sql-logic-test/actions?query=branch%3Amain)

# SQL Logic Test

SQL Logic Test is a suite of more than 7 million tests that test core aspects of SQL.

This project uses SqlLogicTests
<https://www.sqlite.org/sqllogictest/doc/trunk/about.wiki>.
for testing the Calcite compiler infrastructure.  This is a suite
of more than 7 million tests that are designed to be portable.
They only test core aspects of SQL.  For example, the tests only
cover integers, strings, and floating point values, and do not test
corner cases, such as overflows.

The tests in the SQL logic test suite are given as ".test" files.
Each test file is a combination of SQL statements that set up the test
by creating and populating tables, followed by a sequence of queries
over the created tables.

Some SQL Logic Test queries are written multiple times, in different SQL dialects;
the test scripts will only execute only the queries in the chosen dialect.  In
our implementation we use the Postgres dialect.

This program only exercises Calcite for executing the queries; the statements
themselves are delegated to be executed directly by a database engine.

## Running the tests

This project provides a standalone Main class with a main function,
and it can be compiled into a standalone executable.  The executable
recognizes the following command-line arguments:

```
Usage: slt [options] Files or directories with test data (relative to the specified directory)
Options:
-b
Load a list of buggy commands to skip from this file
-d
Directory with SLT tests
-e
Executor to use; one of 'none, JDBC, calcite'
Default: none
-h
Show this help message and exit
Default: false
-i
Install the SLT tests if the directory does not exist
Default: false
-n
Do not execute, just parse the test files
Default: false
-s
Ignore the status of SQL commands executed
Default: false
-x
Stop at the first encountered query error
Default: false
```

Here is an example invocation:

`slt -d ~/git/sqllogictest -e calcite select1.test index`

This invocation assumes that the `sqllogictest` scripts have been installed at `~/git/sqllogictest`,
will run the tests through Calcite, and will execute the test script `select1.test` and
all the test scripts in the `index` directory.  (All these files are relative to the `~/git/sqllogictest/test`
directory).  Installation of the tests is performed if the `-i` flag is provided.

If some statements or queries are expected to fail, they can be added to a file
which is supplied with the `-b` flag.  Each such statement/query must be on a separate line of the file
and must be spelled identically to the way the statement/query appears in the test file,
but with the newline characters removed.

The `-x` flag will stop at the first query error encountered.

The program will run all queries in the specified files and at the end print a list
of failures including an explanation for each failure.  A query can fail either
because it triggers an internal Calcite assertion, or because it produces incorrect
results.  All the queries have been validated with external database engines, so a failure
is very likely to be a true bug in Calcite.

## How tests are executed

This program parses SqlLogicTest files and delegates their execution to
"test executors".  We provide multiple executors:

### The `NoExecutor` test executor

This executor does not really run any tests.  But it can still be used
by the test loading mechanism to check that we correctly parse all
SQL logic test files.

### The `JDBC` executor

This executor parallels the standard ODBC executor from the SQLLogicTest suite,
(which is written in C).  It sends both the statements and queries to a database to be executed.
Any database that supports JDBC and can handle the syntax of the
SQL statements can be used.  We use by default the HSQLDB
<http://hsqldb.org/> database.

### The 'Calcite' executor

This executor uses the JDBC executor to execute the statements storing
the data, and the default Calcite compiler settings to compile and
execute the queries.

### Adding new executors

Calcite is not just a compiler, it is framework for building compilers.
This program only tests the default Calcite compiler configuration.  To test other
Calcite-based compilers one should subclass the `SqlSLTTestExecutor` class
(or the `CalciteExecutor` class).

## Get SQL Logic Test

### From Maven

Get sql-logic-Test from
<a href="https://search.maven.org/#search%7Cga%7C1%7Ca%3Asql-logic-test">Maven central</a>:

```xml
<dependency>
  <groupId>net.hydromatic</groupId>
  <artifactId>sql-logic-test</artifactId>
  <version>0.1</version>
</dependency>
```

### Download and build

You need Java (8 or higher) and Git.

```bash
$ git clone git://github.com/hydromatic/sql-logic-test.git
$ cd sql-logic-test
$ ./mvnw compile
```

On Windows, the last line is

```bash
> mvnw install
```

On Java versions less than 11, you should add parameters
`-Dcheckstyle.version=9.3`.

## More information

* License: MIT
* Author: Julian Hyde
* Blog: https://hydromatic.blogspot.com
* Project page: http://www.hydromatic.net/sql-logic-test
* API: http://www.hydromatic.net/sql-logic-test/apidocs
* Source code: https://github.com/hydromatic/sql-logic-test
* Developers list:
  <a href="mailto:dev@calcite.apache.org">dev at calcite.apache.org</a>
  (<a href="https://mail-archives.apache.org/mod_mbox/calcite-dev/">archive</a>,
  <a href="mailto:dev-subscribe@calcite.apache.org">subscribe</a>)
* Issues: https://github.com/hydromatic/sql-logic-test/issues
* <a href="HISTORY.md">Release notes and history</a>
* <a href="HOWTO.md">HOWTO</a>
