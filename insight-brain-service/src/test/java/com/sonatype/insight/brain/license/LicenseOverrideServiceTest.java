/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.license.LicenseOverrideService.AppliedLicenseOverrides;
import com.sonatype.insight.brain.license.LicenseOverrideService.LicenseOverrideByOwner;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.organization.RootOrganizationConfigMigrationService;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.jaxrs.JsonEncodedComponentIdentifier;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

public class LicenseOverrideServiceTest
    extends AbstractComponentTest
{
  private LicenseOverrideService service;

  @Inject
  private InsightWork work;

  @Inject
  private CurrentUser currentUser;

  private RootOrganizationConfigMigrationService rootOrganizationMigrationService;

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
    rootOrganizationMigrationService = Mockito.mock(RootOrganizationConfigMigrationService.class);
    service = new LicenseOverrideService(work, new OwnerDAO(), currentUser, new LicenseOverrideDAO(),
        rootOrganizationMigrationService);
  }

  @Test
  public void testGetAppliedLicenseOverrides_hierarchyHideRoot_App() {
    Mockito.when(rootOrganizationMigrationService.isMigrated()).thenReturn(false);

    final Application app = tempEntity.newApplicationWithParent("test");
    testGetAppliedLicenseOverrides_hierarchyHideRoot(app, app.getPublicId());
  }

  @Test
  public void testGetAppliedLicenseOverrides_hierarchyHideRoot_Repository() {
    Mockito.when(rootOrganizationMigrationService.isMigrated()).thenReturn(false);

    testGetAppliedLicenseOverrides_hierarchyHideRoot(tempEntity.newRepository());
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
    Mockito.when(rootOrganizationMigrationService.isMigrated()).thenReturn(true);

    final Application app = tempEntity.newApplicationWithParent("test");
    testGetAppliedLicenseOverrides_hierarchy(app, app.getPublicId());
  }

  @Test
  public void testGetAppliedLicenseOverrides_hierarchy_Repository() {
    Mockito.when(rootOrganizationMigrationService.isMigrated()).thenReturn(true);

    testGetAppliedLicenseOverrides_hierarchy(tempEntity.newRepository());
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
