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
for testing the database query engines.  This is a suite
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

## Running the tests

This project provides a standalone Main class with a main function,
and it can be compiled into a standalone executable.  The executable
recognizes the following command-line arguments:

```
slt [options] files_or_directories_with_tests
Executes the SQL Logic Tests using a SQL execution engine
See https://www.sqlite.org/sqllogictest/doc/trunk/about.wiki
Options:
-h            Show this help message and exit
-x            Stop at the first encountered query error
-n            Do not execute, just parse the test files
-e executor   Executor to use
-b filename   Load a list of buggy commands to skip from this file
-v            Increase verbosity
-u username   Postgres user name
-p password   Postgres password
Registered executors:
       hsql
       psql
       none
```

Here is an example invocation:

`slt -e hsql select1.test index`

This invocation will run the tests through HSQLDB, and will execute
the test script `select1.test` and all the test scripts in the `index`
directory.

If some statements or queries are expected to fail, they can be added to a file
which is supplied with the `-b` flag.  Each such statement/query must be on a separate line of the file
and must be spelled identically to the way the statement/query appears in the test file,
but with the newline characters removed.

The `-x` flag will stop at the first query error encountered.

The program will run all queries in the specified files and at the end print a list
of failures including an explanation for each failure.  A query can fail either
because it triggers an internal database, or because it produces incorrect
results.  All the queries have been validated with sqlite, so a failure
is very likely to be a true bug in the tested engine.

## Listing all available tests

You can achieve this using the following invocation:

`slt -e none -v`

## How tests are executed

This program parses SqlLogicTest files and delegates their execution to
"test executors".  We provide multiple executors:

### The `none` test executor

This executor does not really run any tests.  But it can still be used
by the test loading mechanism to check that we correctly parse all
SQL logic test files.

### The `JDBC` executor

This executor parallels the standard ODBC executor from the
SQLLogicTest suite, (which is written in C).  It sends both the
statements and queries to a database to be executed.  The JDBC executor
is an abstract Java class which can be subclassed to implement concrete
executors.

### The `hsql` executor

This is an extension of the JDBC executor that uses the HSQLDB
<http://hsqldb.org/> embedded database.

### The `psql` executor

This executor is an extension of the JDBC executor that uses Postgres
<> as a database.  Prior to using this executor one must create a
Postgres database named "slt" and users with appropriate permissions
to access the database.

### Adding new executors

New executors can be added by extending the `SqlSLTTestExecutor` class.

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
