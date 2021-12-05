package com.github.thahnen

import java.io.FileInputStream
import java.io.IOException
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.Properties

import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test

import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.plugins.JavaPlugin
import org.gradle.testfixtures.ProjectBuilder

import org.gradle.testretry.TestRetryPlugin
import org.gradle.testretry.TestRetryTaskExtension

import com.github.stefanbirkner.systemlambda.SystemLambda.restoreSystemProperties
import com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable


/**
 *  RunTestsSeparateJVMPluginTest:
 *  =============================
 *
 *  jUnit test cases on the RunTestsSeparateJVMPlugin
 */
open class RunTestsSeparateJVMPluginTest {

    companion object {
        // test cases properties file
        private val correct1ProjectPropertiesPath   = resource("correct/project1.properties")
        private val correct2ProjectPropertiesPath   = resource("correct/project2.properties")
        private val correct3ProjectPropertiesPath   = resource("correct/project3.properties")
        private val correct4ProjectPropertiesPath   = resource("correct/project4.properties")
        private val correct5ProjectPropertiesPath   = resource("correct/project5.properties")
        private val correct6ProjectPropertiesPath   = resource("correct/project6.properties")
        private val correct7ProjectPropertiesPath   = resource("correct/project7.properties")
        private val correct8ProjectPropertiesPath   = resource("correct/project8.properties")
        private val correct9ProjectPropertiesPath   = resource("correct/project9.properties")
        private val correct10ProjectPropertiesPath   = resource("correct/project10.properties")
        private val wrong1ProjectPropertiesPath     = resource("wrong/project1.properties")
        private val wrong2ProjectPropertiesPath     = resource("wrong/project2.properties")
        private val wrong3ProjectPropertiesPath     = resource("wrong/project3.properties")
        private val wrong4ProjectPropertiesPath     = resource("wrong/project4.properties")
        private val wrong5ProjectPropertiesPath     = resource("wrong/project5.properties")
        private val wrong6ProjectPropertiesPath     = resource("wrong/project6.properties")

        // test cases properties
        private val correct1Properties = Properties()
        private val correct2Properties = Properties()
        private val correct3Properties = Properties()
        private val correct4Properties = Properties()
        private val correct5Properties = Properties()
        private val correct6Properties = Properties()
        private val correct7Properties = Properties()
        private val correct8Properties = Properties()
        private val correct9Properties = Properties()
        private val correct10Properties = Properties()
        private val wrong1Properties = Properties()
        private val wrong2Properties = Properties()
        private val wrong3Properties = Properties()
        private val wrong4Properties = Properties()
        private val wrong5Properties = Properties()
        private val wrong6Properties = Properties()


        /** internally used simplified resource loader */
        private fun resource(path: String): String {
            return this::class.java.classLoader.getResource(path)!!.path.replace("%20", " ")
        }


        /** 0) Configuration to read properties once before running multiple tests using them */
        @Throws(IOException::class)
        @BeforeClass @JvmStatic fun configureTestsuite() {
            correct1Properties.load(FileInputStream(correct1ProjectPropertiesPath))
            correct2Properties.load(FileInputStream(correct2ProjectPropertiesPath))
            correct3Properties.load(FileInputStream(correct3ProjectPropertiesPath))
            correct4Properties.load(FileInputStream(correct4ProjectPropertiesPath))
            correct5Properties.load(FileInputStream(correct5ProjectPropertiesPath))
            correct6Properties.load(FileInputStream(correct6ProjectPropertiesPath))
            correct7Properties.load(FileInputStream(correct7ProjectPropertiesPath))
            correct8Properties.load(FileInputStream(correct8ProjectPropertiesPath))
            correct9Properties.load(FileInputStream(correct9ProjectPropertiesPath))
            correct10Properties.load(FileInputStream(correct10ProjectPropertiesPath))
            wrong1Properties.load(FileInputStream(wrong1ProjectPropertiesPath))
            wrong2Properties.load(FileInputStream(wrong2ProjectPropertiesPath))
            wrong3Properties.load(FileInputStream(wrong3ProjectPropertiesPath))
            wrong4Properties.load(FileInputStream(wrong4ProjectPropertiesPath))
            wrong5Properties.load(FileInputStream(wrong5ProjectPropertiesPath))
            wrong6Properties.load(FileInputStream(wrong6ProjectPropertiesPath))
        }
    }


