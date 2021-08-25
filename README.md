# RunTestsSeperateJVMPlugin

![example workflow](https://github.com/thahnen/RunTestsSeperateJVMPlugin/actions/workflows/gradle.yml/badge.svg)
![example workflow](https://github.com/thahnen/RunTestsSeperateJVMPlugin/actions/workflows/gradle_validation.yml/badge.svg)
[![Coverage](.github/badges/jacoco.svg)](https://github.com/thahnen/RunTestsSeperateJVMPlugin/actions/workflows/gradle.yml)

Gradle plugin to configure jUnit tests which should be run in a separate JVM. Also allows distinguishing between running
tests sequentially or in parallel.

## Usage

To use this plugin you need to provide names of jUnit test classes to the project. You can use environment variables or
the projects gradle.properties file using the following scheme:

```properties
# List of tasks which should be run in separate JVM and sequentially
plugins.runtestsseparatejvm.listOfTests.sequential=List<String>

# List of tasks which should be run in separate JVM and in parallel
plugins.runtestsseparatejvm.listOfTests.parallel=List<String>
```
