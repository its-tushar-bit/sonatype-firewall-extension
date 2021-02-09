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
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentObligationAttributionDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.CopyrightOverrideDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.legal.ComponentCopyright;
import com.sonatype.insight.brain.model.legal.ComponentLegalPartStatus;
import com.sonatype.insight.brain.model.legal.ComponentObligationAttribution;
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

  @Test
  public void testSaveComponentObligationAttribution_Create() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    ComponentObligationAttributionDTO bodyDto = new ComponentObligationAttributionDTO();
    bodyDto.setComponentIdentifier(
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g", "a", "v")));
    bodyDto.setContent("content");

    restRequest().path(ApiLicenseLegalResource.COMPONENT_OBLIGATION_ATTRIBUTION_PATH)
        .parameter(application.getType(), application.getPublicId())
        .body(bodyDto)
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_COMPONENT_OBLIGATION_ATTRIBUTION, null);
    assertComponentObligationAttributionData(auditDTO, application, bodyDto);
  }

  @Test
  public void testSaveComponentObligationAttribution_Create_Unauthorized() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    ComponentObligationAttributionDTO bodyDto = new ComponentObligationAttributionDTO();
    bodyDto.setComponentIdentifier(
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g", "a", "v")));
    bodyDto.setContent("content");

    restRequest().path(ApiLicenseLegalResource.COMPONENT_OBLIGATION_ATTRIBUTION_PATH)
        .parameter(application.getType(), application.getPublicId())
        .body(bodyDto)
        .with(unauthorizedUser())
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_COMPONENT_OBLIGATION_ATTRIBUTION, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  @Test
  public void testSaveComponentObligationAttribution_Update() throws Exception {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    String obligationName = "obligationName1";
    String content = "content1";
    ComponentObligationAttribution oldComponentObligationAttribution = tempEntity
        .newComponentObligationAttribution(componentIdentifier, application.getOrganizationId(), obligationName,
            content, ComponentLegalService.NOT_IMPLEMENTED);
    ComponentObligationAttributionDTO bodyDto = new ComponentObligationAttributionDTO();
    bodyDto.setId(oldComponentObligationAttribution.getId());
    bodyDto.setComponentIdentifier(ApiComponentIdentifierDTOV2.fromComponentIdentifier(
        ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2")));
    bodyDto.setObligationName("obligationName2");
    bodyDto.setContent("content2");

    restRequest().path(ApiLicenseLegalResource.COMPONENT_OBLIGATION_ATTRIBUTION_PATH)
        .parameter(organization.getType(), organization.getPublicId())
        .body(bodyDto)
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_COMPONENT_OBLIGATION_ATTRIBUTION, null);
    assertComponentObligationAttributionData(auditDTO, organization, bodyDto);
  }

  @Test
  public void testSaveComponentObligationAttribution_Update_Unauthorized() throws Exception {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    String obligationName = "obligationName1";
    String content = "content1";
    ComponentObligationAttribution oldComponentObligationAttribution = tempEntity
        .newComponentObligationAttribution(componentIdentifier, application.getOrganizationId(), obligationName,
            content, ComponentLegalService.NOT_IMPLEMENTED);
    ComponentObligationAttributionDTO bodyDto = new ComponentObligationAttributionDTO();
    bodyDto.setId(oldComponentObligationAttribution.getId());
    bodyDto.setComponentIdentifier(ApiComponentIdentifierDTOV2.fromComponentIdentifier(
        ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2")));
    bodyDto.setObligationName("obligationName2");
    bodyDto.setContent("content2");

    restRequest().path(ApiLicenseLegalResource.COMPONENT_OBLIGATION_ATTRIBUTION_PATH)
        .parameter(organization.getType(), organization.getPublicId())
        .body(bodyDto)
        .with(unauthorizedUser())
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_COMPONENT_OBLIGATION_ATTRIBUTION, "unauthorized");
    assertOrganizationData(auditDTO, organization);
  }

  @Test
  public void testDeleteComponentObligationAttribution() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    ComponentObligationAttribution componentObligationAttribution = tempEntity.newComponentObligationAttribution(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), application.getId(), "obligationName", "content",
        ComponentLegalService.NOT_IMPLEMENTED);

    restRequest().path(ApiLicenseLegalResource.COMPONENT_OBLIGATION_ATTRIBUTION_DELETE_PATH)
        .parameter(componentObligationAttribution.getId())
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_COMPONENT_OBLIGATION_ATTRIBUTION, null);
    assertComponentObligationAttributionData(auditDTO, application,
        new ComponentObligationAttributionDTO(componentObligationAttribution));
  }

  @Test
  public void testDeleteComponentObligationAttribution_Unauthorized() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    ComponentObligationAttribution componentObligationAttribution = tempEntity.newComponentObligationAttribution(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), application.getId(), "obligationName", "content",
        ComponentLegalService.NOT_IMPLEMENTED);

    restRequest().path(ApiLicenseLegalResource.COMPONENT_OBLIGATION_ATTRIBUTION_DELETE_PATH)
        .parameter(componentObligationAttribution.getId())
        .with(unauthorizedUser())
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_COMPONENT_OBLIGATION_ATTRIBUTION, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  private void assertComponentObligationAttributionData(
      AuditDTO actual,
      Owner expectedOwner,
      ComponentObligationAttributionDTO expected)
  {
    assertOwnerData(actual, expectedOwner);
    assertCustomObject(actual, "componentIdentifier", expected.getComponentIdentifier().toComponentIdentifier());
    assertCustomData(actual, "obligationName", expected.getObligationName());
    assertCustomData(actual, "content", expected.getContent());
  }
}
