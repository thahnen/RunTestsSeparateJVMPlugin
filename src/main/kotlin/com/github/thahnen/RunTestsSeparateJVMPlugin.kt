package com.github.thahnen

import java.util.Properties

import kotlin.jvm.Throws

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.testing.Test

import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.retry

import org.gradle.testretry.TestRetryPlugin


/**
 *  RunTestsSeparateJVMPlugin:
 *  =========================
 *
 *  Plugin to enable jUnit test classes to be executed in a separate JVM each. Possibility exists to run tests either
 *  in parallel or sequential.
 *
 *  Result: - target.extensions.getByType(RunTestsSeparateJVMPluginExtension::class.java) for the following properties
 *          - sequentialTests               -> set of jUnit test classes running in separate JVM in sequential order
 *          - parallelTests                 -> set of jUnit test classes running in separate JVM in parallel order
 *          - inheritConfiguration          -> if config should be inherited from standard Gradle task named "test"
 *          - inheritTestRetryConfiguration -> if config of "test-retry" plugin found
 *
 *  @author thahnen
 */
open class RunTestsSeparateJVMPlugin : Plugin<Project> {

    companion object {
        // identifiers of properties connected to this plugin
        internal const val KEY_SEQUENTIAL           = "plugins.runtestsseparatejvm.listOfTests.sequential"
        internal const val KEY_PARALLEL             = "plugins.runtestsseparatejvm.listOfTests.parallel"
        internal const val KEY_INHERIT              = "plugins.runtestsseparatejvm.inheritTestConfiguration"
        internal const val KEY_INHERIT_TESTRETRY    = "plugins.runtestsseparatejvm.inheritTestRetryConfiguration"

        // internal identifiers of properties connected to this plugin
        private const val INTERNAL_SEQUENTIAL           = "listOfTests.sequential"
        private const val INTERNAL_PARALLEL             = "listOfTests.parallel"
        private const val INTERNAL_INHERIT              = "inheritTestConfiguration"
        private const val INTERNAL_INHERIT_TESTRETRY    = "inheritTestRetryConfiguration"

        // identifier of system property / environment variable to disable dependencies for Gradle "test" task
        internal const val KEY_DISABLEDEPENDENCIES = "disableTestDependencies"

        // extension name
        internal const val KEY_EXTENSION = "RunTestsSeparateJVMExtension"

        // new task names
        internal const val sequentialTestsTaskName  = "testSeparateJVMSequentially"
        internal const val parallelTestsTaskName    = "testSeparateJVMInParallel"

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
            throw PluginAppliedUnnecessarilyException(
                "[${this@RunTestsSeparateJVMPlugin::class.simpleName}] Plugin shouldn't be applied when Java plugin " +
                "isn't used!"
            )
        }

        // 2) retrieve necessary property entries
        val properties = readProperties(target)

        // 3) check if "test-retry" plugin applied to target (only then configuration will be inherited)
        val testRetryPluginFound = target.plugins.hasPlugin(TestRetryPlugin::class.java)
        if (!testRetryPluginFound
            && properties.containsKey(INTERNAL_INHERIT_TESTRETRY)
            && (properties[INTERNAL_INHERIT_TESTRETRY] as String).toBoolean()) {
            target.logger.warn(
                "[${this@RunTestsSeparateJVMPlugin::class.simpleName} - WARNING] '$KEY_INHERIT_TESTRETRY' provided " +
                "and set to true but no 'test-retry' plugin applied to this project. You may remove the property " +
                "from this project!"
            )
        }

        // 4) check if configurations should be inherited but no Gradle task named "test" found
        //    -> This should not be possible in any way other than witchcraft!
        //    -> Therefore this should be included even against all odds ;)
        if (properties.containsKey(INTERNAL_INHERIT) && (properties[INTERNAL_INHERIT] as String).toBoolean()) {
            target.tasks.findByName("test")?.let {
                if (it !is Test) {
                    throw TaskNamedTestNotFoundException(
                        "[${this@RunTestsSeparateJVMPlugin::class.simpleName}] A task named 'test' was found in " +
                        "project '${target.name}' but it is not a Gradle task of type ${Test::class.java.name}! But " +
                        "property '$KEY_INHERIT' provided and set to true which needs this task to configure the new " +
                        "test tasks introduced by this plugin!"
                    )
                }
            } ?: run {
                throw TaskNamedTestNotFoundException(
                    "[${this@RunTestsSeparateJVMPlugin::class.simpleName}] No task named 'test' was found in " +
                    "project '${target.name}' of type ${Test::class.java.name}! But property '$KEY_INHERIT' provided " +
                    "and set to true which needs this task to configure the new test tasks introduced by this plugin!"
                )
            }
        }

