/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.List;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationMoveServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApplicationMoveService applicationMoveService;

  @Test(expected = UnauthenticatedException.class)
  public void testGetDestinationOrganizations_Unauthenticated() {
    applicationMoveService.getDestinationOrganizations(app.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetDestinationOrganizations_Unauthorized() {
    login();
    applicationMoveService.getDestinationOrganizations(app.getId());
  }

  @Test
  public void testGetDestinationOrganizations_Authorized() {
    grantWritePermission(app.getId());
    applicationMoveService.getDestinationOrganizations(app.getId());
  }

  @Test
  public void testGetDestinationOrganizations_OrganizationsFilteredByAuthorization() {
    tempEntity.newOrganization();
    Organization org2 = tempEntity.newOrganization();
    grantWritePermission(app.getId());

    List<Organization> orgs = applicationMoveService.getDestinationOrganizations(app.getId());
    assertThat(orgs).isEmpty();

    grantAddApplicationPermission(org2.getId());
    orgs = applicationMoveService.getDestinationOrganizations(app.getId());
    assertThat(orgs).extracting(Organization::getId).containsExactly(org2.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testMoveApplication_Unauthenticated() {
    applicationMoveService.moveApplication(app.getId(), app.getOrganizationId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testMoveApplication_Unauthorized_SourceApp() {
    Organization org = tempEntity.newOrganization();
    grantAddApplicationPermission(org.getId());
    applicationMoveService.moveApplication(app.getId(), org.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testMoveApplication_Unauthorized_DestinationOrg() {
    Organization org = tempEntity.newOrganization();
    grantWritePermission(app.getId());
    applicationMoveService.moveApplication(app.getId(), org.getId());
  }

  @Test
  public void testMoveApplication_Authorized() {
    Organization org = tempEntity.newOrganization();
    grantWritePermission(app.getId());
    grantAddApplicationPermission(org.getId());
    applicationMoveService.moveApplication(app.getId(), org.getId());
  }
}
