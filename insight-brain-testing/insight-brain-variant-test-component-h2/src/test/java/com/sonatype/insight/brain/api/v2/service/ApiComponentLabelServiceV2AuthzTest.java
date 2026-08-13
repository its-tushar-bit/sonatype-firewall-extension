/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

@ComponentH2Test
public class ApiComponentLabelServiceV2AuthzTest
    extends AbstractComponentH2AuthzTest
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

  @Test
  public void testSetComponentLabel_Application_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiComponentLabelService.setComponentLabel(OwnerType.APPLICATION, app.getId(), "bababababa", "label"));
  }

  @Test
  public void testSetComponentLabel_Application_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiComponentLabelService.setComponentLabel(OwnerType.APPLICATION, app.getId(), "bababababa", "label"));
  }

  @Test
  public void testSetComponentLabel_Organization_Authorized() {
    grantWritePermission(org.getId());
    Label label = tempEntity.newLabel(org.getId());
    apiComponentLabelService.setComponentLabel(OwnerType.ORGANIZATION, org.getId(), "bababababa", label.getLabel());
  }

  @Test
  public void testSetComponentLabel_Organization_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiComponentLabelService.setComponentLabel(OwnerType.ORGANIZATION, org.getId(), "bababababa", "label"));
  }

  @Test
  public void testSetComponentLabel_Organization_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiComponentLabelService.setComponentLabel(OwnerType.ORGANIZATION, org.getId(), "bababababa", "label"));
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

  @Test
  public void testDeleteComponentLabel_Application_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiComponentLabelService.deleteComponentLabel(OwnerType.APPLICATION, app.getId(), "bababababa", "label"));
  }

  @Test
  public void testDeleteComponentLabel_Application_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiComponentLabelService.deleteComponentLabel(OwnerType.APPLICATION, app.getId(), "bababababa", "label"));
  }

  @Test
  public void testDeleteComponentLabel_Organization_Authorized() {
    grantWritePermission(org.getId());
    Label label = tempEntity.newLabel(org.getId());
    // Must create a ComponentLabel before we can delete it
    tempEntity.newComponentLabel(org.getId(), label.getId(), "bababababa");
    apiComponentLabelService.deleteComponentLabel(OwnerType.ORGANIZATION, org.getId(), "bababababa", label.getLabel());
  }

  @Test
  public void testDeleteComponentLabel_Organization_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiComponentLabelService.deleteComponentLabel(OwnerType.ORGANIZATION, org.getId(), "bababababa",
            "label"));
  }

  @Test
  public void testDeleteComponentLabel_Organization_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiComponentLabelService.deleteComponentLabel(OwnerType.ORGANIZATION, org.getId(), "bababababa",
            "label"));
  }
}
