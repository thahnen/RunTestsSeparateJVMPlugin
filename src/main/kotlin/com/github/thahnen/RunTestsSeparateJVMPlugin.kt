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

    companion object {
        // identifiers of properties connected to this plugin
        internal val KEY_SEQUENTIAL  = "plugins.runtestsseparatejvm.listOfTests.sequential"
        internal val KEY_PARALLEL    = "plugins.runtestsseparatejvm.listOfTests.parallel"

        // identifier of system property / environment variable to disable dependencies for Gradle "test" task
        internal val KEY_DISABLEDEPENDENCIES = "disableTestDependencies"

        // extension name
        internal val KEY_EXTENSION = "RunTestsSeparateJVMExtension"

        // new task names
        internal val sequentialTestsTaskName = "testSeparateJVMSequentially"
        internal val parallelTestsTaskName   = "testSeparateJVMInParallel"

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

        // 4) evaluate test sets provided
        evaluateTestSets(target, sequentialTests, parallelTests)

        // 5) custom extension to store tha data
        val extension = target.extensions.create<RunTestsSeparateJVMPluginExtension>(KEY_EXTENSION)
        sequentialTests?.let { extension.sequentialTests.set(it) } ?: run { extension.sequentialTests.empty() }
        parallelTests?.let { extension.parallelTests.set(it) } ?: run { extension.parallelTests.empty() }

        // 6) register new task of type "Test" for jUnit tests running sequentially in separate JVM
        sequentialTests?.let {
            target.tasks.register<Test>(sequentialTestsTaskName) {
                val parallelForks = maxParallelForks

                group = "verification"
                setForkEvery(1)
                maxParallelForks = 1

                filter { it.forEach { includeTestsMatching(it) } }

                doLast {
                    maxParallelForks = parallelForks
                }
            }
        }

        // 7) register new task of type "Test" for jUnit tests running in parallel in separate JVM
        parallelTests?.let {
            target.tasks.register<Test>(parallelTestsTaskName) {
                group = "verification"
                setForkEvery(1)

                filter { it.forEach { includeTestsMatching(it) } }
            }
        }

        // 8) remove sequentially / parallel running test from all other tasks of type Test
        target.tasks.withType(Test::class.java) {
            sequentialTests?.let {
                if (this.name != sequentialTestsTaskName) {
                    filter { it.forEach { excludeTestsMatching(it) } }
                }
            }

            parallelTests?.let {
                if (this.name != parallelTestsTaskName) {
                    filter { it.forEach { excludeTestsMatching(it) } }
                }
            }
        }

        // 9) create general Gradle "test" task dependsOn from tasks created
        //    -> only if not disabled using system property / environment variable
        if (!getNoTestTaskDependency()) {
            target.tasks.named("test", Test::class.java) {
                sequentialTests?.let { this.dependsOn(sequentialTestsTaskName) }
                parallelTests?.let { this.dependsOn(parallelTestsTaskName) }
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


    /**
     *  Tries to evaluate if system property / environment variable set to disable Gradle "test" task dependencies
     *
     *  @return true if found, false otherwise
     */
    private fun getNoTestTaskDependency() : Boolean = when {
        System.getProperties().containsKey(KEY_DISABLEDEPENDENCIES) -> true
        System.getenv().containsKey(KEY_DISABLEDEPENDENCIES)        -> true
        else                                                        -> false
    }


    /**
     *  Evaluates tests provided to both tasks. No test class should be provided to both tasks ;)
     *
     *  @param target the project which the plugin is applied to
     *  @param sequentialTests set of test classes running sequentially
     *  @param parallelTests set of test classes running in parallel
     *  @throws TestInBothTasksException when test class(es) provided to both test tasks
     */
    @Throws(TestInBothTasksException::class)
    private fun evaluateTestSets(target: Project, sequentialTests: Set<String>?, parallelTests: Set<String>?) {
        sequentialTests?.let { tests ->
            val filtered = tests.filter { it.contains(".") || it.contains("*") }
            if (filtered.isNotEmpty()) {
                var message = "[${this@RunTestsSeparateJVMPlugin::class.simpleName}] The following test classes " +
                                "provided to be run sequentially contain a package or asterisk. This can lead to " +
                                "incomprehensible test results! With this message you've been warned and my job here " +
                                "is done!"
                filtered.forEach { message += "\n - $it" }

                target.logger.warn(message)
            }
        }

        parallelTests?.let { tests ->
            val filtered = tests.filter { it.contains(".") || it.contains("*") }
            if (filtered.isNotEmpty()) {
                var message = "[${this@RunTestsSeparateJVMPlugin::class.simpleName}] The following test classes " +
                                "provided to be run in parallel contain a package or asterisk. This can lead to " +
                                "incomprehensible test results! With this message you've been warned and my job here " +
                                "is done!"
                filtered.forEach { message += "\n - $it" }

                target.logger.warn(message)
            }
        }

        multipleLet(sequentialTests, parallelTests) { (sTests, pTests) ->
            val intersect = sTests.intersect(pTests)

            if (intersect.isNotEmpty()) {
                var message = "The following test classes provided can not be both executed sequentially and in parallel:"
                intersect.forEach { message += "\n - $it" }

                throw TestInBothTasksException(message)
            }
        }
    }
}
