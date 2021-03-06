package com.github.thahnen

import org.gradle.api.InvalidUserDataException


/**
 *  Base extension for every extension thrown by this plugin
 *
 *  @author thahnen
 */
open class RunTestsSeparateJVMException(message: String) : InvalidUserDataException(message)


/**
 *  Thrown when this plugin is applied to a project which does not have the Gradle provided Java plugin applied. This
 *  plugin tests against the project test tasks (only provided by the Java plugin)!
 */
open class PluginAppliedUnnecessarilyException(message: String) : RunTestsSeparateJVMException(message)


/**
 *  Thrown when this plugin is applied to a project which does not have a Gradle task named "test" or task is not of
 *  type Test (necessary when inheriting configuration)!
 */
open class TaskNamedTestNotFoundException(message: String) : RunTestsSeparateJVMException(message)


/**
 *  Exception thrown when no configuration provided in projects gradle.properties file
 */
open class MissingPropertiesEntryException(message: String) : RunTestsSeparateJVMException(message)


/**
 *  Exception thrown when value of necessary property entry is invalid (no content)
 */
open class PropertiesEntryInvalidException(message: String) : RunTestsSeparateJVMException(message)


/**
 *  Exception thrown when test class provided in property entry for both sequential / parallel testing
 */
open class TestInBothTasksException(message: String) : RunTestsSeparateJVMException(message)


/**
 *  Exception thrown when timeout property provided cannot be cast to long!
 */
open class TimeoutValueInvalidException(message: String) : RunTestsSeparateJVMException(message)
