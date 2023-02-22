/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.tenancy.MultiTenantDatabaseTestSupport;

import org.junit.Test;

import static com.sonatype.insight.brain.tenancy.Tenant.GLOBAL_TENANT;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAs;
import static org.assertj.core.api.Assertions.assertThat;

public class GlobalConfigFallbackTest
    extends MultiTenantDatabaseTestSupport
{
  private final SystemConfigurationPropertyDAO underTest = new SystemConfigurationPropertyDAO();

  @Test
  public void testConfigFallbackToGlobal_whenTenantValueNotSet() {
    String configKey = "key";
    String globalConfigValue = "global-value";
    String tenantConfigValue = "tenant-value";

    testAs(GLOBAL_TENANT, t -> underTest.insert(new SystemConfigurationProperty(configKey, globalConfigValue)));

    testAsNewTenant(t1 -> {
      String value = underTest.get(configKey);

      assertThat(value).isEqualTo(globalConfigValue);

      underTest.insert(new SystemConfigurationProperty(configKey, tenantConfigValue));

      String value2 = underTest.get(configKey);

      assertThat(value2).isEqualTo(tenantConfigValue);
    });
  }
}
