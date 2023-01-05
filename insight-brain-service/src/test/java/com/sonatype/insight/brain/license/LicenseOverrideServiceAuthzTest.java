/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.mockito.Mockito.mock;

public class LicenseOverrideServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  private static final ComponentIdentifier COMPONENT_IDENTIFIER = ComponentIdentifier.createMavenCoordinates("g", "a",
      "1");

  @Inject
  private LicenseOverrideService licenseOverrideService;

  private final HttpServletRequest mockRequest = mock(HttpServletRequest.class);

  private void testAddLicenseOverride_Authorized(final Owner owner) throws Exception {
    testAddLicenseOverride_Authorized(owner, owner.getId());
  }

  private void testAddLicenseOverride_Authorized(final Owner owner, final String ownerId) throws Exception {
    grantPermission(owner.getId(), Permission.CHANGE_LICENSES);
    LicenseOverride override = new LicenseOverride(null, COMPONENT_IDENTIFIER, LicenseOverrideStatus.CONFIRMED,
        (String) null, "test");
    tempEntity.register(override);
    licenseOverrideService.addLicenseOverride(owner.getType(), ownerId, override, null, mockRequest);
  }

  @Test
  public void testAddLicenseOverride_Authorized_App() throws Exception {
    testAddLicenseOverride_Authorized(app, app.getPublicId());
  }

  @Test
  public void testAddLicenseOverride_Authorized_Org() throws Exception {
    testAddLicenseOverride_Authorized(org);
  }

  @Test
  public void testAddLicenseOverride_Authorized_Repository() throws Exception {
    testAddLicenseOverride_Authorized(repository);
  }

  @Test
  public void testAddLicenseOverride_Authorized_RepositoryContainer() throws Exception {
    testAddLicenseOverride_Authorized(RepositoryContainer.SINGLETON);
  }

  private void testAddLicenseOverride_Unauthorized(final Owner owner) throws Exception {
    testAddLicenseOverride_Unauthorized(owner, owner.getId());
  }

  private void testAddLicenseOverride_Unauthorized(final Owner owner, final String ownerId) throws Exception {
    grantReadPermission(owner.getId());
    LicenseOverride override = new LicenseOverride(null, COMPONENT_IDENTIFIER, LicenseOverrideStatus.CONFIRMED,
        (String) null, "test");
    licenseOverrideService.addLicenseOverride(owner.getType(), ownerId, override, null, mockRequest);
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddLicenseOverride_Unauthorized_App() throws Exception {
    testAddLicenseOverride_Unauthorized(app, app.getPublicId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddLicenseOverride_Unauthorized_Org() throws Exception {
    testAddLicenseOverride_Unauthorized(org);
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddLicenseOverride_Unauthorized_Repository() throws Exception {
    testAddLicenseOverride_Unauthorized(repository);
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddLicenseOverride_Unauthorized_RepositoryContainer() throws Exception {
    testAddLicenseOverride_Unauthorized(RepositoryContainer.SINGLETON);
  }

  private void testAddLicenseOverride_Unauthenticated(final Owner owner) throws Exception {
    testAddLicenseOverride_Unauthenticated(owner, owner.getId());
  }

  private void testAddLicenseOverride_Unauthenticated(final Owner owner, final String ownerId) throws Exception {
    LicenseOverride override = new LicenseOverride(null, COMPONENT_IDENTIFIER, LicenseOverrideStatus.CONFIRMED,
        (String) null, "test");
    licenseOverrideService.addLicenseOverride(owner.getType(), ownerId, override, null, mockRequest);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddLicenseOverride_Unauthenticated_App() throws Exception {
    testAddLicenseOverride_Unauthenticated(app, app.getPublicId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddLicenseOverride_Unauthenticated_Org() throws Exception {
    testAddLicenseOverride_Unauthenticated(org);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddLicenseOverride_Unauthenticated_Repository() throws Exception {
    testAddLicenseOverride_Unauthenticated(repository);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddLicenseOverride_Unauthenticated_RepositoryContainer() throws Exception {
    testAddLicenseOverride_Unauthenticated(RepositoryContainer.SINGLETON);
  }

  private void testDeleteLicenseOverride_Authorized(final Owner owner) throws Exception {
    testDeleteLicenseOverride_Authorized(owner, owner.getId());
  }

  private void testDeleteLicenseOverride_Authorized(final Owner owner, final String ownerId) throws Exception {
    grantPermission(owner.getId(), Permission.CHANGE_LICENSES);
    LicenseOverride override = tempEntity.newLicenseOverride(owner.getId(), COMPONENT_IDENTIFIER,
        LicenseOverrideStatus.CONFIRMED, (String) null);
    licenseOverrideService.deleteLicenseOverride(owner.getType(), ownerId, override.getId(), null, mockRequest);
  }

  @Test
  public void testDeleteLicenseOverride_Authorized_App() throws Exception {
    testDeleteLicenseOverride_Authorized(app, app.getPublicId());
  }

  @Test
  public void testDeleteLicenseOverride_Authorized_Org() throws Exception {
    testDeleteLicenseOverride_Authorized(org);
  }

  @Test
  public void testDeleteLicenseOverride_Authorized_Repository() throws Exception {
    testDeleteLicenseOverride_Authorized(repository);
  }

  @Test
  public void testDeleteLicenseOverride_Authorized_RepositoryContainer() throws Exception {
    testDeleteLicenseOverride_Authorized(RepositoryContainer.SINGLETON);
  }

  private void testDeleteLicenseOverride_Unauthorized(final Owner owner) throws Exception {
    testDeleteLicenseOverride_Unauthorized(owner, owner.getId());
  }

  private void testDeleteLicenseOverride_Unauthorized(final Owner owner, final String ownerId) throws Exception {
    grantReadPermission(owner.getId());

    LicenseOverride override = tempEntity.newLicenseOverride(owner.getId(), COMPONENT_IDENTIFIER,
        LicenseOverrideStatus.CONFIRMED, (String) null);
    licenseOverrideService.deleteLicenseOverride(owner.getType(), ownerId, override.getId(), null, mockRequest);
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteLicenseOverride_Unauthorized_App() throws Exception {
    testDeleteLicenseOverride_Unauthorized(app, app.getPublicId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteLicenseOverride_Unauthorized_Org() throws Exception {
    testDeleteLicenseOverride_Unauthorized(org);
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteLicenseOverride_Unauthorized_Repository() throws Exception {
    testDeleteLicenseOverride_Unauthorized(repository);
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteLicenseOverride_Unauthorized_RepositoryContainer() throws Exception {
    testDeleteLicenseOverride_Unauthorized(RepositoryContainer.SINGLETON);
  }

  private void testDeleteLicenseOverride_Unauthenticated(final Owner owner) throws Exception {
    testDeleteLicenseOverride_Unauthenticated(owner, owner.getId());
  }

  private void testDeleteLicenseOverride_Unauthenticated(final Owner owner, final String ownerId) throws Exception {
    LicenseOverride override = tempEntity.newLicenseOverride(owner.getId(), COMPONENT_IDENTIFIER,
        LicenseOverrideStatus.CONFIRMED, (String) null);
    licenseOverrideService.deleteLicenseOverride(owner.getType(), ownerId, override.getId(), null, mockRequest);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteLicenseOverride_Unauthenticated_App() throws Exception {
    testDeleteLicenseOverride_Unauthenticated(app, app.getPublicId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteLicenseOverride_Unauthenticated_Org() throws Exception {
    testDeleteLicenseOverride_Unauthenticated(org);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteLicenseOverride_Unauthenticated_Repository() throws Exception {
    testDeleteLicenseOverride_Unauthenticated(repository);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteLicenseOverride_Unauthenticated_RepositoryContainer() throws Exception {
    testDeleteLicenseOverride_Unauthenticated(RepositoryContainer.SINGLETON);
  }

  private void testGetAppliedLicenseOverrides_Authorized(final Owner owner) throws Exception {
    testGetAppliedLicenseOverrides_Authorized(owner, owner.getId());
  }

  private void testGetAppliedLicenseOverrides_Authorized(final Owner owner, final String ownerId) {
    grantReadPermission(owner.getId());
    licenseOverrideService.getAppliedLicenseOverrides(owner.getType(), ownerId, COMPONENT_IDENTIFIER);
  }

  @Test
  public void testGetAppliedLicenseOverrides_Authorized_App() throws Exception {
    testGetAppliedLicenseOverrides_Authorized(app, app.getPublicId());
  }

  @Test
  public void testGetAppliedLicenseOverrides_Authorized_Org() throws Exception {
    testGetAppliedLicenseOverrides_Authorized(org);
  }

  @Test
  public void testGetAppliedLicenseOverrides_Authorized_Repository() throws Exception {
    testGetAppliedLicenseOverrides_Authorized(repository);
  }

  @Test
  public void testGetAppliedLicenseOverrides_Authorized_RepositoryContainer() throws Exception {
    testGetAppliedLicenseOverrides_Authorized(RepositoryContainer.SINGLETON);
  }

  private void testGetAppliedLicenseOverrides_Unauthorized(final Owner owner) throws Exception {
    testGetAppliedLicenseOverrides_Unauthorized(owner, owner.getId());
  }

  private void testGetAppliedLicenseOverrides_Unauthorized(final Owner owner, final String ownerId) {
    login();
    licenseOverrideService.getAppliedLicenseOverrides(owner.getType(), ownerId, COMPONENT_IDENTIFIER);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetAppliedLicenseOverrides_Unauthorized_App() throws Exception {
    testGetAppliedLicenseOverrides_Unauthorized(app, app.getPublicId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetAppliedLicenseOverrides_Unauthorized_Org() throws Exception {
    testGetAppliedLicenseOverrides_Unauthorized(org);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetAppliedLicenseOverrides_Unauthorized_Repository() throws Exception {
    testGetAppliedLicenseOverrides_Unauthorized(repository);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetAppliedLicenseOverrides_Unauthorized_RepositoryContainer() throws Exception {
    testGetAppliedLicenseOverrides_Unauthorized(RepositoryContainer.SINGLETON);
  }

  private void testGetAppliedLicenseOverrides_Unauthenticated(final Owner owner) throws Exception {
    testGetAppliedLicenseOverrides_Unauthenticated(owner, owner.getId());
  }

  private void testGetAppliedLicenseOverrides_Unauthenticated(final Owner owner, final String ownerId) {
    licenseOverrideService.getAppliedLicenseOverrides(owner.getType(), ownerId, COMPONENT_IDENTIFIER);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetAppliedLicenseOverrides_Unauthenticated_App() throws Exception {
    testGetAppliedLicenseOverrides_Unauthenticated(app, app.getPublicIdLowercase());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetAppliedLicenseOverrides_Unauthenticated_Org() throws Exception {
    testGetAppliedLicenseOverrides_Unauthenticated(org);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetAppliedLicenseOverrides_Unauthenticated_Repository() throws Exception {
    testGetAppliedLicenseOverrides_Unauthenticated(repository);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetAppliedLicenseOverrides_Unauthenticated_RepositoryContainer() throws Exception {
    testGetAppliedLicenseOverrides_Unauthenticated(RepositoryContainer.SINGLETON);
  }
}
