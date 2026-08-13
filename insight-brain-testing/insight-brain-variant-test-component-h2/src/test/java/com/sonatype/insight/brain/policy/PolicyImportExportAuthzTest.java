/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ComponentH2Test
public class PolicyImportExportAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private PolicyImportExport policyImportExport;

  @Test
  public void testExportApplication_Unauthorized() {
    login();
    assertThatExceptionOfType(UnauthorizedException.class)
        .isThrownBy(() -> policyImportExport.exportApplication(app));
  }

  @Test
  public void testExportApplication_Authorized() {
    grantReadPermission(app.getId());
    policyImportExport.exportApplication(app);
  }

  @Test
  public void testExportOrganization_Unauthorized() {
    login();
    assertThatExceptionOfType(UnauthorizedException.class)
        .isThrownBy(() -> policyImportExport.exportOrganization(org));
  }

  @Test
  public void testExportOrganization_Authorized() {
    grantReadPermission(org.getId());
    policyImportExport.exportOrganization(org);
  }

  @Test
  public void testImportOrganization_Unauthorized() {
    login();
    assertThatExceptionOfType(UnauthorizedException.class)
        .isThrownBy(() -> policyImportExport.importOrganization(org, new PolicyExportResult()));
  }

  @Test
  public void testImportOrganization_Authorized() {
    grantWritePermission(org.getId());
    policyImportExport.importOrganization(org, new PolicyExportResult());
  }
}
