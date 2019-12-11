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

import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ApiThirdPartyScanServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiThirdPartyScanService apiThirdPartyEvaluationService;

  @Test
  public void testEvaluateComponents_Authorized() throws Exception {
    String bom = getBomFile("/ApiThirdPartyEvaluationServiceAuthzTest/valid_sbom.xml");

    grantReadPermission(app.getId());
    apiThirdPartyEvaluationService.scanComponents(app.getId(), "clair", "build", bom, null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testEvaluateComponents_Unauthenticated() {
    apiThirdPartyEvaluationService.scanComponents(app.getId(), "clair", "build", "", null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testEvaluateComponents_UnauthorizedButAuthenticated() {
    login();
    apiThirdPartyEvaluationService.scanComponents(app.getId(), "clair", "build", "", null);
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

  private String getBomFile(String path) throws Exception {
    byte[] bytes = Files.readAllBytes(Paths.get(getClass().getResource(path).toURI()));
    return new String(bytes, StandardCharsets.UTF_8);
  }
}
