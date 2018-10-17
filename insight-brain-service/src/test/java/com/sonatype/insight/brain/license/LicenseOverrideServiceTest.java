/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import java.io.IOException;
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
import com.sonatype.insight.brain.license.LicenseOverrideService.LicenseOverrideByOwner;
import com.sonatype.insight.brain.migration.RootOrganizationConfigMigrationUtils;
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
import com.sonatype.insight.jaxrs.JsonEncodedComponentIdentifier;

import com.google.common.collect.ImmutableSet;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static com.sonatype.insight.brain.model.OwnerType.ORGANIZATION;
import static com.sonatype.insight.brain.model.license.LicenseOverrideStatus.OVERRIDDEN;
import static com.sonatype.insight.brain.webhook.EventAction.CREATED;
import static com.sonatype.insight.brain.webhook.EventAction.DELETED;
import static com.sonatype.insight.brain.webhook.EventAction.UPDATED;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.BDDMockito.given;

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

  private RootOrganizationConfigMigrationUtils rootOrganizationConfigMigrationUtils;

  private TestEventHandler<LicenseOverrideEvent> handler;

  @After
  public void after() {
    if (handler != null) {
      eventBus.unregister(handler);
    }
  }

  private void testGetAppliedLicenseOverrides_hierarchyHideRoot(final Owner owner) {
    testGetAppliedLicenseOverrides_hierarchyHideRoot(owner, owner.getId());
  }

  private void testGetAppliedLicenseOverrides_hierarchyHideRoot(final Owner owner, final String ownerId) {
    final AppliedLicenseOverrides overrides = service.getAppliedLicenseOverrides(owner.getType(), ownerId,
        JsonEncodedComponentIdentifier.copy(ComponentIdentifier.createMavenCoordinates("g", "a", "v")));

    assertThat(overrides.licenseOverridesByOwner, hasSize(2));
    assertThat(overrides.licenseOverridesByOwner, hasItem(ownerId(ownerId)));
    assertThat(overrides.licenseOverridesByOwner, hasItem(ownerId(owner.getParentOwnerId())));
    assertThat(overrides.licenseOverridesByOwner, not(hasItem(ownerId(Organization.ROOT_ORGANIZATION_ID))));
  }

  @Before
  public void setup() {
    rootOrganizationConfigMigrationUtils = Mockito.mock(RootOrganizationConfigMigrationUtils.class);
    service = new LicenseOverrideService(work, new OwnerDAO(), currentUser, new LicenseOverrideDAO(), new LicenseDAO(),
        rootOrganizationConfigMigrationUtils, licenseOverrideEventService);
  }

  @Test
  public void testGetAppliedLicenseOverrides_hierarchyHideRoot_App() {
    Mockito.when(rootOrganizationConfigMigrationUtils.isMigrated()).thenReturn(false);

    final Application app = tempEntity.newApplicationWithParent("test");
    testGetAppliedLicenseOverrides_hierarchyHideRoot(app, app.getPublicId());
  }

  @Test
  public void testGetAppliedLicenseOverrides_hierarchyHideRoot_Repository() {
    Mockito.when(rootOrganizationConfigMigrationUtils.isMigrated()).thenReturn(false);

    testGetAppliedLicenseOverrides_hierarchyHideRoot(tempEntity.newRepository());
  }

  @Test
  public void testGetAppliedLicenseOverrides_hierarchyHideRoot_RepositoryContainer() {
    Mockito.when(rootOrganizationConfigMigrationUtils.isMigrated()).thenReturn(false);
    Owner owner = RepositoryContainer.SINGLETON;
    final AppliedLicenseOverrides overrides = service.getAppliedLicenseOverrides(owner.getType(), owner.getId(),
        JsonEncodedComponentIdentifier.copy(ComponentIdentifier.createMavenCoordinates("g", "a", "v")));
    assertThat(overrides.licenseOverridesByOwner, hasSize(1));
    assertThat(overrides.licenseOverridesByOwner, hasItem(ownerId(owner.getId())));
    assertThat(overrides.licenseOverridesByOwner, not(hasItem(ownerId(Organization.ROOT_ORGANIZATION_ID))));
  }

  private void testGetAppliedLicenseOverrides_hierarchy(final Owner owner) {
    testGetAppliedLicenseOverrides_hierarchy(owner, owner.getId());
  }

  private void testGetAppliedLicenseOverrides_hierarchy(final Owner owner, final String ownerId) {
    final AppliedLicenseOverrides overrides = service.getAppliedLicenseOverrides(owner.getType(), ownerId,
        JsonEncodedComponentIdentifier.copy(ComponentIdentifier.createMavenCoordinates("g", "a", "v")));

    assertThat(overrides.licenseOverridesByOwner, hasSize(3));
    assertThat(overrides.licenseOverridesByOwner, hasItem(ownerId(ownerId)));
    assertThat(overrides.licenseOverridesByOwner, hasItem(ownerId(owner.getParentOwnerId())));
    assertThat(overrides.licenseOverridesByOwner, hasItem(ownerId(Organization.ROOT_ORGANIZATION_ID)));
  }

  @Test
  public void testGetAppliedLicenseOverrides_hierarchy_App() {
    Mockito.when(rootOrganizationConfigMigrationUtils.isMigrated()).thenReturn(true);

    final Application app = tempEntity.newApplicationWithParent("test");
    testGetAppliedLicenseOverrides_hierarchy(app, app.getPublicId());
  }

  @Test
  public void testGetAppliedLicenseOverrides_hierarchy_Repository() {
    Mockito.when(rootOrganizationConfigMigrationUtils.isMigrated()).thenReturn(true);

    testGetAppliedLicenseOverrides_hierarchy(tempEntity.newRepository());
  }

  @Test
  public void testGetAppliedLicenseOverrides_hierarchy_RepositoryContainer() {
    Mockito.when(rootOrganizationConfigMigrationUtils.isMigrated()).thenReturn(true);
    Owner owner = RepositoryContainer.SINGLETON;
    final AppliedLicenseOverrides overrides = service.getAppliedLicenseOverrides(owner.getType(), owner.getId(),
        JsonEncodedComponentIdentifier.copy(ComponentIdentifier.createMavenCoordinates("g", "a", "v")));
    assertThat(overrides.licenseOverridesByOwner, hasSize(2));
    assertThat(overrides.licenseOverridesByOwner, hasItem(ownerId(owner.getId())));
    assertThat(overrides.licenseOverridesByOwner, hasItem(ownerId(Organization.ROOT_ORGANIZATION_ID)));
  }

  @Test
  public void testCreateLicenseOverridePostEvents() throws InterruptedException, IOException {
    handler = new TestEventHandler<>(new CountDownLatch(1));
    eventBus.register(handler);

    Organization organization = tempEntity.newOrganization();
    LicenseOverride licenseOverride = new LicenseOverride(organization.getId(),
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), OVERRIDDEN, "MIT", "comment");
    HttpServletRequest mockHttpRequest = Mockito.mock(HttpServletRequest.class);
    given(mockHttpRequest.getHeader("X-Forwarded-For")).willReturn("1.1.1.1");

    service.addLicenseOverride(ORGANIZATION, organization.getId(), licenseOverride, null, mockHttpRequest);

    assertThat(handler.getLatch().await(5, SECONDS), is(true));
    assertThat(handler.getEvent().action, is(CREATED));
    assertThat(handler.getEvent().licenseOverride, sameInstance(licenseOverride));
    assertThat(handler.getEvent().initiator, is("testuser"));
    assertThat(handler.getEvent().licenseOverride.getOwnerId(), is(organization.getId()));
  }

  @Test
  public void testUpdateLicenseOverridePostEvents() throws InterruptedException, IOException {
    handler = new TestEventHandler<>(new CountDownLatch(1));
    eventBus.register(handler);

    Organization organization = tempEntity.newOrganization();
    LicenseOverride licenseOverride = tempEntity.newLicenseOverride(organization.getId(),
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), OVERRIDDEN, "MIT", "comment");
    HttpServletRequest mockHttpRequest = Mockito.mock(HttpServletRequest.class);
    given(mockHttpRequest.getHeader("X-Forwarded-For")).willReturn("1.1.1.1");

    service.addLicenseOverride(ORGANIZATION, organization.getId(), licenseOverride, null, mockHttpRequest);

    assertThat(handler.getLatch().await(5, SECONDS), is(true));
    assertThat(handler.getEvent().action, is(UPDATED));
    assertThat(handler.getEvent().licenseOverride, sameInstance(licenseOverride));
    assertThat(handler.getEvent().initiator, is("testuser"));
    assertThat(handler.getEvent().licenseOverride.getOwnerId(), is(organization.getId()));
  }

  @Test
  public void testDeleteLicenseOverridePostEvents() throws InterruptedException, IOException {
    handler = new TestEventHandler<>(new CountDownLatch(1));
    eventBus.register(handler);

    Organization organization = tempEntity.newOrganization();
    ComponentIdentifier mavenCoordinates = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    Set<String> licenses = ImmutableSet.of("MIT");
    LicenseOverride licenseOverride = tempEntity.newLicenseOverride(organization.getId(), mavenCoordinates, OVERRIDDEN,
        licenses, "comment");
    final String licenseOverrideId = licenseOverride.getId();
    HttpServletRequest mockHttpRequest = Mockito.mock(HttpServletRequest.class);
    given(mockHttpRequest.getHeader("X-Forwarded-For")).willReturn("1.1.1.1");

    service.deleteLicenseOverride(ORGANIZATION, organization.getId(), licenseOverrideId, null, mockHttpRequest);

    assertThat(handler.getLatch().await(5, SECONDS), is(true));
    assertThat(handler.getEvent().action, is(DELETED));
    assertThat(handler.getEvent().licenseOverride.getOwnerId(), is(organization.getId()));
    assertThat(handler.getEvent().licenseOverride.getComment(), is("comment"));
    assertThat(handler.getEvent().licenseOverride.getComponentIdentifier(), is(mavenCoordinates));
    assertThat(handler.getEvent().licenseOverride.getId(), is(licenseOverrideId));
    assertThat(handler.getEvent().licenseOverride.getLicenseIds(), is(licenses));
    assertThat(handler.getEvent().initiator, is("testuser"));
    assertThat(handler.getEvent().licenseOverride.getOwnerId(), is(organization.getId()));
  }

  private Matcher<LicenseOverrideByOwner> ownerId(final String ownerId) {
    return new BaseMatcher<LicenseOverrideByOwner>()
    {
      @Override
      public boolean matches(Object item) {
        if (item instanceof LicenseOverrideByOwner) {
          return ownerId.equals(((LicenseOverrideByOwner) item).ownerId);
        }
        return false;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("Expected ownerId: " + ownerId);
      }
    };
  }
}
