/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SuccessMetricsServiceTest
    extends AbstractComponentTest
{
  @Inject
  private SuccessMetricsService successMetricsService;

  @Test
  public void testGet() {
    systemConfigurationPropertyDAO
        .update(new SystemConfigurationProperty(SuccessMetricsService.PROPERTY_ENABLED, "false"));
    assertThat(successMetricsService.get().enabled).isFalse();

    systemConfigurationPropertyDAO
        .update(new SystemConfigurationProperty(SuccessMetricsService.PROPERTY_ENABLED, "true"));
    assertThat(successMetricsService.get().enabled).isTrue();
  }

  @Test
  public void testUpdate() {
    SuccessMetricsConfigurationDTO configuration = new SuccessMetricsConfigurationDTO();
    assertThat(successMetricsService.update(configuration).enabled).isFalse();
    assertThat(systemConfigurationPropertyDAO.getByName(SuccessMetricsService.PROPERTY_ENABLED).getValue())
        .isEqualTo("false");

    configuration.enabled = true;
    assertThat(successMetricsService.update(configuration).enabled).isTrue();
    assertThat(systemConfigurationPropertyDAO.getByName(SuccessMetricsService.PROPERTY_ENABLED).getValue())
        .isEqualTo("true");
  }
}
