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

public class LabelServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private LabelService labelService;

  @Test
  public void testGetLabelsForApplication_Authorized() {
    grantReadPermission(app.getId());
    labelService.getLabels(IdUtils.TYPE_APPLICATION, app.getPublicId(), false);
  }

  @Test
  public void testGetLabelsForOrganization_Authorized() {
    grantReadPermission(org.getId());
    labelService.getLabels(IdUtils.TYPE_ORGANIZATION, org.getId(), false);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetLabelsForApplication_Unauthenticated() {
    labelService.getLabels(IdUtils.TYPE_APPLICATION, app.getPublicId(), false);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetLabelsForOrganization_Unauthenticated() {
    labelService.getLabels(IdUtils.TYPE_ORGANIZATION, org.getId(), false);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetLabelsForApplication_Unauthorized() {
    login();
    labelService.getLabels(IdUtils.TYPE_APPLICATION, app.getPublicId(), false);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetLabelsForOrganization_Unauthorized() {
    login();
    labelService.getLabels(IdUtils.TYPE_ORGANIZATION, org.getId(), false);
  }

  @Test
  public void testGetApplicableLabelsForApplication_Authorized() {
    grantReadPermission(app.getId());
    labelService.getApplicableLabels(IdUtils.TYPE_APPLICATION, app.getPublicId());
  }

  @Test
  public void testGetApplicableLabelsForOrganization_Authorized() {
    grantReadPermission(org.getId());
    labelService.getApplicableLabels(IdUtils.TYPE_ORGANIZATION, org.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetApplicableLabelsForApplication_Unauthenticated() {
    labelService.getApplicableLabels(IdUtils.TYPE_APPLICATION, app.getPublicId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetApplicableLabelsForOrganization_Unauthenticated() {
    labelService.getApplicableLabels(IdUtils.TYPE_ORGANIZATION, org.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetApplicableLabelsForApplication_Unauthorized() {
    login();
    labelService.getApplicableLabels(IdUtils.TYPE_APPLICATION, app.getPublicId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetApplicableLabelsForOrganization_Unauthorized() {
    login();
    labelService.getApplicableLabels(IdUtils.TYPE_ORGANIZATION, org.getId());
  }

  @Test
  public void testGetApplicableContextsForApplication_Authorized() {
    grantWritePermission(app.getId());
    labelService.getApplicableContexts(app.getPublicId(), tempEntity.newLabel(app.getId()).getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetApplicableContextsForApplication_Unauthenticated() {
    labelService.getApplicableContexts(app.getPublicId(), tempEntity.newLabel(app.getId()).getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetApplicableContextsForApplication_Unauthorized() {
    login();
    labelService.getApplicableContexts(app.getPublicId(), tempEntity.newLabel(app.getId()).getId());
  }

  @Test
  public void testAddLabelForApplication_Authorized() {
    grantWritePermission(app.getId());
    labelService.addLabel(IdUtils.TYPE_APPLICATION, app.getPublicId(), new Label(null, "testing"));
  }

  @Test
  public void testAddLabelForOrganization_Authorized() {
    grantWritePermission(org.getId());
    labelService.addLabel(IdUtils.TYPE_ORGANIZATION, org.getId(), new Label(null, "testing"));
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddLabelForApplication_Unauthenticated() {
    labelService.addLabel(IdUtils.TYPE_APPLICATION, app.getPublicId(), new Label(null, "testing"));
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddLabelForOrganization_Unauthenticated() {
    labelService.addLabel(IdUtils.TYPE_ORGANIZATION, org.getId(), new Label(null, "testing"));
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddLabelForApplication_Unauthorized() {
    grantReadPermission(app.getId());
    labelService.addLabel(IdUtils.TYPE_APPLICATION, app.getPublicId(), new Label(null, "testing"));
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddLabelForOrganization_Unauthorized() {
    grantReadPermission(org.getId());
    labelService.addLabel(IdUtils.TYPE_ORGANIZATION, org.getId(), new Label(null, "testing"));
  }

  @Test
  public void testUpdateLabelForApplication_Authorized() {
    grantWritePermission(app.getId());
    labelService.updateLabel(IdUtils.TYPE_APPLICATION, app.getPublicId(), tempEntity.newLabel(app.getId()));
  }

  @Test
  public void testUpdateLabelForOrganization_Authorized() {
    grantWritePermission(org.getId());
    labelService.updateLabel(IdUtils.TYPE_ORGANIZATION, org.getId(), tempEntity.newLabel(org.getId()));
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdateLabelForApplication_Unauthenticated() {
    labelService.updateLabel(IdUtils.TYPE_APPLICATION, app.getPublicId(), tempEntity.newLabel(app.getId()));
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdateLabelForOrganization_Unauthenticated() {
    labelService.updateLabel(IdUtils.TYPE_ORGANIZATION, org.getId(), tempEntity.newLabel(org.getId()));
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpdateLabelForApplication_Unauthorized() {
    grantReadPermission(app.getId());
    labelService.updateLabel(IdUtils.TYPE_APPLICATION, app.getPublicId(), tempEntity.newLabel(app.getId()));
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpdateLabelForOrganization_Unauthorized() {
    grantReadPermission(org.getId());
    labelService.updateLabel(IdUtils.TYPE_ORGANIZATION, org.getId(), tempEntity.newLabel(org.getId()));
  }

  @Test
  public void testDeleteLabelForApplication_Authorized() {
    grantWritePermission(app.getId());
    labelService.deleteLabel(IdUtils.TYPE_APPLICATION, app.getPublicId(), tempEntity.newLabel(app.getId()).getId());
  }

  @Test
  public void testDeleteLabelForOrganization_Authorized() {
    grantWritePermission(org.getId());
    labelService.deleteLabel(IdUtils.TYPE_ORGANIZATION, org.getId(), tempEntity.newLabel(org.getId()).getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteLabelForApplication_Unauthenticated() {
    labelService.deleteLabel(IdUtils.TYPE_APPLICATION, app.getPublicId(), tempEntity.newLabel(app.getId()).getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteLabelForOrganization_Unauthenticated() {
    labelService.deleteLabel(IdUtils.TYPE_ORGANIZATION, org.getId(), tempEntity.newLabel(org.getId()).getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteLabelForApplication_Unauthorized() {
    grantReadPermission(app.getId());
    labelService.deleteLabel(IdUtils.TYPE_APPLICATION, app.getPublicId(), tempEntity.newLabel(app.getId()).getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteLabelForOrganization_Unauthorized() {
    grantReadPermission(org.getId());
    labelService.deleteLabel(IdUtils.TYPE_ORGANIZATION, org.getId(), tempEntity.newLabel(org.getId()).getId());
  }
}
