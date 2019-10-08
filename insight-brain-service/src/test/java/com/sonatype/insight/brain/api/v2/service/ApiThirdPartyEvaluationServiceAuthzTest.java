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

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class ApiThirdPartyEvaluationServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiThirdPartyEvaluationService apiThirdPartyEvaluationService;

  @Test
  public void testEvaluateComponents_Authorized() throws Exception {
    byte[] bytes = Files.readAllBytes(
        Paths.get(getClass().getResource("/ApiThirdPartyEvaluationServiceAuthzTest/valid_sbom.xml").toURI()));
    String sbom = new String(bytes, StandardCharsets.UTF_8);

    grantReadPermission(app.getId());
    apiThirdPartyEvaluationService.scanComponents(app.getId(), "clair", "Build", sbom);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testEvaluateComponents_Unauthenticated() {
    apiThirdPartyEvaluationService.scanComponents(app.getId(), "clair", "Build", "");
  }

  @Test(expected = UnauthorizedException.class)
  public void testEvaluateComponents_UnauthorizedButAuthenticated() {
    login();
    apiThirdPartyEvaluationService.scanComponents(app.getId(), "clair", "Build", "");
  }
}
