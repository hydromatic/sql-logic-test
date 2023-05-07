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
# SQL Logic Test HOWTO

Here's some miscellaneous documentation about using and developing SQL Logic Test.

# Release

Create an issue with summary "`Release sql-logic-test version x.y`"
([example](https://github.com/hydromatic/sql-logic-test/issues/15)).

Make sure that `./mvnw clean install site` runs on JDK 8, 11 and 17
on Linux, macOS and Windows.
Also check [GitHub Actions](https://github.com/hydromatic/sql-logic-test/actions?query=branch%3Amain).

Update the [release history](HISTORY.md),
the version number at the bottom of [README](README.md),
and the copyright date in [NOTICE](NOTICE);
commit these changes as `Release x.y`.

Use JDK 11.

```
export GPG_TTY=$(tty)
./mvnw clean
./mvnw release:clean
git clean -nx
git clean -fx
./mvnw -Prelease release:prepare
./mvnw -Prelease release:perform
```

Then go to [Sonatype](https://oss.sonatype.org/#stagingRepositories),
log in, close the repository, and release.

Make sure that the [site](http://www.hydromatic.net/sql-logic-test/) has been updated.

Mark the issue fixed, referencing the release commit (whichever commit
had the message "`[maven-release-plugin] prepare release sql-logic-test-x.y`").

[Announce the release](https://twitter.com/julianhyde/status/1652409133180817408).

Convert the `sql-logic-test-x.y` tag into a
[GitHub release](https://github.com/hydromatic/sql-logic-test/tags)
named "`x.y`".

Amend and commit `HOWTO.md` and `HISTORY.md`, if necessary.
