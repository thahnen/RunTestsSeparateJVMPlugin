package com.github.thahnen

import java.io.FileInputStream
import java.io.IOException
import java.util.Properties

import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test

import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.plugins.JavaPlugin
import org.gradle.testfixtures.ProjectBuilder

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
        private val correct1ProjectPropertiesPath   = this::class.java.classLoader.getResource("project_correct1.properties")!!
                                                        .path.replace("%20", " ")
        private val correct2ProjectPropertiesPath   = this::class.java.classLoader.getResource("project_correct2.properties")!!
                                                        .path.replace("%20", " ")
        private val correct3ProjectPropertiesPath   = this::class.java.classLoader.getResource("project_correct3.properties")!!
                                                        .path.replace("%20", " ")
        private val correct4ProjectPropertiesPath   = this::class.java.classLoader.getResource("project_correct4.properties")!!
                                                        .path.replace("%20", " ")
        private val correct5ProjectPropertiesPath   = this::class.java.classLoader.getResource("project_correct5.properties")!!
                                                        .path.replace("%20", " ")
        private val wrong1ProjectPropertiesPath     = this::class.java.classLoader.getResource("project_wrong1.properties")!!
                                                        .path.replace("%20", " ")
        private val wrong2ProjectPropertiesPath     = this::class.java.classLoader.getResource("project_wrong2.properties")!!
                                                        .path.replace("%20", " ")
        private val wrong3ProjectPropertiesPath     = this::class.java.classLoader.getResource("project_wrong3.properties")!!
                                                        .path.replace("%20", " ")

        // test cases properties
        private val correct1Properties = Properties()
        private val correct2Properties = Properties()
        private val correct3Properties = Properties()
        private val correct4Properties = Properties()
        private val correct5Properties = Properties()
        private val wrong1Properties = Properties()
        private val wrong2Properties = Properties()
        private val wrong3Properties = Properties()


        /** 0) Configuration to read properties once before running multiple tests using them */
        @Throws(IOException::class)
        @BeforeClass @JvmStatic fun configureTestsuite() {
            correct1Properties.load(FileInputStream(correct1ProjectPropertiesPath))
            correct2Properties.load(FileInputStream(correct2ProjectPropertiesPath))
            correct3Properties.load(FileInputStream(correct3ProjectPropertiesPath))
            correct4Properties.load(FileInputStream(correct4ProjectPropertiesPath))
            correct5Properties.load(FileInputStream(correct5ProjectPropertiesPath))
            wrong1Properties.load(FileInputStream(wrong1ProjectPropertiesPath))
            wrong2Properties.load(FileInputStream(wrong2ProjectPropertiesPath))
            wrong3Properties.load(FileInputStream(wrong3ProjectPropertiesPath))
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
            assert(e.cause is PluginAppliedUnnecessarilyException)
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
            assert(e.cause is MissingPropertiesEntryException)
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
                assert(e.cause is PropertiesEntryInvalidException)
            }

            Assert.assertFalse(project.plugins.hasPlugin(RunTestsSeparateJVMPlugin::class.java))
        }
    }


    /** 5) Tests applying the plugin (with project properties file, tests in both properties) */
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
            assert(e.cause is TestInBothTasksException)
        }

        Assert.assertFalse(project.plugins.hasPlugin(RunTestsSeparateJVMPlugin::class.java))
    }


    /** 6) Tests applying the plugin (with incorrect environment variables used) */
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
                    assert(e.cause is PropertiesEntryInvalidException)
                }

                Assert.assertFalse(project.plugins.hasPlugin(RunTestsSeparateJVMPlugin::class.java))
            }
        }
    }


    /** 7) Tests applying the plugin (with correct project properties file) */
    @Test fun testApplyPluginWithCorrectPropertiesToProject() {
        listOf(correct1Properties, correct2Properties, correct3Properties).forEach {
            val project = ProjectBuilder.builder().build()

            // apply Java plugin
            project.pluginManager.apply(JavaPlugin::class.java)

            // project gradle.properties reference (project_correct1.properties.set can not be used directly!)
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


    /** 8) Tests applying the plugin (with environment variables used) */
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


    /** 9) Tests applying the plugin and evaluates that logger works correctly (with asterisks / full package) */
    @Test fun testEvaluateAsteriskAndPackageLogging() {
        listOf(correct4Properties, correct5Properties).forEach {
            val project = ProjectBuilder.builder().build()

            // apply Java plugin
            project.pluginManager.apply(JavaPlugin::class.java)

            // project gradle.properties reference (project_correct1.properties.set can not be used directly!)
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


    /** 10) Tests applying the plugin and evaluates that the extensions set by plugin exists */
    @Test fun testEvaluatePluginExtension() {
        val project = ProjectBuilder.builder().build()

        // apply Java plugin
        project.pluginManager.apply(JavaPlugin::class.java)

        // project gradle.properties reference (project_correct1.properties.set can not be used directly!)
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


    /** 11) Tests applying the plugin and evaluates that the tasks created are correct */
    @Test fun testEvaluateTasks() {
        val project = ProjectBuilder.builder().build()

        // apply Java plugin
        project.pluginManager.apply(JavaPlugin::class.java)

        // project gradle.properties reference (project_correct1.properties.set can not be used directly!)
        val propertiesExtension = project.extensions.getByType(ExtraPropertiesExtension::class.java)
        correct1Properties.keys.forEach { key ->
            propertiesExtension[key as String] = correct1Properties.getProperty(key)
        }

        // apply plugin
        project.pluginManager.apply(RunTestsSeparateJVMPlugin::class.java)

        // assert that tasks "testSeparateJVMSequentially" / "testSeparateJVMInParallel" added correctly
        val testSequentially = project.tasks.getByName(RunTestsSeparateJVMPlugin.sequentialTestsTaskName) as org.gradle.api.tasks.testing.Test
        val testInParallel = project.tasks.getByName(RunTestsSeparateJVMPlugin.parallelTestsTaskName) as org.gradle.api.tasks.testing.Test

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


    /** 12) Tests applying the plugin and evaluates the standard "test" task */
    @Test fun testEvaluateGradleTestTask() {
        val project = ProjectBuilder.builder().build()

        // apply Java plugin
        project.pluginManager.apply(JavaPlugin::class.java)

        // project gradle.properties reference (project_correct1.properties.set can not be used directly!)
        val propertiesExtension = project.extensions.getByType(ExtraPropertiesExtension::class.java)
        correct1Properties.keys.forEach { key ->
            propertiesExtension[key as String] = correct1Properties.getProperty(key)
        }

        // apply plugin
        project.pluginManager.apply(RunTestsSeparateJVMPlugin::class.java)

        // assert that standard Gradle "test" task configured correctly
        val testTask = project.tasks.getByName("test") as org.gradle.api.tasks.testing.Test

        listOf(RunTestsSeparateJVMPlugin.sequentialTestsTaskName, RunTestsSeparateJVMPlugin.parallelTestsTaskName).forEach {
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


    /** 13) Tests applying the plugin and evaluates the "test" task when dependencies disabled (system properties) */
    @Test fun testEvaluateGradleTestTaskDisabledBySystemProperty() {
        val project = ProjectBuilder.builder().build()

        // apply Java plugin
        project.pluginManager.apply(JavaPlugin::class.java)

        // project gradle.properties reference (project_correct1.properties.set can not be used directly!)
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

            listOf(RunTestsSeparateJVMPlugin.sequentialTestsTaskName, RunTestsSeparateJVMPlugin.parallelTestsTaskName).forEach {
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


    /** 14) Tests applying the plugin and evaluates the "test" task when dependencies disabled (environment variable) */
    @Test fun testEvaluateGradleTestTaskDisabledByEnvironmentVariable() {
        val project = ProjectBuilder.builder().build()

        // apply Java plugin
        project.pluginManager.apply(JavaPlugin::class.java)

        // project gradle.properties reference (project_correct1.properties.set can not be used directly!)
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

            listOf(RunTestsSeparateJVMPlugin.sequentialTestsTaskName, RunTestsSeparateJVMPlugin.parallelTestsTaskName).forEach {
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
}
