package com.github.thahnen

import java.io.FileInputStream
import java.io.IOException
import java.util.Properties

import org.junit.Assert
import org.junit.Before
import org.junit.Test

import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.plugins.JavaPlugin
import org.gradle.testfixtures.ProjectBuilder

import com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable


/**
 *  RunTestsSeparateJVMPluginTest:
 *  =============================
 *
 *  jUnit test cases on the RunTestsSeparateJVMPlugin
 */
open class RunTestsSeparateJVMPluginTest {

    // test cases properties file
    private val correct1ProjectPropertiesPath   = this.javaClass.classLoader.getResource("project_correct1.properties")!!
                                                    .path.replace("%20", " ")
    private val correct2ProjectPropertiesPath   = this.javaClass.classLoader.getResource("project_correct2.properties")!!
                                                    .path.replace("%20", " ")
    private val correct3ProjectPropertiesPath   = this.javaClass.classLoader.getResource("project_correct3.properties")!!
                                                    .path.replace("%20", " ")
    private val wrong1ProjectPropertiesPath     = this.javaClass.classLoader.getResource("project_wrong1.properties")!!
                                                    .path.replace("%20", " ")
    private val wrong2ProjectPropertiesPath     = this.javaClass.classLoader.getResource("project_wrong2.properties")!!
                                                    .path.replace("%20", " ")

    // test cases properties
    private val correct1Properties = Properties()
    private val correct2Properties = Properties()
    private val correct3Properties = Properties()
    private val wrong1Properties = Properties()
    private val wrong2Properties = Properties()


    /** 0) Configuration to read properties once before running multiple tests using them */
    @Throws(IOException::class)
    @Before fun configureTestsuite() {
        correct1Properties.load(FileInputStream(correct1ProjectPropertiesPath))
        correct2Properties.load(FileInputStream(correct2ProjectPropertiesPath))
        correct3Properties.load(FileInputStream(correct3ProjectPropertiesPath))
        wrong1Properties.load(FileInputStream(wrong1ProjectPropertiesPath))
        wrong2Properties.load(FileInputStream(wrong2ProjectPropertiesPath))
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

            // project gradle.properties reference (project_correct1.properties.set can not be used directly!)
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


    /** 4) Tests applying the plugin (with incorrect environment variables used) */
    /*@Test fun testApplyPluginWithIncorrectEnvironmentVariablesToProject() {
        //
    }*/


    /** 5) Tests applying the plugin (with correct project properties file) */
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


    /** 6) Tests applying the plugin (with environment variables used) */
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


    /** 7) Tests applying the plugin and evaluates that the extensions set by plugin exists */
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
    }


    /** 8) Tests applying the plugin and evaluates that the tasks created are correct */
    /*@Test fun testEvaluateTasks() {
        //
    }*/
}
