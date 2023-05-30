/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import javax.inject.Provider;

import com.sonatype.insight.brain.dataaccess.configuration.saml.SamlConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.tenancy.DeletedTenantDAO;
import com.sonatype.insight.brain.model.configuration.saml.SamlConfiguration;
import com.sonatype.insight.brain.service.TenantLifecycle;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantManaged;
import com.sonatype.insight.brain.tenancy.TenantManager;
import com.sonatype.insight.brain.tenancy.TenantValidator;
import com.sonatype.insight.brain.tenancy.MultiTenantDatabaseTestSupport;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static com.sonatype.insight.brain.telemetry.RealmTelemetryCollector.SAML_CONFIGURED;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAs;
import static org.assertj.core.api.Assertions.assertThat;

public class MultiTenantRealmTelemetryCollectorTest
    extends MultiTenantDatabaseTestSupport
{
  private RealmTelemetryCollector telemetryCollector;

  private SamlConfigurationDAO samlConfigurationDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    Collection<TenantManaged> tenantManagedBeans = Collections.emptyList();
    Provider<TenantLifecycle> tenantLifecycleProvider = () -> Mockito.mock(TenantLifecycle.class);
    TenantValidator tenantValidator = new TenantValidator(multiTenantDatabaseTestRule.operationalDataStore);
    DeletedTenantDAO deletedTenantDAO = new DeletedTenantDAO();

    tenantManager =
        new TenantManager(tenantManagedBeans, multiTenantDatabaseTestRule.insightConfig, tenantLifecycleProvider,
            multiTenantDatabaseTestRule.databaseProvisionUtils, tenantValidator, deletedTenantDAO);

    samlConfigurationDAO = new SamlConfigurationDAO();
    telemetryCollector = new RealmTelemetryCollector(samlConfigurationDAO);
  }

  @Test
  public void testShouldNotLeakDataBetweenTenants_whenMultiTenantMode() {
    Tenant tenant1 = testAsNewTenant(t1 -> {
      // No-op initialization
    });

    testAsNewTenant(t2 -> {
      SamlConfiguration samlConfiguration = new SamlConfiguration();
      samlConfiguration.setIdentityProviderMetadataXml(getSamlMetadata("valid.xml"));
      samlConfiguration.setEntityId("id");
      samlConfigurationDAO.insert(samlConfiguration);

      TelemetryData telemetryData = telemetryCollector.collectData();
      assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.REALM);
      assertThat(telemetryData.getAttributes()).containsEntry(SAML_CONFIGURED, "true");
    });

    testAs(tenant1, t1 -> {
      TelemetryData telemetryData = telemetryCollector.collectData();
      assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.REALM);
      assertThat(telemetryData.getAttributes()).containsEntry(SAML_CONFIGURED, "false");
    });
  }

  private String getSamlMetadata(String resourceName) {
    try {
      return Resources.toString(getClass().getResource("/" + getClass().getSimpleName() + "/" + resourceName),
          StandardCharsets.UTF_8);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
