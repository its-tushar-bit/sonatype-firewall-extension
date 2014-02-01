/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.HashMap;
import java.util.Map;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.utils.IdUtils;

import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

public class ContextResolverTest
{
  private ContextResolver resolver = new ContextResolver();

  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  @Test
  public void testResolveContextIds_GlobalContext() {
    Map<AuthzContext.Key, Object> parameters = new HashMap<AuthzContext.Key, Object>();
    assertThat(resolver.resolveContextIds(parameters), contains(MembershipMapping.GLOBAL_CONTEXT_ID));
  }

  @Test
  public void testResolveContextIds_TypedContext_Application() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Map<AuthzContext.Key, Object> parameters = new HashMap<AuthzContext.Key, Object>();
    parameters.put(AuthzContext.Key.ID, app.getPublicId());
    parameters.put(AuthzContext.Key.TYPE, IdUtils.TYPE_APPLICATION);
    assertThat(resolver.resolveContextIds(parameters),
        contains(app.getId(), org.getId(), MembershipMapping.GLOBAL_CONTEXT_ID));
  }

  @Test
  public void testResolveContextIds_TypedContext_Organization() {
    Organization org = tempEntity.newOrganization();
    Map<AuthzContext.Key, Object> parameters = new HashMap<AuthzContext.Key, Object>();
    parameters.put(AuthzContext.Key.ID, org.getId());
    parameters.put(AuthzContext.Key.TYPE, IdUtils.TYPE_ORGANIZATION);
    assertThat(resolver.resolveContextIds(parameters), contains(org.getId(), MembershipMapping.GLOBAL_CONTEXT_ID));
  }

  @Test
  public void testResolveContextIds_OrganizationEntity() {
    Organization org = tempEntity.newOrganization();
    Map<AuthzContext.Key, Object> parameters = new HashMap<AuthzContext.Key, Object>();
    parameters.put(AuthzContext.Key.ORGANIZATION, org);
    assertThat(resolver.resolveContextIds(parameters), contains(org.getId(), MembershipMapping.GLOBAL_CONTEXT_ID));
  }

  @Test
  public void testResolveContextIds_OrganizationEntityOwner() {
    Organization org = new Organization("test-org");
    org.setId("not-to-be-considered-as-context");
    Map<AuthzContext.Key, Object> parameters = new HashMap<AuthzContext.Key, Object>();
    parameters.put(AuthzContext.Key.ORGANIZATION_OWNER, org);
    assertThat(resolver.resolveContextIds(parameters), contains(MembershipMapping.GLOBAL_CONTEXT_ID));
  }

  @Test
  public void testResolveContextIds_OrganizationId() {
    Organization org = tempEntity.newOrganization();
    Map<AuthzContext.Key, Object> parameters = new HashMap<AuthzContext.Key, Object>();
    parameters.put(AuthzContext.Key.ORGANIZATION_ID, org.getId());
    assertThat(resolver.resolveContextIds(parameters), contains(org.getId(), MembershipMapping.GLOBAL_CONTEXT_ID));
  }

  @Test
  public void testResolveContextIds_ApplicationEntity() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    app.setOrganizationId("not-to-be-considered");
    Map<AuthzContext.Key, Object> parameters = new HashMap<AuthzContext.Key, Object>();
    parameters.put(AuthzContext.Key.APPLICATION, app);
    assertThat(resolver.resolveContextIds(parameters),
        contains(app.getId(), org.getId(), MembershipMapping.GLOBAL_CONTEXT_ID));
  }

  @Test
  public void testResolveContextIds_ApplicationEntityOwner() {
    Organization org = tempEntity.newOrganization();
    Application app = new Application("test-app", "test-app", org.getId());
    app.setId("not-to-be-considered-as-context");
    Map<AuthzContext.Key, Object> parameters = new HashMap<AuthzContext.Key, Object>();
    parameters.put(AuthzContext.Key.APPLICATION_OWNER, app);
    assertThat(resolver.resolveContextIds(parameters), contains(org.getId(), MembershipMapping.GLOBAL_CONTEXT_ID));
    app.setOrganizationId(null);
    assertThat(resolver.resolveContextIds(parameters), contains(MembershipMapping.GLOBAL_CONTEXT_ID));
  }

  @Test
  public void testResolveContextIds_ApplicationId() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Map<AuthzContext.Key, Object> parameters = new HashMap<AuthzContext.Key, Object>();
    parameters.put(AuthzContext.Key.APPLICATION_ID, app.getId());
    assertThat(resolver.resolveContextIds(parameters),
        contains(app.getId(), org.getId(), MembershipMapping.GLOBAL_CONTEXT_ID));
  }

  @Test
  public void testResolveContextIds_ApplicationPublicId() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Map<AuthzContext.Key, Object> parameters = new HashMap<AuthzContext.Key, Object>();
    parameters.put(AuthzContext.Key.APPLICATION_PUBLIC_ID, app.getPublicId());
    assertThat(resolver.resolveContextIds(parameters),
        contains(app.getId(), org.getId(), MembershipMapping.GLOBAL_CONTEXT_ID));
  }
}
