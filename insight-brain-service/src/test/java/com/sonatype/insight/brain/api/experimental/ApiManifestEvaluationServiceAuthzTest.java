/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.io.IOException;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ApiManifestEvaluationServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  public ApiManifestEvaluationService apiManifestEvaluationService;

  @Test
  public void testPerformManifestScan_Authorized() throws Exception {
    grantEvaluateApplicationPermission(app.getId());

    assertThatThrownBy(() ->
        apiManifestEvaluationService.performManifestScan(app.getId(), "stage", "a-branch", "useragent")
    ).isInstanceOf(IOException.class)
        .hasMessage("No SCM configuration defined for this application");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testPerformManifestScan_Unauthenticated() throws Exception {
    apiManifestEvaluationService.performManifestScan(app.getId(), "stage", "a-branch", "useragent");
  }

  @Test(expected = UnauthorizedException.class)
  public void testPerformManifestScan_Unauthorized() throws Exception {
    login();
    apiManifestEvaluationService.performManifestScan(app.getId(), "stage", "a-branch", "useragent");
  }
}
