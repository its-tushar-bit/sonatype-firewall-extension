/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import com.sonatype.insight.brain.configuration.saml.SamlConfigurationService;
import com.sonatype.insight.brain.dataaccess.TestSamlFactory;
import com.sonatype.insight.brain.db.AbstractMultiTenantDatabaseTest;
import com.sonatype.insight.brain.model.configuration.saml.SamlConfiguration;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.telemetry.RealmTelemetryCollector.SAML_CONFIGURED;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsTenant;
import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class MultiTenantRealmTelemetryCollectorTest
    extends AbstractMultiTenantDatabaseTest
{
  private RealmTelemetryCollector telemetryCollector;

  private SamlConfigurationService samlConfigurationService;

  @Before
  @Override
  public void setup() {
    super.setup();

    samlConfigurationService = new SamlConfigurationService(
        daoFactory.createSamlConfigurationInternalDAO(),
        new TestSamlFactory().createSamlConfigurationAdapter());
    telemetryCollector = new RealmTelemetryCollector(samlConfigurationService);
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
      samlConfigurationService.insert(samlConfiguration);

      TelemetryData telemetryData = telemetryCollector.collectData();
      assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.REALM);
      assertThat(telemetryData.getAttributes()).containsEntry(SAML_CONFIGURED, "true");
    });

    testAsTenant(tenant1, t1 -> {
      TelemetryData telemetryData = telemetryCollector.collectData();
      assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.REALM);
      assertThat(telemetryData.getAttributes()).containsEntry(SAML_CONFIGURED, "false");
    });
  }

  private String getSamlMetadata(String resourceName) {
    try {
      return Resources.toString(MultiTenantRealmTelemetryCollectorTest.class.getResource(
          "/" + MultiTenantRealmTelemetryCollectorTest.class.getSimpleName() + "/" + resourceName),
          StandardCharsets.UTF_8);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
