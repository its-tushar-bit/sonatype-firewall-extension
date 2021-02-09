/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentCopyrightDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentObligationAttributionDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.CopyrightOverrideDTO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.legal.ComponentCopyright;
import com.sonatype.insight.brain.model.legal.ComponentLegalPartStatus;
import com.sonatype.insight.brain.model.legal.ComponentObligationAttribution;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import com.google.common.collect.Lists;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class ComponentLegalServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ComponentLegalService componentLegalService;

  @Test(expected = UnauthenticatedException.class)
  public void testSaveComponentCopyright_ApplicationScope_Unauthenticated() {
    componentLegalService
        .saveComponentCopyright(OwnerType.APPLICATION, app.getPublicId(), buildComponentCopyrightDTO());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSaveComponentCopyright_OrganizationScope_Unauthenticated() {
    componentLegalService
        .saveComponentCopyright(OwnerType.ORGANIZATION, org.getPublicId(), buildComponentCopyrightDTO());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSaveComponentCopyright_RootScope_Unauthenticated() {
    componentLegalService
        .saveComponentCopyright(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID,
            buildComponentCopyrightDTO());
  }

  @Test(expected = UnauthorizedException.class)
  public void testSaveComponentCopyright_ApplicationScope_Unauthorized() {
    login();
    componentLegalService
        .saveComponentCopyright(OwnerType.APPLICATION, app.getPublicId(), buildComponentCopyrightDTO());
  }

  @Test
  public void testSaveComponentCopyright_ApplicationScope_Authorized() {
    grantLegalReviewerPermission(app.getId());
    componentLegalService
        .saveComponentCopyright(OwnerType.APPLICATION, app.getPublicId(), buildComponentCopyrightDTO());
  }

  @Test(expected = UnauthorizedException.class)
  public void testSaveComponentCopyright_OrganizationScope_Unauthorized() {
    login();
    componentLegalService
        .saveComponentCopyright(OwnerType.ORGANIZATION, org.getPublicId(), buildComponentCopyrightDTO());
  }

  @Test
  public void testSaveComponentCopyright_OrganizationScope_Authorized() {
    grantLegalReviewerPermission(org.getId());
    componentLegalService
        .saveComponentCopyright(OwnerType.APPLICATION, app.getPublicId(), buildComponentCopyrightDTO());
    componentLegalService
        .saveComponentCopyright(OwnerType.ORGANIZATION, org.getPublicId(), buildComponentCopyrightDTO());
  }

  @Test(expected = UnauthorizedException.class)
  public void testSaveComponentCopyright_RootScope_Unauthorized() {
    login();
    componentLegalService
        .saveComponentCopyright(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID,
            buildComponentCopyrightDTO());
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

  @Test(expected = UnauthorizedException.class)
  public void testSaveComponentCopyright_Unauthorized_UpdateAtLowerScope() {
    ComponentCopyrightDTO componentCopyrightDTO = buildComponentCopyrightDTO();
    ComponentCopyright componentCopyright = tempEntity.newComponentCopyright(
        componentCopyrightDTO.getComponentIdentifier().toComponentIdentifier(), org.getId(),
        ComponentLegalService.NOT_IMPLEMENTED);
    componentCopyrightDTO.setId(componentCopyright.getId());
    grantLegalReviewerPermission(app.getId());
    componentLegalService.saveComponentCopyright(OwnerType.APPLICATION, app.getPublicId(), componentCopyrightDTO);
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
                ComponentLegalPartStatus.ENABLED
            ),
            new CopyrightOverrideDTO(
                null,
                "originalContentHash2",
                "content2",
                ComponentLegalPartStatus.DISABLED
            )
        ),
        null,
        null
    );
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSaveComponentObligationAttribution_ApplicationScope_Unauthenticated() {
    componentLegalService.saveComponentObligationAttribution(OwnerType.APPLICATION, app.getPublicId(),
        buildComponentObligationAttributionDTO());
  }

  @Test(expected = UnauthorizedException.class)
  public void testSaveComponentObligationAttribution_ApplicationScope_Unauthorized() {
    login();
    componentLegalService.saveComponentObligationAttribution(OwnerType.APPLICATION, app.getPublicId(),
        buildComponentObligationAttributionDTO());
  }

  @Test
  public void testSaveComponentObligationAttribution_ApplicationScope_Authorized() {
    grantLegalReviewerPermission(app.getId());
    componentLegalService.saveComponentObligationAttribution(OwnerType.APPLICATION, app.getPublicId(),
        buildComponentObligationAttributionDTO());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSaveComponentObligationAttribution_OrganizationScope_Unauthenticated() {
    componentLegalService.saveComponentObligationAttribution(OwnerType.ORGANIZATION, org.getPublicId(),
        buildComponentObligationAttributionDTO());
  }

  @Test(expected = UnauthorizedException.class)
  public void testSaveComponentObligationAttribution_OrganizationScope_Unauthorized() {
    login();
    componentLegalService.saveComponentObligationAttribution(OwnerType.ORGANIZATION, org.getPublicId(),
        buildComponentObligationAttributionDTO());
  }

  @Test
  public void testSaveComponentObligationAttribution_OrganizationScope_Authorized() {
    grantLegalReviewerPermission(org.getId());
    componentLegalService.saveComponentObligationAttribution(OwnerType.ORGANIZATION, org.getPublicId(),
        buildComponentObligationAttributionDTO());
    componentLegalService.saveComponentObligationAttribution(OwnerType.APPLICATION, app.getPublicId(),
        buildComponentObligationAttributionDTO());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSaveComponentObligationAttribution_RootOrganizationScope_Unauthenticated() {
    componentLegalService.saveComponentObligationAttribution(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID,
        buildComponentObligationAttributionDTO());
  }

  @Test(expected = UnauthorizedException.class)
  public void testSaveComponentObligationAttribution_RootOrganizationScope_Unauthorized() {
    login();
    componentLegalService.saveComponentObligationAttribution(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID,
        buildComponentObligationAttributionDTO());
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

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteComponentObligationAttribution_ApplicationScope_Unauthenticated() {
    ComponentObligationAttribution componentObligationAttribution = tempEntity.newComponentObligationAttribution(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), app.getId(), null, "content",
        ComponentLegalService.NOT_IMPLEMENTED);
    componentLegalService.deleteComponentObligationAttribution(componentObligationAttribution.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteComponentObligationAttribution_ApplicationScope_Unauthorized() {
    ComponentObligationAttribution componentObligationAttribution = tempEntity.newComponentObligationAttribution(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), app.getId(), null, "content",
        ComponentLegalService.NOT_IMPLEMENTED);
    login();
    componentLegalService.deleteComponentObligationAttribution(componentObligationAttribution.getId());
  }

  @Test
  public void testDeleteComponentObligationAttribution_ApplicationScope_Authorized() {
    ComponentObligationAttribution componentObligationAttribution = tempEntity.newComponentObligationAttribution(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), app.getId(), null, "content",
        ComponentLegalService.NOT_IMPLEMENTED);
    grantLegalReviewerPermission(app.getId());
    componentLegalService.deleteComponentObligationAttribution(componentObligationAttribution.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteComponentObligationAttribution_OrganizationScope_Unauthenticated() {
    ComponentObligationAttribution componentObligationAttribution = tempEntity.newComponentObligationAttribution(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), org.getId(), null, "content",
        ComponentLegalService.NOT_IMPLEMENTED);
    componentLegalService.deleteComponentObligationAttribution(componentObligationAttribution.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteComponentObligationAttribution_OrganizationScope_Unauthorized() {
    ComponentObligationAttribution componentObligationAttribution = tempEntity.newComponentObligationAttribution(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), org.getId(), null, "content",
        ComponentLegalService.NOT_IMPLEMENTED);
    login();
    componentLegalService.deleteComponentObligationAttribution(componentObligationAttribution.getId());
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

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteComponentObligationAttribution_RootOrganizationScope_Unauthenticated() {
    ComponentObligationAttribution componentObligationAttribution = tempEntity.newComponentObligationAttribution(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), Organization.ROOT_ORGANIZATION_ID, null, "content",
        ComponentLegalService.NOT_IMPLEMENTED);
    componentLegalService.deleteComponentObligationAttribution(componentObligationAttribution.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteComponentObligationAttribution_RootOrganizationScope_Unauthorized() {
    ComponentObligationAttribution componentObligationAttribution = tempEntity.newComponentObligationAttribution(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), Organization.ROOT_ORGANIZATION_ID, null, "content",
        ComponentLegalService.NOT_IMPLEMENTED);
    login();
    componentLegalService.deleteComponentObligationAttribution(componentObligationAttribution.getId());
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

  @Test(expected = UnauthorizedException.class)
  public void testSaveComponentObligationAttribution_Unauthorized_UpdateAtLowerScope() {
    ComponentObligationAttributionDTO componentObligationAttributionDTO = buildComponentObligationAttributionDTO();
    ComponentObligationAttribution componentObligationAttribution = tempEntity.newComponentObligationAttribution(
        componentObligationAttributionDTO.getComponentIdentifier().toComponentIdentifier(), org.getId(), null,
        componentObligationAttributionDTO.getContent(), ComponentLegalService.NOT_IMPLEMENTED);
    componentObligationAttributionDTO.setId(componentObligationAttribution.getId());
    grantLegalReviewerPermission(app.getId());
    componentLegalService.saveComponentObligationAttribution(OwnerType.APPLICATION, app.getPublicId(),
        componentObligationAttributionDTO);
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteComponentObligationAttribution_Unauthorized_DeleteAtLowerScope() {
    ComponentObligationAttributionDTO componentObligationAttributionDTO = buildComponentObligationAttributionDTO();
    ComponentObligationAttribution componentObligationAttribution = tempEntity.newComponentObligationAttribution(
        componentObligationAttributionDTO.getComponentIdentifier().toComponentIdentifier(), org.getId(), null,
        componentObligationAttributionDTO.getContent(), ComponentLegalService.NOT_IMPLEMENTED);
    grantLegalReviewerPermission(app.getId());
    componentLegalService.deleteComponentObligationAttribution(componentObligationAttribution.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetComponentObligationAttribution_Unauthenticated() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    componentLegalService
        .getComponentObligationAttributions(app.getType(), app.getPublicId(), componentIdentifier, "obligationName");
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetComponentObligationAttribution_Unauthorized() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    login();
    componentLegalService
        .getComponentObligationAttributions(app.getType(), app.getPublicId(), componentIdentifier, "obligationName");
  }

  @Test
  public void testGetComponentObligationAttribution_Authorized() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    grantLegalReviewerPermission(app.getId());
    componentLegalService
        .getComponentObligationAttributions(app.getType(), app.getPublicId(), componentIdentifier, "obligationName");
  }

  private ComponentObligationAttributionDTO buildComponentObligationAttributionDTO() {
    ComponentObligationAttributionDTO componentObligationAttributionDTO = new ComponentObligationAttributionDTO();
    componentObligationAttributionDTO.setComponentIdentifier(
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g", "a", "v")));
    componentObligationAttributionDTO.setContent("content");
    return componentObligationAttributionDTO;
  }
}
