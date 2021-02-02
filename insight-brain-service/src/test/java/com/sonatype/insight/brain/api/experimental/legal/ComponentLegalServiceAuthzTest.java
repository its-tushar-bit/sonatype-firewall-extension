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
import com.sonatype.insight.brain.api.v2.dto.legal.CopyrightOverrideDTO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.legal.ComponentLegalPartStatus;
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
}