    /** 1) Tests only applying the plugin (without Java plugin applied) */
    @Test fun testApplyPluginWithoutJavaPluginToProject() {
        val project = ProjectBuilder.builder().build()

        try {
            // try applying plugin (should fail)
            project.pluginManager.apply(RunTestsSeparateJVMPlugin::class.java)
        } catch (e: Exception) {
            // assert applying did not work
            // INFO: equal to check on InvalidUserDataException as it is based on it
            Assert.assertTrue(e.cause is PluginAppliedUnnecessarilyException)
        }

        Assert.assertFalse(project.plugins.hasPlugin(RunTestsSeparateJVMPlugin::class.java))
    }


    /** 2) Tests only applying the plugin (without environment variable / project properties used for configuration) */
    @Test fun testApplyPluginWithoutPropertiesToProject() {
        val project = ProjectBuilder.builder().build()

        // apply Java plugin
        project.pluginManager.apply(JavaPlugin::class.java)

        Assert.assertTrue(project.plugins.hasPlugin(JavaPlugin::class.java))

        try {
            // try applying plugin (should fail)
            project.pluginManager.apply(RunTestsSeparateJVMPlugin::class.java)
        } catch (e: Exception) {
            // assert applying did not work
            // INFO: equal to check on InvalidUserDataException as it is based on it
            Assert.assertTrue(e.cause is MissingPropertiesEntryException)
        }

        Assert.assertFalse(project.plugins.hasPlugin(RunTestsSeparateJVMPlugin::class.java))
    }


    /** 3) Tests applying the plugin (with incorrect project properties file) */
    @Test fun testApplyPluginWithIncorrectPropertiesToProject() {
        listOf(wrong1Properties, wrong2Properties).forEach {
            val project = ProjectBuilder.builder().build()

            // apply Java plugin
            project.pluginManager.apply(JavaPlugin::class.java)

            // project gradle.properties reference (can not be used directly!)
            val propertiesExtension = project.extensions.getByType(ExtraPropertiesExtension::class.java)
            it.keys.forEach { key ->
                propertiesExtension[key as String] = it.getProperty(key)
            }

            try {
                // try applying plugin (should fail)
                project.pluginManager.apply(RunTestsSeparateJVMPlugin::class.java)
            } catch (e: Exception) {
                // assert applying did not work
                // INFO: equal to check on InvalidUserDataException as it is based on it
                Assert.assertTrue(e.cause is PropertiesEntryInvalidException)
            }

            Assert.assertFalse(project.plugins.hasPlugin(RunTestsSeparateJVMPlugin::class.java))
        }
    }


    /** 4) Tests applying the plugin (with incorrect project properties file -> only inherit config property) */
    @Test fun testApplyPluginWithOnlyInheritPropertyToProject() {
        val project = ProjectBuilder.builder().build()

        // apply Java plugin
        project.pluginManager.apply(JavaPlugin::class.java)

        // project gradle.properties reference (can not be used directly!)
        val propertiesExtension = project.extensions.getByType(ExtraPropertiesExtension::class.java)
        wrong4Properties.keys.forEach { key ->
            propertiesExtension[key as String] = wrong4Properties.getProperty(key)
        }

        try {
            // try applying plugin (should fail)
            project.pluginManager.apply(RunTestsSeparateJVMPlugin::class.java)
        } catch (e: Exception) {
            // assert applying did not work
            // INFO: equal to check on InvalidUserDataException as it is based on it
            Assert.assertTrue(e.cause is MissingPropertiesEntryException)
        }

        Assert.assertFalse(project.plugins.hasPlugin(RunTestsSeparateJVMPlugin::class.java))
    }


    /** 5) Tests applying the plugin (with incorrect environment variable -> only inherit config property) */
    @Test fun testApplyPluginWithOnlyInheritPropertyByEnvironmentVariableToProject() {
        val project = ProjectBuilder.builder().build()

        // apply Java plugin
        project.pluginManager.apply(JavaPlugin::class.java)

        withEnvironmentVariable(
            RunTestsSeparateJVMPlugin.KEY_INHERIT, wrong4Properties[RunTestsSeparateJVMPlugin.KEY_INHERIT] as String
        ).execute {
            Assert.assertTrue(System.getenv().containsKey(RunTestsSeparateJVMPlugin.KEY_INHERIT))
            Assert.assertEquals(
                wrong4Properties[RunTestsSeparateJVMPlugin.KEY_INHERIT],
                System.getenv(RunTestsSeparateJVMPlugin.KEY_INHERIT)
            )

            try {
                // try applying plugin (should fail)
                project.pluginManager.apply(RunTestsSeparateJVMPlugin::class.java)
            } catch (e: Exception) {
                // assert applying did not work
                // INFO: equal to check on InvalidUserDataException as it is based on it
                Assert.assertTrue(e.cause is MissingPropertiesEntryException)
            }

            Assert.assertFalse(project.plugins.hasPlugin(RunTestsSeparateJVMPlugin::class.java))
        }
    }


