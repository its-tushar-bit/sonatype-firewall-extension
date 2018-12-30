/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ApiComponentLabelServiceV2AuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiComponentLabelServiceV2 apiComponentLabelService;

  @Test
  public void testSetApplicationComponentLabel_Authorized() {
    grantWritePermission(app.getId());
    Label label = tempEntity.newLabel(org.getId());
    apiComponentLabelService.setApplicationComponentLabel(app.getId(), "bababababa", label.getLabel());
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetApplicationComponentLabel_Unauthorized() {
    login();
    apiComponentLabelService.setApplicationComponentLabel(app.getId(), "bababababa", "label");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetApplicationComponentLabel_Unauthenticated() {
    apiComponentLabelService.setApplicationComponentLabel(app.getId(), "bababababa", "label");
  }

  @Test
  public void testSetApplicationComponentLabel_UnknownApplicationId() {
    login();
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      apiComponentLabelService.setApplicationComponentLabel("fakeappid", "bababababa", "label");
    }).withMessage("Could not find an application with ID fakeappid.");
  }

  @Test
  public void testDeleteApplicationComponentLabel_Authorized() {
    grantWritePermission(app.getId());
    Label label = tempEntity.newLabel(org.getId());
    apiComponentLabelService.deleteApplicationComponentLabel(app.getId(), "bababababa", label.getLabel());
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteApplicationComponentLabel_Unauthorized() {
    login();
    apiComponentLabelService.deleteApplicationComponentLabel(app.getId(), "bababababa", "label");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteApplicationComponentLabel_Unauthenticated() {
    apiComponentLabelService.deleteApplicationComponentLabel(app.getId(), "bababababa", "label");
  }

  @Test
  public void testDeleteApplicationComponentLabel_UnknownApplicationId() {
    login();
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      apiComponentLabelService.deleteApplicationComponentLabel("fakeappid", "bababababa", "label");
    }).withMessage("Could not find an application with ID fakeappid.");
  }
}
