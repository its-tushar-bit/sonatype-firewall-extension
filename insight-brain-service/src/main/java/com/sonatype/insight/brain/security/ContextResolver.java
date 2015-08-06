/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.security.AuthzContext.Key;

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

  private class OrganizationIterator
      implements Iterator<String>, Iterable<String>
  {
    private String prevOrgId;

    private String nextOrgId;

    public OrganizationIterator(String firstOrgId) {
      nextOrgId = firstOrgId;
    }

    @Override
    public Iterator<String> iterator() {
      return new OrganizationIterator(nextOrgId);
    }

    @Override
    public String next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      prevOrgId = nextOrgId;
      nextOrgId = null;
      return prevOrgId;
    }

    @Override
    public boolean hasNext() {
      if (nextOrgId == null) {
        if (prevOrgId != null) {
          nextOrgId = orgDAO.getByIdNotNull(prevOrgId).getParentOrganizationId();
          prevOrgId = null;
        }
      }
      return nextOrgId != null;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

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
      return resolveContextIdsForOrg(org.getId());
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

  private final ContextIdResolver<Organization> ORGANIZATION_OWNER = new ContextIdResolver<Organization>()
  {
    @Override
    public Iterable<String> resolveContextIds(Organization org) {
      String parentId = org.getParentOrganizationId();
      if (parentId == null) {
        parentId = Organization.ROOT_ORGANIZATION_ID;
      }
      return resolveContextIdsForOrg(parentId);
    }
  };

  Iterable<String> resolveContextIdsForOrg(final String orgId) {
    return Iterables.concat(new OrganizationIterator(orgId), GLOBAL_CONTEXT);
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
      OwnerType type = get(parameters, AuthzContext.Key.TYPE, OwnerType.class);
      switch (type) {
        case APPLICATION:
          if (parameters.get(Key.ID) != null) {
            String id = get(parameters, Key.ID, String.class);
            return APPLICATION_PUBLIC_ID.resolveContextIds(id);
          }
          else {
            String id = get(parameters, Key.INTERNAL_ID, String.class);
            return APPLICATION_ID.resolveContextIds(id);
          }
        case ORGANIZATION:
          String id;
          if (parameters.get(Key.ID) != null) {
            id = get(parameters, Key.ID, String.class);
          }
          else {
            id = get(parameters, Key.INTERNAL_ID, String.class);
          }
          return ORGANIZATION_ID.resolveContextIds(id);
        case GLOBAL:
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
