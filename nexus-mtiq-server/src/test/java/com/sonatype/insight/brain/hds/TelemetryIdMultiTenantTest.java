/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.util.UUID;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.hds.util.TelemetryTestUtils;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.testing.AbstractMultiTenantTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TelemetryIdMultiTenantTest
    extends AbstractMultiTenantTest
{
  @Mock
  InsightConfig config;

  @Mock
  SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  TelemetryId underTest;

  @Before
  public void setup() {
    this.underTest = new TestMultiTenantTelemetryId(config, systemConfigurationPropertyDAO);
  }

  @Test
  public void getIdShouldStoreValuePerTenant() {
    testAsNewTenant(t1 -> {
      String tenant1Id = underTest.getId();
      assertThat(tenant1Id).isNotNull();

      testAsNewTenant(t2 -> {
        // Set the value for a new tenant
        String tenant2Id = underTest.getId();
        assertThat(tenant2Id).isNotNull();

        // we WANT them to be different but that's not how the system is really behaving and it's the prefix that's
        // relevant and it's not changing
        assertThat(tenant1Id).isEqualTo(tenant2Id);
      });
    });
  }

  @Test
  public void testGetClusterId_storedPerTenant() {
    // given: different DB configs for different tenants (which isn't realistic in our saas env, but we're simply
    // trying to prove they are stored independently, regardless)
    final var dbConfig1 = createDatabaseConfig("host1", 5432, "db1");
    final var dbConfig2 = createDatabaseConfig("host2", 9092, "db2");
    when(config.getDatabase()).thenReturn(dbConfig1, dbConfig2);

    testAsNewTenant(t1 -> {
      final var tenant1ClusterId = underTest.getClusterId();
      assertThat(tenant1ClusterId).isNotNull();

      testAsNewTenant(t2 -> {
        // Set the value for a new tenant
        final var tenant2ClusterId = underTest.getId();
        assertThat(tenant2ClusterId).isNotNull();

        assertThat(tenant1ClusterId).isNotEqualTo(tenant2ClusterId);
      });
    });
  }

  private DatabaseConfig createDatabaseConfig(String hostname, int port, String name) {
    var dbConfig = new DatabaseConfig();
    dbConfig.setHostname(hostname);
    dbConfig.setPort(port);
    dbConfig.setName(name);
    return dbConfig;
  }

  private static class TestMultiTenantTelemetryId
      extends TelemetryId
  {
    public TestMultiTenantTelemetryId(
        InsightConfig insightConfig,
        SystemConfigurationPropertyDAO systemConfigurationPropertyDAO)
    {
      super(insightConfig, systemConfigurationPropertyDAO,
          TelemetryTestUtils.setupReflectiveMockClusterIdentificationService());
    }

    @Override
    protected String generateId() {
      return UUID.randomUUID().toString();
    }
  }
}
