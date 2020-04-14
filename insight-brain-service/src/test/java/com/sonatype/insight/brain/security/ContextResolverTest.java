/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.HashMap;
import java.util.Map;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.security.AuthzContext.Key;

import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ContextResolverTest
{
  private ContextResolver resolver = new ContextResolver();

  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  @Test
  public void testResolveContextIds_GlobalContext() {
    Map<AuthzContext.Key, Object> parameters = new HashMap<>();
    assertThat(resolver.resolveContextIds(parameters)).containsExactly(MembershipMapping.GLOBAL_CONTEXT_ID);
  }

  @Test
  public void testResolveContextIds_TypedContext_Application() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Map<AuthzContext.Key, Object> parameters = new HashMap<>();
    parameters.put(AuthzContext.Key.ID, app.getPublicId());
    parameters.put(AuthzContext.Key.TYPE, OwnerType.APPLICATION);
    assertThat(resolver.resolveContextIds(parameters)).containsExactly(app.getId(), org.getId(),
        org.getParentOrganizationId(), MembershipMapping.GLOBAL_CONTEXT_ID);
  }

  @Test
  public void testResolveContextIds_TypedContext_Application_InternalId() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Map<AuthzContext.Key, Object> parameters = new HashMap<>();
    parameters.put(Key.INTERNAL_ID, app.getId());
    parameters.put(AuthzContext.Key.TYPE, OwnerType.APPLICATION);
    assertThat(resolver.resolveContextIds(parameters)).containsExactly(app.getId(), org.getId(),
        org.getParentOrganizationId(), MembershipMapping.GLOBAL_CONTEXT_ID);
  }

  @Test
  public void testResolveContextIds_TypedContext_Organization() {
    Organization org = tempEntity.newOrganization();
    Map<AuthzContext.Key, Object> parameters = new HashMap<>();
    parameters.put(AuthzContext.Key.ID, org.getId());
    parameters.put(AuthzContext.Key.TYPE, OwnerType.ORGANIZATION);
    assertThat(resolver.resolveContextIds(parameters)).containsExactly(org.getId(), org.getParentOrganizationId(),
        MembershipMapping.GLOBAL_CONTEXT_ID);
  }

  @Test
  public void testResolveContextIds_OrganizationEntity() {
    Organization org = tempEntity.newOrganization();
    String realParentOrgId = org.getParentOrganizationId();
    org.setParentOrganizationId("not-to-be-considered");
    Map<AuthzContext.Key, Object> parameters = new HashMap<>();
    parameters.put(AuthzContext.Key.ORGANIZATION, org);
    assertThat(resolver.resolveContextIds(parameters)).containsExactly(org.getId(), realParentOrgId,
        MembershipMapping.GLOBAL_CONTEXT_ID);
  }

  @Test
  public void testResolveContextIds_OrganizationEntityOwner() {
    Organization org = new Organization("test-org");
    org.setId("not-to-be-considered-as-context");
    Map<AuthzContext.Key, Object> parameters = new HashMap<>();
    parameters.put(AuthzContext.Key.ORGANIZATION_OWNER, org);
    assertThat(resolver.resolveContextIds(parameters)).containsExactly(Organization.ROOT_ORGANIZATION_ID,
        MembershipMapping.GLOBAL_CONTEXT_ID);
  }

  @Test
  public void testResolveContextIds_OrganizationId() {
    Organization org = tempEntity.newOrganization();
    Map<AuthzContext.Key, Object> parameters = new HashMap<>();
    parameters.put(AuthzContext.Key.ORGANIZATION_ID, org.getId());
    assertThat(resolver.resolveContextIds(parameters)).containsExactly(org.getId(), org.getParentOrganizationId(),
        MembershipMapping.GLOBAL_CONTEXT_ID);
  }

  @Test
  public void testResolveContextIds_ApplicationEntity() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    app.setOrganizationId("not-to-be-considered");
    Map<AuthzContext.Key, Object> parameters = new HashMap<>();
    parameters.put(AuthzContext.Key.APPLICATION, app);
    assertThat(resolver.resolveContextIds(parameters)).containsExactly(app.getId(), org.getId(),
        org.getParentOrganizationId(), MembershipMapping.GLOBAL_CONTEXT_ID);
  }

  @Test
  public void testResolveContextIds_ApplicationEntityOwner() {
    Organization org = tempEntity.newOrganization();
    Application app = new Application("test-app", "test-app", org.getId());
    app.setId("not-to-be-considered-as-context");
    Map<AuthzContext.Key, Object> parameters = new HashMap<>();
    parameters.put(AuthzContext.Key.APPLICATION_OWNER, app);
    assertThat(resolver.resolveContextIds(parameters)).containsExactly(org.getId(), org.getParentOrganizationId(),
        MembershipMapping.GLOBAL_CONTEXT_ID);
  }

  @Test
  public void testResolveContextIds_ApplicationId() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Map<AuthzContext.Key, Object> parameters = new HashMap<>();
    parameters.put(AuthzContext.Key.APPLICATION_ID, app.getId());
    assertThat(resolver.resolveContextIds(parameters)).containsExactly(app.getId(), org.getId(),
        org.getParentOrganizationId(), MembershipMapping.GLOBAL_CONTEXT_ID);
  }

  @Test
  public void testResolveContextIds_ApplicationPublicId() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Map<AuthzContext.Key, Object> parameters = new HashMap<>();
    parameters.put(AuthzContext.Key.APPLICATION_PUBLIC_ID, app.getPublicId());
    assertThat(resolver.resolveContextIds(parameters)).containsExactly(app.getId(), org.getId(),
        org.getParentOrganizationId(), MembershipMapping.GLOBAL_CONTEXT_ID);
  }

  @Test
  public void testResolveContextIds_TypedContext_Repository() {
    Repository repository = tempEntity.newRepository();
    Map<AuthzContext.Key, Object> parameters = new HashMap<>();
    parameters.put(AuthzContext.Key.ID, repository.getId());
    parameters.put(AuthzContext.Key.TYPE, OwnerType.REPOSITORY);
    assertThat(resolver.resolveContextIds(parameters)).containsExactly(repository.getId(),
        repository.getParentOwnerId(), Organization.ROOT_ORGANIZATION_ID, MembershipMapping.GLOBAL_CONTEXT_ID);
  }

  @Test
  public void testResolveContextIds_TypedContext_Repository_InternalId() {
    Repository repository = tempEntity.newRepository();
    Map<AuthzContext.Key, Object> parameters = new HashMap<>();
    parameters.put(AuthzContext.Key.INTERNAL_ID, repository.getId());
    parameters.put(AuthzContext.Key.TYPE, OwnerType.REPOSITORY);
    assertThat(resolver.resolveContextIds(parameters)).containsExactly(repository.getId(),
        repository.getParentOwnerId(), Organization.ROOT_ORGANIZATION_ID, MembershipMapping.GLOBAL_CONTEXT_ID);
  }

  @Test
  public void testResolveContextIds_RepositoryId() {
    Repository repository = tempEntity.newRepository();
    Map<AuthzContext.Key, Object> parameters = new HashMap<>();
    parameters.put(AuthzContext.Key.REPOSITORY_ID, repository.getId());
    assertThat(resolver.resolveContextIds(parameters)).containsExactly(repository.getId(),
        repository.getParentOwnerId(), Organization.ROOT_ORGANIZATION_ID, MembershipMapping.GLOBAL_CONTEXT_ID);
  }

  @Test
  public void testResolveContextIds_RepositoryEntity_Existing() {
    Map<AuthzContext.Key, Object> parameters = new HashMap<>();
    Repository repository = tempEntity.newRepository();
    parameters.put(AuthzContext.Key.REPOSITORY, repository);
    assertThat(resolver.resolveContextIds(parameters)).containsExactly(repository.getId(),
        repository.getParentOwnerId(), Organization.ROOT_ORGANIZATION_ID, MembershipMapping.GLOBAL_CONTEXT_ID);
  }

  @Test
  public void testResolveContextIds_RepositoryEntity_New() {
    Map<AuthzContext.Key, Object> parameters = new HashMap<>();
    Repository newRepository = new Repository("repositoryManagerId", "publicId");
    parameters.put(AuthzContext.Key.REPOSITORY, newRepository);
    assertThat(resolver.resolveContextIds(parameters)).containsExactly(RepositoryContainer.REPOSITORY_CONTAINER_ID,
        Organization.ROOT_ORGANIZATION_ID, MembershipMapping.GLOBAL_CONTEXT_ID);
  }

  @Test
  public void testResolveContextIds_RepositoryContainer() {
    Map<AuthzContext.Key, Object> parameters = new HashMap<>();
    parameters.put(AuthzContext.Key.ID, RepositoryContainer.REPOSITORY_CONTAINER_ID);
    parameters.put(AuthzContext.Key.TYPE, OwnerType.REPOSITORY_CONTAINER);
    assertThat(resolver.resolveContextIds(parameters)).containsExactly(RepositoryContainer.REPOSITORY_CONTAINER_ID,
        Organization.ROOT_ORGANIZATION_ID, MembershipMapping.GLOBAL_CONTEXT_ID);
  }
}
