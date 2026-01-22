/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import org.junit.Test;

import jakarta.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.sonatype.insight.brain.organization.OwnerTelemetryCollector.ALL_OWNER_IDS_NAMES;
import static com.sonatype.insight.brain.organization.OwnerTelemetryCollector.OwnerData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class OrganizationTelemetryCollectorTest extends AbstractComponentTest
{
  @Inject
  private OrganizationTelemetryCollector telemetryCollector;

  @Inject
  private Configuration configuration;

  @Inject
  private ApiConfigurationService configurationService;

  @Inject
  private TelemetryUtils telemetryUtils;

  @Test
  public void testIsClusterTelemetry() {
    assertThat(telemetryCollector.isClusterTelemetry()).isTrue();
  }

  @Test
  public void testCollectData_FeatureEnabled_CollectOrganizationIds() {
    Organization org1 = tempEntity.newOrganization();
    Organization org2 = tempEntity.newOrganization();
    Organization org3 = tempEntity.newOrganization();

    TelemetryData telemetryData = telemetryCollector.collectData();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.REAL_OWNER_IDS);
    assertThat(telemetryData.getAttributes()).containsKey(ALL_OWNER_IDS_NAMES);
    List<OwnerData> appData = (List<OwnerData>) telemetryData.getAttributes().get(ALL_OWNER_IDS_NAMES);
    assertThat(appData).hasSize(4).extracting("ownerId", "ownerName", "ownerType", "parentOwnerId").contains(
        tuple("ROOT_ORGANIZATION_ID", "Root Organization", OwnerType.ORGANIZATION.toString(), null),
        tuple(org1.getId(), org1.getName(), OwnerType.ORGANIZATION.toString(), org1.getParentOrganizationId()),
        tuple(org2.getId(), org2.getName(), OwnerType.ORGANIZATION.toString(), org2.getParentOrganizationId()),
        tuple(org3.getId(), org3.getName(), OwnerType.ORGANIZATION.toString(), org3.getParentOrganizationId())
    );
  }

  @Test
  public void testCollectData_FeatureEnabled_CollectOrganizationIds_obfuscatesInformationIfAdvancedReportingDisabled() {
    // Toggle advanced reporting to make sure values are being obfuscated accordingly
    Map<String, Object> properties =
        Collections.singletonMap(SystemConfigurationProperty.ADVANCED_REPORTING_INSIGHTS_ENABLED, false);
    configurationService.setConfigurationInDatabaseNoAuthz(properties);
    configuration.configurationChanged(properties.keySet());

    Organization org1 = tempEntity.newOrganization();
    Organization org2 = tempEntity.newOrganization();
    Organization org3 = tempEntity.newOrganization();

    TelemetryData telemetryData = telemetryCollector.collectData();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.REAL_OWNER_IDS);
    assertThat(telemetryData.getAttributes()).containsKey(ALL_OWNER_IDS_NAMES);
    List<OwnerData> appData = (List<OwnerData>) telemetryData.getAttributes().get(ALL_OWNER_IDS_NAMES);
    assertThat(appData).hasSize(4).extracting("ownerId", "ownerName", "ownerType", "parentOwnerId").contains(
        tuple(
            telemetryUtils.obfuscate("ROOT_ORGANIZATION_ID"),
            telemetryUtils.obfuscate("Root Organization"),
            OwnerType.ORGANIZATION.toString(),
            telemetryUtils.obfuscate(null)
        ),
        tuple(
            telemetryUtils.obfuscate(org1.getId()),
            telemetryUtils.obfuscate(org1.getName()),
            OwnerType.ORGANIZATION.toString(),
            telemetryUtils.obfuscate(org1.getParentOrganizationId())
        ),
        tuple(
            telemetryUtils.obfuscate(org2.getId()),
            telemetryUtils.obfuscate(org2.getName()),
            OwnerType.ORGANIZATION.toString(),
            telemetryUtils.obfuscate(org2.getParentOrganizationId())
        ),
        tuple(
            telemetryUtils.obfuscate(org3.getId()),
            telemetryUtils.obfuscate(org3.getName()),
            OwnerType.ORGANIZATION.toString(),
            telemetryUtils.obfuscate(org3.getParentOrganizationId())
        )
    );
  }
}
