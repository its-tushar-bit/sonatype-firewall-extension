/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.db.AbstractMultiTenantDatabaseTest;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GlobalConfigFallbackTest
    extends AbstractMultiTenantDatabaseTest
{
  private SystemConfigurationPropertyDAO underTest;

  @BeforeEach
  public void before() {
    underTest = daoFactory.createSystemConfigurationPropertyDAO();
  }

  @Test
  public void testConfigFallbackToGlobal_whenTenantValueNotSet() {
    String configKey = "key";
    String globalConfigValue = "global-value";
    String tenantConfigValue = "tenant-value";

    testAsGlobalTenant(t -> underTest.insert(new SystemConfigurationProperty(configKey, globalConfigValue)));

    testAsNewTenant(t1 -> {
      String value = underTest.get(configKey);

      assertThat(value).isEqualTo(globalConfigValue);

      underTest.insert(new SystemConfigurationProperty(configKey, tenantConfigValue));

      String value2 = underTest.get(configKey);

      assertThat(value2).isEqualTo(tenantConfigValue);
    });
  }
}
