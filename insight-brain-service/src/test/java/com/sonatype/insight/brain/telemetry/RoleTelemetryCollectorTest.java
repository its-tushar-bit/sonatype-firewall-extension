/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.junit.Test;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class RoleTelemetryCollectorTest
    extends AbstractComponentTest
{
  @Inject
  private RoleTelemetryCollector telemetryCollector;

  @Inject
  private RoleDAO roleDAO;

  @Test
  public void testIsClusterTelemetry() {
    assertThat(telemetryCollector.isClusterTelemetry()).isTrue();
  }

  @Test
  public void testCollectAllData() {
    List<String> roleNames = new ArrayList<>(roleDAO.getAll().stream().map(Role::getName).collect(toList()));
    Role customRole = tempEntity.newRole(false, Permission.WRITE);
    String obfuscatedCustomRoleName = HdsClientAnalytics.obfuscate(customRole.getName());
    roleNames.add(obfuscatedCustomRoleName);

    String user = "testuser";
    String group = "testgroup";
    String app1Id = tempEntity.newApplicationWithParent().getId();
    tempEntity.newMembershipMapping(app1Id, customRole.getId(), user);
    tempEntity.newMembershipMapping(app1Id, customRole.getId(), "2nd-user");
    tempEntity.newMembershipMapping(app1Id, customRole.getId(), group, MemberType.GROUP);
    String app2Id = tempEntity.newApplicationWithParent().getId();
    tempEntity.newMembershipMapping(app2Id, customRole.getId(), user);
    tempEntity.newMembershipMapping(app2Id, customRole.getId(), group, MemberType.GROUP);

    List<TelemetryData> allTelemetryData = telemetryCollector.collectAllData();
    assertThat(allTelemetryData).hasSameSizeAs(roleDAO.getAll()).allSatisfy(telemetryData -> {
      assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.ROLE_USAGE);
      assertThat(telemetryData.getAttributes()).containsOnlyKeys(RoleTelemetryCollector.ROLE_NAME,
          RoleTelemetryCollector.ROLE_PERMISSIONS, RoleTelemetryCollector.ROLE_USER_COUNT,
          RoleTelemetryCollector.ROLE_GROUP_COUNT);
    }).extracting(telemetryData -> telemetryData.getAttributes().get(RoleTelemetryCollector.ROLE_NAME))
        .containsExactlyInAnyOrderElementsOf(roleNames);

    TelemetryData telemetryData = allTelemetryData.stream()
        .filter(data -> obfuscatedCustomRoleName.equals(data.getAttributes().get(RoleTelemetryCollector.ROLE_NAME)))
        .findFirst().get();
    assertThat(telemetryData.getAttributes())
        .containsEntry(RoleTelemetryCollector.ROLE_PERMISSIONS, Collections.singleton(Permission.WRITE))
        .containsEntry(RoleTelemetryCollector.ROLE_USER_COUNT, 2)
        .containsEntry(RoleTelemetryCollector.ROLE_GROUP_COUNT, 1);
  }
}
