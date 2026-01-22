/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.label;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

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

  @Test(expected = UnauthenticatedException.class)
  public void testGetComponentLabelsForApplication_Unauthenticated() {
    componentLabelService.getComponentLabels(OwnerType.APPLICATION, app.getPublicId(), "bad");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetComponentLabelsForOrganization_Unauthenticated() {
    componentLabelService.getComponentLabels(OwnerType.ORGANIZATION, org.getId(), "bad");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetComponentLabelsForRepository_Unauthenticated() {
    componentLabelService.getComponentLabels(OwnerType.REPOSITORY, repository.getId(), "bad");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetComponentLabelsForRepositoryContainer_Unauthenticated() {
    componentLabelService.getComponentLabels(OwnerType.REPOSITORY_CONTAINER,
        RepositoryContainer.REPOSITORY_CONTAINER_ID, "bad");
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetComponentLabelsForApplication_Unauthorized() {
    login();
    componentLabelService.getComponentLabels(OwnerType.APPLICATION, app.getPublicId(), "bad");
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetComponentLabelsForOrganization_Unauthorized() {
    login();
    componentLabelService.getComponentLabels(OwnerType.ORGANIZATION, org.getId(), "bad");
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetComponentLabelsForRepository_Unauthorized() {
    login();
    componentLabelService.getComponentLabels(OwnerType.REPOSITORY, repository.getId(), "bad");
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetComponentLabelsForRepositoryContainer_Unauthorized() {
    login();
    componentLabelService.getComponentLabels(OwnerType.REPOSITORY_CONTAINER,
        RepositoryContainer.REPOSITORY_CONTAINER_ID, "bad");
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

  @Test(expected = UnauthenticatedException.class)
  public void testSetComponentLabelForApplication_Unauthenticated() {
    Label label = tempEntity.newLabel(org.getId());
    componentLabelService.setComponentLabel(OwnerType.APPLICATION, app.getPublicId(), "bad", label);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetComponentLabelForOrganization_Unauthenticated() {
    Label label = tempEntity.newLabel(org.getId());
    componentLabelService.setComponentLabel(OwnerType.ORGANIZATION, org.getId(), "bad", label);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetComponentLabelForRepository_Unauthenticated() {
    Label label = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID);
    componentLabelService.setComponentLabel(OwnerType.REPOSITORY, repository.getId(), "bad", label);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetComponentLabelForRepositoryContainer_Unauthenticated() {
    Label label = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID);
    componentLabelService.setComponentLabel(OwnerType.REPOSITORY_CONTAINER,
        RepositoryContainer.REPOSITORY_CONTAINER_ID, "bad", label);
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetComponentLabelForApplication_Unauthorized() {
    grantReadPermission(app.getId());

    Label label = tempEntity.newLabel(org.getId());
    componentLabelService.setComponentLabel(OwnerType.APPLICATION, app.getPublicId(), "bad", label);
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetComponentLabelForOrganization_Unauthorized() {
    grantReadPermission(org.getId());

    Label label = tempEntity.newLabel(org.getId());
    componentLabelService.setComponentLabel(OwnerType.ORGANIZATION, org.getId(), "bad", label);
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetComponentLabelForRepository_Unauthorized() {
    grantReadPermission(repository.getId());

    Label label = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID);
    componentLabelService.setComponentLabel(OwnerType.REPOSITORY, repository.getId(), "bad", label);
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetComponentLabelForRepositoryContainer_Unauthorized() {
    grantReadPermission(Organization.ROOT_ORGANIZATION_ID);

    Label label = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID);
    componentLabelService.setComponentLabel(OwnerType.REPOSITORY_CONTAINER,
        RepositoryContainer.REPOSITORY_CONTAINER_ID, "bad", label);
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

  @Test(expected = UnauthenticatedException.class)
  public void testRemoveComponentLabelForApplication_Unauthenticated() {
    Label label = tempEntity.newLabel(org.getId());
    tempEntity.newComponentLabel(app.getId(), label.getId(), "bad");
    componentLabelService.deleteComponentLabel(OwnerType.APPLICATION, app.getPublicId(), "bad", label.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testRemoveComponentLabelForOrganization_Unauthenticated() {
    Label label = tempEntity.newLabel(org.getId());
    tempEntity.newComponentLabel(org.getId(), label.getId(), "bad");
    componentLabelService.deleteComponentLabel(OwnerType.ORGANIZATION, org.getId(), "bad", label.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testRemoveComponentLabelForRepository_Unauthenticated() {
    Label label = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID);
    tempEntity.newComponentLabel(repository.getId(), label.getId(), "bad");
    componentLabelService.deleteComponentLabel(OwnerType.REPOSITORY, repository.getId(), "bad", label.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testRemoveComponentLabelForRepositoryContainer_Unauthenticated() {
    Label label = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID);
    tempEntity.newComponentLabel(RepositoryContainer.REPOSITORY_CONTAINER_ID, label.getId(), "bad");
    componentLabelService.deleteComponentLabel(OwnerType.REPOSITORY_CONTAINER,
        RepositoryContainer.REPOSITORY_CONTAINER_ID, "bad", label.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testRemoveComponentLabelForApplication_Unauthorized() {
    grantReadPermission(app.getId());

    Label label = tempEntity.newLabel(org.getId());
    tempEntity.newComponentLabel(app.getId(), label.getId(), "bad");
    componentLabelService.deleteComponentLabel(OwnerType.APPLICATION, app.getPublicId(), "bad", label.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testRemoveComponentLabelForOrganization_Unauthorized() {
    grantReadPermission(org.getId());

    Label label = tempEntity.newLabel(org.getId());
    tempEntity.newComponentLabel(org.getId(), label.getId(), "bad");
    componentLabelService.deleteComponentLabel(OwnerType.ORGANIZATION, org.getId(), "bad", label.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testRemoveComponentLabelForRepository_Unauthorized() {
    grantReadPermission(repository.getId());

    Label label = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID);
    tempEntity.newComponentLabel(repository.getId(), label.getId(), "bad");
    componentLabelService.deleteComponentLabel(OwnerType.REPOSITORY, repository.getId(), "bad", label.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testRemoveComponentLabelForRepositoryContainer_Unauthorized() {
    grantReadPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    Label label = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID);
    tempEntity.newComponentLabel(RepositoryContainer.REPOSITORY_CONTAINER_ID, label.getId(), "bad");
    componentLabelService.deleteComponentLabel(OwnerType.REPOSITORY_CONTAINER,
        RepositoryContainer.REPOSITORY_CONTAINER_ID, "bad", label.getId());
  }
}
