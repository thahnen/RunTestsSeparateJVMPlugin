package com.github.thahnen

import org.gradle.api.provider.Property


/**
 *  RunTestsSeparateJVMPluginExtension:
 *  ==================================
 *
 *  Extension to this plugin but not for configuration, only for storing data as project.ext / project.extra.
 *
 *  @author thahnen
 */
abstract class RunTestsSeparateJVMPluginExtension {

    /** stores all jUnit test classes which should be run in separate JVM but executed sequentially */
    abstract val sequentialTests: Property<Set<String>>

    /** stores all jUnit test classes which should be run in separate JVM but executed in parallel */
    abstract val parallelTests: Property<Set<String>>
}
