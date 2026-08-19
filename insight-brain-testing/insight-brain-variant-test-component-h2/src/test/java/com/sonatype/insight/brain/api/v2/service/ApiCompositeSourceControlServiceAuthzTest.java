/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiCompositeSourceControlDTO;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @since 1.78
 */
@ComponentH2Test
public class ApiCompositeSourceControlServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  private static final String VALID_URL = "https://example.com/organization/project";

  @BeforeEach
  public void before() {
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
  }

  @Inject
  public ApiCompositeSourceControlService sourceControlService;

  @Inject
  private TestProductLicense testProductLicense;

  @Test
  public void testGetCompositeSourceControlByOwner_AuthorizedApp() {
    grantReadPermission(app.getId());
    SourceControl sourceControl = tempEntity.newSourceControl(
        app.getId(), VALID_URL, "token", null);
    ApiCompositeSourceControlDTO sourceControlByApplicationId =
        sourceControlService.getCompositeSourceControlByOwner(
            OwnerType.APPLICATION, app.getId());
    assertThat(sourceControlByApplicationId.id).isEqualTo(sourceControl.getId());
  }

  @Test
  public void testGetCompositeSourceControlByOwner_AuthorizedOrg() {
    grantReadPermission(org.getId());
    SourceControl sourceControl = tempEntity.newSourceControl(
        org.getId(), null, null);
    ApiCompositeSourceControlDTO sourceControlByOrgId =
        sourceControlService.getCompositeSourceControlByOwner(
            OwnerType.ORGANIZATION, org.getId());
    assertThat(sourceControlByOrgId.id).isEqualTo(sourceControl.getId());
  }

  @Test
  public void testGetCompositeSourceControlByOwner_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> sourceControlService.getCompositeSourceControlByOwner(OwnerType.APPLICATION, app.getId()));
  }

  @Test
  public void testGetCompositeSourceControlByOwner_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> sourceControlService.getCompositeSourceControlByOwner(OwnerType.APPLICATION, app.getId()));
  }

  @Test
  public void testGetCompositeSourceControlByOwner_InvalidLicense() {
    testProductLicense.setMissingFeatures(LicensedFeature.AUTOMATION, LicensedFeature.NOTIFICATIONS);
    grantReadPermission(app.getId());
    assertThrows(InvalidLicenseException.class,
        () -> sourceControlService.getCompositeSourceControlByOwner(OwnerType.APPLICATION, app.getId()));
  }
}