    /** 6) Tests applying the plugin (with project properties file, tests in both properties) */
    @Test fun testApplyPluginWithTestsInBothPropertiesToProject() {
        val project = ProjectBuilder.builder().build()

        // apply Java plugin
        project.pluginManager.apply(JavaPlugin::class.java)

        // project gradle.properties reference (can not be used directly!)
        val propertiesExtension = project.extensions.getByType(ExtraPropertiesExtension::class.java)
        wrong3Properties.keys.forEach { key ->
            propertiesExtension[key as String] = wrong3Properties.getProperty(key)
        }

        try {
            // try applying plugin (should fail)
            project.pluginManager.apply(RunTestsSeparateJVMPlugin::class.java)
        } catch (e: Exception) {
            // assert applying did not work
            // INFO: equal to check on InvalidUserDataException as it is based on it
            Assert.assertTrue(e.cause is TestInBothTasksException)
        }

        Assert.assertFalse(project.plugins.hasPlugin(RunTestsSeparateJVMPlugin::class.java))
    }


    /** 7) Tests applying the plugin (with incorrect environment variables used) */
    @Test fun testApplyPluginWithIncorrectEnvironmentVariablesToProject() {
        listOf(wrong1Properties, wrong2Properties).forEach {
            val project = ProjectBuilder.builder().build()

            // apply Java plugin
            project.pluginManager.apply(JavaPlugin::class.java)

            withEnvironmentVariable(
                RunTestsSeparateJVMPlugin.KEY_SEQUENTIAL,
                it[RunTestsSeparateJVMPlugin.KEY_SEQUENTIAL] as String
            ).and(
                RunTestsSeparateJVMPlugin.KEY_PARALLEL,
                it[RunTestsSeparateJVMPlugin.KEY_PARALLEL] as String
            ).execute {
                try {
                    // try applying plugin (should fail)
                    project.pluginManager.apply(RunTestsSeparateJVMPlugin::class.java)
                } catch (e: Exception) {
                    // assert applying did not work
                    // INFO: equal to check on InvalidUserDataException as it is based on it
                    Assert.assertTrue(e.cause is PropertiesEntryInvalidException)
                }

                Assert.assertFalse(project.plugins.hasPlugin(RunTestsSeparateJVMPlugin::class.java))
            }
        }
    }


    /** 8) Tests applying the plugin (with correct project properties file) */
    @Test fun testApplyPluginWithCorrectPropertiesToProject() {
        listOf(correct1Properties, correct2Properties, correct3Properties).forEach {
            val project = ProjectBuilder.builder().build()

            // apply Java plugin
            project.pluginManager.apply(JavaPlugin::class.java)

            // project gradle.properties reference (correct/project.properties.set can not be used directly!)
            val propertiesExtension = project.extensions.getByType(ExtraPropertiesExtension::class.java)
            it.keys.forEach { key ->
                propertiesExtension[key as String] = it.getProperty(key)
            }

            // apply plugin
            project.pluginManager.apply(RunTestsSeparateJVMPlugin::class.java)

            // assert that plugin was applied to the project
            Assert.assertTrue(project.plugins.hasPlugin(RunTestsSeparateJVMPlugin::class.java))
        }
    }


    /** 9) Tests applying the plugin (with environment variables used) */
    @Test fun testApplyPluginWithCorrectEnvironmentVariablesToProject() {
        val project = ProjectBuilder.builder().build()

        // apply Java plugin
        project.pluginManager.apply(JavaPlugin::class.java)

        withEnvironmentVariable(
            RunTestsSeparateJVMPlugin.KEY_SEQUENTIAL,
            correct1Properties[RunTestsSeparateJVMPlugin.KEY_SEQUENTIAL] as String
        ).and(
            RunTestsSeparateJVMPlugin.KEY_PARALLEL,
            correct1Properties[RunTestsSeparateJVMPlugin.KEY_PARALLEL] as String
        ).execute {
            // apply plugin
            project.pluginManager.apply(RunTestsSeparateJVMPlugin::class.java)

            // assert that plugin was applied to the project
            Assert.assertTrue(project.plugins.hasPlugin(RunTestsSeparateJVMPlugin::class.java))
        }
    }


