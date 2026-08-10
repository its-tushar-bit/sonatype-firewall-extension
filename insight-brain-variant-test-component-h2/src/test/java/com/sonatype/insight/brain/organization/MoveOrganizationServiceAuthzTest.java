/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.List;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class MoveOrganizationServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private MoveOrganizationService moveOrganizationService;

  @Test
  public void testMoveOrganization_Unauthenticated() {
    boolean failEarlyOnError = true;
    Organization movedOrganization = tempEntity.newOrganization();
    Organization newParent = tempEntity.newOrganization();

    assertThrows(UnauthenticatedException.class, () -> moveOrganizationService
        .moveOrganization(movedOrganization.getId(), newParent.getId(), failEarlyOnError));
  }

  @Test
  public void testMoveOrganization_Unauthorized() {
    login();

    boolean failEarlyOnError = true;
    Organization movedOrganization = tempEntity.newOrganization();
    Organization newParent = tempEntity.newOrganization();

    assertThrows(UnauthorizedException.class, () -> moveOrganizationService
        .moveOrganization(movedOrganization.getId(), newParent.getId(), failEarlyOnError));
  }

  @Test
  public void testMoveOrganization_SourceWritePermission_Unauthorized() {
    login();

    boolean failEarlyOnError = true;
    Organization movedOrganization = tempEntity.newOrganization();
    Organization newParent = tempEntity.newOrganization();

    grantWritePermission(movedOrganization.getId());

    assertThrows(UnauthorizedException.class, () -> moveOrganizationService
        .moveOrganization(movedOrganization.getId(), newParent.getId(), failEarlyOnError));
  }

  @Test
  public void testMoveOrganization_DestinationWritePermission_Unauthorized() {
    login();

    boolean failEarlyOnError = true;
    Organization movedOrganization = tempEntity.newOrganization();
    Organization newParent = tempEntity.newOrganization();

    grantWritePermission(newParent.getId());

    assertThrows(UnauthorizedException.class, () -> moveOrganizationService
        .moveOrganization(movedOrganization.getId(), newParent.getId(), failEarlyOnError));
  }

  @Test
  public void testMoveOrganization() {
    login();

    boolean failEarlyOnError = true;
    Organization movedOrganization = tempEntity.newOrganization();
    Organization newParent = tempEntity.newOrganization();

    grantWritePermission(movedOrganization.getId());
    grantWritePermission(newParent.getId());

    moveOrganizationService.moveOrganization(movedOrganization.getId(), newParent.getId(), failEarlyOnError);
  }

  @Test
  public void testGetMoveOrganizationErrorsForExport() {
    login();

    Organization movedOrganization = tempEntity.newOrganization();
    Organization newParent = tempEntity.newOrganization();

    grantReadPermission(movedOrganization.getId());
    grantReadPermission(newParent.getId());

    moveOrganizationService.getMoveOrganizationErrors(movedOrganization.getId(), newParent.getId());
  }

  @Test
  public void testGetMoveOrganizationErrorsForExport_Unauthorized() {
    login();

    Organization movedOrganization = tempEntity.newOrganization();
    Organization newParent = tempEntity.newOrganization();

    assertThrows(UnauthorizedException.class, () -> moveOrganizationService
        .getMoveOrganizationErrors(movedOrganization.getId(), newParent.getId()));
  }

  @Test
  public void testGetMoveOrganizationErrorsForExport_Unauthenticated() {
    Organization movedOrganization = tempEntity.newOrganization();
    Organization newParent = tempEntity.newOrganization();

    assertThrows(UnauthenticatedException.class, () -> moveOrganizationService
        .getMoveOrganizationErrors(movedOrganization.getId(), newParent.getId()));
  }

  @Test
  public void testGetDestinationOrganizations_Unauthenticated() {
    Organization movedOrganization = tempEntity.newOrganization();
    assertThrows(UnauthenticatedException.class,
        () -> moveOrganizationService.getDestinationOrganizations(movedOrganization.getId()));
  }

  @Test
  public void testGetDestinationOrganizations_Unauthorized() {
    login();

    Organization movedOrganization = tempEntity.newOrganization();
    assertThrows(UnauthorizedException.class,
        () -> moveOrganizationService.getDestinationOrganizations(movedOrganization.getId()));
  }

  @Test
  public void testGetDestinationOrganizations_ReadOnlyAuthorized() {
    Organization movedOrganization = tempEntity.newOrganization();
    grantReadPermission(movedOrganization.getId());

    assertThrows(UnauthorizedException.class,
        () -> moveOrganizationService.getDestinationOrganizations(movedOrganization.getId()));
  }

  @Test
  public void testGetDestinationOrganizations() {
    Organization movedOrganization = tempEntity.newOrganization();
    grantWritePermission(movedOrganization.getId());

    moveOrganizationService.getDestinationOrganizations(movedOrganization.getId());
  }

  @Test
  public void testGetDestinationOrganizations_onlyReturnOrganizationsWithWritePermission() {
    Organization movedOrganization = tempEntity.newOrganization();
    grantWritePermission(movedOrganization.getId());

    tempEntity.newOrganization();
    Organization organizationWithOnlyReadPermission = tempEntity.newOrganization();
    Organization writeAuthorizedOrganization = tempEntity.newOrganization();

    grantReadPermission(organizationWithOnlyReadPermission.getId());
    grantWritePermission(writeAuthorizedOrganization.getId());

    List<Organization> destinationOrganizations =
        moveOrganizationService.getDestinationOrganizations(movedOrganization.getId());
    assertThat(destinationOrganizations).hasSize(1);
    assertThat(destinationOrganizations.get(0).getId()).isEqualTo(writeAuthorizedOrganization.getId());
  }
}
