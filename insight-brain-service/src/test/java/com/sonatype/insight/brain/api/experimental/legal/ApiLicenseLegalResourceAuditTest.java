/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.util.Arrays;
import java.util.Collections;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalObligationDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentCopyrightDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentLegalFileDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentObligationAttributionDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.CopyrightOverrideDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.LegalFileOverrideDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.legal.ComponentCopyright;
import com.sonatype.insight.brain.model.legal.ComponentLegalFile;
import com.sonatype.insight.brain.model.legal.ComponentLegalPartStatus;
import com.sonatype.insight.brain.model.legal.ComponentObligation;
import com.sonatype.insight.brain.model.legal.ComponentObligationAttribution;
import com.sonatype.insight.brain.model.legal.LegalFileType;
import com.sonatype.insight.brain.model.legal.ObligationStatus;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.assertj.core.util.Lists;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
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
  public void testSaveComponentLegalFile_Notices() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentLegalFileDTO componentLegalFileDTO = new ComponentLegalFileDTO();
    componentLegalFileDTO
        .setComponentIdentifier(ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier));
    componentLegalFileDTO.setLegalFileType(LegalFileType.NOTICE);
    LegalFileOverrideDTO notice1 =
        new LegalFileOverrideDTO(null, "notice1", ComponentLegalPartStatus.ENABLED);
    LegalFileOverrideDTO notice2 =
        new LegalFileOverrideDTO(null, "notice2", ComponentLegalPartStatus.ENABLED);
    LegalFileOverrideDTO notice3 =
        new LegalFileOverrideDTO(null, "notice3", ComponentLegalPartStatus.DISABLED);
    componentLegalFileDTO.setLegalFileOverrides(Arrays.asList(notice1, notice2, notice3));

    HttpResponse response = restRequest().path(ApiLicenseLegalResource.COMPONENT_LEGAL_FILE_PATH)
        .parameter(app.getType().toString(), app.getPublicId())
        .body(componentLegalFileDTO)
        .post();

    ComponentLegalFileDTO resultDto = response.getBody(ComponentLegalFileDTO.class);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_COMPONENT_LEGAL_FILE, null);
    assertApplicationData(auditDTO, app);
    assertCustomObject(auditDTO, "componentIdentifier", componentIdentifier);
    assertCustomData(auditDTO, "notices", Arrays
        .asList(resultDto.getLegalFileOverrides().get(0).getId(), resultDto.getLegalFileOverrides().get(1).getId()));
  }

  @Test
  public void testSaveComponentLegalFile_Licenses() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentLegalFileDTO componentLegalFileDTO = new ComponentLegalFileDTO();
    componentLegalFileDTO.setLegalFileType(LegalFileType.LICENSE);
    componentLegalFileDTO
        .setComponentIdentifier(ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier));
    LegalFileOverrideDTO license1 =
        new LegalFileOverrideDTO(null, "license1", ComponentLegalPartStatus.ENABLED);
    LegalFileOverrideDTO license2 =
        new LegalFileOverrideDTO(null, "license2", ComponentLegalPartStatus.ENABLED);
    LegalFileOverrideDTO license3 =
        new LegalFileOverrideDTO(null, "license3", ComponentLegalPartStatus.DISABLED);
    componentLegalFileDTO.setLegalFileOverrides(Arrays.asList(license1, license2, license3));

    HttpResponse response = restRequest().path(ApiLicenseLegalResource.COMPONENT_LEGAL_FILE_PATH)
        .parameter(app.getType().toString(), app.getPublicId())
        .body(componentLegalFileDTO)
        .post();

    ComponentLegalFileDTO resultDto = response.getBody(ComponentLegalFileDTO.class);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_COMPONENT_LEGAL_FILE, null);
    assertApplicationData(auditDTO, app);
    assertCustomObject(auditDTO, "componentIdentifier", componentIdentifier);
    assertCustomData(auditDTO, "licenses", Arrays
        .asList(resultDto.getLegalFileOverrides().get(0).getId(), resultDto.getLegalFileOverrides().get(1).getId()));
  }

  @Test
  public void testSaveComponentLegalFile_Unauthorized() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentLegalFile componentLegalFile =
        tempEntity.newComponentLegalFile(componentIdentifier, app.getId(), LegalFileType.NOTICE, "legalContentHash");

    restRequest().path(ApiLicenseLegalResource.COMPONENT_LEGAL_FILE_PATH)
        .parameter(app.getType().toString(), app.getPublicId())
        .body(new ComponentLegalFileDTO(componentLegalFile, Collections.emptyList()))
        .with(unauthorizedUser())
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_COMPONENT_LEGAL_FILE, "unauthorized");
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

  @Test
  public void testSaveComponentObligation_Create() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    ApiLicenseLegalObligationDTO bodyDto = new ApiLicenseLegalObligationDTO();
    bodyDto.setComponentIdentifier(ApiComponentIdentifierDTOV2.fromComponentIdentifier(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v")));
    bodyDto.setName("obligationName");
    bodyDto.setStatus(ObligationStatus.OPEN);

    restRequest()
        .path(ApiLicenseLegalResource.COMPONENT_OBLIGATION_PATH)
        .parameter(application.getType(), application.getPublicId())
        .body(bodyDto)
        .post();

    assertAuditLog(AuditEvent.SAVE_COMPONENT_OBLIGATIONS, null);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_COMPONENT_OBLIGATION, null);
    assertComponentObligationData(auditDTO, application, bodyDto);
  }

  @Test
  public void testSaveComponentObligation_Create_Unauthorized() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    ApiLicenseLegalObligationDTO bodyDto = new ApiLicenseLegalObligationDTO();
    bodyDto.setComponentIdentifier(ApiComponentIdentifierDTOV2.fromComponentIdentifier(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v")));
    bodyDto.setName("obligationName");
    bodyDto.setStatus(ObligationStatus.OPEN);

    restRequest()
        .path(ApiLicenseLegalResource.COMPONENT_OBLIGATION_PATH)
        .parameter(application.getType(), application.getPublicId())
        .body(bodyDto)
        .with(unauthorizedUser())
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.SAVE_COMPONENT_OBLIGATIONS, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  @Test
  public void testSaveComponentObligation_Update() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    ComponentObligation componentObligation = tempEntity.newComponentObligation(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), application.getId(), "obligationName", null,
        ObligationStatus.OPEN, ComponentLegalService.NOT_IMPLEMENTED);
    ApiLicenseLegalObligationDTO bodyDto = new ApiLicenseLegalObligationDTO(componentObligation);
    bodyDto.setComment("updatedComment");

    restRequest()
        .path(ApiLicenseLegalResource.COMPONENT_OBLIGATION_PATH)
        .parameter(application.getType(), application.getPublicId())
        .body(bodyDto)
        .post();

    assertAuditLog(AuditEvent.SAVE_COMPONENT_OBLIGATIONS, null);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_COMPONENT_OBLIGATION, null);
    assertComponentObligationData(auditDTO, application, bodyDto);
  }

  @Test
  public void testSaveComponentObligation_Update_Unauthorized() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    ComponentObligation componentObligation = tempEntity.newComponentObligation(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), application.getId(), "obligationName", null,
        ObligationStatus.OPEN, ComponentLegalService.NOT_IMPLEMENTED);
    ApiLicenseLegalObligationDTO bodyDto = new ApiLicenseLegalObligationDTO(componentObligation);
    bodyDto.setComment("updatedComment");

    restRequest()
        .path(ApiLicenseLegalResource.COMPONENT_OBLIGATION_PATH)
        .parameter(application.getType(), application.getPublicId())
        .body(bodyDto)
        .with(unauthorizedUser())
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.SAVE_COMPONENT_OBLIGATIONS, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  @Test
  public void testDeleteComponentObligation() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    ComponentObligation componentObligation = tempEntity.newComponentObligation(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), application.getId(), "obligationName", null,
        ObligationStatus.OPEN, ComponentLegalService.NOT_IMPLEMENTED);

    restRequest()
        .path(ApiLicenseLegalResource.COMPONENT_OBLIGATION_DELETE_PATH)
        .query("componentObligationId", componentObligation.getId())
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_COMPONENT_OBLIGATION, null);
    assertComponentObligationData(auditDTO, application, new ApiLicenseLegalObligationDTO(componentObligation));
  }

  @Test
  public void testDeleteComponentObligation_Unauthorized() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    ComponentObligation componentObligation = tempEntity.newComponentObligation(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), application.getId(), "obligationName", null,
        ObligationStatus.OPEN, ComponentLegalService.NOT_IMPLEMENTED);

    restRequest()
        .path(ApiLicenseLegalResource.COMPONENT_OBLIGATION_DELETE_PATH)
        .query("componentObligationId", componentObligation.getId())
        .with(unauthorizedUser())
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_COMPONENT_OBLIGATION, "unauthorized");
    assertComponentObligationData(auditDTO, application, new ApiLicenseLegalObligationDTO(componentObligation));
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

  private void assertComponentObligationData(
      AuditDTO actual,
      Owner expectedOwner,
      ApiLicenseLegalObligationDTO expected)
  {
    assertOwnerData(actual, expectedOwner);
    assertCustomObject(actual, "componentIdentifier", expected.getComponentIdentifier().toComponentIdentifier());
    assertCustomData(actual, "obligationName", expected.getName());
    assertCustomData(actual, "obligationStatus", expected.getStatus().toString());
    assertCustomData(actual, "comment", expected.getComment());
  }
}
