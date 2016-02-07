/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;

import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.security.AuthzContext.Key;

import org.apache.shiro.subject.Subject;

@Named
public class PermissionService
{
  public Set<Permission> hasPermissions(Subject subject,
                                        OwnerType ownerType,
                                        String ownerId,
                                        Set<Permission> permissions)
  {
    EnumSet<Permission> result = EnumSet.noneOf(Permission.class);

    if (subject.isAuthenticated()) {
      Map<Key, ContextParameter> contextMap;
      switch (ownerType) {
        case APPLICATION:
          contextMap = Collections.singletonMap(Key.APPLICATION_ID, new ContextParameter(Key.APPLICATION_ID, ownerId,
              false));
          break;
        case ORGANIZATION:
          contextMap = Collections.singletonMap(Key.ORGANIZATION_ID, new ContextParameter(Key.ORGANIZATION_ID, ownerId,
              false));
          break;
        case REPOSITORY_CONTAINER:
          contextMap = new HashMap<>();
          contextMap.put(AuthzContext.Key.ID, new ContextParameter(Key.ID, ownerId, false));
          contextMap.put(AuthzContext.Key.TYPE, new ContextParameter(Key.TYPE, OwnerType.REPOSITORY_CONTAINER, false));
          break;
        case REPOSITORY:
          contextMap = Collections.singletonMap(Key.REPOSITORY_ID, new ContextParameter(Key.REPOSITORY_ID, ownerId,
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
