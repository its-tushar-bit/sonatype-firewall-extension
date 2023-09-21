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

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.license.LicenseOverrideService.AppliedLicenseOverrides;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.webhook.LicenseOverrideEvent;
import com.sonatype.insight.brain.webhook.LicenseOverrideEventService;
import com.sonatype.insight.brain.webhook.TestEventHandler;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.OwnerType.ORGANIZATION;
import static com.sonatype.insight.brain.model.license.LicenseOverrideStatus.OVERRIDDEN;
import static com.sonatype.insight.brain.webhook.EventAction.CREATED;
import static com.sonatype.insight.brain.webhook.EventAction.DELETED;
import static com.sonatype.insight.brain.webhook.EventAction.UPDATED;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LicenseOverrideServiceTest
    extends AbstractComponentTest
{
  private LicenseOverrideService service;

  @Inject
  private InsightWork work;

  @Inject
  private CurrentUser currentUser;

  @Inject
  private AsyncEventBus eventBus;

  @Inject
  private LicenseOverrideEventService licenseOverrideEventService;

  private TestEventHandler<LicenseOverrideEvent> handler;

  @After
  public void after() {
    if (handler != null) {
      eventBus.unregister(handler);
    }
  }

  @Before
  public void setup() {
    service = new LicenseOverrideService(work, new OwnerDAO(), currentUser, new LicenseOverrideDAO(), new LicenseDAO(),
        licenseOverrideEventService);
  }

  private void testGetAppliedLicenseOverridesNoAuth_hierarchy(final Owner owner) {
    testGetAppliedLicenseOverridesNoAuth_hierarchy(owner, owner.getId());
  }

  private void testGetAppliedLicenseOverridesNoAuth_hierarchy(final Owner owner, final String ownerId) {
    final AppliedLicenseOverrides overrides = service.getAppliedLicenseOverridesNoAuth(owner.getType(), ownerId,
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"));

    List<String> ownerIds = new OwnerDAO().getOwnerIds(owner);
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
    handler = new TestEventHandler<>(new CountDownLatch(1));
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
    handler = new TestEventHandler<>(new CountDownLatch(1));
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
    handler = new TestEventHandler<>(new CountDownLatch(1));
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
}
