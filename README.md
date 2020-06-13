Wire Maven Plugin
==================================

The *wire-maven-plugin* wraps the [Wire](https://square.github.io/wire/) protobuf compiler.

[![Apache License, Version 2.0, January 2004](https://img.shields.io/github/license/apache/maven.svg?label=License)](LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/de.m3y.maven/wire-maven-plugin.svg)](http://search.maven.org/#search%7Cga%7C1%7Cde.m3y.maven.wire-maven-plugin)

This is a (resurrected) [fork](https://github.com/square/wire/tree/3.0.2/wire-maven-plugin) of the original plugin by the Wire project,
which [dropped](https://github.com/square/wire/pull/1326) this plugin.

What is it good for?
--------------------

* Integrates the [Wire](https://square.github.io/wire/) Protobuf compiler in your Maven project
* Supports Java code generator
* Kotlin code generator is on the TODO list

Check out the [plugin web site][site] including [usage][site_usage] for details for Maven goal [generate-sources](https://marcelmay.github.io/wire-maven-plugin/generate-sources-mojo.html).

[site]: http://marcelmay.github.io/wire-maven-plugin/
[site_usage]: https://marcelmay.github.io/wire-maven-plugin/usage.html
[repo-snapshot]: https://oss.sonatype.org/content/repositories/snapshots/de/m3y/maven/wire-maven-plugin/

Quickstart
----------

1. Put your ProtoBuf definition in ```src/main/proto```

2. Configure plugin in pom.xml
   ```xml
   <build>
     ...
     <plugins>
   
       <plugin>
         <groupId>de.m3y.maven</groupId>
         <artifactId>wire-maven-plugin</artifactId>
         <version>1.0</version> <!-- Check for latest version! -->
         <executions>
           <execution>
               <phase>generate-sources</phase>
               <goals>
                   <goal>generate-sources</goal>
               </goals>
           </execution>
         </executions>
       </plugin>
   
     </plugins>
   </build>
   ```

3. Add Wire runtime dependency in pom.xml (required by Wire compiler generated sources)
   ```xml
   <dependency>
      <groupId>com.squareup.wire</groupId>                                                                                             
      <artifactId>wire-runtime</artifactId>                                                                                            
      <version>3.2.2</version> <!-- Check for latest version -->
   </dependency>
   ```

See the [integration test](src/it/generate-java) if you look for a working example,
check out the [generate-sources goal documentation](https://marcelmay.github.io/wire-maven-plugin/generate-sources-mojo.html)
and have a look at the [Wire project docs](https://square.github.io/wire/).

Development
-----------

* Build the plugin

  ```mvn clean install```

  Make sure you got [Maven 3.6+][maven_download] or higher.

* Build the site

  ```mvn site -Psite```
  or
  ```mvn site-deploy -Psite```

* Release

    ```
    mvn release:prepare -Prelease
    mvn release:perform -Prelease
    ```

* Deploy snapshot

  ```mvn clean deploy -Prelease```

  Note: The release profile contains the snapshot repository for distribution management

[maven_download]: http://maven.apache.org

