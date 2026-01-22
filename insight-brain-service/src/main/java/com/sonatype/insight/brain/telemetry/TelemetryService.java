/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
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

  private final CurrentUser currentUser;

  private final HashFunction obfuscationFunction = Hashing.sha256();

  @Inject
  public TelemetryService(TelemetrySender telemetrySender, RoleDAO roleDAO, CurrentUser currentUser) {
    this.telemetrySender = telemetrySender;
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

    return roleDAO.getObfuscatedRolesByUserCaseInsensitiveAndGroups(userPrincipal.getUsername(),
            userPrincipal.getMembership());
  }

  private String obfuscate(String input) {
    return obfuscationFunction.hashString(input, StandardCharsets.UTF_8).toString();
  }
}