    /** 10) Tests applying the plugin (with system properties used) */
    @Test fun testApplyPluginWithCorrectSystemPropertiesToProject() {
        val project = ProjectBuilder.builder().build()

        // apply Java plugin
        project.pluginManager.apply(JavaPlugin::class.java)

        restoreSystemProperties {
            // project gradle.properties reference (correct/project6.properties.set can not be used directly!)
            correct6Properties.keys.forEach { key ->
                System.setProperty(key as String, correct6Properties.getProperty(key))

                Assert.assertEquals(correct6Properties.getProperty(key), System.getProperty(key))
            }

            // apply plugin
            project.pluginManager.apply(RunTestsSeparateJVMPlugin::class.java)

            // assert that plugin was applied to the project
            Assert.assertTrue(project.plugins.hasPlugin(RunTestsSeparateJVMPlugin::class.java))
        }
    }


    /** 11) Tests applying the plugin and evaluates that logger works correctly (with asterisks / full package) */
    @Test fun testEvaluateAsteriskAndPackageLogging() {
        listOf(correct4Properties, correct5Properties).forEach {
            val project = ProjectBuilder.builder().build()

            // apply Java plugin
            project.pluginManager.apply(JavaPlugin::class.java)

            // project gradle.properties reference (correct/project.properties.set can not be used directly!)
            val propertiesExtension = project.extensions.getByType(ExtraPropertiesExtension::class.java)
            it.keys.forEach { key ->
                propertiesExtension[key as String] = it.getProperty(key)
            }

            // set listener to evaluate output logged by plugin
            project.logging.addStandardOutputListener { message ->
                Assert.assertTrue(
                    message.contains(
                        "[${RunTestsSeparateJVMPlugin::class.simpleName}] The following test classes provided"
                    ) && message.contains(
                        "contain a package or asterisk. This can lead to incomprehensible test results! With " +
                        "this message you've been warned and my job here is done!"
                    )
                )
            }

            // apply plugin
            project.pluginManager.apply(RunTestsSeparateJVMPlugin::class.java)

            // assert that plugin was applied to the project
            Assert.assertTrue(project.plugins.hasPlugin(RunTestsSeparateJVMPlugin::class.java))
        }
    }


    /** 12) Tests applying the plugin and evaluates that the extensions set by plugin exists */
    @Test fun testEvaluatePluginExtension() {
        val project = ProjectBuilder.builder().build()

        // apply Java plugin
        project.pluginManager.apply(JavaPlugin::class.java)

        // project gradle.properties reference (correct/project1.properties.set can not be used directly!)
        val propertiesExtension = project.extensions.getByType(ExtraPropertiesExtension::class.java)
        correct1Properties.keys.forEach { key ->
            propertiesExtension[key as String] = correct1Properties.getProperty(key)
        }

        // apply plugin
        project.pluginManager.apply(RunTestsSeparateJVMPlugin::class.java)

        // assert that extension exists and is configured correctly
        val extension = project.extensions.getByType(RunTestsSeparateJVMPluginExtension::class.java)

        Assert.assertEquals(
            RunTestsSeparateJVMPlugin.parseListByCommas(
                correct1Properties.getProperty(RunTestsSeparateJVMPlugin.KEY_SEQUENTIAL)
            ), extension.sequentialTests.get()
        )

        Assert.assertEquals(
            RunTestsSeparateJVMPlugin.parseListByCommas(
                correct1Properties.getProperty(RunTestsSeparateJVMPlugin.KEY_PARALLEL)
            ), extension.parallelTests.get()
        )

        // assert that extension is saved by name provided in plugin
        val findByName = project.extensions.findByName(RunTestsSeparateJVMPlugin.KEY_EXTENSION)
        Assert.assertNotNull(findByName)
        Assert.assertEquals(extension, findByName!!)
    }


