package com.github.thahnen

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty


/**
 *  RunTestsSeparateJVMPluginExtension:
 *  ==================================
 *
 *  Extension to this plugin but not for configuration, only for storing data as project.ext / project.extra.
 *
 *  @author thahnen
 */
@Suppress("UnnecessaryAbstractClass")
abstract class RunTestsSeparateJVMPluginExtension {

    /** stores all jUnit test classes which should be run in separate JVM but executed sequentially */
    abstract val sequentialTests: SetProperty<String>

    /** stores all jUnit test classes which should be run in separate JVM but executed in parallel */
    abstract val parallelTests: SetProperty<String>

    /**
     *  stores if both (or only one, depends on properties provided) new test tasks should inherit configurations from
     *  default Gradle test task named "test"
     */
    abstract val inheritConfiguration: Property<Boolean>

    /**
     *  stores if both (or only one, depends on properties provided) new test tasks should inherit configurations from
     *  Gradle "test-retry" plugin if found in test task named "test"
     */
    abstract val inheritTestRetryConfiguration: Property<Boolean>
}
