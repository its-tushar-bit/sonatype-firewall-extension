/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.label;

import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

@ComponentH2Test
public class ComponentLabelServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private ComponentLabelService componentLabelService;

  @Test
  public void testGetComponentLabelsForApplication_Authorized() {
    grantReadPermission(app.getId());
    componentLabelService.getComponentLabels(OwnerType.APPLICATION, app.getPublicId(), "bad");
  }

  @Test
  public void testGetComponentLabelsForOrganization_Authorized() {
    grantReadPermission(org.getId());
    componentLabelService.getComponentLabels(OwnerType.ORGANIZATION, org.getId(), "bad");
  }

  @Test
  public void testGetComponentLabelsForRepository_Authorized() {
    grantReadPermission(repository.getId());
    componentLabelService.getComponentLabels(OwnerType.REPOSITORY, repository.getId(), "bad");
  }

  @Test
  public void testGetComponentLabelsForRepositoryContainer_Authorized() {
    grantReadPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    componentLabelService.getComponentLabels(OwnerType.REPOSITORY_CONTAINER,
        RepositoryContainer.REPOSITORY_CONTAINER_ID, "bad");
  }

  @Test
  public void testGetComponentLabelsForApplication_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> componentLabelService.getComponentLabels(OwnerType.APPLICATION, app.getPublicId(), "bad"));
  }

  @Test
  public void testGetComponentLabelsForOrganization_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> componentLabelService.getComponentLabels(OwnerType.ORGANIZATION, org.getId(), "bad"));
  }

  @Test
  public void testGetComponentLabelsForRepository_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> componentLabelService.getComponentLabels(OwnerType.REPOSITORY, repository.getId(), "bad"));
  }

  @Test
  public void testGetComponentLabelsForRepositoryContainer_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> componentLabelService.getComponentLabels(OwnerType.REPOSITORY_CONTAINER,
            RepositoryContainer.REPOSITORY_CONTAINER_ID, "bad"));
  }

  @Test
  public void testGetComponentLabelsForApplication_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> componentLabelService.getComponentLabels(OwnerType.APPLICATION, app.getPublicId(), "bad"));
  }

  @Test
  public void testGetComponentLabelsForOrganization_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> componentLabelService.getComponentLabels(OwnerType.ORGANIZATION, org.getId(), "bad"));
  }

  @Test
  public void testGetComponentLabelsForRepository_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> componentLabelService.getComponentLabels(OwnerType.REPOSITORY, repository.getId(), "bad"));
  }

  @Test
  public void testGetComponentLabelsForRepositoryContainer_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> componentLabelService.getComponentLabels(OwnerType.REPOSITORY_CONTAINER,
            RepositoryContainer.REPOSITORY_CONTAINER_ID, "bad"));
  }

  @Test
  public void testSetComponentLabelForApplication_Authorized() {
    grantWritePermission(app.getId());

    Label label = tempEntity.newLabel(org.getId());
    componentLabelService.setComponentLabel(OwnerType.APPLICATION, app.getPublicId(), "bad", label);
  }

  @Test
  public void testSetComponentLabelForOrganization_Authorized() {
    grantWritePermission(org.getId());

    Label label = tempEntity.newLabel(org.getId());
    componentLabelService.setComponentLabel(OwnerType.ORGANIZATION, org.getId(), "bad", label);
  }

  @Test
  public void testSetComponentLabelForRepository_Authorized() {
    grantWritePermission(repository.getId());

    Label label = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID);
    componentLabelService.setComponentLabel(OwnerType.REPOSITORY, repository.getId(), "bad", label);
  }

  @Test
  public void testSetComponentLabelForRepositoryContainer_Authorized() {
    grantWritePermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    Label label = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID);
    componentLabelService.setComponentLabel(OwnerType.REPOSITORY_CONTAINER,
        RepositoryContainer.REPOSITORY_CONTAINER_ID, "bad", label);
  }

  @Test
  public void testSetComponentLabelForApplication_Unauthenticated() {
    Label label = tempEntity.newLabel(org.getId());
    assertThrows(UnauthenticatedException.class,
        () -> componentLabelService.setComponentLabel(OwnerType.APPLICATION, app.getPublicId(), "bad", label));
  }

  @Test
  public void testSetComponentLabelForOrganization_Unauthenticated() {
    Label label = tempEntity.newLabel(org.getId());
    assertThrows(UnauthenticatedException.class,
        () -> componentLabelService.setComponentLabel(OwnerType.ORGANIZATION, org.getId(), "bad", label));
  }

  @Test
  public void testSetComponentLabelForRepository_Unauthenticated() {
    Label label = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID);
    assertThrows(UnauthenticatedException.class,
        () -> componentLabelService.setComponentLabel(OwnerType.REPOSITORY, repository.getId(), "bad", label));
  }

  @Test
  public void testSetComponentLabelForRepositoryContainer_Unauthenticated() {
    Label label = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID);
    assertThrows(UnauthenticatedException.class,
        () -> componentLabelService.setComponentLabel(OwnerType.REPOSITORY_CONTAINER,
            RepositoryContainer.REPOSITORY_CONTAINER_ID, "bad", label));
  }

  @Test
  public void testSetComponentLabelForApplication_Unauthorized() {
    grantReadPermission(app.getId());

    Label label = tempEntity.newLabel(org.getId());
    assertThrows(UnauthorizedException.class,
        () -> componentLabelService.setComponentLabel(OwnerType.APPLICATION, app.getPublicId(), "bad", label));
  }

  @Test
  public void testSetComponentLabelForOrganization_Unauthorized() {
    grantReadPermission(org.getId());

    Label label = tempEntity.newLabel(org.getId());
    assertThrows(UnauthorizedException.class,
        () -> componentLabelService.setComponentLabel(OwnerType.ORGANIZATION, org.getId(), "bad", label));
  }

  @Test
  public void testSetComponentLabelForRepository_Unauthorized() {
    grantReadPermission(repository.getId());

    Label label = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID);
    assertThrows(UnauthorizedException.class,
        () -> componentLabelService.setComponentLabel(OwnerType.REPOSITORY, repository.getId(), "bad", label));
  }

  @Test
  public void testSetComponentLabelForRepositoryContainer_Unauthorized() {
    grantReadPermission(Organization.ROOT_ORGANIZATION_ID);

    Label label = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID);
    assertThrows(UnauthorizedException.class,
        () -> componentLabelService.setComponentLabel(OwnerType.REPOSITORY_CONTAINER,
            RepositoryContainer.REPOSITORY_CONTAINER_ID, "bad", label));
  }

  @Test
  public void testRemoveComponentLabelForApplication_Authorized() {
    grantWritePermission(app.getId());

    Label label = tempEntity.newLabel(org.getId());
    tempEntity.newComponentLabel(app.getId(), label.getId(), "bad");
    componentLabelService.deleteComponentLabel(OwnerType.APPLICATION, app.getPublicId(), "bad", label.getId());
  }

  @Test
  public void testRemoveComponentLabelForOrganization_Authorized() {
    grantWritePermission(org.getId());

    Label label = tempEntity.newLabel(org.getId());
    tempEntity.newComponentLabel(org.getId(), label.getId(), "bad");
    componentLabelService.deleteComponentLabel(OwnerType.ORGANIZATION, org.getId(), "bad", label.getId());
  }

  @Test
  public void testRemoveComponentLabelForRepository_Authorized() {
    grantWritePermission(repository.getId());

    Label label = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID);
    tempEntity.newComponentLabel(repository.getId(), label.getId(), "bad");
    componentLabelService.deleteComponentLabel(OwnerType.REPOSITORY, repository.getId(), "bad", label.getId());
  }

  @Test
  public void testRemoveComponentLabelForRepositoryContainer_Authorized() {
    grantWritePermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    Label label = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID);
    tempEntity.newComponentLabel(RepositoryContainer.REPOSITORY_CONTAINER_ID, label.getId(), "bad");
    componentLabelService.deleteComponentLabel(OwnerType.REPOSITORY_CONTAINER,
        RepositoryContainer.REPOSITORY_CONTAINER_ID, "bad", label.getId());
  }

  @Test
  public void testRemoveComponentLabelForApplication_Unauthenticated() {
    Label label = tempEntity.newLabel(org.getId());
    tempEntity.newComponentLabel(app.getId(), label.getId(), "bad");
    assertThrows(UnauthenticatedException.class,
        () -> componentLabelService.deleteComponentLabel(OwnerType.APPLICATION, app.getPublicId(), "bad",
            label.getId()));
  }

  @Test
  public void testRemoveComponentLabelForOrganization_Unauthenticated() {
    Label label = tempEntity.newLabel(org.getId());
    tempEntity.newComponentLabel(org.getId(), label.getId(), "bad");
    assertThrows(UnauthenticatedException.class,
        () -> componentLabelService.deleteComponentLabel(OwnerType.ORGANIZATION, org.getId(), "bad", label.getId()));
  }

  @Test
  public void testRemoveComponentLabelForRepository_Unauthenticated() {
    Label label = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID);
    tempEntity.newComponentLabel(repository.getId(), label.getId(), "bad");
    assertThrows(UnauthenticatedException.class,
        () -> componentLabelService.deleteComponentLabel(OwnerType.REPOSITORY, repository.getId(), "bad",
            label.getId()));
  }

  @Test
  public void testRemoveComponentLabelForRepositoryContainer_Unauthenticated() {
    Label label = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID);
    tempEntity.newComponentLabel(RepositoryContainer.REPOSITORY_CONTAINER_ID, label.getId(), "bad");
    assertThrows(UnauthenticatedException.class,
        () -> componentLabelService.deleteComponentLabel(OwnerType.REPOSITORY_CONTAINER,
            RepositoryContainer.REPOSITORY_CONTAINER_ID, "bad", label.getId()));
  }

  @Test
  public void testRemoveComponentLabelForApplication_Unauthorized() {
    grantReadPermission(app.getId());

    Label label = tempEntity.newLabel(org.getId());
    tempEntity.newComponentLabel(app.getId(), label.getId(), "bad");
    assertThrows(UnauthorizedException.class,
        () -> componentLabelService.deleteComponentLabel(OwnerType.APPLICATION, app.getPublicId(), "bad",
            label.getId()));
  }

  @Test
  public void testRemoveComponentLabelForOrganization_Unauthorized() {
    grantReadPermission(org.getId());

    Label label = tempEntity.newLabel(org.getId());
    tempEntity.newComponentLabel(org.getId(), label.getId(), "bad");
    assertThrows(UnauthorizedException.class,
        () -> componentLabelService.deleteComponentLabel(OwnerType.ORGANIZATION, org.getId(), "bad", label.getId()));
  }

  @Test
  public void testRemoveComponentLabelForRepository_Unauthorized() {
    grantReadPermission(repository.getId());

    Label label = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID);
    tempEntity.newComponentLabel(repository.getId(), label.getId(), "bad");
    assertThrows(UnauthorizedException.class,
        () -> componentLabelService.deleteComponentLabel(OwnerType.REPOSITORY, repository.getId(), "bad",
            label.getId()));
  }

  @Test
  public void testRemoveComponentLabelForRepositoryContainer_Unauthorized() {
    grantReadPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    Label label = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID);
    tempEntity.newComponentLabel(RepositoryContainer.REPOSITORY_CONTAINER_ID, label.getId(), "bad");
    assertThrows(UnauthorizedException.class,
        () -> componentLabelService.deleteComponentLabel(OwnerType.REPOSITORY_CONTAINER,
            RepositoryContainer.REPOSITORY_CONTAINER_ID, "bad", label.getId()));
  }
}
