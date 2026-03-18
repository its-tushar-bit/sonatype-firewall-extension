/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.license.LicenseOverrideService.AppliedLicenseOverrides;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.brain.webhook.LicenseOverrideEvent;
import com.sonatype.insight.brain.webhook.LicenseOverrideEventService;
import com.sonatype.insight.brain.webhook.TestEventHandler;

import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.OwnerType.*;
import static com.sonatype.insight.brain.model.license.LicenseOverrideStatus.OVERRIDDEN;
import static com.sonatype.insight.brain.webhook.EventAction.CREATED;
import static com.sonatype.insight.brain.webhook.EventAction.DELETED;
import static com.sonatype.insight.brain.webhook.EventAction.UPDATED;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LicenseOverrideServiceTest
    extends AbstractComponentTest
{
  private LicenseOverrideService service;

  @Inject
  private OwnerDAO ownerDAO;

  @Inject
  private LicenseDAO licenseDAO;

  @Inject
  private LicenseOverrideDAO licenseOverrideDAO;

  @Inject
  private RepositoryManagerDAO repositoryManagerDAO;

  @Inject
  private InsightWork work;

  @Inject
  private CurrentUser currentUser;

  @Inject
  private AsyncEventBus eventBus;

  @Inject
  private LicenseOverrideEventService licenseOverrideEventService;

  @Inject
  private ClusterLockManager clusterLockManager;

  @Inject
  private IdUtils idUtils;

  private TestEventHandler<LicenseOverrideEvent> handler;

  @After
  public void after() {
    if (handler != null) {
      eventBus.unregister(handler);
    }
  }

  @Before
  public void setup() {
    service = new LicenseOverrideService(work, ownerDAO, currentUser, licenseOverrideDAO, licenseDAO,
        licenseOverrideEventService, clusterLockManager, idUtils);
  }

  private void testGetAppliedLicenseOverridesNoAuth_hierarchy(final Owner owner) {
    testGetAppliedLicenseOverridesNoAuth_hierarchy(owner, owner.getId());
  }

  private void testGetAppliedLicenseOverridesNoAuth_hierarchy(final Owner owner, final String ownerId) {
    final AppliedLicenseOverrides overrides = service.getAppliedLicenseOverridesNoAuth(owner.getType(), ownerId,
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"));

    List<String> ownerIds = ownerDAO.getOwnerIds(owner);
    // For apps, the public id is used as owner id (for applied license overrides)
    ownerIds.set(0, ownerId);
    assertThat(overrides.licenseOverridesByOwner).extracting(licenseOverrideByOwner -> licenseOverrideByOwner.ownerId)
        .containsExactlyInAnyOrder(ownerIds.toArray(new String[0]));
  }

  @Test
  public void testGetAppliedLicenseOverridesNoAuth_hierarchy_App() {
    final Application app = tempEntity.newApplicationWithParent("test");
    testGetAppliedLicenseOverridesNoAuth_hierarchy(app, app.getPublicId());
  }

  @Test
  public void testGetAppliedLicenseOverridesNoAuth_hierarchy_Repository() {
    testGetAppliedLicenseOverridesNoAuth_hierarchy(tempEntity.newRepository());
  }

  @Test
  public void testGetAppliedLicenseOverridesNoAuth_hierarchy_RepositoryManager() {
    testGetAppliedLicenseOverridesNoAuth_hierarchy(tempEntity.newRepositoryManager());
  }

  @Test
  public void testGetAppliedLicenseOverridesNoAuth_hierarchy_RepositoryContainer() {
    Owner owner = RepositoryContainer.SINGLETON;
    final AppliedLicenseOverrides overrides = service.getAppliedLicenseOverridesNoAuth(owner.getType(), owner.getId(),
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"));
    assertThat(overrides.licenseOverridesByOwner).extracting(licenseOverrideByOwner -> licenseOverrideByOwner.ownerId)
        .containsExactlyInAnyOrder(owner.getId(), Organization.ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testCreateLicenseOverridePostEvents() throws InterruptedException, IOException {
    handler = new TestEventHandler<>(new CountDownLatch(1), LicenseOverrideEvent.class);
    eventBus.register(handler);

    Organization organization = tempEntity.newOrganization();
    LicenseOverride licenseOverride = new LicenseOverride(organization.getId(),
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), OVERRIDDEN, "MIT", "comment");
    HttpServletRequest mockHttpRequest = mock(HttpServletRequest.class);
    when(mockHttpRequest.getHeader("X-Forwarded-For")).thenReturn("1.1.1.1");

    service.addLicenseOverride(ORGANIZATION, organization.getId(), licenseOverride, null, mockHttpRequest);

    assertThat(handler.getLatch().await(5, SECONDS)).isTrue();
    assertThat(handler.getEvent().action).isEqualTo(CREATED);
    assertThat(handler.getEvent().licenseOverride).isSameAs(licenseOverride);
    assertThat(handler.getEvent().initiator).isEqualTo("testuser");
    assertThat(handler.getEvent().licenseOverride.getOwnerId()).isEqualTo(organization.getId());
  }

  @Test
  public void testUpdateLicenseOverridePostEvents() throws InterruptedException, IOException {
    handler = new TestEventHandler<>(new CountDownLatch(1), LicenseOverrideEvent.class);
    eventBus.register(handler);

    Organization organization = tempEntity.newOrganization();
    LicenseOverride licenseOverride = tempEntity.newLicenseOverride(organization.getId(),
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), OVERRIDDEN, "MIT", "comment");
    HttpServletRequest mockHttpRequest = mock(HttpServletRequest.class);
    when(mockHttpRequest.getHeader("X-Forwarded-For")).thenReturn("1.1.1.1");

    service.addLicenseOverride(ORGANIZATION, organization.getId(), licenseOverride, null, mockHttpRequest);

    assertThat(handler.getLatch().await(5, SECONDS)).isTrue();
    assertThat(handler.getEvent().action).isEqualTo(UPDATED);
    assertThat(handler.getEvent().licenseOverride).isSameAs(licenseOverride);
    assertThat(handler.getEvent().initiator).isEqualTo("testuser");
    assertThat(handler.getEvent().licenseOverride.getOwnerId()).isEqualTo(organization.getId());
  }

  @Test
  public void testDeleteLicenseOverridePostEvents() throws InterruptedException, IOException {
    handler = new TestEventHandler<>(new CountDownLatch(1), LicenseOverrideEvent.class);
    eventBus.register(handler);

    Organization organization = tempEntity.newOrganization();
    ComponentIdentifier mavenCoordinates = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    Set<String> licenses = Collections.singleton("MIT");
    LicenseOverride licenseOverride = tempEntity.newLicenseOverride(organization.getId(), mavenCoordinates, OVERRIDDEN,
        licenses, "comment");
    final String licenseOverrideId = licenseOverride.getId();
    HttpServletRequest mockHttpRequest = mock(HttpServletRequest.class);
    when(mockHttpRequest.getHeader("X-Forwarded-For")).thenReturn("1.1.1.1");

    service.deleteLicenseOverride(ORGANIZATION, organization.getId(), licenseOverrideId, null, mockHttpRequest);

    assertThat(handler.getLatch().await(5, SECONDS)).isTrue();
    assertThat(handler.getEvent().action).isEqualTo(DELETED);
    assertThat(handler.getEvent().licenseOverride.getOwnerId()).isEqualTo(organization.getId());
    assertThat(handler.getEvent().licenseOverride.getComment()).isEqualTo("comment");
    assertThat(handler.getEvent().licenseOverride.getComponentIdentifier()).isEqualTo(mavenCoordinates);
    assertThat(handler.getEvent().licenseOverride.getId()).isEqualTo(licenseOverrideId);
    assertThat(handler.getEvent().licenseOverride.getLicenseIds()).isEqualTo(licenses);
    assertThat(handler.getEvent().initiator).isEqualTo("testuser");
    assertThat(handler.getEvent().licenseOverride.getOwnerId()).isEqualTo(organization.getId());
  }

  @Test
  public void testDelete_Nonexistent_Application() throws Exception {
    String appPublicId = "LicenseOverrideResourceTest";
    tempEntity.newApplicationWithParent(appPublicId);

    assertThatThrownBy(() -> service.deleteLicenseOverride(APPLICATION, appPublicId, "YettiId", null, null))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("Cannot find a license override with ID " +
            "YettiId.");
  }

  @Test
  public void testDelete_Nonexistent_Organization() throws Exception {
    Organization organization = tempEntity.newOrganization("LicenseOverrideResourceTest");

    assertThatThrownBy(() -> service.deleteLicenseOverride(ORGANIZATION, organization.getId(), "YettiId", null, null))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("Cannot find a license override with ID " +
            "YettiId.");
  }

  @Test
  public void testDelete_Nonexistent_Repository() throws Exception {
    final Repository repository = tempEntity.newRepository();
    assertThatThrownBy(() -> service.deleteLicenseOverride(REPOSITORY, repository.getId(), "YettiId", null, null))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("Cannot find a license override with ID " +
            "YettiId.");
  }

  @Test
  public void testGetAppliedLicenseOverrides_NoComponentIdentifier() throws Exception {
    // Create an organization and an application
    String orgName = "testGetAppliedLicenseOverrides";
    Organization organization = tempEntity.newOrganization(orgName);
    String appPublicId = "testGetAppliedLicenseOverrides";
    tempEntity.newApplication(appPublicId, appPublicId, organization.getId());

    assertThatThrownBy(() -> service.getAppliedLicenseOverridesForRead(APPLICATION, appPublicId, null))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("componentIdentifier is required");
  }

  @Test
  public void testGetAppliedLicenseOverridesForLegalReviewer_NoComponentIdentifier() throws Exception {
    // Create an organization and an application
    String orgName = "testGetAppliedLicenseOverrides";
    Organization organization = tempEntity.newOrganization(orgName);
    String appPublicId = "testGetAppliedLicenseOverrides";
    tempEntity.newApplication(appPublicId, appPublicId, organization.getId());

    assertThatThrownBy(() -> service.getAppliedLicenseOverridesForLegalReviewer(APPLICATION, appPublicId, null))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("componentIdentifier is required");
  }

  @Test
  public void testGetAppliedLicenseOverrides() throws Exception {
    doTestGetAppliedLicenseOverrides(false);
  }

  @Test
  public void testGetAppliedLicenseOverridesForLegalReviewer() throws Exception {
    doTestGetAppliedLicenseOverrides(true);
  }

  private void doTestGetAppliedLicenseOverrides(boolean isLegalReviewer) throws Exception {
    HttpServletRequest mockHttpRequest = mock(HttpServletRequest.class);
    when(mockHttpRequest.getHeader("X-Forwarded-For")).thenReturn("1.1.1.1");

    // Create an organization, an application, and a repository
    String orgName = "testGetAppliedLicenseOverrides";
    Organization organization = tempEntity.newOrganization(orgName);
    Owner rootOrganization = ownerDAO.getParentOwner(organization);
    String orgId = organization.getId();
    String appPublicId = "testGetAppliedLicenseOverrides";
    Application app = tempEntity.newApplication(appPublicId, appPublicId, organization.getId());
    final Repository repository = tempEntity.newRepository();
    final String repoId = repository.getId();
    RepositoryManager repositoryManager = repositoryManagerDAO.getById(repository.getRepositoryManagerId());

    // Verify the applied license overrides for the application
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    AppliedLicenseOverrides appliedAppLicenseOverrides;
    if (isLegalReviewer) {
      appliedAppLicenseOverrides =
          service.getAppliedLicenseOverridesForLegalReviewer(APPLICATION, appPublicId, componentIdentifier);
    }
    else {
      appliedAppLicenseOverrides =
          service.getAppliedLicenseOverridesForRead(APPLICATION, appPublicId, componentIdentifier);
    }

    assertThat(appliedAppLicenseOverrides.licenseOverridesByOwner).hasSize(3);
    assertLicenseOverrideByOwner(app, false, appliedAppLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverrideByOwner(organization, false, appliedAppLicenseOverrides.licenseOverridesByOwner.get(1));
    assertLicenseOverrideByOwner(rootOrganization, false, appliedAppLicenseOverrides.licenseOverridesByOwner.get(2));

    // Verify the applied license overrides for the organization
    AppliedLicenseOverrides appliedOrgLicenseOverrides;
    if (isLegalReviewer) {
      appliedOrgLicenseOverrides =
          service.getAppliedLicenseOverridesForLegalReviewer(ORGANIZATION, orgId, componentIdentifier);
    }
    else {
      appliedOrgLicenseOverrides =
          service.getAppliedLicenseOverridesForRead(ORGANIZATION, orgId, componentIdentifier);
    }
    assertThat(appliedOrgLicenseOverrides).isNotNull();
    assertThat(appliedOrgLicenseOverrides.licenseOverridesByOwner).hasSize(2);
    assertLicenseOverrideByOwner(organization, false, appliedOrgLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverrideByOwner(rootOrganization, false, appliedOrgLicenseOverrides.licenseOverridesByOwner.get(1));

    // Verify the applied license overrides for the repository
    AppliedLicenseOverrides appliedRepoLicenseOverrides;
    if (isLegalReviewer) {
      appliedRepoLicenseOverrides =
          service.getAppliedLicenseOverridesForLegalReviewer(REPOSITORY, repoId, componentIdentifier);
    }
    else {
      appliedRepoLicenseOverrides =
          service.getAppliedLicenseOverridesForRead(REPOSITORY, repoId, componentIdentifier);
    }

    assertThat(appliedRepoLicenseOverrides.licenseOverridesByOwner).hasSize(4);
    assertLicenseOverrideByOwner(repository, false, appliedRepoLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverrideByOwner(repositoryManager, false, appliedRepoLicenseOverrides.licenseOverridesByOwner.get(1));
    assertLicenseOverrideByOwner(RepositoryContainer.SINGLETON, false,
        appliedRepoLicenseOverrides.licenseOverridesByOwner.get(2));
    assertLicenseOverrideByOwner(rootOrganization, false, appliedRepoLicenseOverrides.licenseOverridesByOwner.get(3));

    // Verify the applied license overrides for the repository_container
    AppliedLicenseOverrides appliedRepoContainerLicenseOverrides;
    if (isLegalReviewer) {
      appliedRepoContainerLicenseOverrides =
          service.getAppliedLicenseOverridesForLegalReviewer(REPOSITORY_CONTAINER,
              RepositoryContainer.REPOSITORY_CONTAINER_ID, componentIdentifier);
    }
    else {
      appliedRepoContainerLicenseOverrides =
          service.getAppliedLicenseOverridesForRead(REPOSITORY_CONTAINER,
              RepositoryContainer.REPOSITORY_CONTAINER_ID, componentIdentifier);
    }

    assertThat(appliedRepoContainerLicenseOverrides).isNotNull();
    assertThat(appliedRepoContainerLicenseOverrides.licenseOverridesByOwner).hasSize(2);
    assertLicenseOverrideByOwner(RepositoryContainer.SINGLETON, false,
        appliedRepoContainerLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverrideByOwner(rootOrganization, false,
        appliedRepoContainerLicenseOverrides.licenseOverridesByOwner.get(1));

    // Create a license override for the application
    LicenseOverride appLicenseOverride = new LicenseOverride(null /* ownerId */, componentIdentifier,
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0", "My comment");
    service.addLicenseOverride(APPLICATION, appPublicId, appLicenseOverride, null, mockHttpRequest);

    // Verify the applied license overrides for the application
    if (isLegalReviewer) {
      appliedAppLicenseOverrides =
          service.getAppliedLicenseOverridesForLegalReviewer(APPLICATION, appPublicId, componentIdentifier);
    }
    else {
      appliedAppLicenseOverrides =
          service.getAppliedLicenseOverridesForRead(APPLICATION, appPublicId, componentIdentifier);
    }

    assertThat(appliedAppLicenseOverrides).isNotNull();
    assertThat(appliedAppLicenseOverrides.licenseOverridesByOwner).hasSize(3);
    assertLicenseOverrideByOwner(app, true, appliedAppLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverrideByOwner(organization, false, appliedAppLicenseOverrides.licenseOverridesByOwner.get(1));
    assertLicenseOverrideByOwner(rootOrganization, false, appliedAppLicenseOverrides.licenseOverridesByOwner.get(2));
    assertThat(appliedAppLicenseOverrides.licenseOverridesByOwner.get(0).licenseOverride.getId())
        .isEqualTo(appLicenseOverride.getId());

    // Create a license override for the organization
    LicenseOverride orgLicenseOverride = new LicenseOverride(null /* ownerId */, componentIdentifier,
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0", "My comment");
    service.addLicenseOverride(ORGANIZATION, orgId, orgLicenseOverride, null, mockHttpRequest);

    // Verify the applied license overrides for the application
    if (isLegalReviewer) {
      appliedAppLicenseOverrides =
          service.getAppliedLicenseOverridesForLegalReviewer(APPLICATION, appPublicId, componentIdentifier);
    }
    else {
      appliedAppLicenseOverrides =
          service.getAppliedLicenseOverridesForRead(APPLICATION, appPublicId, componentIdentifier);
    }

    assertThat(appliedAppLicenseOverrides).isNotNull();
    assertThat(appliedAppLicenseOverrides.licenseOverridesByOwner).hasSize(3);
    assertLicenseOverrideByOwner(app, true, appliedAppLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverrideByOwner(organization, true, appliedAppLicenseOverrides.licenseOverridesByOwner.get(1));
    assertLicenseOverrideByOwner(rootOrganization, false, appliedAppLicenseOverrides.licenseOverridesByOwner.get(2));
    assertThat(appliedAppLicenseOverrides.licenseOverridesByOwner.get(0).licenseOverride.getId())
        .isEqualTo(appLicenseOverride.getId());
    assertThat(appliedAppLicenseOverrides.licenseOverridesByOwner.get(1).licenseOverride.getId())
        .isEqualTo(orgLicenseOverride.getId());

    // Verify the applied license overrides for the organization
    if (isLegalReviewer) {
      appliedOrgLicenseOverrides =
          service.getAppliedLicenseOverridesForLegalReviewer(ORGANIZATION, orgId, componentIdentifier);
    }
    else {
      appliedOrgLicenseOverrides =
          service.getAppliedLicenseOverridesForRead(ORGANIZATION, orgId, componentIdentifier);
    }

    assertThat(appliedOrgLicenseOverrides).isNotNull();
    assertThat(appliedOrgLicenseOverrides.licenseOverridesByOwner).hasSize(2);
    assertLicenseOverrideByOwner(organization, true, appliedOrgLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverrideByOwner(rootOrganization, false, appliedOrgLicenseOverrides.licenseOverridesByOwner.get(1));
    assertThat(appliedOrgLicenseOverrides.licenseOverridesByOwner.get(0).licenseOverride.getId())
        .isEqualTo(orgLicenseOverride.getId());

    // Create a license override for the repository container
    LicenseOverride repoLicenseOverride = new LicenseOverride(null /* ownerId */, componentIdentifier,
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0", "My comment2");
    service.addLicenseOverride(REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID,
        repoLicenseOverride, null, mockHttpRequest);

    // Verify the applied root org license overrides for the repository
    if (isLegalReviewer) {
      appliedRepoLicenseOverrides =
          service.getAppliedLicenseOverridesForLegalReviewer(REPOSITORY, repoId, componentIdentifier);
    }
    else {
      appliedRepoLicenseOverrides =
          service.getAppliedLicenseOverridesForRead(REPOSITORY, repoId, componentIdentifier);
    }

    assertThat(appliedRepoLicenseOverrides).isNotNull();
    assertThat(appliedRepoLicenseOverrides.licenseOverridesByOwner).hasSize(4);
    assertLicenseOverrideByOwner(repository, false, appliedRepoLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverrideByOwner(repositoryManager, false, appliedRepoLicenseOverrides.licenseOverridesByOwner.get(1));
    assertLicenseOverrideByOwner(RepositoryContainer.SINGLETON, true,
        appliedRepoLicenseOverrides.licenseOverridesByOwner.get(2));
    assertLicenseOverrideByOwner(rootOrganization, false, appliedRepoLicenseOverrides.licenseOverridesByOwner.get(3));
    assertThat(appliedRepoLicenseOverrides.licenseOverridesByOwner.get(2).licenseOverride.getId())
        .isEqualTo(repoLicenseOverride.getId());

    // Create a license override for the root organization
    LicenseOverride rootOrgLicenseOverride = new LicenseOverride(null /* ownerId */, componentIdentifier,
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0", "My comment2");
    service.addLicenseOverride(ORGANIZATION, rootOrganization.getId(), rootOrgLicenseOverride, null, mockHttpRequest);

    // Verify the applied root org license overrides for the repository
    if (isLegalReviewer) {
      appliedRepoLicenseOverrides =
          service.getAppliedLicenseOverridesForLegalReviewer(REPOSITORY, repoId, componentIdentifier);
    }
    else {
      appliedRepoLicenseOverrides =
          service.getAppliedLicenseOverridesForRead(REPOSITORY, repoId, componentIdentifier);
    }

    assertThat(appliedRepoLicenseOverrides).isNotNull();
    assertThat(appliedRepoLicenseOverrides.licenseOverridesByOwner).hasSize(4);
    assertLicenseOverrideByOwner(repository, false, appliedRepoLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverrideByOwner(repositoryManager, false, appliedRepoLicenseOverrides.licenseOverridesByOwner.get(1));
    assertLicenseOverrideByOwner(RepositoryContainer.SINGLETON, true,
        appliedRepoLicenseOverrides.licenseOverridesByOwner.get(2));
    assertLicenseOverrideByOwner(rootOrganization, true, appliedRepoLicenseOverrides.licenseOverridesByOwner.get(3));
    assertThat(appliedRepoLicenseOverrides.licenseOverridesByOwner.get(3).licenseOverride.getId())
        .isEqualTo(rootOrgLicenseOverride.getId());
  }

  @Test
  public void testDelete_OwnerIdMismatch_Application() throws Exception {
    String appPublicId1 = "LicenseOverrideResourceTest1";
    tempEntity.newApplicationWithParent(appPublicId1);
    String appPublicId2 = "LicenseOverrideResourceTest2";
    tempEntity.newApplicationWithParent(appPublicId2);

    testDelete_OwnerIdMismatch(OwnerType.APPLICATION, appPublicId1, appPublicId2);
  }

  @Test
  public void testDelete_OwnerIdMismatch_Organization() throws Exception {
    Organization organization1 = tempEntity.newOrganization("LicenseOverrideResourceTest1");
    Organization organization2 = tempEntity.newOrganization("LicenseOverrideResourceTest2");

    testDelete_OwnerIdMismatch(OwnerType.ORGANIZATION, organization1.getId(), organization2.getId());
  }

  @Test
  public void testDelete_OwnerIdMismatch_Repository() throws Exception {
    final Repository repository = tempEntity.newRepository();
    final Repository repository2 = tempEntity.newRepository();
    testDelete_OwnerIdMismatch(OwnerType.REPOSITORY, repository.getId(), repository2.getId());
  }

  @Test
  public void testDelete_OwnerIdMismatch_RepositoryManager() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    RepositoryManager repositoryManager2 = tempEntity.newRepositoryManager();
    testDelete_OwnerIdMismatch(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId(), repositoryManager2.getId());
  }

  @Test
  public void testAddLicenseOverride_ValidateComponentIdentifier() throws Exception {
    String appPublicId = "LicenseOverrideResourceTest";
    tempEntity.newApplicationWithParent(appPublicId);

    LicenseOverride licenseOverride = new LicenseOverride(null /* ownerId */, null /* componentIdentifier */,
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0", "My comment");

    assertThatThrownBy(() -> service.addLicenseOverride(APPLICATION, appPublicId, licenseOverride, null, null))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("The component identifier cannot be null.");
  }

  @Test
  public void testAddLicenseOverride_NullBody() throws Exception {
    String appPublicId = "LicenseOverrideResourceTest";
    tempEntity.newApplicationWithParent(appPublicId);

    assertThatThrownBy(() -> service.addLicenseOverride(APPLICATION, appPublicId, null, null, null))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("The license override cannot be null. Validate the body of the request.");
  }

  private void assertLicenseOverrideByOwner(
      Owner owner,
      boolean hasLicenseOverride,
      LicenseOverrideService.LicenseOverrideByOwner actual)
  {
    assertThat(actual.ownerId).isEqualTo(owner instanceof Repository ? owner.getId() : owner.getPublicId());
    assertThat(actual.ownerName).isEqualTo(owner.getName());
    assertThat(actual.ownerType).isEqualTo(owner.getType());
    if (hasLicenseOverride) {
      assertThat(actual.licenseOverride).isNotNull();
    }
    else {
      assertThat(actual.licenseOverride).isNull();
    }
  }

  private void testDelete_OwnerIdMismatch(
      OwnerType ownerType,
      String ownerPublicId1,
      String ownerPublicId2) throws Exception
  {
    HttpServletRequest mockHttpRequest = mock(HttpServletRequest.class);
    when(mockHttpRequest.getHeader("X-Forwarded-For")).thenReturn("1.1.1.1");

    LicenseOverride licenseOverride = new LicenseOverride(null /* ownerId */,
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"),
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0",
        "My comment");
    service.addLicenseOverride(ownerType, ownerPublicId1, licenseOverride, null, mockHttpRequest);

    assertThatThrownBy(() -> service.deleteLicenseOverride(ownerType, ownerPublicId2, licenseOverride.getId(), null,
        mockHttpRequest)).isInstanceOf(NotFoundException.class)
            .hasMessage(
                "Cannot find a license override with ID " + licenseOverride.getId() +
                    " for " + ownerType + " ID " + ownerPublicId2);

    // Verify that the license override was not deleted
    licenseOverrideDAO.getByIdNotNull(licenseOverride.getId());
  }
}
