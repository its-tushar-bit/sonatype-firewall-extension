/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.jaxrs.JsonEncodedComponentIdentifier;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.mockito.Mockito.mock;

public class LicenseOverrideServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  private static final ComponentIdentifier COMPONENT_IDENTIFIER =
      ComponentIdentifier.createMavenCoordinates("g", "a", "1");

  @Inject
  private LicenseOverrideService licenseOverrideService;

  private HttpServletRequest mockRequest = mock(HttpServletRequest.class);

  @Test
  public void testAddLicenseOverrideForApplication_Authorized() throws Exception {
    grantWritePermission(app.getId());
    LicenseOverride override = new LicenseOverride(null, COMPONENT_IDENTIFIER, LicenseOverrideStatus.CONFIRMED,
        (String) null, "test");
    licenseOverrideService.addLicenseOverride(OwnerType.APPLICATION, app.getPublicId(), override, null, mockRequest);
  }

  @Test
  public void testAddLicenseOverrideForOrganization_Authorized() throws Exception {
    grantWritePermission(org.getId());
    LicenseOverride override = new LicenseOverride(null, COMPONENT_IDENTIFIER, LicenseOverrideStatus.CONFIRMED,
        (String) null, "test");
    licenseOverrideService.addLicenseOverride(OwnerType.ORGANIZATION, org.getId(), override, null, mockRequest);
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddLicenseOverrideForApplication_Unauthorized() throws Exception {
    grantReadPermission(app.getId());

    LicenseOverride override = new LicenseOverride(null, COMPONENT_IDENTIFIER, LicenseOverrideStatus.CONFIRMED,
        (String) null, "test");
    licenseOverrideService.addLicenseOverride(OwnerType.APPLICATION, app.getPublicId(), override, null,
        mockRequest);
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddLicenseOverrideForOrganization_Unauthorized() throws Exception {
    grantReadPermission(org.getId());

    LicenseOverride override = new LicenseOverride(null, COMPONENT_IDENTIFIER, LicenseOverrideStatus.CONFIRMED,
        (String) null, "test");
    licenseOverrideService.addLicenseOverride(OwnerType.ORGANIZATION, org.getId(), override, null,
        mockRequest);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddLicenseOverrideForApplication_Unauthenticated() throws Exception {
    LicenseOverride override = new LicenseOverride(null, COMPONENT_IDENTIFIER, LicenseOverrideStatus.CONFIRMED,
        (String) null, "test");
    licenseOverrideService.addLicenseOverride(OwnerType.APPLICATION, app.getPublicId(), override, null,
        mockRequest);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddLicenseOverrideForOrganization_Unauthenticated() throws Exception {
    LicenseOverride override = new LicenseOverride(null, COMPONENT_IDENTIFIER, LicenseOverrideStatus.CONFIRMED,
        (String) null, "test");
    licenseOverrideService.addLicenseOverride(OwnerType.ORGANIZATION, org.getId(), override, null,
        mockRequest);
  }

  @Test
  public void testDeleteLicenseOverrideForApplication_Authorized() throws Exception {
    grantWritePermission(app.getId());

    LicenseOverride override = tempEntity
        .newLicenseOverride(app.getId(), COMPONENT_IDENTIFIER, LicenseOverrideStatus.CONFIRMED, (String) null);
    licenseOverrideService.deleteLicenseOverride(OwnerType.APPLICATION, app.getPublicId(),
        override.getId(), null, mockRequest);
  }

  @Test
  public void testDeleteLicenseOverrideForOrganization_Authorized() throws Exception {
    grantWritePermission(org.getId());

    LicenseOverride override = tempEntity
        .newLicenseOverride(org.getId(), COMPONENT_IDENTIFIER, LicenseOverrideStatus.CONFIRMED, (String) null);
    licenseOverrideService.deleteLicenseOverride(OwnerType.ORGANIZATION, org.getId(), override.getId(),
        null, mockRequest);
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteLicenseOverrideForApplication_Unauthorized() throws Exception {
    grantReadPermission(app.getId());

    LicenseOverride override = tempEntity.newLicenseOverride(app.getId(), COMPONENT_IDENTIFIER,
        LicenseOverrideStatus.CONFIRMED, (String) null);
    licenseOverrideService.deleteLicenseOverride(OwnerType.APPLICATION, app.getPublicId(),
        override.getId(), null, mockRequest);
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteLicenseOverrideForOrganization_Unauthorized() throws Exception {
    grantReadPermission(org.getId());

    LicenseOverride override = tempEntity.newLicenseOverride(app.getId(), COMPONENT_IDENTIFIER,
        LicenseOverrideStatus.CONFIRMED, (String) null);
    licenseOverrideService.deleteLicenseOverride(OwnerType.ORGANIZATION, org.getId(), override.getId(),
        null, mockRequest);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteLicenseOverrideForApplication_Unauthenticated() throws Exception {
    LicenseOverride override = tempEntity.newLicenseOverride(app.getId(), COMPONENT_IDENTIFIER,
        LicenseOverrideStatus.CONFIRMED, (String) null);
    licenseOverrideService.deleteLicenseOverride(OwnerType.APPLICATION, app.getPublicId(),
        override.getId(), null, mockRequest);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteLicenseOverrideForOrganization_Unauthenticated() throws Exception {
    LicenseOverride override = tempEntity
        .newLicenseOverride(app.getId(), COMPONENT_IDENTIFIER, LicenseOverrideStatus.CONFIRMED, (String) null);
    licenseOverrideService.deleteLicenseOverride(OwnerType.ORGANIZATION, org.getId(), override.getId(),
        null, mockRequest);
  }

  @Test
  public void testGetAppliedLicenseOverridesForApplication_Authorized() throws Exception {
    grantReadPermission(app.getId());

    licenseOverrideService.getAppliedLicenseOverrides(OwnerType.APPLICATION, app.getPublicId(),
        JsonEncodedComponentIdentifier.copy(COMPONENT_IDENTIFIER));
  }

  @Test
  public void testGetAppliedLicenseOverridesForOrganization_Authorized() throws Exception {
    grantReadPermission(org.getId());

    licenseOverrideService.getAppliedLicenseOverrides(OwnerType.ORGANIZATION, org.getId(),
        JsonEncodedComponentIdentifier.copy(COMPONENT_IDENTIFIER));
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetAppliedLicenseOverridesForApplication_Unauthorized() throws Exception {
    login();

    licenseOverrideService.getAppliedLicenseOverrides(OwnerType.APPLICATION, app.getPublicId(),
        JsonEncodedComponentIdentifier.copy(COMPONENT_IDENTIFIER));
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetAppliedLicenseOverridesForOrganization_Unauthorized() throws Exception {
    login();

    licenseOverrideService.getAppliedLicenseOverrides(OwnerType.ORGANIZATION, org.getId(),
        JsonEncodedComponentIdentifier.copy(COMPONENT_IDENTIFIER));
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetAppliedLicenseOverridesForApplication_Unauthenticated() throws Exception {
    licenseOverrideService.getAppliedLicenseOverrides(OwnerType.APPLICATION, app.getPublicId(),
        JsonEncodedComponentIdentifier.copy(COMPONENT_IDENTIFIER));
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetAppliedLicenseOverridesForOrganization_Unauthenticated() throws Exception {
    licenseOverrideService.getAppliedLicenseOverrides(OwnerType.ORGANIZATION, org.getId(),
        JsonEncodedComponentIdentifier.copy(COMPONENT_IDENTIFIER));
  }
}
