/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Collections;
import java.util.Map;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.utils.IdUtils;

import com.google.common.collect.Iterables;

/**
 * Translates from REST method parameters annotated with {@link AuthzContext} to authorization contexts.
 * 
 * @since 1.7
 */
public class ContextResolver
{
  private static final Iterable<String> GLOBAL_CONTEXT = Collections.singleton(MembershipMapping.GLOBAL_CONTEXT_ID);

  /**
   * Resolves the internal ids of the contexts to check for authorization from the given method parameters (keyed by
   * {@link AuthzContext#value()}).
   */
  public Iterable<String> resolveContextIds(Map<AuthzContext.Key, Object> parameters) {
    int count = parameters.size();
    if (parameters.isEmpty()) {
      return GLOBAL_CONTEXT;
    }
    if (count == 1) {
      switch (parameters.keySet().iterator().next()) {
        case APPLICATION:
          Application app = get(parameters, AuthzContext.Key.APPLICATION, Application.class);
          return Iterables.concat(getInternalOwnerIds(IdUtils.TYPE_APPLICATION, app.getPublicId()), GLOBAL_CONTEXT);
        case APPLICATION_OWNER:
          app = get(parameters, AuthzContext.Key.APPLICATION_OWNER, Application.class);
          return Iterables.concat(getInternalOwnerIds(IdUtils.TYPE_ORGANIZATION, app.getOrganizationId()),
              GLOBAL_CONTEXT);
        case APPLICATION_ID:
          String appId = get(parameters, AuthzContext.Key.APPLICATION_ID, String.class);
          app = new ApplicationDAO().getByIdNotNull(appId);
          return Iterables.concat(Collections.singleton(app.getId()),
              getInternalOwnerIds(IdUtils.TYPE_ORGANIZATION, app.getOrganizationId()), GLOBAL_CONTEXT);
        case APPLICATION_PUBLIC_ID:
          String appPublicId = get(parameters, AuthzContext.Key.APPLICATION_PUBLIC_ID, String.class);
          return Iterables.concat(getInternalOwnerIds(IdUtils.TYPE_APPLICATION, appPublicId), GLOBAL_CONTEXT);
        case ORGANIZATION:
          Organization org = get(parameters, AuthzContext.Key.ORGANIZATION, Organization.class);
          return Iterables.concat(getInternalOwnerIds(IdUtils.TYPE_ORGANIZATION, org.getId()), GLOBAL_CONTEXT);
        case ORGANIZATION_OWNER:
          return GLOBAL_CONTEXT;
        case ORGANIZATION_ID:
          String orgId = get(parameters, AuthzContext.Key.ORGANIZATION_ID, String.class);
          return Iterables.concat(getInternalOwnerIds(IdUtils.TYPE_ORGANIZATION, orgId), GLOBAL_CONTEXT);
        default:
          throw new IllegalArgumentException("Cannot resolve context from " + parameters);
      }
    }
    else if (count == 2) {
      String id = get(parameters, AuthzContext.Key.ID, String.class);
      String type = get(parameters, AuthzContext.Key.TYPE, String.class);
      return Iterables.concat(getInternalOwnerIds(type, id), GLOBAL_CONTEXT);
    }
    throw new IllegalArgumentException("Cannot resolve context from " + parameters);
  }

  private Iterable<String> getInternalOwnerIds(String type, String id) {
    if (id == null) {
      return Collections.emptyList();
    }
    return IdUtils.getInternalOwnerIds(type, id);
  }

  private <T> T get(Map<AuthzContext.Key, Object> parameters, AuthzContext.Key key, Class<T> type) {
    Object value = parameters.get(key);
    try {
      if (value == null) {
        throw new NullPointerException("Expected parameter " + key);
      }
      return type.cast(value);
    }
    catch (RuntimeException e) {
      throw new IllegalArgumentException("Cannot resolve context from " + parameters, e);
    }
  }
}
