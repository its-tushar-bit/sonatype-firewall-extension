/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.utils.IdUtils;

import com.google.common.collect.Iterables;

/**
 * Resolves authorization contexts from method parameters/results.
 * 
 * @since 1.7
 */
class ContextResolver
{
  private static final Iterable<String> GLOBAL_CONTEXT = Collections.singleton(MembershipMapping.GLOBAL_CONTEXT_ID);

  private final ApplicationDAO appDAO = new ApplicationDAO();

  private final OrganizationDAO orgDAO = new OrganizationDAO();

  private final ContextIdResolver<Application> INBOUND_APPLICATION = new ContextIdResolver<Application>()
  {
    @Override
    public Iterable<String> resolveContextIds(Application app) {
      // load current entity state and disregard any potential updates expressed in user-supplied data
      app = appDAO.getByIdNotNull(app.getId());
      return APPLICATION.resolveContextIds(app);
    }
  };

  final ContextIdResolver<Application> APPLICATION = new ContextIdResolver<Application>()
  {
    @Override
    public Iterable<String> resolveContextIds(Application app) {
      return Iterables.concat(Collections.singleton(app.getId()), resolveContextIdsForOrg(app.getOrganizationId()));
    }
  };

  private final ContextIdResolver<String> APPLICATION_ID = new ContextIdResolver<String>()
  {
    @Override
    public Iterable<String> resolveContextIds(String appId) {
      Application app = appDAO.getByIdNotNull(appId);
      return APPLICATION.resolveContextIds(app);
    }
  };

  private final ContextIdResolver<String> APPLICATION_PUBLIC_ID = new ContextIdResolver<String>()
  {
    @Override
    public Iterable<String> resolveContextIds(String appPublicId) {
      Application app = appDAO.getByPublicIdNotNull(appPublicId);
      return APPLICATION.resolveContextIds(app);
    }
  };

  private final ContextIdResolver<Application> APPLICATION_OWNER = new ContextIdResolver<Application>()
  {
    @Override
    public Iterable<String> resolveContextIds(Application app) {
      return resolveContextIdsForOrg(app.getOrganizationId());
    }
  };

  private final ContextIdResolver<Organization> INBOUND_ORGANIZATION = new ContextIdResolver<Organization>()
  {
    @Override
    public Iterable<String> resolveContextIds(Organization org) {
      // load current entity state and disregard any potential updates expressed in user-supplied data
      org = orgDAO.getByIdNotNull(org.getId());
      return ORGANIZATION.resolveContextIds(org);
    }
  };

  final ContextIdResolver<Organization> ORGANIZATION = new ContextIdResolver<Organization>()
  {
    @Override
    public Iterable<String> resolveContextIds(Organization org) {
      return Arrays.asList(org.getId(), MembershipMapping.GLOBAL_CONTEXT_ID);
    }
  };

  private final ContextIdResolver<String> ORGANIZATION_ID = new ContextIdResolver<String>()
  {
    @Override
    public Iterable<String> resolveContextIds(String orgId) {
      Organization org = orgDAO.getByIdNotNull(orgId);
      return ORGANIZATION.resolveContextIds(org);
    }
  };

  private static final ContextIdResolver<Organization> ORGANIZATION_OWNER = new ContextIdResolver<Organization>()
  {
    @Override
    public Iterable<String> resolveContextIds(Organization org) {
      return GLOBAL_CONTEXT;
    }
  };

  static Iterable<String> resolveContextIdsForOrg(String orgId) {
    if (orgId == null) {
      return GLOBAL_CONTEXT;
    }
    return Arrays.asList(orgId, MembershipMapping.GLOBAL_CONTEXT_ID);
  }

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
          return INBOUND_APPLICATION.resolveContextIds(app);
        case APPLICATION_OWNER:
          app = get(parameters, AuthzContext.Key.APPLICATION_OWNER, Application.class);
          return APPLICATION_OWNER.resolveContextIds(app);
        case APPLICATION_ID:
          String appId = get(parameters, AuthzContext.Key.APPLICATION_ID, String.class);
          return APPLICATION_ID.resolveContextIds(appId);
        case APPLICATION_PUBLIC_ID:
          String appPublicId = get(parameters, AuthzContext.Key.APPLICATION_PUBLIC_ID, String.class);
          return APPLICATION_PUBLIC_ID.resolveContextIds(appPublicId);
        case ORGANIZATION:
          Organization org = get(parameters, AuthzContext.Key.ORGANIZATION, Organization.class);
          return INBOUND_ORGANIZATION.resolveContextIds(org);
        case ORGANIZATION_OWNER:
          org = get(parameters, AuthzContext.Key.ORGANIZATION_OWNER, Organization.class);
          return ORGANIZATION_OWNER.resolveContextIds(org);
        case ORGANIZATION_ID:
          String orgId = get(parameters, AuthzContext.Key.ORGANIZATION_ID, String.class);
          return ORGANIZATION_ID.resolveContextIds(orgId);
        default:
          throw new IllegalArgumentException("Cannot resolve context from " + parameters);
      }
    }
    else if (count == 2) {
      String id = get(parameters, AuthzContext.Key.ID, String.class);
      String type = get(parameters, AuthzContext.Key.TYPE, String.class);
      if (IdUtils.TYPE_APPLICATION.equals(type)) {
        return APPLICATION_PUBLIC_ID.resolveContextIds(id);
      }
      else if (IdUtils.TYPE_ORGANIZATION.equals(type)) {
        return ORGANIZATION_ID.resolveContextIds(id);
      }
      else if (IdUtils.TYPE_GLOBAL.equals(type)) {
        return GLOBAL_CONTEXT;
      }
    }
    throw new IllegalArgumentException("Cannot resolve context from " + parameters);
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
