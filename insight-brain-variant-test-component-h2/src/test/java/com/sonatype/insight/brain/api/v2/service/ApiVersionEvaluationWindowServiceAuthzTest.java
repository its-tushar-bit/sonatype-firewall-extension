/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import com.sonatype.insight.brain.api.v2.dto.ApiVersionEvaluationWindowDTO;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import jakarta.inject.Inject;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ComponentH2Test
public class ApiVersionEvaluationWindowServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private ApiVersionEvaluationWindowService service;

  @Test
  public void testGetVersionEvaluationWindows_Unauthenticated() {
    assertThatExceptionOfType(UnauthenticatedException.class)
        .isThrownBy(() -> service.getVersionEvaluationWindows(org));
  }

  @Test
  public void testGetVersionEvaluationWindows_Unauthorized() {
    login();

    assertThatExceptionOfType(UnauthorizedException.class)
        .isThrownBy(() -> service.getVersionEvaluationWindows(org));
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

  @Test
  public void testSetVersionEvaluationWindow_Unauthenticated() {
    ApiVersionEvaluationWindowDTO dto = new ApiVersionEvaluationWindowDTO("context1", 10, 30);

    assertThatExceptionOfType(UnauthenticatedException.class)
        .isThrownBy(() -> service.setVersionEvaluationWindow(org, dto));
  }

  @Test
  public void testSetVersionEvaluationWindow_Unauthorized() {
    login();
    ApiVersionEvaluationWindowDTO dto = new ApiVersionEvaluationWindowDTO("context1", 10, 30);

    assertThatExceptionOfType(UnauthorizedException.class)
        .isThrownBy(() -> service.setVersionEvaluationWindow(org, dto));
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

  @Test
  public void testDeleteVersionEvaluationWindows_Unauthenticated() {
    assertThatExceptionOfType(UnauthenticatedException.class)
        .isThrownBy(() -> service.deleteVersionEvaluationWindows(org, "context1"));
  }

  @Test
  public void testDeleteVersionEvaluationWindows_Unauthorized() {
    login();

    assertThatExceptionOfType(UnauthorizedException.class)
        .isThrownBy(() -> service.deleteVersionEvaluationWindows(org, "context1"));
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