    /** 13) Tests applying the plugin and evaluates that the tasks created are correct */
    @Test fun testEvaluateTasks() {
        val project = ProjectBuilder.builder().build()

        // apply Java plugin
        project.pluginManager.apply(JavaPlugin::class.java)

        // project gradle.properties reference (correct/project1.properties.set can not be used directly!)
        val propertiesExtension = project.extensions.getByType(ExtraPropertiesExtension::class.java)
        correct1Properties.keys.forEach { key ->
            propertiesExtension[key as String] = correct1Properties.getProperty(key)
        }

        // apply plugin
        project.pluginManager.apply(RunTestsSeparateJVMPlugin::class.java)

        // assert that tasks "testSeparateJVMSequentially" / "testSeparateJVMInParallel" added correctly
        val testSequentially = project.tasks.getByName(
            RunTestsSeparateJVMPlugin.sequentialTestsTaskName
        ) as org.gradle.api.tasks.testing.Test

        val testInParallel = project.tasks.getByName(
            RunTestsSeparateJVMPlugin.parallelTestsTaskName
        ) as org.gradle.api.tasks.testing.Test

        Assert.assertEquals("verification", testSequentially.group)
        Assert.assertEquals(1, testSequentially.maxParallelForks)
        Assert.assertEquals(1, testSequentially.forkEvery)
        Assert.assertEquals(
            RunTestsSeparateJVMPlugin.parseListByCommas(
                propertiesExtension[RunTestsSeparateJVMPlugin.KEY_SEQUENTIAL] as String
            ),
            testSequentially.filter.includePatterns
        )
        Assert.assertEquals(
            RunTestsSeparateJVMPlugin.parseListByCommas(
                propertiesExtension[RunTestsSeparateJVMPlugin.KEY_PARALLEL] as String
            ),
            testSequentially.filter.excludePatterns
        )

        Assert.assertEquals("verification", testInParallel.group)
        Assert.assertEquals(1, testInParallel.forkEvery)
        Assert.assertEquals(
            RunTestsSeparateJVMPlugin.parseListByCommas(
                propertiesExtension[RunTestsSeparateJVMPlugin.KEY_PARALLEL] as String
            ),
            testInParallel.filter.includePatterns
        )
        Assert.assertEquals(
            RunTestsSeparateJVMPlugin.parseListByCommas(
                propertiesExtension[RunTestsSeparateJVMPlugin.KEY_SEQUENTIAL] as String
            ),
            testInParallel.filter.excludePatterns
        )
    }


    /** 14) Tests applying the plugin and evaluates that the tasks created are correct when inheriting configuration */
    @Test fun testEvaluateTasksInheritedConfiguration() {
        val project = ProjectBuilder.builder().build()

        // apply Java plugin
        project.pluginManager.apply(JavaPlugin::class.java)

        // project gradle.properties reference (correct/project1.properties.set can not be used directly!)
        val propertiesExtension = project.extensions.getByType(ExtraPropertiesExtension::class.java)
        correct6Properties.keys.forEach { key ->
            propertiesExtension[key as String] = correct6Properties.getProperty(key)
        }

        // configure standard Gradle task named "test"
        val testTask = project.tasks.getByName("test") as org.gradle.api.tasks.testing.Test
        testTask.group              = "banana"
        testTask.ignoreFailures     = true
        testTask.failFast           = true
        testTask.jvmArgs            = listOf("-banana")
        testTask.maxHeapSize        = "2G"
        testTask.maxParallelForks   = 4
        testTask.minHeapSize        = "1G"
        testTask.timeout.set(Duration.of(30, ChronoUnit.MINUTES))

        // apply plugin
        project.pluginManager.apply(RunTestsSeparateJVMPlugin::class.java)

        // assert that tasks "testSeparateJVMSequentially" / "testSeparateJVMInParallel" added correctly
        val testSequentially = project.tasks.getByName(
            RunTestsSeparateJVMPlugin.sequentialTestsTaskName
        ) as org.gradle.api.tasks.testing.Test

        val testInParallel = project.tasks.getByName(
            RunTestsSeparateJVMPlugin.parallelTestsTaskName
        ) as org.gradle.api.tasks.testing.Test

        Assert.assertEquals(testTask.group, testSequentially.group)
        Assert.assertEquals(testTask.ignoreFailures, testSequentially.ignoreFailures)
        Assert.assertEquals(testTask.failFast, testSequentially.failFast)
        Assert.assertEquals(testTask.jvmArgs, testSequentially.jvmArgs)
        Assert.assertEquals(testTask.maxHeapSize, testSequentially.maxHeapSize)
        Assert.assertEquals(testTask.minHeapSize, testSequentially.minHeapSize)
        Assert.assertEquals(testTask.timeout.get(), testSequentially.timeout.get())

        Assert.assertEquals(testTask.group, testInParallel.group)
        Assert.assertEquals(testTask.ignoreFailures, testInParallel.ignoreFailures)
        Assert.assertEquals(testTask.failFast, testInParallel.failFast)
        Assert.assertEquals(testTask.jvmArgs, testInParallel.jvmArgs)
        Assert.assertEquals(testTask.maxHeapSize, testInParallel.maxHeapSize)
        Assert.assertEquals(testTask.maxParallelForks, testInParallel.maxParallelForks)
        Assert.assertEquals(testTask.minHeapSize, testInParallel.minHeapSize)
        Assert.assertEquals(testTask.timeout.get(), testInParallel.timeout.get())
    }


