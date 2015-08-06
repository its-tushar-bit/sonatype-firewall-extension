/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.label;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.brain.utils.IdUtils;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class ComponentLabelServiceAuthzTest
    extends AbstractServiceAuthzTest
{

  @Inject
  private ComponentLabelService componentLabelService;

  @Test
  public void testGetComponentLabelsForApplication_Authorized() {
    grantReadPermission(app.getId());
    componentLabelService.getComponentLabels(IdUtils.TYPE_APPLICATION, app.getPublicId(), "bad");
  }

  @Test
  public void testGetComponentLabelsForOrganization_Authorized() {
    grantReadPermission(org.getId());
    componentLabelService.getComponentLabels(IdUtils.TYPE_ORGANIZATION, org.getId(), "bad");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetComponentLabelsForApplication_Unauthenticated() {
    componentLabelService.getComponentLabels(IdUtils.TYPE_APPLICATION, app.getPublicId(), "bad");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetComponentLabelsForOrganization_Unauthenticated() {
    componentLabelService.getComponentLabels(IdUtils.TYPE_ORGANIZATION, org.getId(), "bad");
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetComponentLabelsForApplication_Unauthorized() {
    login();
    componentLabelService.getComponentLabels(IdUtils.TYPE_APPLICATION, app.getPublicId(), "bad");
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetComponentLabelsForOrganization_Unauthorized() {
    login();
    componentLabelService.getComponentLabels(IdUtils.TYPE_ORGANIZATION, org.getId(), "bad");
  }

  @Test
  public void testSetComponentLabelForApplication_Authorized() {
    grantWritePermission(app.getId());

    Label label = tempEntity.newLabel(org.getId());
    componentLabelService.setComponentLabel(IdUtils.TYPE_APPLICATION, app.getPublicId(), "bad", label);
  }

  @Test
  public void testSetComponentLabelForOrganization_Authorized() {
    grantWritePermission(org.getId());

    Label label = tempEntity.newLabel(org.getId());
    componentLabelService.setComponentLabel(IdUtils.TYPE_ORGANIZATION, org.getId(), "bad", label);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetComponentLabelForApplication_Unauthenticated() {
    Label label = tempEntity.newLabel(org.getId());
    componentLabelService.setComponentLabel(IdUtils.TYPE_APPLICATION, app.getPublicId(), "bad", label);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetComponentLabelForOrganization_Unauthenticated() {
    Label label = tempEntity.newLabel(org.getId());
    componentLabelService.setComponentLabel(IdUtils.TYPE_ORGANIZATION, org.getId(), "bad", label);
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetComponentLabelForApplication_Unauthorized() {
    grantReadPermission(app.getId());

    Label label = tempEntity.newLabel(org.getId());
    componentLabelService.setComponentLabel(IdUtils.TYPE_APPLICATION, app.getPublicId(), "bad", label);
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetComponentLabelForOrganization_Unauthorized() {
    grantReadPermission(org.getId());

    Label label = tempEntity.newLabel(org.getId());
    componentLabelService.setComponentLabel(IdUtils.TYPE_ORGANIZATION, org.getId(), "bad", label);
  }

  @Test
  public void testRemoveComponentLabelForApplication_Authorized() {
    grantWritePermission(app.getId());

    Label label = tempEntity.newLabel(org.getId());
    tempEntity.newComponentLabel(app.getId(), label.getId(), "bad");
    componentLabelService.deleteComponentLabel(IdUtils.TYPE_APPLICATION, app.getPublicId(), "bad", label.getId());
  }

  @Test
  public void testRemoveComponentLabelForOrganization_Authorized() {
    grantWritePermission(org.getId());

    Label label = tempEntity.newLabel(org.getId());
    tempEntity.newComponentLabel(org.getId(), label.getId(), "bad");
    componentLabelService.deleteComponentLabel(IdUtils.TYPE_ORGANIZATION, org.getId(), "bad", label.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testRemoveComponentLabelForApplication_Unauthenticated() {
    Label label = tempEntity.newLabel(org.getId());
    tempEntity.newComponentLabel(app.getId(), label.getId(), "bad");
    componentLabelService.deleteComponentLabel(IdUtils.TYPE_APPLICATION, app.getPublicId(), "bad", label.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testRemoveComponentLabelForOrganization_Unauthenticated() {
    Label label = tempEntity.newLabel(org.getId());
    tempEntity.newComponentLabel(org.getId(), label.getId(), "bad");
    componentLabelService.deleteComponentLabel(IdUtils.TYPE_ORGANIZATION, org.getId(), "bad", label.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testRemoveComponentLabelForApplication_Unauthorized() {
    grantReadPermission(app.getId());

    Label label = tempEntity.newLabel(org.getId());
    tempEntity.newComponentLabel(app.getId(), label.getId(), "bad");
    componentLabelService.deleteComponentLabel(IdUtils.TYPE_APPLICATION, app.getPublicId(), "bad", label.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testRemoveComponentLabelForOrganization_Unauthorized() {
    grantReadPermission(org.getId());

    Label label = tempEntity.newLabel(org.getId());
    tempEntity.newComponentLabel(org.getId(), label.getId(), "bad");
    componentLabelService.deleteComponentLabel(IdUtils.TYPE_ORGANIZATION, org.getId(), "bad", label.getId());
  }
}