        // 5) try to parse property entries if found
        var sequentialTests: Set<String>? = null
        var parallelTests: Set<String>? = null

        if (properties.containsKey(INTERNAL_SEQUENTIAL)) {
            sequentialTests = parseListByCommas(properties[INTERNAL_SEQUENTIAL] as String)
            if (sequentialTests.isEmpty() || (sequentialTests.size == 1 && sequentialTests.first() == "")) {
                throw PropertiesEntryInvalidException(
                    "[${this@RunTestsSeparateJVMPlugin::class.simpleName}] $KEY_SEQUENTIAL provided but invalid " +
                    "(empty / blank)!"
                )
            }
        }

        if (properties.containsKey(INTERNAL_PARALLEL)) {
            parallelTests = parseListByCommas(properties[INTERNAL_PARALLEL] as String)
            if (parallelTests.isEmpty() || (parallelTests.size == 1 && parallelTests.first() == "")) {
                throw PropertiesEntryInvalidException(
                    "[${this@RunTestsSeparateJVMPlugin::class.simpleName}] $KEY_PARALLEL provided but invalid " +
                    "(empty / blank)!"
                )
            }
        }

        // 6) evaluate test sets provided
        evaluateTestSets(target, sequentialTests, parallelTests)

        // 7) custom extension to store tha data
        val extension = target.extensions.create<RunTestsSeparateJVMPluginExtension>(KEY_EXTENSION)
        sequentialTests?.let { extension.sequentialTests.set(it) } ?: run { extension.sequentialTests.empty() }
        parallelTests?.let { extension.parallelTests.set(it) } ?: run { extension.parallelTests.empty() }
        extension.inheritConfiguration.set(
            when (properties.containsKey(INTERNAL_INHERIT)) {
                true -> (properties[INTERNAL_INHERIT] as String).toBoolean()
                else -> false
            }
        )
        extension.inheritTestRetryConfiguration.set(
            when (properties.containsKey(INTERNAL_INHERIT_TESTRETRY)) {
                true -> (properties[INTERNAL_INHERIT_TESTRETRY] as String).toBoolean()
                else -> false
            }
        )

        // 8) register new task of type "Test" for jUnit tests running sequentially in separate JVM
        val test = target.tasks.getByName("test") as Test

        sequentialTests?.let {
            target.tasks.register<Test>(sequentialTestsTaskName) {
                var parallelForks: Int?

                if (properties.containsKey(INTERNAL_INHERIT) && (properties[INTERNAL_INHERIT] as String).toBoolean()) {
                    group           = test.group
                    ignoreFailures  = test.ignoreFailures
                    failFast        = test.failFast
                    jvmArgs         = test.jvmArgs
                    maxHeapSize     = test.maxHeapSize
                    minHeapSize     = test.minHeapSize

                    timeout.set(test.timeout)

                    parallelForks = test.maxParallelForks
                } else {
                    group = "verification"

                    parallelForks = maxParallelForks
                }

                description = "Run jUnit test classes in separate JVM single threaded!"
                setForkEvery(1)
                maxParallelForks = 1

                filter { it.forEach { includeTestsMatching(it) } }

                if (testRetryPluginFound
                    && properties.containsKey(INTERNAL_INHERIT_TESTRETRY)
                    && (properties[INTERNAL_INHERIT_TESTRETRY] as String).toBoolean()) {
                    this.retry {
                        maxRetries.set(test.retry.maxRetries.get())
                        failOnPassedAfterRetry.set(test.retry.failOnPassedAfterRetry.get())
                        maxFailures.set(test.retry.maxFailures.get())
                    }
                }

                doLast {
                    maxParallelForks = parallelForks
                }
            }
        }

        // 9) register new task of type "Test" for jUnit tests running in parallel in separate JVM
        parallelTests?.let {
            target.tasks.register<Test>(parallelTestsTaskName) {
                if (properties.containsKey(INTERNAL_INHERIT) && (properties[INTERNAL_INHERIT] as String).toBoolean()) {
                    group               = test.group
                    ignoreFailures      = test.ignoreFailures
                    failFast            = test.failFast
                    jvmArgs             = test.jvmArgs
                    maxHeapSize         = test.maxHeapSize
                    maxParallelForks    = test.maxParallelForks
                    minHeapSize         = test.minHeapSize

                    timeout.set(test.timeout)
                } else {
                    group = "verification"
                }

                description = "Run jUnit test classes in separate JVM in parallel!"
                setForkEvery(1)

                filter { it.forEach { includeTestsMatching(it) } }

                if (testRetryPluginFound
                    && properties.containsKey(INTERNAL_INHERIT_TESTRETRY)
                    && (properties[INTERNAL_INHERIT_TESTRETRY] as String).toBoolean()) {
                    this.retry {
                        maxRetries.set(test.retry.maxRetries.get())
                        failOnPassedAfterRetry.set(test.retry.failOnPassedAfterRetry.get())
                        maxFailures.set(test.retry.maxFailures.get())
                    }
                }
            }
        }

