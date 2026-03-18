/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.util.HashSet;
import java.util.Set;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.service.ConfigurationListener;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.test.LogOutput;

import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class BaseUrlConfigurationMigratorTest
    extends AbstractComponentTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(BaseUrlConfigurationMigrator.class);

  @Inject
  private MigrationTrackerDAO migrationTrackerDAO;

  @Inject
  private InsightConfig insightConfig;

  @Inject
  private BaseUrlConfigurationMigrator baseUrlConfigurationMigrator;

  @Mock
  private ConfigurationListener mockBaseUrlConfigurationListener;

  @Override
  public void configure(Binder binder) {
    // Add the mock listener to the multibinder set
    Multibinder.newSetBinder(binder, ConfigurationListener.class)
        .addBinding()
        .toInstance(mockBaseUrlConfigurationListener);
    super.configure(binder);
  }

  @Before
  @After
  public void clear() {
    migrationTrackerDAO.deleteById(BaseUrlConfigurationMigrator.MIGRATION_ID);
  }

  @Test
  public void testMigrate_FirstRun_InvalidConfig() {
    insightConfig.setBaseUrl("invalid");

    baseUrlConfigurationMigrator.migrate();

    assertThat(migrationTrackerDAO.isTrackerPresent(BaseUrlConfigurationMigrator.MIGRATION_ID)).isTrue();
    assertThat(systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.BASE_URL)).isNull();
    assertThat(systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.FORCE_BASE_URL)).isNull();
    assertThat(logOutput).atWarnLevel().contains(BaseUrlConfigurationMigrator.OBSOLETE_CONFIG_MESSAGE);
  }

  @Test
  public void testMigrate_FirstRun_BaseUrlNull_ForceBaseUrlNull() {
    testMigrate(false, null, null);
  }

  @Test
  public void testMigrate_FirstRun_BaseUrlNull_ForceBaseUrlFalse() {
    testMigrate(false, null, false);
  }

  @Test
  public void testMigrate_FirstRun_BaseUrlNull_ForceBaseUrlTrue() {
    testMigrate(false, null, true);
  }

  @Test
  public void testMigrate_FirstRun_BaseUrlNotNull_ForceBaseUrlNull() {
    testMigrate(false, "http://baseUrl/", null);
  }

  @Test
  public void testMigrate_FirstRun_BaseUrlNotNull_ForceBaseUrlFalse() {
    testMigrate(false, "http://baseUrl/", false);
  }

  @Test
  public void testMigrate_FirstRun_BaseUrlNotNull_ForceBaseUrlTrue() {
    testMigrate(false, "http://baseUrl/", true);
  }

  @Test
  public void testMigrate_AlreadyMigrated_BaseUrlNull_ForceBaseUrlNull() {
    systemConfigurationPropertyDAO.set(SystemConfigurationProperty.BASE_URL, "http://baseUrl/");
    systemConfigurationPropertyDAO.set(SystemConfigurationProperty.FORCE_BASE_URL, String.valueOf(Boolean.TRUE));

    testMigrate(true, null, null);

    assertThat(systemConfigurationPropertyDAO.get(SystemConfigurationProperty.BASE_URL)).isEqualTo("http://baseUrl/");
    assertThat(systemConfigurationPropertyDAO.get(SystemConfigurationProperty.FORCE_BASE_URL)).isEqualTo(
        String.valueOf(Boolean.TRUE));
  }

  @Test
  public void testMigrate_AlreadyMigrated_BaseUrlNotNull_ForceBaseUrlNotNull() {
    systemConfigurationPropertyDAO.set(SystemConfigurationProperty.BASE_URL, "http://baseUrl1/");
    systemConfigurationPropertyDAO.set(SystemConfigurationProperty.FORCE_BASE_URL, String.valueOf(Boolean.TRUE));

    testMigrate(true, "http://baseUrl2/", false);

    assertThat(systemConfigurationPropertyDAO.get(SystemConfigurationProperty.BASE_URL)).isEqualTo("http://baseUrl1/");
    assertThat(systemConfigurationPropertyDAO.get(SystemConfigurationProperty.FORCE_BASE_URL)).isEqualTo(
        String.valueOf(Boolean.TRUE));
  }

  private void testMigrate(boolean alreadyMigrated, String configBaseUrl, Boolean configForceBaseUrl) {
    insightConfig.setBaseUrl(configBaseUrl);
    insightConfig.setForceBaseUrl(configForceBaseUrl);
    if (alreadyMigrated) {
      migrationTrackerDAO.insert(new MigrationTracker(BaseUrlConfigurationMigrator.MIGRATION_ID));
    }

    baseUrlConfigurationMigrator.migrate();

    boolean hasCustomSetting = configBaseUrl != null || configForceBaseUrl != null;
    if (hasCustomSetting) {
      assertThat(logOutput).atWarnLevel().contains(BaseUrlConfigurationMigrator.OBSOLETE_CONFIG_MESSAGE);
    }
    else {
      assertThat(logOutput).doesNotContain(BaseUrlConfigurationMigrator.OBSOLETE_CONFIG_MESSAGE);
    }
    if (alreadyMigrated) {
      verifyNoInteractions(mockBaseUrlConfigurationListener);
    }
    else {
      Set<String> propertyNames = new HashSet<>();
      if (configBaseUrl != null) {
        assertThat(systemConfigurationPropertyDAO.get(SystemConfigurationProperty.BASE_URL)).isEqualTo(configBaseUrl);
      }
      else {
        assertThat(systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.BASE_URL)).isNull();
      }
      if (configBaseUrl != null || configForceBaseUrl != null) {
        propertyNames.add(SystemConfigurationProperty.BASE_URL);
        propertyNames.add(SystemConfigurationProperty.FORCE_BASE_URL);
        assertThat(systemConfigurationPropertyDAO.get(SystemConfigurationProperty.FORCE_BASE_URL)).isEqualTo(
            configForceBaseUrl == null ? null : String.valueOf(configForceBaseUrl));
      }
      else {
        assertThat(systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.FORCE_BASE_URL)).isNull();
      }
      if (propertyNames.isEmpty()) {
        verifyNoInteractions(mockBaseUrlConfigurationListener);
      }
      else {
        verify(mockBaseUrlConfigurationListener).configurationChanged(propertyNames);
      }
    }
  }
}
