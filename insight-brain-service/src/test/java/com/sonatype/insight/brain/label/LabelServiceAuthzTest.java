/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.label;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiLabelDTO;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

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
    labelService.getLabels(OwnerType.APPLICATION, app.getId(), false);
  }

  @Test
  public void testGetLabelsForApplication_Authorized_PublicId() {
    grantReadPermission(app.getId());
    labelService.getLabels(OwnerType.APPLICATION, app.getPublicId(), false);
  }

  @Test
  public void testGetLabelsForOrganization_Authorized() {
    grantReadPermission(org.getId());
    labelService.getLabels(OwnerType.ORGANIZATION, org.getId(), false);
  }

  @Test
  public void testGetLabelsForRepository_Authorized() {
    grantReadPermission(repository.getId());
    labelService.getLabels(OwnerType.REPOSITORY, repository.getId(), false);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetLabelsForApplication_Unauthenticated() {
    labelService.getLabels(OwnerType.APPLICATION, app.getId(), false);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetLabelsForApplication_Unauthenticated_PublicId() {
    labelService.getLabels(OwnerType.APPLICATION, app.getPublicId(), false);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetLabelsForOrganization_Unauthenticated() {
    labelService.getLabels(OwnerType.ORGANIZATION, org.getId(), false);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetLabelsForRepository_Unauthenticated() {
    labelService.getLabels(OwnerType.REPOSITORY, repository.getId(), false);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetLabelsForApplication_Unauthorized() {
    login();
    labelService.getLabels(OwnerType.APPLICATION, app.getId(), false);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetLabelsForApplication_Unauthorized_PublicId() {
    login();
    labelService.getLabels(OwnerType.APPLICATION, app.getPublicId(), false);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetLabelsForOrganization_Unauthorized() {
    login();
    labelService.getLabels(OwnerType.ORGANIZATION, org.getId(), false);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetLabelsForRepository_Unauthorized() {
    login();
    labelService.getLabels(OwnerType.REPOSITORY, repository.getId(), false);
  }

  @Test
  public void testGetApplicableLabelsForApplication_Authorized() {
    grantReadPermission(app.getId());
    labelService.getApplicableLabels(OwnerType.APPLICATION, app.getId());
  }

  @Test
  public void testGetApplicableLabelsForApplication_Authorized_PublicId() {
    grantReadPermission(app.getId());
    labelService.getApplicableLabels(OwnerType.APPLICATION, app.getPublicId());
  }

  @Test
  public void testGetApplicableLabelsForOrganization_Authorized() {
    grantReadPermission(org.getId());
    labelService.getApplicableLabels(OwnerType.ORGANIZATION, org.getId());
  }

  @Test
  public void testGetApplicableLabelsForRepository_Authorized() {
    grantReadPermission(repository.getId());
    labelService.getApplicableLabels(OwnerType.REPOSITORY, repository.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetApplicableLabelsForApplication_Unauthenticated() {
    labelService.getApplicableLabels(OwnerType.APPLICATION, app.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetApplicableLabelsForApplication_Unauthenticated_PublicId() {
    labelService.getApplicableLabels(OwnerType.APPLICATION, app.getPublicId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetApplicableLabelsForOrganization_Unauthenticated() {
    labelService.getApplicableLabels(OwnerType.ORGANIZATION, org.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetApplicableLabelsForRepository_Unauthenticated() {
    labelService.getApplicableLabels(OwnerType.REPOSITORY, repository.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetApplicableLabelsForApplication_Unauthorized() {
    login();
    labelService.getApplicableLabels(OwnerType.APPLICATION, app.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetApplicableLabelsForApplication_Unauthorized_PublicId() {
    login();
    labelService.getApplicableLabels(OwnerType.APPLICATION, app.getPublicId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetApplicableLabelsForOrganization_Unauthorized() {
    login();
    labelService.getApplicableLabels(OwnerType.ORGANIZATION, org.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetApplicableLabelsForRepository_Unauthorized() {
    login();
    labelService.getApplicableLabels(OwnerType.REPOSITORY, repository.getId());
  }

  @Test
  public void testGetApplicableContextsForApplication_Authorized() {
    grantWritePermission(app.getId());
    labelService.getApplicableContexts(OwnerType.APPLICATION, app.getId(), tempEntity.newLabel(app.getId())
        .getId());
  }

  @Test
  public void testGetApplicableContextsForApplication_Authorized_PublicId() {
    grantWritePermission(app.getId());
    labelService.getApplicableContexts(OwnerType.APPLICATION, app.getPublicId(), tempEntity.newLabel(app.getId())
        .getId());
  }

  @Test
  public void testGetApplicableContextsForRepository_Authorized() {
    grantWritePermission(repository.getId());
    labelService.getApplicableContexts(OwnerType.REPOSITORY, repository.getId(), tempEntity.newLabel(app.getId())
        .getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetApplicableContextsForApplication_Unauthenticated() {
    labelService.getApplicableContexts(OwnerType.APPLICATION, app.getId(), tempEntity.newLabel(app.getId())
        .getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetApplicableContextsForApplication_Unauthenticated_PublicId() {
    labelService.getApplicableContexts(OwnerType.APPLICATION, app.getPublicId(), tempEntity.newLabel(app.getId())
        .getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetApplicableContextsForRepository_Unauthenticated() {
    labelService.getApplicableContexts(OwnerType.REPOSITORY, repository.getId(), tempEntity.newLabel(app.getId())
        .getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetApplicableContextsForApplication_Unauthorized() {
    login();
    labelService.getApplicableContexts(OwnerType.APPLICATION, app.getId(), tempEntity.newLabel(app.getId())
        .getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetApplicableContextsForApplication_Unauthorized_PublicId() {
    login();
    labelService.getApplicableContexts(OwnerType.APPLICATION, app.getPublicId(), tempEntity.newLabel(app.getId())
        .getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetApplicableContextsForRepository_Unauthorized() {
    login();
    labelService.getApplicableContexts(OwnerType.REPOSITORY, repository.getId(), tempEntity.newLabel(app.getId())
        .getId());
  }

  @Test
  public void testAddLabelForApplication_Authorized() {
    grantWritePermission(app.getId());
    labelService.addLabel(OwnerType.APPLICATION, app.getId(), newInMemoryLabel());
  }

  @Test
  public void testAddLabelForApplication_Authorized_PublicId() {
    grantWritePermission(app.getId());
    labelService.addLabel(OwnerType.APPLICATION, app.getPublicId(), newInMemoryLabel());
  }

  @Test
  public void testAddLabelForOrganization_Authorized() {
    grantWritePermission(org.getId());
    labelService.addLabel(OwnerType.ORGANIZATION, org.getId(), newInMemoryLabel());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddLabelForApplication_Unauthenticated() {
    labelService.addLabel(OwnerType.APPLICATION, app.getId(), newInMemoryLabel());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddLabelForApplication_Unauthenticated_PublicId() {
    labelService.addLabel(OwnerType.APPLICATION, app.getPublicId(), newInMemoryLabel());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddLabelForOrganization_Unauthenticated() {
    labelService.addLabel(OwnerType.ORGANIZATION, org.getId(), newInMemoryLabel());
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddLabelForApplication_Unauthorized() {
    grantReadPermission(app.getId());
    labelService.addLabel(OwnerType.APPLICATION, app.getId(), newInMemoryLabel());
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddLabelForApplication_Unauthorized_PublicId() {
    grantReadPermission(app.getId());
    labelService.addLabel(OwnerType.APPLICATION, app.getPublicId(), newInMemoryLabel());
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddLabelForOrganization_Unauthorized() {
    grantReadPermission(org.getId());
    labelService.addLabel(OwnerType.ORGANIZATION, org.getId(), newInMemoryLabel());
  }

  @Test
  public void testUpdateLabelForApplication_Authorized() {
    grantWritePermission(app.getId());
    labelService.updateLabel(OwnerType.APPLICATION, app.getId(), newPersistedLabel(app.getId()));
  }

  @Test
  public void testUpdateLabelForApplication_Authorized_PublicId() {
    grantWritePermission(app.getId());
    labelService.updateLabel(OwnerType.APPLICATION, app.getPublicId(), newPersistedLabel(app.getId()));
  }

  @Test
  public void testUpdateLabelForOrganization_Authorized() {
    grantWritePermission(org.getId());
    labelService.updateLabel(OwnerType.ORGANIZATION, org.getId(), newPersistedLabel(org.getId()));
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdateLabelForApplication_Unauthenticated() {
    labelService.updateLabel(OwnerType.APPLICATION, app.getId(), newPersistedLabel(app.getId()));
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdateLabelForApplication_Unauthenticated_PublicId() {
    labelService.updateLabel(OwnerType.APPLICATION, app.getPublicId(), newPersistedLabel(app.getId()));
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdateLabelForOrganization_Unauthenticated() {
    labelService.updateLabel(OwnerType.ORGANIZATION, org.getId(), newPersistedLabel(org.getId()));
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpdateLabelForApplication_Unauthorized() {
    grantReadPermission(app.getId());
    labelService.updateLabel(OwnerType.APPLICATION, app.getId(), newPersistedLabel(app.getId()));
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpdateLabelForApplication_Unauthorized_PublicId() {
    grantReadPermission(app.getId());
    labelService.updateLabel(OwnerType.APPLICATION, app.getPublicId(), newPersistedLabel(app.getId()));
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpdateLabelForOrganization_Unauthorized() {
    grantReadPermission(org.getId());
    labelService.updateLabel(OwnerType.ORGANIZATION, org.getId(), newPersistedLabel(org.getId()));
  }

  @Test
  public void testDeleteLabelForApplication_Authorized() {
    grantWritePermission(app.getId());
    labelService.deleteLabel(OwnerType.APPLICATION, app.getId(), tempEntity.newLabel(app.getId()).getId());
  }

  @Test
  public void testDeleteLabelForApplication_Authorized_PublicId() {
    grantWritePermission(app.getId());
    labelService.deleteLabel(OwnerType.APPLICATION, app.getPublicId(), tempEntity.newLabel(app.getId()).getId());
  }

  @Test
  public void testDeleteLabelForOrganization_Authorized() {
    grantWritePermission(org.getId());
    labelService.deleteLabel(OwnerType.ORGANIZATION, org.getId(), tempEntity.newLabel(org.getId()).getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteLabelForApplication_Unauthenticated() {
    labelService.deleteLabel(OwnerType.APPLICATION, app.getId(), tempEntity.newLabel(app.getId()).getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteLabelForApplication_Unauthenticated_PublicId() {
    labelService.deleteLabel(OwnerType.APPLICATION, app.getPublicId(), tempEntity.newLabel(app.getId()).getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteLabelForOrganization_Unauthenticated() {
    labelService.deleteLabel(OwnerType.ORGANIZATION, org.getId(), tempEntity.newLabel(org.getId()).getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteLabelForApplication_Unauthorized() {
    grantReadPermission(app.getId());
    labelService.deleteLabel(OwnerType.APPLICATION, app.getId(), tempEntity.newLabel(app.getId()).getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteLabelForApplication_Unauthorized_PublicId() {
    grantReadPermission(app.getId());
    labelService.deleteLabel(OwnerType.APPLICATION, app.getPublicId(), tempEntity.newLabel(app.getId()).getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteLabelForOrganization_Unauthorized() {
    grantReadPermission(org.getId());
    labelService.deleteLabel(OwnerType.ORGANIZATION, org.getId(), tempEntity.newLabel(org.getId()).getId());
  }

  private ApiLabelDTO newInMemoryLabel() {
    Label label = new Label(null, "com.sonatype.insight.test.jaxrs.testing");

    ApiLabelDTO dto = new ApiLabelDTO(label.getLabel(), label.getDescription(), label.getColor().name());
    dto.id = label.getId();

    return dto;
  }

  private ApiLabelDTO newPersistedLabel(String id) {
    Label label = tempEntity.newLabel(id);

    ApiLabelDTO dto = new ApiLabelDTO(label.getLabel(), label.getDescription(), label.getColor().name());
    dto.id = label.getId();

    return dto;
  }
}