    /** 15) Tests applying the plugin and evaluates the standard "test" task */
    @Test fun testEvaluateGradleTestTask() {
        val project = ProjectBuilder.builder().build()

        // apply Java plugin
        project.pluginManager.apply(JavaPlugin::class.java)

        // project gradle.properties reference (correct/project1.properties.set can not be used directly!)
        val propertiesExtension = project.extensions.getByType(ExtraPropertiesExtension::class.java)
        correct1Properties.keys.forEach { key ->
            propertiesExtension[key as String] = correct1Properties.getProperty(key)
        }

        // apply plugin
        project.pluginManager.apply(RunTestsSeparateJVMPlugin::class.java)

        // assert that standard Gradle "test" task configured correctly
        val testTask = project.tasks.getByName("test") as org.gradle.api.tasks.testing.Test

        listOf(
            RunTestsSeparateJVMPlugin.sequentialTestsTaskName,
            RunTestsSeparateJVMPlugin.parallelTestsTaskName
        ).forEach {
            Assert.assertTrue(testTask.dependsOn.contains(it))
        }

        listOf(
            RunTestsSeparateJVMPlugin.parseListByCommas(
                propertiesExtension[RunTestsSeparateJVMPlugin.KEY_SEQUENTIAL] as String
            ),
            RunTestsSeparateJVMPlugin.parseListByCommas(
                propertiesExtension[RunTestsSeparateJVMPlugin.KEY_PARALLEL] as String
            )
        ).forEach {
            Assert.assertTrue(testTask.filter.excludePatterns.containsAll(it))
        }
    }


    /** 16) Tests applying the plugin and evaluates the "test" task when dependencies disabled (system properties) */
    @Test fun testEvaluateGradleTestTaskDisabledBySystemProperty() {
        val project = ProjectBuilder.builder().build()

        // apply Java plugin
        project.pluginManager.apply(JavaPlugin::class.java)

        // project gradle.properties reference (correct/project1.properties.set can not be used directly!)
        val propertiesExtension = project.extensions.getByType(ExtraPropertiesExtension::class.java)
        correct1Properties.keys.forEach { key ->
            propertiesExtension[key as String] = correct1Properties.getProperty(key)
        }

        restoreSystemProperties {
            System.setProperty(RunTestsSeparateJVMPlugin.KEY_DISABLEDEPENDENCIES, "Disabled :(")

            // assert that system property is set correctly
            Assert.assertEquals("Disabled :(", System.getProperty(RunTestsSeparateJVMPlugin.KEY_DISABLEDEPENDENCIES))

            // apply plugin
            project.pluginManager.apply(RunTestsSeparateJVMPlugin::class.java)

            // assert that standard Gradle "test" task configured correctly
            val testTask = project.tasks.getByName("test") as org.gradle.api.tasks.testing.Test

            listOf(
                RunTestsSeparateJVMPlugin.sequentialTestsTaskName,
                RunTestsSeparateJVMPlugin.parallelTestsTaskName
            ).forEach {
                Assert.assertFalse(testTask.dependsOn.contains(it))
            }

            listOf(
                RunTestsSeparateJVMPlugin.parseListByCommas(
                    propertiesExtension[RunTestsSeparateJVMPlugin.KEY_SEQUENTIAL] as String
                ),
                RunTestsSeparateJVMPlugin.parseListByCommas(
                    propertiesExtension[RunTestsSeparateJVMPlugin.KEY_PARALLEL] as String
                )
            ).forEach {
                Assert.assertTrue(testTask.filter.excludePatterns.containsAll(it))
            }
        }
    }


