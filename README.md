<!--
{% comment %}
Licensed to Julian Hyde under one or more contributor license
agreements.  See the NOTICE file distributed with this work
for additional information regarding copyright ownership.
Julian Hyde licenses this file to you under the Apache
License, Version 2.0 (the "License"); you may not use this
file except in compliance with the License.  You may obtain a
copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
either express or implied.  See the License for the specific
language governing permissions and limitations under the
License.
{% endcomment %}
-->
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/net.hydromatic/sql-logic-test/badge.svg)](https://maven-badges.herokuapp.com/maven-central/net.hydromatic/sql-logic-test)
[![Build Status](https://github.com/hydromatic/sql-logic-test/actions/workflows/main.yml/badge.svg?branch=main)](https://github.com/hydromatic/sql-logic-test/actions?query=branch%3Amain)

# SQL Logic Test

SQL Logic Test is a suite of more than 7 million tests that test core aspects of SQL.

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

* License: <a href="LICENSE">Apache Software License, Version 2.0</a>
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
