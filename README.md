Wire Maven Plugin
==================================

The *wire-maven-plugin* wraps the [Wire](https://square.github.io/wire/) protobuf compiler}}.

[![Maven Central](https://img.shields.io/maven-central/v/de.m3y.maven/wire-maven-plugin.svg)](http://search.maven.org/#search%7Cga%7C1%7Cde.m3y.maven.wire-maven-plugin)

This is a (resurrected) [fork](https://github.com/square/wire/tree/3.0.2/wire-maven-plugin) of the original plugin by the Wire project,
which had the misfortune of being [dropped](https://github.com/square/wire/pull/1326).

What is it good for?
--------------------

* Integrates the Wire Protobuf compiler in your Maven project
* Supports Java code generator
* Kotlin code generator is on the TODO list

Check out the [plugin web site][site] including [usage][site_usage] for details.

[site]: http://marcelmay.github.io/wire-maven-plugin/
[site_usage]: https://marcelmay.github.io/wire-maven-plugin/usage.html
[repo-snapshot]: https://oss.sonatype.org/content/repositories/snapshots/de/m3y/maven/wire-maven-plugin/

Development
-----------

* Build the plugin

    mvn clean install

  Make sure you got [Maven 3.6+][maven_download] or higher.

* Build the site

    mvn clean install integration-test site -Psite

* Release

    mvn release:prepare -Prelease

    mvn release:perform -Prelease

* Deploy snapshot

    mvn clean deploy -Prelease

  Note: The release profile contains the snapshot repository for distribution management

[maven_download]: http://maven.apache.org

