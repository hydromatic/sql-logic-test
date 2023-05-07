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
# SQL Logic Test release history

For a full list of releases, see <a href="https://github.com/hydromatic/sql-logic-test/releases">github</a>.

## <a href="https://github.com/hydromatic/sql-logic-test/releases/tag/sql-logic-test-0.2">0.2</a> / 2023-05-20

* Extensible options parsing
* `NoSuchFileException` when reading test file/resource from jar
* Remove factory classes
* Move `interface ISqlTestOperation` out of `executors` package
* Add default methods to enumerate tables and views to the `JdbcExecutor` base
  class
* Make methods and classes required for extensions public

Upgrades

* Bump `build-helper-maven-plugin` from 3.3.0 to 3.4.0
* Bump `checkstyle` from 10.10.0 to 10.11.0
* Bump `maven-surefire-plugin` from 3.0.0 to 3.1.0
* Bump `maven-gpg-plugin` from 3.0.1 to 3.1.0
* Bump `checker-qual` from 3.32.0 to 3.34.0

## <a href="https://github.com/hydromatic/sql-logic-test/releases/tag/sql-logic-test-0.1">0.1</a> / 2023-04-29

* Initial release
