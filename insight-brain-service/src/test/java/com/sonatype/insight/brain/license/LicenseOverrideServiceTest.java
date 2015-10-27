/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.license.LicenseOverrideService.AppliedLicenseOverrides;
import com.sonatype.insight.brain.license.LicenseOverrideService.LicenseOverrideByOwner;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.jaxrs.JsonEncodedComponentIdentifier;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

public class LicenseOverrideServiceTest
    extends AbstractComponentTest
{
  @Inject
  private LicenseOverrideService service;

  @Inject
  private InsightConfig config;

  @Test
  public void testGetAppliedLicenseOverrides_hierarchyHideRoot() {
    Application app = tempEntity.newApplicationWithParent("test");

    config.setShowRootOrganization(false);
    AppliedLicenseOverrides overrides = service.getAppliedLicenseOverrides(OwnerType.APPLICATION, "test",
        JsonEncodedComponentIdentifier.copy(ComponentIdentifier.createMavenCoordinates("g", "a", "v")));

    assertThat(overrides.licenseOverridesByOwner, hasSize(2));
    assertThat(overrides.licenseOverridesByOwner, hasItem(ownerId(app.getPublicId())));
    assertThat(overrides.licenseOverridesByOwner, hasItem(ownerId(app.getParentOwnerId())));
    assertThat(overrides.licenseOverridesByOwner, not(hasItem(ownerId(Organization.ROOT_ORGANIZATION_ID))));
  }

  @Test
  public void testGetAppliedLicenseOverrides_hierarchy() {
    Application app = tempEntity.newApplicationWithParent("test");

    config.setShowRootOrganization(true);
    AppliedLicenseOverrides overrides = service.getAppliedLicenseOverrides(OwnerType.APPLICATION, "test",
        JsonEncodedComponentIdentifier.copy(ComponentIdentifier.createMavenCoordinates("g", "a", "v")));

    assertThat(overrides.licenseOverridesByOwner, hasSize(3));
    assertThat(overrides.licenseOverridesByOwner, hasItem(ownerId(app.getPublicId())));
    assertThat(overrides.licenseOverridesByOwner, hasItem(ownerId(app.getParentOwnerId())));
    assertThat(overrides.licenseOverridesByOwner, hasItem(ownerId(Organization.ROOT_ORGANIZATION_ID)));
  }

  @Test
  public void testGetAppliedLicenseOverridesRepository_hierarchy() {
    Repository repository = tempEntity.newRepository();

    config.setShowRootOrganization(true);
    AppliedLicenseOverrides overrides = service.getAppliedLicenseOverrides(OwnerType.REPOSITORY, repository.getId(),
        JsonEncodedComponentIdentifier.copy(ComponentIdentifier.createMavenCoordinates("g", "a", "v")));

    assertThat(overrides.licenseOverridesByOwner, hasSize(3));
    assertThat(overrides.licenseOverridesByOwner, hasItem(ownerId(repository.getId())));
    assertThat(overrides.licenseOverridesByOwner, hasItem(ownerId(repository.getParentOwnerId())));
    assertThat(overrides.licenseOverridesByOwner, hasItem(ownerId(Organization.ROOT_ORGANIZATION_ID)));
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
