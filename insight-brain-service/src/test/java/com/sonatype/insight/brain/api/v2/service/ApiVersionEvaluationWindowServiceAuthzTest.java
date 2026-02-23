/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import com.sonatype.insight.brain.api.v2.dto.ApiVersionEvaluationWindowDTO;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import jakarta.inject.Inject;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class ApiVersionEvaluationWindowServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiVersionEvaluationWindowService service;

  @Test(expected = UnauthenticatedException.class)
  public void testGetVersionEvaluationWindows_Unauthenticated() {
    service.getVersionEvaluationWindows(org);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetVersionEvaluationWindows_Unauthorized() {
    login();

    service.getVersionEvaluationWindows(org);
  }

  @Test
  public void testGetVersionEvaluationWindows_Organization_Authorized() {
    grantReadPermission(org.getId());

    service.getVersionEvaluationWindows(org);
  }

  @Test
  public void testGetVersionEvaluationWindows_Application_Authorized() {
    grantReadPermission(app.getId());

    service.getVersionEvaluationWindows(app);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetVersionEvaluationWindow_Unauthenticated() {
    ApiVersionEvaluationWindowDTO dto = new ApiVersionEvaluationWindowDTO("context1", 10, 30);

    service.setVersionEvaluationWindow(org, dto);
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetVersionEvaluationWindow_Unauthorized() {
    login();
    ApiVersionEvaluationWindowDTO dto = new ApiVersionEvaluationWindowDTO("context1", 10, 30);

    service.setVersionEvaluationWindow(org, dto);
  }

  @Test
  public void testSetVersionEvaluationWindow_Organization_Authorized() {
    grantWritePermission(org.getId());
    ApiVersionEvaluationWindowDTO dto = new ApiVersionEvaluationWindowDTO("context1", 10, 30);

    service.setVersionEvaluationWindow(org, dto);
  }

  @Test
  public void testSetVersionEvaluationWindow_Application_Authorized() {
    grantWritePermission(app.getId());
    ApiVersionEvaluationWindowDTO dto = new ApiVersionEvaluationWindowDTO("context1", 10, 30);

    service.setVersionEvaluationWindow(app, dto);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteVersionEvaluationWindows_Unauthenticated() {
    service.deleteVersionEvaluationWindows(org, "context1");
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteVersionEvaluationWindows_Unauthorized() {
    login();

    service.deleteVersionEvaluationWindows(org, "context1");
  }

  @Test
  public void testDeleteVersionEvaluationWindows_Organization_Authorized() {
    grantWritePermission(org.getId());

    service.deleteVersionEvaluationWindows(org, "context1");
  }

  @Test
  public void testDeleteVersionEvaluationWindows_Application_Authorized() {
    grantWritePermission(app.getId());

    service.deleteVersionEvaluationWindows(app, "context1");
  }
}
