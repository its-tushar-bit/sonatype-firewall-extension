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
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class LabelServiceAuthzTest
    extends AbstractComponentH2AuthzTest
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

  @Test
  public void testGetLabelsForApplication_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> labelService.getLabels(OwnerType.APPLICATION, app.getId(), false));
  }

  @Test
  public void testGetLabelsForApplication_Unauthenticated_PublicId() {
    assertThrows(UnauthenticatedException.class,
        () -> labelService.getLabels(OwnerType.APPLICATION, app.getPublicId(), false));
  }

  @Test
  public void testGetLabelsForOrganization_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> labelService.getLabels(OwnerType.ORGANIZATION, org.getId(), false));
  }

  @Test
  public void testGetLabelsForRepository_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> labelService.getLabels(OwnerType.REPOSITORY, repository.getId(), false));
  }

  @Test
  public void testGetLabelsForApplication_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> labelService.getLabels(OwnerType.APPLICATION, app.getId(), false));
  }

  @Test
  public void testGetLabelsForApplication_Unauthorized_PublicId() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> labelService.getLabels(OwnerType.APPLICATION, app.getPublicId(), false));
  }

  @Test
  public void testGetLabelsForOrganization_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> labelService.getLabels(OwnerType.ORGANIZATION, org.getId(), false));
  }

  @Test
  public void testGetLabelsForRepository_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> labelService.getLabels(OwnerType.REPOSITORY, repository.getId(), false));
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

  @Test
  public void testGetApplicableLabelsForApplication_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> labelService.getApplicableLabels(OwnerType.APPLICATION, app.getId()));
  }

  @Test
  public void testGetApplicableLabelsForApplication_Unauthenticated_PublicId() {
    assertThrows(UnauthenticatedException.class,
        () -> labelService.getApplicableLabels(OwnerType.APPLICATION, app.getPublicId()));
  }

  @Test
  public void testGetApplicableLabelsForOrganization_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> labelService.getApplicableLabels(OwnerType.ORGANIZATION, org.getId()));
  }

  @Test
  public void testGetApplicableLabelsForRepository_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> labelService.getApplicableLabels(OwnerType.REPOSITORY, repository.getId()));
  }

  @Test
  public void testGetApplicableLabelsForApplication_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> labelService.getApplicableLabels(OwnerType.APPLICATION, app.getId()));
  }

  @Test
  public void testGetApplicableLabelsForApplication_Unauthorized_PublicId() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> labelService.getApplicableLabels(OwnerType.APPLICATION, app.getPublicId()));
  }

  @Test
  public void testGetApplicableLabelsForOrganization_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> labelService.getApplicableLabels(OwnerType.ORGANIZATION, org.getId()));
  }

  @Test
  public void testGetApplicableLabelsForRepository_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> labelService.getApplicableLabels(OwnerType.REPOSITORY, repository.getId()));
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

  @Test
  public void testGetApplicableContextsForApplication_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> labelService.getApplicableContexts(OwnerType.APPLICATION, app.getId(),
            tempEntity.newLabel(app.getId()).getId()));
  }

  @Test
  public void testGetApplicableContextsForApplication_Unauthenticated_PublicId() {
    assertThrows(UnauthenticatedException.class,
        () -> labelService.getApplicableContexts(OwnerType.APPLICATION, app.getPublicId(),
            tempEntity.newLabel(app.getId()).getId()));
  }

  @Test
  public void testGetApplicableContextsForRepository_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> labelService.getApplicableContexts(OwnerType.REPOSITORY, repository.getId(),
            tempEntity.newLabel(app.getId()).getId()));
  }

  @Test
  public void testGetApplicableContextsForApplication_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> labelService.getApplicableContexts(OwnerType.APPLICATION, app.getId(),
            tempEntity.newLabel(app.getId()).getId()));
  }

  @Test
  public void testGetApplicableContextsForApplication_Unauthorized_PublicId() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> labelService.getApplicableContexts(OwnerType.APPLICATION, app.getPublicId(),
            tempEntity.newLabel(app.getId()).getId()));
  }

  @Test
  public void testGetApplicableContextsForRepository_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> labelService.getApplicableContexts(OwnerType.REPOSITORY, repository.getId(),
            tempEntity.newLabel(app.getId()).getId()));
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

  @Test
  public void testAddLabelForApplication_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> labelService.addLabel(OwnerType.APPLICATION, app.getId(), newInMemoryLabel()));
  }

  @Test
  public void testAddLabelForApplication_Unauthenticated_PublicId() {
    assertThrows(UnauthenticatedException.class,
        () -> labelService.addLabel(OwnerType.APPLICATION, app.getPublicId(), newInMemoryLabel()));
  }

  @Test
  public void testAddLabelForOrganization_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> labelService.addLabel(OwnerType.ORGANIZATION, org.getId(), newInMemoryLabel()));
  }

  @Test
  public void testAddLabelForApplication_Unauthorized() {
    grantReadPermission(app.getId());
    assertThrows(UnauthorizedException.class,
        () -> labelService.addLabel(OwnerType.APPLICATION, app.getId(), newInMemoryLabel()));
  }

  @Test
  public void testAddLabelForApplication_Unauthorized_PublicId() {
    grantReadPermission(app.getId());
    assertThrows(UnauthorizedException.class,
        () -> labelService.addLabel(OwnerType.APPLICATION, app.getPublicId(), newInMemoryLabel()));
  }

  @Test
  public void testAddLabelForOrganization_Unauthorized() {
    grantReadPermission(org.getId());
    assertThrows(UnauthorizedException.class,
        () -> labelService.addLabel(OwnerType.ORGANIZATION, org.getId(), newInMemoryLabel()));
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

  @Test
  public void testUpdateLabelForApplication_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> labelService.updateLabel(OwnerType.APPLICATION, app.getId(), newPersistedLabel(app.getId())));
  }

  @Test
  public void testUpdateLabelForApplication_Unauthenticated_PublicId() {
    assertThrows(UnauthenticatedException.class,
        () -> labelService.updateLabel(OwnerType.APPLICATION, app.getPublicId(), newPersistedLabel(app.getId())));
  }

  @Test
  public void testUpdateLabelForOrganization_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> labelService.updateLabel(OwnerType.ORGANIZATION, org.getId(), newPersistedLabel(org.getId())));
  }

  @Test
  public void testUpdateLabelForApplication_Unauthorized() {
    grantReadPermission(app.getId());
    assertThrows(UnauthorizedException.class,
        () -> labelService.updateLabel(OwnerType.APPLICATION, app.getId(), newPersistedLabel(app.getId())));
  }

  @Test
  public void testUpdateLabelForApplication_Unauthorized_PublicId() {
    grantReadPermission(app.getId());
    assertThrows(UnauthorizedException.class,
        () -> labelService.updateLabel(OwnerType.APPLICATION, app.getPublicId(), newPersistedLabel(app.getId())));
  }

  @Test
  public void testUpdateLabelForOrganization_Unauthorized() {
    grantReadPermission(org.getId());
    assertThrows(UnauthorizedException.class,
        () -> labelService.updateLabel(OwnerType.ORGANIZATION, org.getId(), newPersistedLabel(org.getId())));
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

  @Test
  public void testDeleteLabelForApplication_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> labelService.deleteLabel(OwnerType.APPLICATION, app.getId(), tempEntity.newLabel(app.getId()).getId()));
  }

  @Test
  public void testDeleteLabelForApplication_Unauthenticated_PublicId() {
    assertThrows(UnauthenticatedException.class,
        () -> labelService.deleteLabel(OwnerType.APPLICATION, app.getPublicId(),
            tempEntity.newLabel(app.getId()).getId()));
  }

  @Test
  public void testDeleteLabelForOrganization_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> labelService.deleteLabel(OwnerType.ORGANIZATION, org.getId(), tempEntity.newLabel(org.getId()).getId()));
  }

  @Test
  public void testDeleteLabelForApplication_Unauthorized() {
    grantReadPermission(app.getId());
    assertThrows(UnauthorizedException.class,
        () -> labelService.deleteLabel(OwnerType.APPLICATION, app.getId(), tempEntity.newLabel(app.getId()).getId()));
  }

  @Test
  public void testDeleteLabelForApplication_Unauthorized_PublicId() {
    grantReadPermission(app.getId());
    assertThrows(UnauthorizedException.class,
        () -> labelService.deleteLabel(OwnerType.APPLICATION, app.getPublicId(),
            tempEntity.newLabel(app.getId()).getId()));
  }

  @Test
  public void testDeleteLabelForOrganization_Unauthorized() {
    grantReadPermission(org.getId());
    assertThrows(UnauthorizedException.class,
        () -> labelService.deleteLabel(OwnerType.ORGANIZATION, org.getId(), tempEntity.newLabel(org.getId()).getId()));
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
