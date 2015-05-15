/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;

import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.utils.IdUtils;

import org.apache.shiro.subject.Subject;

@Named
public class PermissionService
{
  public Set<Permission> hasPermissions(Subject subject, String ownerType, String ownerId, Set<Permission> permissions)
  {
    EnumSet<Permission> result = EnumSet.noneOf(Permission.class);

    if (subject.isAuthenticated()) {
      Map<Key, ContextParameter> contextMap;
      switch (ownerType) {
        case IdUtils.TYPE_APPLICATION:
          contextMap = Collections.singletonMap(Key.APPLICATION_ID, new ContextParameter(Key.APPLICATION_ID, ownerId,
              false));
          break;
        case IdUtils.TYPE_ORGANIZATION:
          contextMap = Collections.singletonMap(Key.ORGANIZATION_ID, new ContextParameter(Key.ORGANIZATION_ID, ownerId,
              false));
          break;
        default:
          contextMap = Collections.emptyMap();
      }

      AuthorizationChecker authzChecker = new AuthorizationChecker();

      for (Permission permission : permissions) {
        if (authzChecker.isPermitted((UserPrincipal) subject.getPrincipal(), permission, contextMap)) {
          result.add(permission);
        }
      }
    }

    return result;
  }
}
