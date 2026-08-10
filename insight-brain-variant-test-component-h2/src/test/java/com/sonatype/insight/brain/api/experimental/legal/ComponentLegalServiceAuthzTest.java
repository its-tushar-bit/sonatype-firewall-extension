/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import java.util.Collections;
import java.util.List;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalObligationDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentCopyrightDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentLegalFileDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentObligationAttributionDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.CopyrightOverrideDTO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.legal.ComponentCopyright;
import com.sonatype.insight.brain.model.legal.ComponentLegalFile;
import com.sonatype.insight.brain.model.legal.ComponentLegalPartStatus;
import com.sonatype.insight.brain.model.legal.ComponentObligation;
import com.sonatype.insight.brain.model.legal.ComponentObligationAttribution;
import com.sonatype.insight.brain.model.legal.LegalFileType;
import com.sonatype.insight.brain.model.legal.ObligationStatus;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.common.collect.Lists;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class ComponentLegalServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private ComponentLegalService componentLegalService;

  @Test
  public void testSaveComponentCopyright_ApplicationScope_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> componentLegalService
        .saveComponentCopyright(OwnerType.APPLICATION, app.getPublicId(), buildComponentCopyrightDTO()));
  }

  @Test
  public void testSaveComponentCopyright_OrganizationScope_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> componentLegalService
        .saveComponentCopyright(OwnerType.ORGANIZATION, org.getPublicId(), buildComponentCopyrightDTO()));
  }

  @Test
  public void testSaveComponentCopyright_RootScope_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> componentLegalService
        .saveComponentCopyright(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID,
            buildComponentCopyrightDTO()));
  }

  @Test
  public void testSaveComponentCopyright_ApplicationScope_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> componentLegalService
        .saveComponentCopyright(OwnerType.APPLICATION, app.getPublicId(), buildComponentCopyrightDTO()));
  }

  @Test
  public void testSaveComponentCopyright_ApplicationScope_Authorized() {
    grantLegalReviewerPermission(app.getId());
    componentLegalService
        .saveComponentCopyright(OwnerType.APPLICATION, app.getPublicId(), buildComponentCopyrightDTO());
  }

  @Test
  public void testSaveComponentCopyright_OrganizationScope_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> componentLegalService
        .saveComponentCopyright(OwnerType.ORGANIZATION, org.getPublicId(), buildComponentCopyrightDTO()));
  }

  @Test
  public void testSaveComponentCopyright_OrganizationScope_Authorized() {
    grantLegalReviewerPermission(org.getId());
    componentLegalService
        .saveComponentCopyright(OwnerType.APPLICATION, app.getPublicId(), buildComponentCopyrightDTO());
    componentLegalService
        .saveComponentCopyright(OwnerType.ORGANIZATION, org.getPublicId(), buildComponentCopyrightDTO());
  }

  @Test
  public void testSaveComponentCopyright_RootScope_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> componentLegalService
        .saveComponentCopyright(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID,
            buildComponentCopyrightDTO()));
  }

  @Test
  public void testSaveComponentCopyright_RootScope_Authorized() {
    grantLegalReviewerPermission(Organization.ROOT_ORGANIZATION_ID);
    componentLegalService
        .saveComponentCopyright(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID,
            buildComponentCopyrightDTO());
    componentLegalService
        .saveComponentCopyright(OwnerType.ORGANIZATION, org.getPublicId(), buildComponentCopyrightDTO());
    componentLegalService
        .saveComponentCopyright(OwnerType.APPLICATION, app.getPublicId(), buildComponentCopyrightDTO());
  }

  @Test
  public void testSaveComponentCopyright_Unauthorized_UpdateAtLowerScope() {
    ComponentCopyrightDTO componentCopyrightDTO = buildComponentCopyrightDTO();
    ComponentCopyright componentCopyright = tempEntity.newComponentCopyright(
        componentCopyrightDTO.getComponentIdentifier().toComponentIdentifier(), org.getId(),
        ComponentLegalService.NOT_IMPLEMENTED);
    componentCopyrightDTO.setId(componentCopyright.getId());
    grantLegalReviewerPermission(app.getId());
    assertThrows(UnauthorizedException.class, () -> componentLegalService
        .saveComponentCopyright(OwnerType.APPLICATION, app.getPublicId(), componentCopyrightDTO));
  }

  @Test
  public void testSaveComponentCopyright_contentOverLimit() {
    grantLegalReviewerPermission(org.getId());
    ComponentCopyrightDTO copyright = buildComponentCopyrightDTO();
    copyright.getCopyrightOverrides().get(0).setContent(String.valueOf(new char[1001]));
    assertThrows(InvalidComponentCopyrightException.class, () -> componentLegalService
        .saveComponentCopyright(OwnerType.APPLICATION, app.getPublicId(), copyright));
  }

  private ComponentCopyrightDTO buildComponentCopyrightDTO() {
    ApiComponentIdentifierDTOV2 componentIdentifier = ApiComponentIdentifierDTOV2
        .fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"));

    return new ComponentCopyrightDTO(
        null,
        componentIdentifier,
        Lists.newArrayList(new CopyrightOverrideDTO(
            null,
            "originalContentHash",
            "content",
            ComponentLegalPartStatus.ENABLED),
            new CopyrightOverrideDTO(
                null,
                "originalContentHash2",
                "content2",
                ComponentLegalPartStatus.DISABLED)),
        null,
        null);
  }

  @Test
  public void testSaveComponentLegalFile_ApplicationScope_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> componentLegalService
        .saveComponentLegalFile(OwnerType.APPLICATION, app.getPublicId(), buildComponentLegalFileDTO()));
  }

  @Test
  public void testSaveComponentLegalFile_OrganizationScope_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> componentLegalService
        .saveComponentLegalFile(OwnerType.ORGANIZATION, org.getPublicId(), buildComponentLegalFileDTO()));
  }

  @Test
  public void testSaveComponentLegalFile_RootScope_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> componentLegalService
        .saveComponentLegalFile(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID,
            buildComponentLegalFileDTO()));
  }

  @Test
  public void testSaveComponentLegalFile_ApplicationScope_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> componentLegalService
        .saveComponentLegalFile(OwnerType.APPLICATION, app.getPublicId(), buildComponentLegalFileDTO()));
  }

  @Test
  public void testSaveComponentLegalFile_ApplicationScope_Authorized() {
    grantLegalReviewerPermission(app.getId());
    componentLegalService
        .saveComponentLegalFile(OwnerType.APPLICATION, app.getPublicId(), buildComponentLegalFileDTO());
  }

  @Test
  public void testSaveComponentLegalFile_OrganizationScope_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> componentLegalService
        .saveComponentLegalFile(OwnerType.ORGANIZATION, org.getPublicId(), buildComponentLegalFileDTO()));
  }

  @Test
  public void testSaveComponentLegalFile_OrganizationScope_Authorized() {
    grantLegalReviewerPermission(org.getId());
    componentLegalService
        .saveComponentLegalFile(OwnerType.APPLICATION, app.getPublicId(), buildComponentLegalFileDTO());
    componentLegalService
        .saveComponentLegalFile(OwnerType.ORGANIZATION, org.getPublicId(), buildComponentLegalFileDTO());
  }

  @Test
  public void testSaveComponentLegalFile_RootScope_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> componentLegalService
        .saveComponentLegalFile(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID,
            buildComponentLegalFileDTO()));
  }

  @Test
  public void testSaveComponentLegalFile_RootScope_Authorized() {
    grantLegalReviewerPermission(Organization.ROOT_ORGANIZATION_ID);
    componentLegalService
        .saveComponentLegalFile(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID,
            buildComponentLegalFileDTO());
    componentLegalService
        .saveComponentLegalFile(OwnerType.ORGANIZATION, org.getPublicId(), buildComponentLegalFileDTO());
    componentLegalService
        .saveComponentLegalFile(OwnerType.APPLICATION, app.getPublicId(), buildComponentLegalFileDTO());
  }

  @Test
  public void testSaveComponentLegalFile_Unauthorized_UpdateAtLowerScope() {
    ComponentLegalFileDTO componentLegalFileDTO = buildComponentLegalFileDTO();
    ComponentLegalFile componentLegalFile = tempEntity.newComponentLegalFile(
        componentLegalFileDTO.getComponentIdentifier().toComponentIdentifier(), org.getId(), LegalFileType.NOTICE,
        ComponentLegalService.NOT_IMPLEMENTED);
    componentLegalFileDTO.setId(componentLegalFile.getId());
    grantLegalReviewerPermission(app.getId());
    assertThrows(UnauthorizedException.class,
        () -> componentLegalService.saveComponentLegalFile(OwnerType.APPLICATION, app.getPublicId(),
            componentLegalFileDTO));
  }

  private ComponentLegalFileDTO buildComponentLegalFileDTO() {
    ComponentLegalFileDTO componentLegalFileDTO = new ComponentLegalFileDTO();
    componentLegalFileDTO.setLegalFileType(LegalFileType.NOTICE);
    componentLegalFileDTO.setComponentIdentifier(
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g", "a", "v")));
    return componentLegalFileDTO;
  }

  @Test
  public void testSaveComponentObligationAttribution_ApplicationScope_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> componentLegalService
        .saveComponentObligationAttribution(OwnerType.APPLICATION, app.getPublicId(),
            buildComponentObligationAttributionDTO()));
  }

  @Test
  public void testSaveComponentObligationAttribution_ApplicationScope_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> componentLegalService
        .saveComponentObligationAttribution(OwnerType.APPLICATION, app.getPublicId(),
            buildComponentObligationAttributionDTO()));
  }

  @Test
  public void testSaveComponentObligationAttribution_ApplicationScope_Authorized() {
    grantLegalReviewerPermission(app.getId());
    componentLegalService.saveComponentObligationAttribution(OwnerType.APPLICATION, app.getPublicId(),
        buildComponentObligationAttributionDTO());
  }

  @Test
  public void testSaveComponentObligationAttribution_ApplicationScope_ContentTooLong() {
    grantLegalReviewerPermission(app.getId());
    ComponentObligationAttributionDTO attribution = buildComponentObligationAttributionDTO();
    attribution.setContent(String.valueOf(new char[1001]));
    assertThrows(BadRequestException.class,
        () -> componentLegalService.saveComponentObligationAttribution(OwnerType.APPLICATION, app.getPublicId(),
            attribution));
  }

  @Test
  public void testSaveComponentObligationAttribution_OrganizationScope_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> componentLegalService
        .saveComponentObligationAttribution(OwnerType.ORGANIZATION, org.getPublicId(),
            buildComponentObligationAttributionDTO()));
  }

  @Test
  public void testSaveComponentObligationAttribution_OrganizationScope_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> componentLegalService
        .saveComponentObligationAttribution(OwnerType.ORGANIZATION, org.getPublicId(),
            buildComponentObligationAttributionDTO()));
  }

  @Test
  public void testSaveComponentObligationAttribution_OrganizationScope_Authorized() {
    grantLegalReviewerPermission(org.getId());
    componentLegalService.saveComponentObligationAttribution(OwnerType.ORGANIZATION, org.getPublicId(),
        buildComponentObligationAttributionDTO());
    componentLegalService.saveComponentObligationAttribution(OwnerType.APPLICATION, app.getPublicId(),
        buildComponentObligationAttributionDTO());
  }

  @Test
  public void testSaveComponentObligationAttribution_RootOrganizationScope_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> componentLegalService
        .saveComponentObligationAttribution(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID,
            buildComponentObligationAttributionDTO()));
  }

  @Test
  public void testSaveComponentObligationAttribution_RootOrganizationScope_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> componentLegalService
        .saveComponentObligationAttribution(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID,
            buildComponentObligationAttributionDTO()));
  }

  @Test
  public void testSaveComponentObligationAttribution_RootOrganizationScope_Authorized() {
    grantLegalReviewerPermission(Organization.ROOT_ORGANIZATION_ID);
    componentLegalService.saveComponentObligationAttribution(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID,
        buildComponentObligationAttributionDTO());
    componentLegalService.saveComponentObligationAttribution(OwnerType.ORGANIZATION, org.getPublicId(),
        buildComponentObligationAttributionDTO());
    componentLegalService.saveComponentObligationAttribution(OwnerType.APPLICATION, app.getPublicId(),
        buildComponentObligationAttributionDTO());
  }

  @Test
  public void testDeleteComponentObligationAttribution_ApplicationScope_Unauthenticated() {
    ComponentObligationAttribution componentObligationAttribution = tempEntity.newComponentObligationAttribution(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), app.getId(), null, "content",
        ComponentLegalService.NOT_IMPLEMENTED);
    assertThrows(UnauthenticatedException.class,
        () -> componentLegalService.deleteComponentObligationAttribution(componentObligationAttribution.getId()));
  }

  @Test
  public void testDeleteComponentObligationAttribution_ApplicationScope_Unauthorized() {
    ComponentObligationAttribution componentObligationAttribution = tempEntity.newComponentObligationAttribution(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), app.getId(), null, "content",
        ComponentLegalService.NOT_IMPLEMENTED);
    login();
    assertThrows(UnauthorizedException.class,
        () -> componentLegalService.deleteComponentObligationAttribution(componentObligationAttribution.getId()));
  }

  @Test
  public void testDeleteComponentObligationAttribution_ApplicationScope_Authorized() {
    ComponentObligationAttribution componentObligationAttribution = tempEntity.newComponentObligationAttribution(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), app.getId(), null, "content",
        ComponentLegalService.NOT_IMPLEMENTED);
    grantLegalReviewerPermission(app.getId());
    componentLegalService.deleteComponentObligationAttribution(componentObligationAttribution.getId());
  }

  @Test
  public void testDeleteComponentObligationAttribution_OrganizationScope_Unauthenticated() {
    ComponentObligationAttribution componentObligationAttribution = tempEntity.newComponentObligationAttribution(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), org.getId(), null, "content",
        ComponentLegalService.NOT_IMPLEMENTED);
    assertThrows(UnauthenticatedException.class,
        () -> componentLegalService.deleteComponentObligationAttribution(componentObligationAttribution.getId()));
  }

  @Test
  public void testDeleteComponentObligationAttribution_OrganizationScope_Unauthorized() {
    ComponentObligationAttribution componentObligationAttribution = tempEntity.newComponentObligationAttribution(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), org.getId(), null, "content",
        ComponentLegalService.NOT_IMPLEMENTED);
    login();
    assertThrows(UnauthorizedException.class,
        () -> componentLegalService.deleteComponentObligationAttribution(componentObligationAttribution.getId()));
  }

  @Test
  public void testDeleteComponentObligationAttribution_OrganizationScope_Authorized() {
    ComponentObligationAttribution componentObligationAttributionOrg = tempEntity.newComponentObligationAttribution(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), org.getId(), null, "content",
        ComponentLegalService.NOT_IMPLEMENTED);
    ComponentObligationAttribution componentObligationAttributionApp = tempEntity.newComponentObligationAttribution(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), app.getId(), null, "content",
        ComponentLegalService.NOT_IMPLEMENTED);
    grantLegalReviewerPermission(org.getId());
    componentLegalService.deleteComponentObligationAttribution(componentObligationAttributionOrg.getId());
    componentLegalService.deleteComponentObligationAttribution(componentObligationAttributionApp.getId());
  }

  @Test
  public void testDeleteComponentObligationAttribution_RootOrganizationScope_Unauthenticated() {
    ComponentObligationAttribution componentObligationAttribution = tempEntity.newComponentObligationAttribution(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), Organization.ROOT_ORGANIZATION_ID, null, "content",
        ComponentLegalService.NOT_IMPLEMENTED);
    assertThrows(UnauthenticatedException.class,
        () -> componentLegalService.deleteComponentObligationAttribution(componentObligationAttribution.getId()));
  }

  @Test
  public void testDeleteComponentObligationAttribution_RootOrganizationScope_Unauthorized() {
    ComponentObligationAttribution componentObligationAttribution = tempEntity.newComponentObligationAttribution(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), Organization.ROOT_ORGANIZATION_ID, null, "content",
        ComponentLegalService.NOT_IMPLEMENTED);
    login();
    assertThrows(UnauthorizedException.class,
        () -> componentLegalService.deleteComponentObligationAttribution(componentObligationAttribution.getId()));
  }

  @Test
  public void testDeleteComponentObligationAttribution_RootOrganizationScope_Authorized() {
    ComponentObligationAttribution componentObligationAttributionRootOrg = tempEntity.newComponentObligationAttribution(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), Organization.ROOT_ORGANIZATION_ID, null, "content",
        ComponentLegalService.NOT_IMPLEMENTED);
    ComponentObligationAttribution componentObligationAttributionOrg = tempEntity.newComponentObligationAttribution(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), org.getId(), null, "content",
        ComponentLegalService.NOT_IMPLEMENTED);
    ComponentObligationAttribution componentObligationAttributionApp = tempEntity.newComponentObligationAttribution(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), app.getId(), null, "content",
        ComponentLegalService.NOT_IMPLEMENTED);
    grantLegalReviewerPermission(Organization.ROOT_ORGANIZATION_ID);
    componentLegalService.deleteComponentObligationAttribution(componentObligationAttributionRootOrg.getId());
    componentLegalService.deleteComponentObligationAttribution(componentObligationAttributionOrg.getId());
    componentLegalService.deleteComponentObligationAttribution(componentObligationAttributionApp.getId());
  }

  @Test
  public void testSaveComponentObligationAttribution_Unauthorized_UpdateAtLowerScope() {
    ComponentObligationAttributionDTO componentObligationAttributionDTO = buildComponentObligationAttributionDTO();
    ComponentObligationAttribution componentObligationAttribution = tempEntity.newComponentObligationAttribution(
        componentObligationAttributionDTO.getComponentIdentifier().toComponentIdentifier(), org.getId(), null,
        componentObligationAttributionDTO.getContent(), ComponentLegalService.NOT_IMPLEMENTED);
    componentObligationAttributionDTO.setId(componentObligationAttribution.getId());
    grantLegalReviewerPermission(app.getId());
    assertThrows(UnauthorizedException.class,
        () -> componentLegalService.saveComponentObligationAttribution(OwnerType.APPLICATION, app.getPublicId(),
            componentObligationAttributionDTO));
  }

  @Test
  public void testDeleteComponentObligationAttribution_Unauthorized_DeleteAtLowerScope() {
    ComponentObligationAttributionDTO componentObligationAttributionDTO = buildComponentObligationAttributionDTO();
    ComponentObligationAttribution componentObligationAttribution = tempEntity.newComponentObligationAttribution(
        componentObligationAttributionDTO.getComponentIdentifier().toComponentIdentifier(), org.getId(), null,
        componentObligationAttributionDTO.getContent(), ComponentLegalService.NOT_IMPLEMENTED);
    grantLegalReviewerPermission(app.getId());
    assertThrows(UnauthorizedException.class,
        () -> componentLegalService.deleteComponentObligationAttribution(componentObligationAttribution.getId()));
  }

  @Test
  public void testGetComponentObligationAttribution_Unauthenticated() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    assertThrows(UnauthenticatedException.class, () -> componentLegalService
        .getComponentObligationAttributions(app.getType(), app.getPublicId(), componentIdentifier, "obligationName"));
  }

  @Test
  public void testGetComponentObligationAttribution_Unauthorized() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    login();
    assertThrows(UnauthorizedException.class, () -> componentLegalService
        .getComponentObligationAttributions(app.getType(), app.getPublicId(), componentIdentifier, "obligationName"));
  }

  @Test
  public void testGetComponentObligationAttribution_Authorized() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    grantLegalReviewerPermission(app.getId());
    componentLegalService
        .getComponentObligationAttributions(app.getType(), app.getPublicId(), componentIdentifier, "obligationName");
  }

  @Test
  public void testSaveComponentObligations_ApplicationScope_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> componentLegalService
        .saveComponentObligations(app.getType(), app.getPublicId(), createMinimalComponentObligationDTOs()));
  }

  @Test
  public void testSaveComponentObligations_ApplicationScope_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> componentLegalService
        .saveComponentObligations(app.getType(), app.getPublicId(), createMinimalComponentObligationDTOs()));
  }

  @Test
  public void testSaveComponentObligations_ApplicationScope_Authorized() {
    grantLegalReviewerPermission(app.getId());
    componentLegalService
        .saveComponentObligations(app.getType(), app.getPublicId(), createMinimalComponentObligationDTOs());
  }

  @Test
  public void testSaveComponentObligations_OrganizationScope_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> componentLegalService
        .saveComponentObligations(org.getType(), org.getPublicId(), createMinimalComponentObligationDTOs()));
  }

  @Test
  public void testSaveComponentObligations_OrganizationScope_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> componentLegalService
        .saveComponentObligations(org.getType(), org.getPublicId(), createMinimalComponentObligationDTOs()));
  }

  @Test
  public void testSaveComponentObligations_OrganizationScope_Authorized() {
    grantLegalReviewerPermission(org.getId());
    componentLegalService
        .saveComponentObligations(org.getType(), org.getPublicId(), createMinimalComponentObligationDTOs());
  }

  @Test
  public void testDeleteComponentObligation_ApplicationScope_Unauthenticated() {
    ComponentObligation componentObligation = tempEntity.newComponentObligation(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), app.getId(), "obligationName", null,
        ObligationStatus.OPEN, ComponentLegalService.NOT_IMPLEMENTED);
    assertThrows(UnauthenticatedException.class,
        () -> componentLegalService.deleteComponentObligations(Collections.singletonList(componentObligation.getId())));
  }

  @Test
  public void testDeleteComponentObligation_ApplicationScope_Unauthorized() {
    login();
    ComponentObligation componentObligation = tempEntity.newComponentObligation(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), app.getId(), "obligationName", null,
        ObligationStatus.OPEN, ComponentLegalService.NOT_IMPLEMENTED);
    assertThrows(UnauthorizedException.class,
        () -> componentLegalService.deleteComponentObligations(Collections.singletonList(componentObligation.getId())));
  }

  @Test
  public void testDeleteComponentObligation_ApplicationScope_Authorized() {
    grantLegalReviewerPermission(app.getId());
    ComponentObligation componentObligation = tempEntity.newComponentObligation(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), app.getId(), "obligationName", null,
        ObligationStatus.OPEN, ComponentLegalService.NOT_IMPLEMENTED);
    componentLegalService.deleteComponentObligations(Collections.singletonList(componentObligation.getId()));
  }

  @Test
  public void testDeleteComponentObligation_OrganizationScope_Unauthenticated() {
    ComponentObligation componentObligation = tempEntity.newComponentObligation(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), org.getId(), "obligationName", null,
        ObligationStatus.OPEN, ComponentLegalService.NOT_IMPLEMENTED);
    assertThrows(UnauthenticatedException.class,
        () -> componentLegalService.deleteComponentObligations(Collections.singletonList(componentObligation.getId())));
  }

  @Test
  public void testDeleteComponentObligation_OrganizationScope_Unauthorized() {
    login();
    ComponentObligation componentObligation = tempEntity.newComponentObligation(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), org.getId(), "obligationName", null,
        ObligationStatus.OPEN, ComponentLegalService.NOT_IMPLEMENTED);
    assertThrows(UnauthorizedException.class,
        () -> componentLegalService.deleteComponentObligations(Collections.singletonList(componentObligation.getId())));
  }

  @Test
  public void testDeleteComponentObligation_OrganizationScope_Authorized() {
    grantLegalReviewerPermission(org.getId());
    ComponentObligation componentObligation = tempEntity.newComponentObligation(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), org.getId(), "obligationName", null,
        ObligationStatus.OPEN, ComponentLegalService.NOT_IMPLEMENTED);
    componentLegalService.deleteComponentObligations(Collections.singletonList(componentObligation.getId()));
  }

  @Test
  public void testGetComponentObligation_ApplicationScope_Unauthenticated() {
    ComponentObligation componentObligation = tempEntity.newComponentObligation(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), app.getId(), "obligationName", null,
        ObligationStatus.OPEN, ComponentLegalService.NOT_IMPLEMENTED);
    assertThrows(UnauthenticatedException.class,
        () -> componentLegalService.getComponentObligation(app.getType(), app.getPublicId(),
            componentObligation.getComponentIdentifier(), componentObligation.getObligationName()));
  }

  @Test
  public void testGetComponentObligation_ApplicationScope_Unauthorized() {
    login();
    ComponentObligation componentObligation = tempEntity.newComponentObligation(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), app.getId(), "obligationName", null,
        ObligationStatus.OPEN, ComponentLegalService.NOT_IMPLEMENTED);
    assertThrows(UnauthorizedException.class,
        () -> componentLegalService.getComponentObligation(app.getType(), app.getPublicId(),
            componentObligation.getComponentIdentifier(), componentObligation.getObligationName()));
  }

  @Test
  public void testGetComponentObligation_ApplicationScope_Authorized() {
    grantLegalReviewerPermission(app.getId());
    ComponentObligation componentObligation = tempEntity.newComponentObligation(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), app.getId(), "obligationName", null,
        ObligationStatus.OPEN, ComponentLegalService.NOT_IMPLEMENTED);
    componentLegalService.getComponentObligation(app.getType(), app.getPublicId(),
        componentObligation.getComponentIdentifier(), componentObligation.getObligationName());
  }

  @Test
  public void testGetComponentObligation_OrganizationScope_Unauthenticated() {
    ComponentObligation componentObligation = tempEntity.newComponentObligation(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), org.getId(), "obligationName", null,
        ObligationStatus.OPEN, ComponentLegalService.NOT_IMPLEMENTED);
    assertThrows(UnauthenticatedException.class,
        () -> componentLegalService.getComponentObligation(org.getType(), org.getPublicId(),
            componentObligation.getComponentIdentifier(), componentObligation.getObligationName()));
  }

  @Test
  public void testGetComponentObligation_OrganizationScope_Unauthorized() {
    login();
    ComponentObligation componentObligation = tempEntity.newComponentObligation(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), org.getId(), "obligationName", null,
        ObligationStatus.OPEN, ComponentLegalService.NOT_IMPLEMENTED);
    assertThrows(UnauthorizedException.class,
        () -> componentLegalService.getComponentObligation(org.getType(), org.getPublicId(),
            componentObligation.getComponentIdentifier(), componentObligation.getObligationName()));
  }

  @Test
  public void testGetComponentObligation_OrganizationScope_Authorized() {
    grantLegalReviewerPermission(org.getId());
    ComponentObligation componentObligation = tempEntity.newComponentObligation(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), org.getId(), "obligationName", null,
        ObligationStatus.OPEN, ComponentLegalService.NOT_IMPLEMENTED);
    componentLegalService.getComponentObligation(org.getType(), org.getPublicId(),
        componentObligation.getComponentIdentifier(), componentObligation.getObligationName());
  }

  @Test
  public void testSaveComponentObligations_Unauthorized_UpdateAtLowerScope() {
    List<ApiLicenseLegalObligationDTO> componentObligationDTOs = createMinimalComponentObligationDTOs();
    ComponentObligation componentObligation = tempEntity.newComponentObligation(
        componentObligationDTOs.get(0).getComponentIdentifier().toComponentIdentifier(), app.getOrganizationId(),
        componentObligationDTOs.get(0).getName(), null,
        componentObligationDTOs.get(0).getStatus(),
        ComponentLegalService.NOT_IMPLEMENTED);
    componentObligationDTOs.get(0).setId(componentObligation.getId());
    grantLegalReviewerPermission(app.getId());
    assertThrows(UnauthorizedException.class, () -> componentLegalService
        .saveComponentObligations(OwnerType.APPLICATION, app.getPublicId(), componentObligationDTOs));
  }

  @Test
  public void testDeleteComponentObligation_Unauthorized_DeleteAtLowerScope() {
    ComponentObligation componentObligation = tempEntity.newComponentObligation(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), app.getOrganizationId(), "obligationName", null,
        ObligationStatus.OPEN, ComponentLegalService.NOT_IMPLEMENTED);
    grantLegalReviewerPermission(app.getId());
    assertThrows(UnauthorizedException.class,
        () -> componentLegalService.deleteComponentObligations(Collections.singletonList(componentObligation.getId())));
  }

  @Test
  public void testGetComponentCopyright_ApplicationScope_Unauthenticated() {
    tempEntity.newComponentCopyright(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"),
        app.getId(), "lch");
    assertThrows(UnauthenticatedException.class, () -> componentLegalService
        .getComponentCopyrightWithHierarchy(app.getType(), app.getPublicId(),
            ComponentIdentifier.createMavenCoordinates("g", "a", "v")));
  }

  @Test
  public void testGetComponentCopyright_ApplicationScope_Unauthorized() {
    login();
    tempEntity.newComponentCopyright(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"),
        app.getId(), "lch");
    assertThrows(UnauthorizedException.class, () -> componentLegalService
        .getComponentCopyrightWithHierarchy(app.getType(), app.getPublicId(),
            ComponentIdentifier.createMavenCoordinates("g", "a", "v")));
  }

  @Test
  public void testGetComponentCopyright_ApplicationScope_Authorized() {
    grantLegalReviewerPermission(app.getId());
    final ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    tempEntity.newComponentCopyright(componentIdentifier,
        app.getId(), "lch");
    componentLegalService
        .getComponentCopyrightWithHierarchy(app.getType(), app.getPublicId(), componentIdentifier);
  }

  @Test
  public void testGetComponentCopyright_OrganizationScope_Unauthenticated() {
    tempEntity.newComponentCopyright(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"),
        org.getId(), "lch");
    assertThrows(UnauthenticatedException.class, () -> componentLegalService
        .getComponentCopyrightWithHierarchy(app.getType(), org.getPublicId(),
            ComponentIdentifier.createMavenCoordinates("g", "a", "v")));
  }

  @Test
  public void testGetComponentCopyright_OrganizationScope_Unauthorized() {
    login();
    tempEntity.newComponentCopyright(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"),
        org.getId(), "lch");
    assertThrows(UnauthorizedException.class, () -> componentLegalService
        .getComponentCopyrightWithHierarchy(org.getType(), org.getPublicId(),
            ComponentIdentifier.createMavenCoordinates("g", "a", "v")));
  }

  @Test
  public void testGetComponentCopyright_OrganizationScope_Authorized() {
    grantLegalReviewerPermission(org.getId());
    final ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    tempEntity.newComponentCopyright(componentIdentifier,
        org.getId(), "lch");
    componentLegalService
        .getComponentCopyrightWithHierarchy(org.getType(), org.getPublicId(), componentIdentifier);
  }

  @Test
  public void testGetComponentLegalFile_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> componentLegalService.getComponentLegalFile(null, null, null, null));
  }

  @Test
  public void testGetComponentLegalFile_Unauthorized_RootOrganization() {
    login();
    assertThrows(UnauthorizedException.class, () -> componentLegalService
        .getComponentLegalFile(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID, null, null));
  }

  @Test
  public void testGetComponentLegalFile_Unauthorized_Organization() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> componentLegalService.getComponentLegalFile(org.getType(), org.getPublicId(), null, null));
  }

  @Test
  public void testGetComponentLegalFile_Unauthorized_Application() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> componentLegalService.getComponentLegalFile(app.getType(), app.getPublicId(), null, null));
  }

  @Test
  public void testGetComponentLegalFile_Authorized_RootOrganization() {
    grantLegalReviewerPermission(Organization.ROOT_ORGANIZATION_ID);
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    componentLegalService
        .getComponentLegalFile(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID, componentIdentifier, null);
    componentLegalService.getComponentLegalFile(org.getType(), org.getPublicId(), componentIdentifier, null);
    componentLegalService.getComponentLegalFile(app.getType(), app.getPublicId(), componentIdentifier, null);
  }

  @Test
  public void testGetComponentLegalFile_Authorized_Organization() {
    grantLegalReviewerPermission(org.getId());
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    assertThatExceptionOfType(UnauthorizedException.class).isThrownBy(() -> componentLegalService
        .getComponentLegalFile(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID, componentIdentifier, null));
    componentLegalService.getComponentLegalFile(org.getType(), org.getPublicId(), componentIdentifier, null);
    componentLegalService.getComponentLegalFile(app.getType(), app.getPublicId(), componentIdentifier, null);
  }

  @Test
  public void testGetComponentLegalFile_Authorized_Application() {
    grantLegalReviewerPermission(app.getId());
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    assertThatExceptionOfType(UnauthorizedException.class).isThrownBy(() -> componentLegalService
        .getComponentLegalFile(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID, componentIdentifier, null));
    assertThatExceptionOfType(UnauthorizedException.class).isThrownBy(
        () -> componentLegalService.getComponentLegalFile(org.getType(), org.getPublicId(), componentIdentifier, null));
    componentLegalService.getComponentLegalFile(app.getType(), app.getPublicId(), componentIdentifier, null);
  }

  private ComponentObligationAttributionDTO buildComponentObligationAttributionDTO() {
    ComponentObligationAttributionDTO componentObligationAttributionDTO = new ComponentObligationAttributionDTO();
    componentObligationAttributionDTO.setComponentIdentifier(
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g", "a", "v")));
    componentObligationAttributionDTO.setContent("content");
    return componentObligationAttributionDTO;
  }

  private List<ApiLicenseLegalObligationDTO> createMinimalComponentObligationDTOs() {
    ApiLicenseLegalObligationDTO componentObligationDTO = new ApiLicenseLegalObligationDTO();
    componentObligationDTO.setComponentIdentifier(
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g", "a", "v")));
    componentObligationDTO.setName("obligationName");
    componentObligationDTO.setStatus(ObligationStatus.OPEN);
    return Lists.newArrayList(componentObligationDTO);
  }
}
