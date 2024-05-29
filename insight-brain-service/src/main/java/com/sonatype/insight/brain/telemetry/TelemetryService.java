/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.telemetry.model.TelemetryData;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

/**
 * @since 1.47
 */
@Named
public class TelemetryService
{
  public static final String SESSION_ID_ATTR = "session_id";

  public static final String USER_ROLES_ATTR = "user_roles";

  private final TelemetrySender telemetrySender;

  private final RoleDAO roleDAO;

  private final MembershipMappingDAO membershipMappingDAO;

  private final CurrentUser currentUser;

  private final HashFunction obfuscationFunction = Hashing.sha256();

  @Inject
  public TelemetryService(
      TelemetrySender telemetrySender,
      MembershipMappingDAO membershipMappingDAO,
      RoleDAO roleDAO,
      CurrentUser currentUser)
  {
    this.telemetrySender = telemetrySender;
    this.membershipMappingDAO = membershipMappingDAO;
    this.roleDAO = roleDAO;
    this.currentUser = currentUser;
  }

  void forwardFrontendTelemetryToHds(TelemetryData data, String sessionId) {
    Map<String, Object> attributes = data.getAttributes();

    attributes.put(SESSION_ID_ATTR, obfuscate(sessionId));
    attributes.put(USER_ROLES_ATTR, getObfuscatedUserRoles());

    telemetrySender.send(data);
  }

  private Set<String> getObfuscatedUserRoles() {
    UserPrincipal userPrincipal = currentUser.getUserPrincipal();

    Collection<MembershipMapping> memberships =
        membershipMappingDAO.getByUserCaseInsensitiveAndGroups(userPrincipal.getUsername(),
            userPrincipal.getMembership());

    Set<String> retval = new HashSet<>();

    for (MembershipMapping membership : memberships) {
      Role role = roleDAO.getById(membership.getRoleId());
      String roleIdentifier = role.isBuiltIn() ? role.getName() : "CUSTOM";

      retval.add(roleIdentifier);
    }

    return retval;
  }

  private String obfuscate(String input) {
    return obfuscationFunction.hashString(input, StandardCharsets.UTF_8).toString();
  }
}
