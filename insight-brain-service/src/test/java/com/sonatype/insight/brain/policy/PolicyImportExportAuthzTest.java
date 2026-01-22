/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class PolicyImportExportAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private PolicyImportExport policyImportExport;

  @Test(expected = UnauthorizedException.class)
  public void testExportApplication_Unauthorized() {
    login();
    policyImportExport.exportApplication(app);
  }

  @Test
  public void testExportApplication_Authorized() {
    grantReadPermission(app.getId());
    policyImportExport.exportApplication(app);
  }

  @Test(expected = UnauthorizedException.class)
  public void testExportOrganization_Unauthorized() {
    login();
    policyImportExport.exportOrganization(org);
  }

  @Test
  public void testExportOrganization_Authorized() {
    grantReadPermission(org.getId());
    policyImportExport.exportOrganization(org);
  }

  @Test(expected = UnauthorizedException.class)
  public void testImportOrganization_Unauthorized() {
    login();
    policyImportExport.importOrganization(org, new PolicyExportResult());
  }

  @Test
  public void testImportOrganization_Authorized() {
    grantWritePermission(org.getId());
    policyImportExport.importOrganization(org, new PolicyExportResult());
  }
}
