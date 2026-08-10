/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.List;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class ApplicationMoveServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private ApplicationMoveService applicationMoveService;

  @Test
  public void testGetDestinationOrganizations_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> applicationMoveService.getDestinationOrganizations(app.getId()));
  }

  @Test
  public void testGetDestinationOrganizations_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> applicationMoveService.getDestinationOrganizations(app.getId()));
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

  @Test
  public void testMoveApplication_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> applicationMoveService.moveApplication(app.getId(), app.getOrganizationId()));
  }

  @Test
  public void testMoveApplication_Unauthorized_SourceApp() {
    Organization org = tempEntity.newOrganization();
    grantAddApplicationPermission(org.getId());
    assertThrows(UnauthorizedException.class,
        () -> applicationMoveService.moveApplication(app.getId(), org.getId()));
  }

  @Test
  public void testMoveApplication_Unauthorized_DestinationOrg() {
    Organization org = tempEntity.newOrganization();
    grantWritePermission(app.getId());
    assertThrows(UnauthorizedException.class,
        () -> applicationMoveService.moveApplication(app.getId(), org.getId()));
  }

  @Test
  public void testMoveApplication_Authorized() {
    Organization org = tempEntity.newOrganization();
    grantWritePermission(app.getId());
    grantAddApplicationPermission(org.getId());
    applicationMoveService.moveApplication(app.getId(), org.getId());
  }
}
