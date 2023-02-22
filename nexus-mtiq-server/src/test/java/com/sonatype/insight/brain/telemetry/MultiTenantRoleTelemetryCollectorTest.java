/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.dataaccess.security.RolePermissionDAO;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.tenancy.MultiTenantDatabaseTestSupport;
import com.sonatype.insight.telemetry.model.TelemetryData;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MultiTenantRoleTelemetryCollectorTest
    extends MultiTenantDatabaseTestSupport
{
  private RoleTelemetryCollector telemetryCollector;

  private RoleDAO roleDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    roleDAO = new RoleDAO();
    RolePermissionDAO rolePermissionDAO = new RolePermissionDAO();
    MembershipMappingDAO membershipMappingDAO = new MembershipMappingDAO();
    telemetryCollector = new RoleTelemetryCollector(roleDAO, rolePermissionDAO, membershipMappingDAO);
  }

  @Test
  public void testShouldNotLeakDataBetweenTenants_whenMultiTenantMode() {
    testAsNewTenant(t1 -> {
      Role role = new Role("role", "description");
      roleDAO.insert(role);

      List<TelemetryData> telemetryData = telemetryCollector.collectAllData();

      assertThat(telemetryData)
          .extracting(data -> data.getAttributes().get(RoleTelemetryCollector.ROLE_NAME))
          .containsOnlyOnce(HdsClientAnalytics.obfuscate("role"));
    });

    testAsNewTenant(t2 -> {
      List<TelemetryData> telemetryData = telemetryCollector.collectAllData();

      assertThat(telemetryData).isNotEmpty()
          .extracting(data -> data.getAttributes().get(RoleTelemetryCollector.ROLE_NAME))
          .doesNotContain(HdsClientAnalytics.obfuscate("role"));
    });
  }
}
