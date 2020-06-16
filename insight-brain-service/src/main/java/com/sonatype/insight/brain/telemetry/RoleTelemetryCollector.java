/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.dataaccess.security.RolePermissionDAO;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import static java.util.stream.Collectors.toList;

/**
 * @since 1.83
 */
@Named
@Singleton
public class RoleTelemetryCollector
    implements TelemetryCollector
{
  public static final String ROLE_NAME = "role_name";

  public static final String ROLE_PERMISSIONS = "role_permissions";

  public static final String ROLE_USER_COUNT = "role_user_count";

  public static final String ROLE_GROUP_COUNT = "role_group_count";

  private final RoleDAO roleDAO;

  private final RolePermissionDAO rolePermissionDAO;

  private final MembershipMappingDAO membershipMappingDAO;

  @Inject
  public RoleTelemetryCollector(
      RoleDAO roleDAO,
      RolePermissionDAO rolePermissionDAO,
      MembershipMappingDAO membershipMappingDAO)
  {
    this.roleDAO = roleDAO;
    this.rolePermissionDAO = rolePermissionDAO;
    this.membershipMappingDAO = membershipMappingDAO;
  }

  @Override
  public boolean isClusterTelemetry() {
    return true;
  }

  @Override
  public List<TelemetryData> collectAllData() {
    return roleDAO.getAll().stream().map(this::collectData).collect(toList());
  }

  private TelemetryData collectData(Role role) {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.ROLE_USAGE);
    telemetryData.put(ROLE_NAME, role.isBuiltIn() ? role.getName() : HdsClientAnalytics.obfuscate(role.getName()));
    telemetryData.put(ROLE_PERMISSIONS, rolePermissionDAO.getPermissionsForRole(role.getId()));
    Set<String> users = new HashSet<>();
    Set<String> groups = new HashSet<>();
    for (MembershipMapping membershipMapping : membershipMappingDAO.getByRoleIds(Collections.singleton(role.getId()))) {
      switch (membershipMapping.getMemberType()) {
        case USER:
          users.add(membershipMapping.getMemberName());
          break;
        case GROUP:
          groups.add(membershipMapping.getMemberName());
          break;
        default:
          throw new IllegalStateException("Unsupported member type: " + membershipMapping.getMemberType());
      }
    }
    telemetryData.put(ROLE_USER_COUNT, users.size());
    telemetryData.put(ROLE_GROUP_COUNT, groups.size());
    return telemetryData;
  }
}