    /** 17) Tests applying the plugin and evaluates the "test" task when dependencies disabled (environment variable) */
    @Test fun testEvaluateGradleTestTaskDisabledByEnvironmentVariable() {
        val project = ProjectBuilder.builder().build()

        // apply Java plugin
        project.pluginManager.apply(JavaPlugin::class.java)

        // project gradle.properties reference (correct/project1.properties.set can not be used directly!)
        val propertiesExtension = project.extensions.getByType(ExtraPropertiesExtension::class.java)
        correct1Properties.keys.forEach { key ->
            propertiesExtension[key as String] = correct1Properties.getProperty(key)
        }

        withEnvironmentVariable(
            RunTestsSeparateJVMPlugin.KEY_DISABLEDEPENDENCIES, "Disabled :("
        ).execute {
            // assert that environment variable is set correctly
            Assert.assertEquals("Disabled :(", System.getenv(RunTestsSeparateJVMPlugin.KEY_DISABLEDEPENDENCIES))

            // apply plugin
            project.pluginManager.apply(RunTestsSeparateJVMPlugin::class.java)

            // assert that standard Gradle "test" task configured correctly
            val testTask = project.tasks.getByName("test") as org.gradle.api.tasks.testing.Test

            listOf(
                RunTestsSeparateJVMPlugin.sequentialTestsTaskName,
                RunTestsSeparateJVMPlugin.parallelTestsTaskName
            ).forEach {
                Assert.assertFalse(testTask.dependsOn.contains(it))
            }

            listOf(
                RunTestsSeparateJVMPlugin.parseListByCommas(
                    propertiesExtension[RunTestsSeparateJVMPlugin.KEY_SEQUENTIAL] as String
                ),
                RunTestsSeparateJVMPlugin.parseListByCommas(
                    propertiesExtension[RunTestsSeparateJVMPlugin.KEY_PARALLEL] as String
                )
            ).forEach {
                Assert.assertTrue(testTask.filter.excludePatterns.containsAll(it))
            }
        }
    }


    /** 18) Tests applying the plugin (with correct project properties file) but no TestRetryExtension */
    @Test fun testApplyPluginWithoutTestRetryExtensionToProject() {
        listOf(correct7Properties, correct8Properties).forEach {
            val project = ProjectBuilder.builder().build()

            // apply Java plugin
            project.pluginManager.apply(JavaPlugin::class.java)

            // project gradle.properties reference (project_correct7/8.properties. set can not be used directly!)
            val propertiesExtension = project.extensions.getByType(ExtraPropertiesExtension::class.java)
            it.keys.forEach { key ->
                propertiesExtension[key as String] = it.getProperty(key)
            }

            // apply plugin
            project.pluginManager.apply(RunTestsSeparateJVMPlugin::class.java)

            // assert that plugin was applied to the project
            Assert.assertTrue(project.plugins.hasPlugin(RunTestsSeparateJVMPlugin::class.java))
        }
    }


    /** 19) Tests applying the plugin (with correct project properties file) and TestRetryExtension */
    @Test fun testApplyPluginWithTestRetryExtensionToProject() {
        val project = ProjectBuilder.builder().build()

        // apply Java plugin
        project.pluginManager.apply(JavaPlugin::class.java)

        // project gradle.properties reference (correct/project7.properties.set can not be used directly!)
        val propertiesExtension = project.extensions.getByType(ExtraPropertiesExtension::class.java)
        correct7Properties.keys.forEach { key ->
            propertiesExtension[key as String] = correct7Properties.getProperty(key)
        }

        // apply "test-retry" plugin
        project.pluginManager.apply(TestRetryPlugin::class.java)
        Assert.assertTrue(project.plugins.hasPlugin(TestRetryPlugin::class.java))

        // configure standard Gradle task named "test"
        val testTask = project.tasks.getByName("test") as org.gradle.api.tasks.testing.Test
        val testTaskTestRetryExtension = testTask.extensions.getByType(TestRetryTaskExtension::class.java)
        testTaskTestRetryExtension.maxRetries.set(10)
        testTaskTestRetryExtension.failOnPassedAfterRetry.set(true)
        testTaskTestRetryExtension.maxFailures.set(10)

        // apply plugin
        project.pluginManager.apply(RunTestsSeparateJVMPlugin::class.java)

        // assert that "testSeparateJVMSequentially"/"testSeparateJVMInParallel" inherited TestRetryExtension correctly
        val testSequentially = project.tasks.getByName(
            RunTestsSeparateJVMPlugin.sequentialTestsTaskName
        ) as org.gradle.api.tasks.testing.Test
        val testSequentiallyRetryExtension = testSequentially.extensions.getByType(TestRetryTaskExtension::class.java)

        val testInParallel = project.tasks.getByName(
            RunTestsSeparateJVMPlugin.parallelTestsTaskName
        ) as org.gradle.api.tasks.testing.Test
        val testInParallelRetryExtension = testInParallel.extensions.getByType(TestRetryTaskExtension::class.java)

        Assert.assertEquals(
            testTaskTestRetryExtension.maxRetries.get(),
            testSequentiallyRetryExtension.maxRetries.get()
        )
        Assert.assertEquals(
            testTaskTestRetryExtension.failOnPassedAfterRetry.get(),
            testSequentiallyRetryExtension.failOnPassedAfterRetry.get()
        )
        Assert.assertEquals(
            testTaskTestRetryExtension.maxFailures.get(),
            testSequentiallyRetryExtension.maxFailures.get()
        )

        Assert.assertEquals(
            testTaskTestRetryExtension.maxRetries.get(),
            testInParallelRetryExtension.maxRetries.get()
        )
        Assert.assertEquals(
            testTaskTestRetryExtension.failOnPassedAfterRetry.get(),
            testInParallelRetryExtension.failOnPassedAfterRetry.get()
        )
        Assert.assertEquals(
            testTaskTestRetryExtension.maxFailures.get(),
            testInParallelRetryExtension.maxFailures.get()
        )
    }


