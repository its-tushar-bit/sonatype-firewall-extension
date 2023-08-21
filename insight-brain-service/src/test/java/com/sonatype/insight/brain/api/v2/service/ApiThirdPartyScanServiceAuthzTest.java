/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.scan.file.ThirdPartyUtils.SbomFormat;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ApiThirdPartyScanServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  private static final long SINCE_UTC_TIMESTAMP = 1621220400000L;

  @Inject
  private ApiThirdPartyScanService apiThirdPartyEvaluationService;

  @Test
  public void testEvaluateComponents_Authorized() throws Exception {
    String bom = getBomFile("/ApiThirdPartyEvaluationServiceAuthzTest/valid_sbom.xml");

    grantEvaluateApplicationPermission(app.getId());
    apiThirdPartyEvaluationService.scanComponents(app.getId(), "clair", "build", bom, null, SbomFormat.XML);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testEvaluateComponents_Unauthenticated() {
    apiThirdPartyEvaluationService.scanComponents(app.getId(), "clair", "build", "", null, SbomFormat.XML);
  }

  @Test(expected = UnauthorizedException.class)
  public void testEvaluateComponents_Unauthorized() {
    login();
    apiThirdPartyEvaluationService.scanComponents(app.getId(), "clair", "build", "", null, SbomFormat.XML);
  }
  
  @Test(expected = UnauthenticatedException.class)
  public void testGetScanStatus_Unauthenticated() {
    apiThirdPartyEvaluationService.getScanStatus(app.getId(), "scanRequestId");
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetScanStatus_Unauthorized() {
    login();
    apiThirdPartyEvaluationService.getScanStatus(app.getId(), "scanRequestId");
  }

  @Test
  public void testGetScanStatus_Authorized() {
    grantEvaluateApplicationPermission(app.getId());
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> apiThirdPartyEvaluationService.getScanStatus(app.getId(), "scanRequestId"))
        .withMessage("Policy evaluation status with id %s for public application id %s was not found.",
            "scanRequestId", app.getPublicId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetIdeUsersOverview_Unauthenticated() {
    apiThirdPartyEvaluationService.getIdeUsersOverview(SINCE_UTC_TIMESTAMP);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetIdeUsersOverview_Unauthorized() {
    login();
    apiThirdPartyEvaluationService.getIdeUsersOverview(SINCE_UTC_TIMESTAMP);
  }

  @Test
  public void testGetIdeUsersOverview_Authorized() {
    grantReadPermission(Organization.ROOT_ORGANIZATION_ID);
    apiThirdPartyEvaluationService.getIdeUsersOverview(SINCE_UTC_TIMESTAMP);
  }

  private String getBomFile(String path) throws Exception {
    byte[] bytes = Files.readAllBytes(Paths.get(getClass().getResource(path).toURI()));
    return new String(bytes, StandardCharsets.UTF_8);
  }
}
