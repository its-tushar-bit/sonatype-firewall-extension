/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.HashMap;
import java.util.Map;

import com.sonatype.insight.brain.AbstractDataTest;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.security.AuthzContext.Key;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ContextResolverTest
    extends AbstractDataTest
{
  private OrganizationDAO organizationDAO;

  private ContextResolver resolver;

  @Before
  public void before() {
    organizationDAO = daoFactory.createOrganizationDAO();
    resolver = new ContextResolver(daoFactory.createApplicationDAO(), organizationDAO,
        daoFactory.createRepositoryManagerDAO(), daoFactory.createRepositoryDAO(), daoFactory.createOwnerDAO());
  }

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
  public void testResolveContextIds_TypedContext_RepositoryManager() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Map<AuthzContext.Key, Object> parameters = new HashMap<>();
    parameters.put(AuthzContext.Key.ID, repositoryManager.getId());
    parameters.put(AuthzContext.Key.TYPE, OwnerType.REPOSITORY_MANAGER);
    assertThat(resolver.resolveContextIds(parameters)).containsExactly(repositoryManager.getId(),
        RepositoryContainer.REPOSITORY_CONTAINER_ID, Organization.ROOT_ORGANIZATION_ID,
        MembershipMapping.GLOBAL_CONTEXT_ID);
  }

  @Test
  public void testResolveContextIds_TypedContext_Repository() {
    Repository repository = tempEntity.newRepository();
    Map<AuthzContext.Key, Object> parameters = new HashMap<>();
    parameters.put(AuthzContext.Key.ID, repository.getId());
    parameters.put(AuthzContext.Key.TYPE, OwnerType.REPOSITORY);
    assertThat(resolver.resolveContextIds(parameters)).containsExactly(repository.getId(),
        repository.getParentOwnerId(), RepositoryContainer.REPOSITORY_CONTAINER_ID, Organization.ROOT_ORGANIZATION_ID,
        MembershipMapping.GLOBAL_CONTEXT_ID);
  }

  @Test
  public void testResolveContextIds_TypedContext_RepositoryManager_InternalId() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Map<AuthzContext.Key, Object> parameters = new HashMap<>();
    parameters.put(AuthzContext.Key.INTERNAL_ID, repositoryManager.getId());
    parameters.put(AuthzContext.Key.TYPE, OwnerType.REPOSITORY_MANAGER);
    assertThat(resolver.resolveContextIds(parameters)).containsExactly(repositoryManager.getId(),
        RepositoryContainer.REPOSITORY_CONTAINER_ID, Organization.ROOT_ORGANIZATION_ID,
        MembershipMapping.GLOBAL_CONTEXT_ID);
  }

  @Test
  public void testResolveContextIds_TypedContext_Repository_InternalId() {
    Repository repository = tempEntity.newRepository();
    Map<AuthzContext.Key, Object> parameters = new HashMap<>();
    parameters.put(AuthzContext.Key.INTERNAL_ID, repository.getId());
    parameters.put(AuthzContext.Key.TYPE, OwnerType.REPOSITORY);
    assertThat(resolver.resolveContextIds(parameters)).containsExactly(repository.getId(),
        repository.getParentOwnerId(), RepositoryContainer.REPOSITORY_CONTAINER_ID, Organization.ROOT_ORGANIZATION_ID,
        MembershipMapping.GLOBAL_CONTEXT_ID);
  }

  @Test
  public void testResolveContextIds_RepositoryId() {
    Repository repository = tempEntity.newRepository();
    Map<AuthzContext.Key, Object> parameters = new HashMap<>();
    parameters.put(AuthzContext.Key.REPOSITORY_ID, repository.getId());
    assertThat(resolver.resolveContextIds(parameters)).containsExactly(repository.getId(),
        repository.getParentOwnerId(), RepositoryContainer.REPOSITORY_CONTAINER_ID, Organization.ROOT_ORGANIZATION_ID,
        MembershipMapping.GLOBAL_CONTEXT_ID);
  }

  @Test
  public void testResolveContextIds_RepositoryManagerId() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Map<AuthzContext.Key, Object> parameters = new HashMap<>();
    parameters.put(AuthzContext.Key.REPOSITORY_MANAGER_ID, repositoryManager.getId());
    assertThat(resolver.resolveContextIds(parameters)).containsExactly(repositoryManager.getId(),
        RepositoryContainer.REPOSITORY_CONTAINER_ID, Organization.ROOT_ORGANIZATION_ID,
        MembershipMapping.GLOBAL_CONTEXT_ID);
  }

  @Test
  public void testResolveContextIds_RepositoryEntity_Existing() {
    Map<AuthzContext.Key, Object> parameters = new HashMap<>();
    Repository repository = tempEntity.newRepository();
    parameters.put(AuthzContext.Key.REPOSITORY, repository);
    assertThat(resolver.resolveContextIds(parameters)).containsExactly(repository.getId(),
        repository.getParentOwnerId(), RepositoryContainer.REPOSITORY_CONTAINER_ID, Organization.ROOT_ORGANIZATION_ID,
        MembershipMapping.GLOBAL_CONTEXT_ID);
  }

  @Test
  public void testResolveContextIds_RepositoryManagerEntity_New() {
    Map<AuthzContext.Key, Object> parameters = new HashMap<>();
    RepositoryManager newRepositoryManager = new RepositoryManager();
    parameters.put(AuthzContext.Key.REPOSITORY_MANAGER, newRepositoryManager);
    assertThat(resolver.resolveContextIds(parameters)).containsExactly(RepositoryContainer.REPOSITORY_CONTAINER_ID,
        Organization.ROOT_ORGANIZATION_ID, MembershipMapping.GLOBAL_CONTEXT_ID);
  }

  @Test
  public void testResolveContextIds_RepositoryManagerEntity_Existing() {
    Map<AuthzContext.Key, Object> parameters = new HashMap<>();
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    parameters.put(AuthzContext.Key.REPOSITORY_MANAGER, repositoryManager);
    assertThat(resolver.resolveContextIds(parameters)).containsExactly(repositoryManager.getId(),
        RepositoryContainer.REPOSITORY_CONTAINER_ID, Organization.ROOT_ORGANIZATION_ID,
        MembershipMapping.GLOBAL_CONTEXT_ID);
  }

  @Test
  public void testResolveContextIds_RepositoryEntity_NewRepositoryManager_NewRepository() {
    Map<AuthzContext.Key, Object> parameters = new HashMap<>();
    Repository newRepository = new Repository(null /* repositoryManagerId */, "publicId");
    parameters.put(AuthzContext.Key.REPOSITORY, newRepository);
    assertThat(resolver.resolveContextIds(parameters)).containsExactly(RepositoryContainer.REPOSITORY_CONTAINER_ID,
        Organization.ROOT_ORGANIZATION_ID, MembershipMapping.GLOBAL_CONTEXT_ID);
  }

  @Test
  public void testResolveContextIds_RepositoryEntity_ExistingManager_NewRepository() {
    Map<AuthzContext.Key, Object> parameters = new HashMap<>();
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository newRepository = new Repository(repositoryManager.getId(), "publicId");
    parameters.put(AuthzContext.Key.REPOSITORY, newRepository);
    assertThat(resolver.resolveContextIds(parameters)).containsExactly(repositoryManager.getId(),
        RepositoryContainer.REPOSITORY_CONTAINER_ID, Organization.ROOT_ORGANIZATION_ID,
        MembershipMapping.GLOBAL_CONTEXT_ID);
  }

  @Test
  public void testResolveContextIds_RepositoryContainer() {
    Map<AuthzContext.Key, Object> parameters = new HashMap<>();
    parameters.put(AuthzContext.Key.ID, RepositoryContainer.REPOSITORY_CONTAINER_ID);
    parameters.put(AuthzContext.Key.TYPE, OwnerType.REPOSITORY_CONTAINER);
    assertThat(resolver.resolveContextIds(parameters)).containsExactly(RepositoryContainer.REPOSITORY_CONTAINER_ID,
        Organization.ROOT_ORGANIZATION_ID, MembershipMapping.GLOBAL_CONTEXT_ID);
  }

  @Test
  public void testResolveContextIds_Owner_RootOrganization() {
    Map<AuthzContext.Key, Object> parameters = new HashMap<>();
    Owner owner = organizationDAO.getByIdNotNull(Organization.ROOT_ORGANIZATION_ID);
    parameters.put(AuthzContext.Key.OWNER, owner);
    assertThat(resolver.resolveContextIds(parameters)).containsExactly(Organization.ROOT_ORGANIZATION_ID,
        MembershipMapping.GLOBAL_CONTEXT_ID);
  }

  @Test
  public void testResolveContextIds_Owner_Organization() {
    Map<AuthzContext.Key, Object> parameters = new HashMap<>();
    Owner owner = tempEntity.newOrganization();
    parameters.put(AuthzContext.Key.OWNER, owner);
    assertThat(resolver.resolveContextIds(parameters)).containsExactly(owner.getId(), Organization.ROOT_ORGANIZATION_ID,
        MembershipMapping.GLOBAL_CONTEXT_ID);
  }

  @Test
  public void testResolveContextIds_Owner_Application() {
    Map<AuthzContext.Key, Object> parameters = new HashMap<>();
    Owner owner = tempEntity.newApplicationWithParent();
    parameters.put(AuthzContext.Key.OWNER, owner);
    assertThat(resolver.resolveContextIds(parameters)).containsExactly(owner.getId(), owner.getParentOwnerId(),
        Organization.ROOT_ORGANIZATION_ID, MembershipMapping.GLOBAL_CONTEXT_ID);
  }

  @Test
  public void testResolveContextIds_Owner_RepositoryContainer() {
    Map<AuthzContext.Key, Object> parameters = new HashMap<>();
    Owner owner = RepositoryContainer.SINGLETON;
    parameters.put(AuthzContext.Key.OWNER, owner);
    assertThat(resolver.resolveContextIds(parameters)).containsExactly(RepositoryContainer.REPOSITORY_CONTAINER_ID,
        Organization.ROOT_ORGANIZATION_ID, MembershipMapping.GLOBAL_CONTEXT_ID);
  }

  @Test
  public void testResolveContextIds_Owner_RepositoryManager() {
    Map<AuthzContext.Key, Object> parameters = new HashMap<>();
    Owner owner = tempEntity.newRepositoryManager();
    parameters.put(AuthzContext.Key.OWNER, owner);
    assertThat(resolver.resolveContextIds(parameters)).containsExactly(owner.getId(),
        RepositoryContainer.REPOSITORY_CONTAINER_ID, Organization.ROOT_ORGANIZATION_ID,
        MembershipMapping.GLOBAL_CONTEXT_ID);
  }

  @Test
  public void testResolveContextIds_Owner_Repository() {
    Map<AuthzContext.Key, Object> parameters = new HashMap<>();
    Owner owner = tempEntity.newRepository();
    parameters.put(AuthzContext.Key.OWNER, owner);
    assertThat(resolver.resolveContextIds(parameters)).containsExactly(owner.getId(), owner.getParentOwnerId(),
        RepositoryContainer.REPOSITORY_CONTAINER_ID, Organization.ROOT_ORGANIZATION_ID,
        MembershipMapping.GLOBAL_CONTEXT_ID);
  }
}