    /** 20) Tests applying the plugin with explicit timeout */
    @Test fun testApplyPluginWithExplicitTimeout() {
        listOf(correct9Properties, correct10Properties).forEach {
            val project = ProjectBuilder.builder().build()

            // apply Java plugin
            project.pluginManager.apply(JavaPlugin::class.java)

            // project gradle.properties reference (correct/project.properties.set can not be used directly!)
            val propertiesExtension = project.extensions.getByType(ExtraPropertiesExtension::class.java)
            it.keys.forEach { key ->
                propertiesExtension[key as String] = it.getProperty(key)
            }

            // apply plugin
            project.pluginManager.apply(RunTestsSeparateJVMPlugin::class.java)

            // assert that extension exists and is configured correctly
            val extension = project.extensions.getByType(RunTestsSeparateJVMPluginExtension::class.java)

            Assert.assertEquals(
                it[RunTestsSeparateJVMPlugin.KEY_TIMEOUT_SEQUENTIAL].toString().toLong(),
                extension.sequentialTimeout.get()
            )
            Assert.assertEquals(
                it[RunTestsSeparateJVMPlugin.KEY_TIMEOUT_PARALLEL].toString().toLong(),
                extension.parallelTimeout.get()
            )

            // assert that task timeouts are configured correctly
            val testSequentially = project.tasks.getByName(
                RunTestsSeparateJVMPlugin.sequentialTestsTaskName
            ) as org.gradle.api.tasks.testing.Test
            val testInParallel = project.tasks.getByName(
                RunTestsSeparateJVMPlugin.parallelTestsTaskName
            ) as org.gradle.api.tasks.testing.Test

            Assert.assertEquals(
                Duration.of(
                    it[RunTestsSeparateJVMPlugin.KEY_TIMEOUT_SEQUENTIAL].toString().toLong(), ChronoUnit.MINUTES
                ),
                testSequentially.timeout.get()
            )
            Assert.assertEquals(
                Duration.of(
                    it[RunTestsSeparateJVMPlugin.KEY_TIMEOUT_PARALLEL].toString().toLong(), ChronoUnit.MINUTES
                ),
                testInParallel.timeout.get()
            )
        }
    }


    /** 21) Tests applying the plugin with explicit timeout (but is invalid) */
    @Test fun testApplyPluginWithExplicitTimeoutWrong() {
        listOf(wrong5Properties, wrong6Properties).forEach {
            val project = ProjectBuilder.builder().build()

            // apply Java plugin
            project.pluginManager.apply(JavaPlugin::class.java)

            // project gradle.properties reference (correct/project.properties.set can not be used directly!)
            val propertiesExtension = project.extensions.getByType(ExtraPropertiesExtension::class.java)
            it.keys.forEach { key ->
                propertiesExtension[key as String] = it.getProperty(key)
            }

            try {
                // try applying plugin (should fail)
                project.pluginManager.apply(RunTestsSeparateJVMPlugin::class.java)
            } catch (e: Exception) {
                // assert applying did not work
                // INFO: equal to check on InvalidUserDataException as it is based on it
                Assert.assertTrue(e.cause is TimeoutValueInvalidException)
            }

            Assert.assertFalse(project.plugins.hasPlugin(RunTestsSeparateJVMPlugin::class.java))
        }
    }
}