        // 10) remove sequentially / parallel running test from all other tasks of type Test
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

        // 11) create general Gradle "test" task dependsOn from tasks created
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
            target.properties.containsKey(KEY_SEQUENTIAL)
                -> properties[INTERNAL_SEQUENTIAL] = target.properties[KEY_SEQUENTIAL]

            System.getProperties().containsKey(KEY_SEQUENTIAL)
                -> properties[INTERNAL_SEQUENTIAL] = System.getProperties()[KEY_SEQUENTIAL]

            System.getenv().containsKey(KEY_SEQUENTIAL)
                -> properties[INTERNAL_SEQUENTIAL] = System.getenv(KEY_SEQUENTIAL)
        }

        when {
            target.properties.containsKey(KEY_PARALLEL)
                -> properties[INTERNAL_PARALLEL] = target.properties[KEY_PARALLEL]

            System.getProperties().containsKey(KEY_PARALLEL)
                -> properties[INTERNAL_PARALLEL] = System.getProperties()[KEY_PARALLEL]

            System.getenv().containsKey(KEY_PARALLEL)
                -> properties[INTERNAL_PARALLEL] = System.getenv(KEY_PARALLEL)
        }

        when {
            target.properties.containsKey(KEY_INHERIT)
                -> properties[INTERNAL_INHERIT] = target.properties[KEY_INHERIT]

            System.getProperties().containsKey(KEY_INHERIT)
                -> properties[INTERNAL_INHERIT] = System.getProperties()[KEY_INHERIT]

            System.getenv().containsKey(KEY_INHERIT)
                -> properties[INTERNAL_INHERIT] = System.getenv(KEY_INHERIT)
        }

        when {
            target.properties.containsKey(KEY_INHERIT_TESTRETRY)
                -> properties[INTERNAL_INHERIT_TESTRETRY] = target.properties[KEY_INHERIT_TESTRETRY]

            System.getProperties().containsKey(KEY_INHERIT_TESTRETRY)
                -> properties[INTERNAL_INHERIT_TESTRETRY] = System.getProperties()[KEY_INHERIT_TESTRETRY]

            System.getenv().containsKey(KEY_INHERIT_TESTRETRY)
                -> properties[INTERNAL_INHERIT_TESTRETRY] = System.getenv(KEY_INHERIT_TESTRETRY)
        }

        if (properties.size == 0 || (properties.size == 1 && properties.containsKey(INTERNAL_INHERIT))) {
            // This should not be possible
            throw MissingPropertiesEntryException(
                "[${this@RunTestsSeparateJVMPlugin::class.simpleName}] Neither property for jUnit tests with " +
                "separate JVM running sequentially ($KEY_SEQUENTIAL) found or property for jUnit tests with separate " +
                "JVM running in parallel ($KEY_PARALLEL)!"
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
                var message = "[${this@RunTestsSeparateJVMPlugin::class.simpleName} - WARNING] The following test " +
                                "classes provided to be run sequentially contain a package or asterisk. This can " +
                                "lead to incomprehensible test results! With this message you've been warned and my " +
                                "job here is done!"
                filtered.forEach { message += "\n - $it" }

                target.logger.warn(message)
            }
        }

        parallelTests?.let { tests ->
            val filtered = tests.filter { it.contains(".") || it.contains("*") }
            if (filtered.isNotEmpty()) {
                var message = "[${this@RunTestsSeparateJVMPlugin::class.simpleName} - WARNING] The following test " +
                                "classes provided to be run in parallel contain a package or asterisk. This can lead " +
                                "to incomprehensible test results! With this message you've been warned and my job " +
                                "here is done!"
                filtered.forEach { message += "\n - $it" }

                target.logger.warn(message)
            }
        }

        multipleLet(sequentialTests, parallelTests) { (sTests, pTests) ->
            val intersect = sTests.intersect(pTests)

            if (intersect.isNotEmpty()) {
                var message = "[${this@RunTestsSeparateJVMPlugin::class.simpleName}] The following test classes " +
                                "provided can not be both executed in separate JVM sequentially and in parallel:"
                intersect.forEach { message += "\n - $it" }

                throw TestInBothTasksException(message)
            }
        }
    }
}
