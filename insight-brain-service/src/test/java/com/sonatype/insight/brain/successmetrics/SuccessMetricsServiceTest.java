/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class SuccessMetricsServiceTest
    extends AbstractComponentTest
{
  @Inject
  private SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  @Inject
  private SuccessMetricsService successMetricsService;

  @Test
  public void testGet() {
    systemConfigurationPropertyDAO
        .update(new SystemConfigurationProperty(SuccessMetricsService.PROPERTY_ENABLED, "false"));
    assertThat(successMetricsService.get().enabled, is(false));

    systemConfigurationPropertyDAO
        .update(new SystemConfigurationProperty(SuccessMetricsService.PROPERTY_ENABLED, "true"));
    assertThat(successMetricsService.get().enabled, is(true));
  }

  @Test
  public void testUpdate() {
    SuccessMetricsConfigurationDTO configuration = new SuccessMetricsConfigurationDTO();
    assertThat(successMetricsService.update(configuration).enabled, is(false));
    assertThat(systemConfigurationPropertyDAO.getByName(SuccessMetricsService.PROPERTY_ENABLED).getValue(),
        is("false"));

    configuration.enabled = true;
    assertThat(successMetricsService.update(configuration).enabled, is(true));
    assertThat(systemConfigurationPropertyDAO.getByName(SuccessMetricsService.PROPERTY_ENABLED).getValue(), is("true"));
  }
}
