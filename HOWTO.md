# Empty HOWTO

Here's some miscellaneous documentation about using and developing Empty.

# Release

Make sure that `./mvnw clean install site` runs on JDK 8, 11 and 17
on Linux, macOS and Windows.
Also check [Travis CI](https://travis-ci.org/julianhyde/empty).

Update the [release history](HISTORY.md),
the version number at the bottom of [README](README.md),
and the copyright date in [NOTICE](NOTICE).

```
./mvnw clean
./mvnw release:clean
git clean -nx
git clean -fx
read -s GPG_PASSPHRASE
./mvnw -Prelease -Dgpg.passphrase=${GPG_PASSPHRASE} release:prepare
./mvnw -Prelease -Dgpg.passphrase=${GPG_PASSPHRASE} release:perform
```

Then go to [Sonatype](https://oss.sonatype.org/#stagingRepositories),
log in, close the repository, and release.

Make sure that the [site](http://www.hydromatic.net/empty/) has been updated.

[Announce the release](https://twitter.com/julianhyde/status/622842100736856064).
