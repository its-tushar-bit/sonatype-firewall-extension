/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.organization.OwnerTelemetryCollector.OwnerData;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.organization.ApplicationTelemetryCollector.ALL_OWNER_IDS_NAMES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@ComponentH2Test
public class ApplicationTelemetryCollectorTest
    extends AbstractComponentH2Test
{
  @Inject
  private ApplicationTelemetryCollector telemetryCollector;

  @Inject
  private Configuration configuration;

  @Inject
  private ApiConfigurationService configurationService;

  @Inject
  private TelemetryUtils telemetryUtils;

  private Organization org;

  @BeforeEach
  public void before() {
    org = tempEntity.newOrganization();
  }

  @Test
  public void testIsClusterTelemetry() {
    assertThat(telemetryCollector.isClusterTelemetry()).isTrue();
  }

  @Test
  public void testCollectData_FeatureEnabled_CollectApplicationIds() {
    Application app1 = tempEntity.newApplication(org.getId());
    Application app2 = tempEntity.newApplication(org.getId());
    Application app3 = tempEntity.newApplication(tempEntity.newOrganization().getId());

    TelemetryData telemetryData = telemetryCollector.collectData();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.REAL_OWNER_IDS);
    assertThat(telemetryData.getAttributes()).containsKey(ALL_OWNER_IDS_NAMES);
    List<OwnerData> appData = (List<OwnerData>) telemetryData.getAttributes().get(ALL_OWNER_IDS_NAMES);
    assertThat(appData).hasSize(3)
        .extracting("ownerId", "ownerName", "ownerType", "parentOwnerId")
        .contains(
            tuple(app1.getId(), app1.getName(), OwnerType.APPLICATION.toString(), app1.getParentOwnerId()),
            tuple(app2.getId(), app2.getName(), OwnerType.APPLICATION.toString(), app2.getParentOwnerId()),
            tuple(app3.getId(), app3.getName(), OwnerType.APPLICATION.toString(), app3.getParentOwnerId()));
  }

  @Test
  public void testCollectData_FeatureEnabled_CollectApplicationIds_obfuscatesInformationIfAdvancedReportingDisabled() {
    // Toggle advanced reporting to make sure values are being obfuscated accordingly
    Map<String, Object> properties =
        Collections.singletonMap(SystemConfigurationProperty.ADVANCED_REPORTING_INSIGHTS_ENABLED, false);
    configurationService.setConfigurationInDatabaseNoAuthz(properties);
    configuration.configurationChanged(properties.keySet());

    Application app1 = tempEntity.newApplication(org.getId());
    Application app2 = tempEntity.newApplication(org.getId());
    Application app3 = tempEntity.newApplication(tempEntity.newOrganization().getId());

    TelemetryData telemetryData = telemetryCollector.collectData();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.REAL_OWNER_IDS);
    assertThat(telemetryData.getAttributes()).containsKey(ALL_OWNER_IDS_NAMES);
    List<OwnerData> appData = (List<OwnerData>) telemetryData.getAttributes().get(ALL_OWNER_IDS_NAMES);
    assertThat(appData).hasSize(3)
        .extracting("ownerId", "ownerName", "ownerType", "parentOwnerId")
        .contains(
            tuple(
                telemetryUtils.obfuscate(app1.getId()),
                telemetryUtils.obfuscate(app1.getName()),
                OwnerType.APPLICATION.toString(),
                telemetryUtils.obfuscate(app1.getParentOwnerId())),
            tuple(
                telemetryUtils.obfuscate(app2.getId()),
                telemetryUtils.obfuscate(app2.getName()),
                OwnerType.APPLICATION.toString(),
                telemetryUtils.obfuscate(app2.getParentOwnerId())),
            tuple(
                telemetryUtils.obfuscate(app3.getId()),
                telemetryUtils.obfuscate(app3.getName()),
                OwnerType.APPLICATION.toString(),
                telemetryUtils.obfuscate(app3.getParentOwnerId())));
  }
}
