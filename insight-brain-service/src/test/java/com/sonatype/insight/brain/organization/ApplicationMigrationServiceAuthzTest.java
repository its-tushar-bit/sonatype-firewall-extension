/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

public class ApplicationMigrationServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApplicationMigrationService applicationMigrationService;

  @Test(expected = UnauthenticatedException.class)
  public void testGetDestinationOrganizations_Unauthenticated() {
    applicationMigrationService.getDestinationOrganizations(app.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetDestinationOrganizations_Unauthorized() {
    login();
    applicationMigrationService.getDestinationOrganizations(app.getId());
  }

  @Test
  public void testGetDestinationOrganizations_Authorized() {
    grantWritePermission(app.getId());
    applicationMigrationService.getDestinationOrganizations(app.getId());
  }

  @Test
  public void testGetDestinationOrganizations_OrganizationsFilteredByAuthorization() {
    tempEntity.newOrganization();
    Organization org2 = tempEntity.newOrganization();
    grantWritePermission(app.getId());

    List<Organization> orgs = applicationMigrationService.getDestinationOrganizations(app.getId());
    assertThat(orgs, hasSize(0));

    grantAddApplicationPermission(org2.getId());
    orgs = applicationMigrationService.getDestinationOrganizations(app.getId());
    assertThat(orgs, hasSize(1));
    assertThat(orgs.get(0).getId(), is(org2.getId()));
  }
}
