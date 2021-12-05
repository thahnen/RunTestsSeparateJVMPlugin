# RunTestsSeparateJVMPlugin

![example workflow](https://github.com/thahnen/RunTestsSeperateJVMPlugin/actions/workflows/gradle.yml/badge.svg)
![example workflow](https://github.com/thahnen/RunTestsSeperateJVMPlugin/actions/workflows/gradle_validation.yml/badge.svg)
[![Coverage](.github/badges/jacoco.svg)](https://github.com/thahnen/RunTestsSeperateJVMPlugin/actions/workflows/gradle.yml)

Gradle plugin to configure jUnit tests which should be run in a separate JVM. Also allows distinguishing between running
tests sequentially or in parallel.

## Usage

To find out how to apply this plugin to your Gradle project see the information over at the
[Gradle Plugin Portal](https://plugins.gradle.org/plugin/com.github.thahnen.runtestsseparatejvm)!

To use this plugin you need to provide names of jUnit test classes to the project. You can use environment variables or
system properties or the projects gradle.properties file using the following scheme:

```properties
# List of tasks which should be run in separate JVM and sequentially
plugins.runtestsseparatejvm.listOfTests.sequential=Set<String>

# List of tasks which should be run in separate JVM and in parallel
plugins.runtestsseparatejvm.listOfTests.parallel=Set<String>

# (Optional) Timeout for running task in separate JVM and sequentially
# -> Unit is minutes (would override inherited timeout) / ignored when no tests given!
plugins.runtestsseparatejvm.timeout.sequential=Int

# (Optional) Timeout for running task in separate JVM and in parallel
# -> Unit is minutes (would override inherited timeout) / ignored when no tests given!
plugins.runtestsseparatejvm.timeout.parallel=Int

# (Optional) Inherit configuration from standard Gradle test task named "test"
# -> This only inherits a fraction of possible configurations yet (including timeout)!
plugins.runtestsseparatejvm.inheritTestConfiguration=Boolean

# (Optional) Inherit configuration from Gradle "test-retry" plugin and its extension of task named "test" if found
plugins.runtestsseparatejvm.inheritTestRetryConfiguration=Boolean

# (Optional) Silence warnings / information produced by this plugin
plugins.runtestsseparatejvm.silenced=Boolean
```

You can choose to only use a configuration to run tests sequentially or in parallel or both!
