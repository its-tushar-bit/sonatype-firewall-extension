/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.db.rule.DatabaseContainerRule;
import com.sonatype.insight.brain.search.SearchIndexRule;
import com.sonatype.insight.brain.testing.AbstractBaseIntegrationTest;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.Test;
import org.springframework.boot.test.context.TestConfiguration;

public class AbstractMultiTenantBaseIntegrationTestConfigurationTest
{
  @Test
  public void shouldCollectNestedTestConfigurationsFromTheMultiTenantTestHierarchy() {
    assertThat(AbstractMultiTenantBaseIntegrationTest.collectNestedTestConfigurationClasses(TestHarness.class))
        .containsExactly(
            AbstractMultiTenantBaseIntegrationTest.MtiqTestConfiguration.class,
            AbstractMultiTenantBaseIntegrationTest.MtiqTestConfigurationWithTestEncryptionKeyStore.class,
            TestHarness.TestHarnessConfig.class);
  }

  @Test
  public void shouldKeepTheDefaultMtiqDatabaseConfiguratorAsASingleton() throws Exception {
    Field defaultConfiguratorField =
        AbstractMultiTenantBaseIntegrationTest.class.getDeclaredField("DEFAULT_MTIQ_DATABASE_CONFIGURATOR");

    defaultConfiguratorField.setAccessible(true);

    Object defaultConfigurator = defaultConfiguratorField.get(null);

    assertThat(defaultConfigurator)
        .isInstanceOf(AbstractMultiTenantBaseIntegrationTest.MtiqDatabaseConfigurator.class)
        .isSameAs(defaultConfiguratorField.get(null));
  }

  @Test
  public void shouldRestartReusableServerWhenNestedMtiqTestConfigurationsDiffer() throws Exception {
    TestInsightBrainService.Configurator configurator = mock(TestInsightBrainService.Configurator.class);
    when(configurator.isReusable()).thenReturn(true);

    TestInsightBrainServiceRule brainRule = mock(TestInsightBrainServiceRule.class);
    when(brainRule.getIsHdsProxyRequired()).thenReturn(false);
    when(brainRule.getConfigurator()).thenReturn(configurator);

    TestCLMServer runningServer = mock(TestCLMServer.class);
    when(runningServer.getCLMServer()).thenReturn(brainRule);

    Field testClmServerField = AbstractBaseIntegrationTest.class.getDeclaredField("testCLMServer");
    testClmServerField.setAccessible(true);
    Object previousServer = testClmServerField.get(null);

    Field testConfigurationClassField = AbstractBaseIntegrationTest.class.getDeclaredField("testConfigurationClass");
    testConfigurationClassField.setAccessible(true);
    Object previousConfigurationClass = testConfigurationClassField.get(null);

    Field testConfigurationClassesField = getOptionalField("testConfigurationClasses");
    Object previousConfigurationClasses =
        testConfigurationClassesField == null ? null : testConfigurationClassesField.get(null);

    try {
      testClmServerField.set(null, runningServer);
      testConfigurationClassField.set(null, AbstractBaseIntegrationTest.class);
      if (testConfigurationClassesField != null) {
        testConfigurationClassesField.set(null, new TestHarness().getTestConfigurationClasses());
      }

      SecondHarness secondHarness = new SecondHarness();
      secondHarness.databaseContainerRule = mock(DatabaseContainerRule.class);
      when(secondHarness.databaseContainerRule.isFixtureReusable()).thenReturn(true);
      secondHarness.searchIndexRule = mock(SearchIndexRule.class);
      when(secondHarness.searchIndexRule.isFixtureReusable()).thenReturn(true);

      invokeMaybeStopTestIqServer(secondHarness, configurator);

      verify(runningServer).stop();
      assertThat(testClmServerField.get(null)).isNull();
    }
    finally {
      testClmServerField.set(null, previousServer);
      testConfigurationClassField.set(null, previousConfigurationClass);
      if (testConfigurationClassesField != null) {
        testConfigurationClassesField.set(null, previousConfigurationClasses);
      }
    }
  }

  private static void invokeMaybeStopTestIqServer(
      AbstractBaseIntegrationTest test,
      TestInsightBrainService.Configurator configurator) throws Exception
  {
    Method method = AbstractBaseIntegrationTest.class.getDeclaredMethod("maybeStopTestIqServer",
        TestInsightBrainService.Configurator.class);
    method.setAccessible(true);
    method.invoke(test, configurator);
  }

  private static Field getOptionalField(String fieldName) throws Exception {
    try {
      Field field = AbstractBaseIntegrationTest.class.getDeclaredField(fieldName);
      field.setAccessible(true);
      return field;
    }
    catch (NoSuchFieldException e) {
      return null;
    }
  }

  private static class TestHarness
      extends AbstractMultiTenantBaseIntegrationTest
  {
    @Override
    public void setUpTestLicenseThreatGroups() {
      // no-op
    }

    @TestConfiguration
    static class TestHarnessConfig
    {
    }
  }

  private static class SecondHarness
      extends AbstractMultiTenantBaseIntegrationTest
  {
    @Override
    public void setUpTestLicenseThreatGroups() {
      // no-op
    }

    @TestConfiguration
    static class SecondHarnessConfig
    {
    }
  }
}
