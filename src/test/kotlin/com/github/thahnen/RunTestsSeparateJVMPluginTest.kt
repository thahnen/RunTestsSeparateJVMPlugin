package com.github.thahnen

import java.io.FileInputStream
import java.io.IOException
import java.util.Properties

import org.junit.Assert
import org.junit.Before
import org.junit.Test

import org.gradle.api.plugins.JavaPlugin
import org.gradle.testfixtures.ProjectBuilder


/**
 *  RunTestsSeparateJVMPluginTest:
 *  =============================
 *
 *  jUnit test cases on the RunTestsSeparateJVMPlugin
 */
open class RunTestsSeparateJVMPluginTest {

    // test cases properties file
    private val correctProjectPropertiesPath    = this.javaClass.classLoader.getResource("project.properties")!!
                                                    .path.replace("%20", " ")
    private val wrong1ProjectPropertiesPath     = this.javaClass.classLoader.getResource("project_wrong1.properties")!!
                                                    .path.replace("%20", " ")
    private val wrong2ProjectPropertiesPath     = this.javaClass.classLoader.getResource("project_wrong2.properties")!!
                                                    .path.replace("%20", " ")

    // test cases properties
    private val correctProperties = Properties()
    private val wrong1Properties = Properties()
    private val wrong2Properties = Properties()


    /** 0) Configuration to read properties once before running multiple tests using them */
    @Throws(IOException::class)
    @Before fun configureTestsuite() {
        correctProperties.load(FileInputStream(correctProjectPropertiesPath))
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
}
