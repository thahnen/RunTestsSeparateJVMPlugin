package com.github.thahnen

import java.util.Properties

import kotlin.jvm.Throws

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.testing.Test

import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register


/**
 *  RunTestsSeparateJVMPlugin:
 *  =========================
 *
 *  Plugin to enable jUnit test classes to be executed in a separate JVM each. Possibility exists to run tests either
 *  in parallel or sequential.
 *
 *  Result: - target.extensions.getByType(RunTestsSeparateJVMPluginExtension::class.java) for the following properties
 *          - sequentialTests   -> set of jUnit test classes running in separate JVM in sequential order
 *          - parallelTests     -> set of jUnit test classes running in separate JVM in parallel order
 *
 *  @author thahnen
 */
open class RunTestsSeparateJVMPlugin : Plugin<Project> {

    // identifiers of properties connected to this plugin
    private val KEY_SEQUENTIAL  = "plugins.runtestsseparatejvm.listOfTests.sequential"
    private val KEY_PARALLEL    = "plugins.runtestsseparatejvm.listOfTests.parallel"

    // extension name
    private val KEY_EXTENSION = "RunTestsSeparateJVMExtension"

    // new task names
    private val sequentialTestsTaskName = "testSeparateJVMSequentially"
    private val parallelTestsTaskName   = "testSeparateJVMInParallel"


    companion object {
        /**
         *  Parses a list of values separated by comma to set
         *
         *  @param list list as string
         *  @return set of values
         */
        internal fun parseListByCommas(list: String) : Set<String> = list.replace(" ", "").split(",").toSet()
    }


    /** Overrides the abstract "apply" function */
    override fun apply(target: Project) {
        // 1) check if Java plugin applied to target (necessary because check on test task)
        if (!target.plugins.hasPlugin(JavaPlugin::class.java)) {
            throw PluginAppliedUnnecessarilyException("Plugin shouldn't be applied when Java plugin isn't used!")
        }

        // 2) retrieve necessary property entries
        val properties = readProperties(target)

        // 3) try to parse property entries if found
        var sequentialTests: Set<String>? = null
        var parallelTests: Set<String>? = null

        if (properties.containsKey("listOfTests.sequential")) {
            sequentialTests = parseListByCommas(properties["listOfTests.sequential"] as String)
            if (sequentialTests.isEmpty() || (sequentialTests.size == 1 && sequentialTests.first() == "")) {
                throw PropertiesEntryInvalidException("$KEY_SEQUENTIAL provided but invalid (empty / blank)!")
            }
        }

        if (properties.containsKey("listOfTests.parallel")) {
            parallelTests = parseListByCommas(properties["listOfTests.parallel"] as String)
            if (parallelTests.isEmpty() || (parallelTests.size == 1 && parallelTests.first() == "")) {
                throw PropertiesEntryInvalidException("$KEY_PARALLEL provided but invalid (empty / blank)!")
            }
        }

        // 4) custom extension to store tha data
        val extension = target.extensions.create<RunTestsSeparateJVMPluginExtension>(KEY_EXTENSION)
        sequentialTests?.let { extension.sequentialTests.set(it) }
        parallelTests?.let { extension.parallelTests.set(it) }

        // 5) register new task of type "Test" for jUnit tests running sequentially in separate JVM
        sequentialTests?.let {
            target.tasks.register<Test>(sequentialTestsTaskName) {
                val parallelForks = maxParallelForks

                group = "verification"
                setForkEvery(1)
                maxParallelForks = 1

                filter {
                    it.forEach { includeTestsMatching(it) }
                }

                doLast {
                    maxParallelForks = parallelForks
                }
            }
        }

        // 6) register new task of type "Test" for jUnit tests running in parallel in separate JVM
        parallelTests?.let {
            target.tasks.register<Test>(parallelTestsTaskName) {
                group = "verification"
                setForkEvery(1)

                filter {
                    it.forEach { includeTestsMatching(it) }
                }
            }
        }

        // 7) remove sequentially running test from all other tasks of type Test
        sequentialTests?.let {
            target.tasks.withType(Test::class.java) {
                if (this.name != sequentialTestsTaskName) {
                    filter {
                        it.forEach { excludeTestsMatching(it) }
                    }
                }
            }
        }

        // 8) remove parallel running test from all other tasks of type Test
        parallelTests?.let {
            target.tasks.withType(Test::class.java) {
                if (this.name != parallelTestsTaskName) {
                    filter {
                        it.forEach { excludeTestsMatching(it) }
                    }
                }
            }
        }
    }


    /**
     *  Tries to retrieve the necessary properties file entry (can be provided as system property / environment variable
     *  as well)
     *
     *  @param target the project which the plugin is applied to
     *  @return necessary properties key-value pair
     *  @throws MissingPropertiesEntryException when necessary property entries not found / provided
     */
    @Throws(MissingPropertiesEntryException::class)
    private fun readProperties(target: Project) : Properties {
        val properties = Properties()

        when {
            target.properties.containsKey(KEY_SEQUENTIAL)       -> properties["listOfTests.sequential"] = target.properties[KEY_SEQUENTIAL]
            System.getProperties().containsKey(KEY_SEQUENTIAL)  -> properties["listOfTests.sequential"] = System.getProperties()[KEY_SEQUENTIAL]
            System.getenv().containsKey(KEY_SEQUENTIAL)         -> properties["listOfTests.sequential"] = System.getenv(KEY_SEQUENTIAL)
        }

        when {
            target.properties.containsKey(KEY_PARALLEL)         -> properties["listOfTests.parallel"] = target.properties[KEY_PARALLEL]
            System.getProperties().containsKey(KEY_PARALLEL)    -> properties["listOfTests.parallel"] = System.getProperties()[KEY_PARALLEL]
            System.getenv().containsKey(KEY_PARALLEL)           -> properties["listOfTests.parallel"] = System.getenv(KEY_PARALLEL)
        }

        if (properties.size == 0) {
            // This should not be possible
            throw MissingPropertiesEntryException(
                "Neither property for jUnit tests with separate JVM running sequentially ($KEY_SEQUENTIAL) found or" +
                "property for jUnit tests with separate JVM running in parallel ($KEY_PARALLEL)"
            )
        }

        return properties
    }
}
