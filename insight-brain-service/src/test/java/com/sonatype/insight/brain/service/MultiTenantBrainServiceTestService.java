/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.sonatype.insight.brain.db.DatabaseContainer;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;

import com.google.inject.AbstractModule;

/**
 * MultiTenantBrainServiceTestService is for adding multi-tenant functionality into AbstractBrainServiceTest
 * as the MTIQ functionality is in child packages this is achieved using method handlers.
 */
public class MultiTenantBrainServiceTestService
{
  private static Class<? extends TestInsightBrainService> testInsightBrainServiceClass = null;

  private static Supplier<DatabaseContainer> initDatabaseContainerHandler = null;

  private static Consumer<AbstractBrainServiceTest> beforeTestHandler = null;

  private static Consumer<AbstractBrainServiceTest> afterTestHandler = null;

  private static Consumer<TestCLMServer> afterAllTestsHandler = null;

  private static DatabaseContainer databaseContainer = null;

  private static Configurator configurator = null;

  private static AbstractModule brainModules = null;

  /**
   * setup will register an override TestInsightBrainService class and handlers for
   * database container initialization, and before, after, after all test methods.
   * It will also cache AbstractModule to allow mock class implementations in insight brain.
   */
  public static void setup(
      Class<? extends TestInsightBrainService> testInsightBrainServiceClass,
      Supplier<DatabaseContainer> initDatabaseContainerFunc,
      Consumer<AbstractBrainServiceTest> beforeTestsHandler,
      Consumer<AbstractBrainServiceTest> afterTestHandler,
      Consumer<TestCLMServer> afterAllTestsHandler,
      AbstractModule brainModules
  )
  {
    MultiTenantBrainServiceTestService.testInsightBrainServiceClass = testInsightBrainServiceClass;
    MultiTenantBrainServiceTestService.initDatabaseContainerHandler = initDatabaseContainerFunc;
    MultiTenantBrainServiceTestService.beforeTestHandler = beforeTestsHandler;
    MultiTenantBrainServiceTestService.afterTestHandler = afterTestHandler;
    MultiTenantBrainServiceTestService.afterAllTestsHandler = afterAllTestsHandler;
    MultiTenantBrainServiceTestService.brainModules = brainModules;
  }

  /**
   * resetTestInstances will clear test DB and insight brain configuration ready for new tests.
   */
  public static void resetTestInstances() {
    databaseContainer = null;
    configurator = null;
  }

  /**
   * stop will remove MTIQ overrides and allow AbstractBrainServiceTest to be run for single tenant tests.
   */
  public static void stop() {
    resetTestInstances();

    testInsightBrainServiceClass = null;
    initDatabaseContainerHandler = null;
    beforeTestHandler = null;
    afterTestHandler = null;
    afterAllTestsHandler = null;
    brainModules = null;
  }

  public static boolean isTestingAgainstMtiq() {
    return testInsightBrainServiceClass != null;
  }

  /**
   * createNewTestInsightBrainService will return a new test instance of testInsightBrainServiceClass.
   */
  public static TestInsightBrainService createNewTestInsightBrainService()
      throws InstantiationException, IllegalAccessException
  {
    if (testInsightBrainServiceClass != null) {
      return testInsightBrainServiceClass.newInstance();
    }
    return null;
  }

  /**
   * beforeTestHandler should be called from @before.
   */
  public static void beforeTestHandler(AbstractBrainServiceTest brainServiceTest) {
    if (beforeTestHandler != null) {
      beforeTestHandler.accept(brainServiceTest);
    }
  }

  /**
   * beforeTestHandler should be called from @after.
   */
  public static void afterTestHandler(AbstractBrainServiceTest brainServiceTest) {
    if (afterTestHandler != null) {
      afterTestHandler.accept(brainServiceTest);
    }
  }

  /**
   * beforeTestHandler should be called from @afterClass.
   */
  public static void afterAllTestsHandler(TestCLMServer testCLMServer) {
    if (afterAllTestsHandler != null) {
      afterAllTestsHandler.accept(testCLMServer);
    }
  }

  public static AbstractModule getBrainModules() {
    return brainModules;
  }

  public static Configurator getConfigurator() {
    return configurator;
  }

  public static void setConfigurator(Configurator configurator) {
    MultiTenantBrainServiceTestService.configurator = configurator;
  }

  public static void setDatabaseContainer(DatabaseContainer databaseContainer) {
    MultiTenantBrainServiceTestService.databaseContainer = databaseContainer;
  }

  public static DatabaseContainer getDatabaseContainer() {
    if (databaseContainer == null && initDatabaseContainerHandler != null) {
      initDatabaseContainerHandler.get();
    }
    return databaseContainer;
  }
}
