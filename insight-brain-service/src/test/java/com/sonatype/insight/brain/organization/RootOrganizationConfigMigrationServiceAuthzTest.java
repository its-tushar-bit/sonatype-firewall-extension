/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.migration.RootOrganizationConfigMigrationUtils;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Before;
import org.junit.Test;

public class RootOrganizationConfigMigrationServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private RootOrganizationConfigMigrationService service;

  @Inject
  private MigrationTrackerDAO migrationTrackerDAO;

  @Before
  public void before() {
    migrationTrackerDAO.delete(migrationTrackerDAO.getById(RootOrganizationConfigMigrationUtils.MIGRATION_CONFIG_ID));
    migrationTrackerDAO.delete(migrationTrackerDAO.getById(RootOrganizationConfigMigrationUtils.MIGRATION_ID));
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetRootOrganizationTemplate_Unauthenticated() {
    service.setRootOrganizationTemplate(org.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetRootOrganizationTemplate_Unauthorized() {
    login();
    service.setRootOrganizationTemplate(org.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetRootOrganizationTemplate_OrgPerm() {
    grantWritePermission(org.getId());
    service.setRootOrganizationTemplate(org.getId());
  }

  @Test
  public void testSetRootOrganizationTemplate() {
    grantWritePermission();
    service.setRootOrganizationTemplate(org.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetRootOrganizationEmptyTemplate_Unauthenticated() {
    service.setRootOrganizationEmptyTemplate();
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetRootOrganizationEmptyTemplate_Unauthorized() {
    login();
    service.setRootOrganizationEmptyTemplate();
  }

  @Test
  public void testSetRootOrganizationEmptyTemplate() {
    grantWritePermission();
    service.setRootOrganizationEmptyTemplate();
  }
}
