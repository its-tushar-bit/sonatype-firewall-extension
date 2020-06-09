/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Test;

import static com.sonatype.insight.brain.api.experimental.ApiConfigFeaturesService.FEATURE_DASHBOARD;
import static com.sonatype.insight.brain.api.experimental.ApiConfigFeaturesService.FEATURE_REPORTS_LIST;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.DASHBOARD_DISABLED;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.REPORTS_LIST_DISABLED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ApiConfigFeaturesServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiConfigFeaturesService service;

  private SystemConfigurationPropertyDAO systemConfigurationPropertyDAO = new SystemConfigurationPropertyDAO();

  @Test
  public void testGetPropertyNameForFeature() {
    assertThat(service.getPropertyNameForFeature("dashboard")).isEqualTo(DASHBOARD_DISABLED);
    assertThat(service.getPropertyNameForFeature("reportsList")).isEqualTo(REPORTS_LIST_DISABLED);

    assertThatThrownBy(() -> {
      service.getPropertyNameForFeature("bogus-feature");
    }).isInstanceOf(BadRequestException.class).hasMessage("Feature not supported: bogus-feature");
  }

  @Test
  public void testDisableFeature_Dashboard() {
    service.disableFeature(FEATURE_DASHBOARD);
    assertThat(systemConfigurationPropertyDAO.getByName(DASHBOARD_DISABLED).getValue()).isEqualTo("true");
  }

  @Test
  public void testDisableFeature_Dashboard_AlreadyDisabled() {
    tempEntity.newSystemConfigurationProperty(DASHBOARD_DISABLED, "true");
    assertThatThrownBy(() -> {
      service.disableFeature(FEATURE_DASHBOARD);
    }).isInstanceOf(BadRequestException.class).hasMessage("Feature is already disabled.");
  }

  @Test
  public void testEnableFeature_Dashboard() {
    tempEntity.newSystemConfigurationProperty(DASHBOARD_DISABLED, "true");
    service.enableFeature(FEATURE_DASHBOARD);
    assertThat(systemConfigurationPropertyDAO.getByName(DASHBOARD_DISABLED)).isNull();
  }

  @Test
  public void testEnableFeature_Dashboard_AlreadyEnabled() {
    assertThatThrownBy(() -> {
      service.enableFeature(FEATURE_DASHBOARD);
    }).isInstanceOf(BadRequestException.class).hasMessage("Feature is already enabled.");
  }

  @Test
  public void testDisableFeature_ReportsList() {
    service.disableFeature(FEATURE_REPORTS_LIST);
    assertThat(systemConfigurationPropertyDAO.getByName(REPORTS_LIST_DISABLED).getValue()).isEqualTo("true");
  }

  @Test
  public void testDisableFeature_ReportsList_AlreadyDisabled() {
    tempEntity.newSystemConfigurationProperty(REPORTS_LIST_DISABLED, "true");

    assertThatThrownBy(() -> {
      service.disableFeature(FEATURE_REPORTS_LIST);
    }).isInstanceOf(BadRequestException.class).hasMessage("Feature is already disabled.");
  }

  @Test
  public void testEnableFeature_ReportsList() {
    tempEntity.newSystemConfigurationProperty(REPORTS_LIST_DISABLED, "true");
    service.enableFeature(FEATURE_REPORTS_LIST);
    assertThat(systemConfigurationPropertyDAO.getByName(REPORTS_LIST_DISABLED)).isNull();
  }

  @Test
  public void testEnableFeature_ReportsList_AlreadyEnabled() {
    assertThatThrownBy(() -> {
      service.enableFeature(FEATURE_REPORTS_LIST);
    }).isInstanceOf(BadRequestException.class).hasMessage("Feature is already enabled.");
  }
}
