/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentCopyrightDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.CopyrightOverrideDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.legal.ComponentCopyright;
import com.sonatype.insight.brain.model.legal.ComponentLegalPartStatus;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.assertj.core.util.Lists;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiLicenseLegalResourceAuditTest
    extends AbstractAuditTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.LICENSE_LEGAL_RESOURCE_PATH);
  }

  @Test
  public void testSaveNewComponentCopyright() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    ComponentCopyrightDTO componentCopyrightDTO = new ComponentCopyrightDTO();
    componentCopyrightDTO
        .setComponentIdentifier(ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier));

    CopyrightOverrideDTO copyrightOverrideDTOEnabled = new CopyrightOverrideDTO();
    copyrightOverrideDTOEnabled.setContent("content");
    copyrightOverrideDTOEnabled.setOriginalContentHash("originalHash");
    copyrightOverrideDTOEnabled.setStatus(ComponentLegalPartStatus.ENABLED);

    CopyrightOverrideDTO copyrightOverrideDTODisabled = new CopyrightOverrideDTO();
    copyrightOverrideDTODisabled.setContent("unwantedContent");
    copyrightOverrideDTODisabled.setOriginalContentHash("originalHash");
    copyrightOverrideDTODisabled.setStatus(ComponentLegalPartStatus.DISABLED);

    componentCopyrightDTO
        .setCopyrightOverrides(Lists.newArrayList(copyrightOverrideDTOEnabled, copyrightOverrideDTODisabled));

    HttpResponse response =
        restRequest().path(ApiLicenseLegalResource.COMPONENT_COPYRIGHT_PATH)
            .parameter(app.getType().toString(), app.getPublicId())
            .body(componentCopyrightDTO)
            .post();
    assertResponseStatus(200, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_COMPONENT_COPYRIGHT, null);
    assertApplicationData(auditDTO, app);
    assertThat(auditDTO.data.get("copyrights")).isEqualTo(Lists.newArrayList("content"));
    assertCustomObject(auditDTO, "componentIdentifier", componentIdentifier);
  }

  @Test
  public void testSaveExistingComponentCopyright() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    ComponentCopyright componentCopyright =
        tempEntity.newComponentCopyright(componentIdentifier, app.getId(), "legalContentHash");
    CopyrightOverrideDTO copyrightOverrideDTO = CopyrightOverrideDTO.fromCopyrightOverride(
        tempEntity.newCopyrightOverride("original", "hash", "content", ComponentLegalPartStatus.ENABLED,
            componentCopyright.getId()));
    copyrightOverrideDTO.setContent("updated content");

    HttpResponse response =
        restRequest().path(ApiLicenseLegalResource.COMPONENT_COPYRIGHT_PATH)
            .parameter(app.getType().toString(), app.getPublicId())
            .body(
                ComponentCopyrightDTO
                    .fromComponentCopyright(componentCopyright,
                        Lists.newArrayList(copyrightOverrideDTO)))
            .post();
    assertResponseStatus(200, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_COMPONENT_COPYRIGHT, null);
    assertApplicationData(auditDTO, app);
    assertThat(auditDTO.data.get("copyrights")).isEqualTo(Lists.newArrayList("updated content"));
    assertCustomObject(auditDTO, "componentIdentifier", componentIdentifier);
  }

  @Test
  public void testSaveComponentCopyright_Unauthorized() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    ComponentCopyright componentCopyright =
        tempEntity.newComponentCopyright(componentIdentifier, app.getPublicId(), "legalContentHash1");

    restRequest().path(ApiLicenseLegalResource.COMPONENT_COPYRIGHT_PATH)
        .parameter(app.getType().toString(), app.getPublicId())
        .body(
            ComponentCopyrightDTO
                .fromComponentCopyright(componentCopyright, Lists.newArrayList()))
        .with(unauthorizedUser())
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_COMPONENT_COPYRIGHT, "unauthorized");
    assertApplicationData(auditDTO, app);
  }
}
