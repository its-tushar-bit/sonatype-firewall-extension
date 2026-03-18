/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.config;

import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.service.AbstractMultiTenantBaseIntegrationTest;

import org.junit.Test;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.AUTOMATIC_QUARANTINE_RELEASE_TIME_INTERVAL_IN_MINUTES;
import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class MultiTenantConfigurationDefaultsServiceTest
    extends AbstractMultiTenantBaseIntegrationTest
{
  @Test
  public void shouldSetGlobalConfigurationOnSystemStart() {
    testAsGlobal(t -> {
      SystemConfigurationPropertyDAO dao = getCLMServer().getInstance(SystemConfigurationPropertyDAO.class);

      assertThat(dao.get(AUTOMATIC_QUARANTINE_RELEASE_TIME_INTERVAL_IN_MINUTES)).isEqualTo("120");
    });
  }
}
