/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.List;
import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.organization.ApplicationTelemetryCollector.OwnerData;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.organization.ApplicationTelemetryCollector.ALL_OWNER_IDS_NAMES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class ApplicationTelemetryCollectorTest
    extends AbstractComponentTest
{
  @Inject
  private ApplicationTelemetryCollector telemetryCollector;

  private Organization org;

  @Before
  public void before() {
    org = tempEntity.newOrganization();
  }

  @Test
  public void testIsClusterTelemetry() {
    assertThat(telemetryCollector.isClusterTelemetry()).isTrue();
  }

  @Test
  public void testCollectData_FeatureEnabled_CollectApplicationIds() {
    toggleFeature(true);
    Application app1 = tempEntity.newApplication(org.getId());
    Application app2 = tempEntity.newApplication(org.getId());
    Application app3 = tempEntity.newApplication(tempEntity.newOrganization().getId());

    TelemetryData telemetryData = telemetryCollector.collectData();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.REAL_OWNER_IDS);
    assertThat(telemetryData.getAttributes()).containsKey(ALL_OWNER_IDS_NAMES);
    List<OwnerData> appData = (List<OwnerData>) telemetryData.getAttributes().get(ALL_OWNER_IDS_NAMES);
    assertThat(appData).hasSize(3).extracting("ownerId", "ownerName").contains(
        tuple(app1.getId(), app1.getName()),
        tuple(app2.getId(), app2.getName()),
        tuple(app3.getId(), app3.getName()));
  }

  @Test
  public void testCollectData_FeatureDisabled_DoesNotCollectApplicationIds() {
    toggleFeature(false);
    tempEntity.newApplication(org.getId());

    TelemetryData telemetryData = telemetryCollector.collectData();
    assertThat(telemetryData).isNull();
  }

  private static void toggleFeature(boolean toggle) {
    ApiConfigFeaturesService.SystemConfigurationPropertyFeature
        .INTEGRATED_ENTERPRISE_REPORTING.setEnabled(toggle);
  }

  @After
  public void after() {
    toggleFeature(false);
  }
}
