/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class ApiComponentLabelServiceV2AuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiComponentLabelServiceV2 apiComponentLabelService;

  @Test
  public void testSetComponentLabel_Application_Authorized() {
    grantWritePermission(app.getId());
    Label label = tempEntity.newLabel(org.getId());
    apiComponentLabelService.setComponentLabel(OwnerType.APPLICATION, app.getId(), "bababababa",
        label.getLabel());
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetComponentLabel_Application_Unauthorized() {
    login();
    apiComponentLabelService.setComponentLabel(OwnerType.APPLICATION, app.getId(), "bababababa", "label");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetComponentLabel_Application_Unauthenticated() {
    apiComponentLabelService.setComponentLabel(OwnerType.APPLICATION, app.getId(), "bababababa", "label");
  }

  @Test
  public void testSetComponentLabel_Organization_Authorized() {
    grantWritePermission(org.getId());
    Label label = tempEntity.newLabel(org.getId());
    apiComponentLabelService.setComponentLabel(OwnerType.ORGANIZATION, org.getId(), "bababababa", label.getLabel());
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetComponentLabel_Organization_Unauthorized() {
    login();
    apiComponentLabelService.setComponentLabel(OwnerType.ORGANIZATION, org.getId(), "bababababa", "label");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetComponentLabel_Organization_Unauthenticated() {
    apiComponentLabelService.setComponentLabel(OwnerType.ORGANIZATION, org.getId(), "bababababa", "label");
  }

  @Test
  public void testDeleteComponentLabel_Application_Authorized() {
    grantWritePermission(app.getId());
    Label label = tempEntity.newLabel(org.getId());
    // Must create a ComponentLabel before we can delete it
    tempEntity.newComponentLabel(app.getId(), label.getId(), "bababababa");
    apiComponentLabelService.deleteComponentLabel(OwnerType.APPLICATION, app.getId(), "bababababa",
        label.getLabel());
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteComponentLabel_Application_Unauthorized() {
    login();
    apiComponentLabelService.deleteComponentLabel(OwnerType.APPLICATION, app.getId(), "bababababa", "label");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteComponentLabel_Application_Unauthenticated() {
    apiComponentLabelService.deleteComponentLabel(OwnerType.APPLICATION, app.getId(), "bababababa", "label");
  }

  @Test
  public void testDeleteComponentLabel_Organization_Authorized() {
    grantWritePermission(org.getId());
    Label label = tempEntity.newLabel(org.getId());
    // Must create a ComponentLabel before we can delete it
    tempEntity.newComponentLabel(org.getId(), label.getId(), "bababababa");
    apiComponentLabelService.deleteComponentLabel(OwnerType.ORGANIZATION, org.getId(), "bababababa", label.getLabel());
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteComponentLabel_Organization_Unauthorized() {
    login();
    apiComponentLabelService.deleteComponentLabel(OwnerType.ORGANIZATION, org.getId(), "bababababa", "label");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteComponentLabel_Organization_Unauthenticated() {
    apiComponentLabelService.deleteComponentLabel(OwnerType.ORGANIZATION, org.getId(), "bababababa", "label");
  }
}
