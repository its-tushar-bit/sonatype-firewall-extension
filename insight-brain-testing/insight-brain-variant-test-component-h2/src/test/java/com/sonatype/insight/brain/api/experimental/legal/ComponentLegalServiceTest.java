/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.InvalidComponentIdentifierException;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalObligationDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentCopyrightDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentCopyrightWithOwnerDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentLegalFileDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentObligationAttributionDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentSourceLinkDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.CopyrightOverrideDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.LegalFileOverrideDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.LegalSourceLinkDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.SourceLinkOverrideDTO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentCopyrightDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentLegalFileDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentObligationAttributionDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentObligationDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentSourceLinkDAO;
import com.sonatype.insight.brain.dataaccess.legal.CopyrightOverrideDAO;
import com.sonatype.insight.brain.dataaccess.legal.LegalFileOverrideDAO;
import com.sonatype.insight.brain.dataaccess.legal.SourceLinkOverrideDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.legal.ComponentCopyright;
import com.sonatype.insight.brain.model.legal.ComponentLegalFile;
import com.sonatype.insight.brain.model.legal.ComponentLegalPartStatus;
import com.sonatype.insight.brain.model.legal.ComponentObligation;
import com.sonatype.insight.brain.model.legal.ComponentObligationAttribution;
import com.sonatype.insight.brain.model.legal.ComponentSourceLink;
import com.sonatype.insight.brain.model.legal.CopyrightOverride;
import com.sonatype.insight.brain.model.legal.LegalFileOverride;
import com.sonatype.insight.brain.model.legal.LegalFileType;
import com.sonatype.insight.brain.model.legal.ObligationStatus;
import com.sonatype.insight.brain.model.legal.SourceLinkOverride;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.model.LicensedFeature;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class ComponentLegalServiceTest
    extends AbstractComponentH2Test
{
  @Inject
  private ComponentLegalService componentLegalService;

  @Inject
  private ComponentCopyrightDAO componentCopyrightDAO;

  @Inject
  private ComponentObligationDAO componentObligationDAO;

  @Inject
  private ComponentObligationAttributionDAO componentObligationAttributionDAO;

  @Inject
  private ComponentLegalFileDAO componentLegalFileDAO;

  @Inject
  private LegalFileOverrideDAO legalFileOverrideDAO;

  @Inject
  private CopyrightOverrideDAO copyrightOverrideDAO;

  @Inject
  private TestProductLicense testProductLicense;

  @Inject
  private ComponentSourceLinkDAO componentSourceLinkDAO;

  @Inject
  private SourceLinkOverrideDAO sourceLinkOverrideDAO;

  @Test
  public void testSaveComponentCopyright_Unlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.ADVANCED_LEGAL_PACK);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> componentLegalService.saveComponentCopyright(null, null, null));
  }

  @Test
  public void testSaveNewComponentCopyright() {
    Application application = tempEntity.newApplicationWithParent();
    ApiComponentIdentifierDTOV2 componentIdentifier = ApiComponentIdentifierDTOV2
        .fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"));

    ComponentCopyrightDTO componentCopyrightDTO = new ComponentCopyrightDTO(
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
                ComponentLegalPartStatus.DISABLED),
            new CopyrightOverrideDTO(
                null,
                null,
                null,
                ComponentLegalPartStatus.ENABLED)),
        null,
        null);

    ComponentCopyrightDTO returnedComponentCopyrightDTO =
        componentLegalService
            .saveComponentCopyright(OwnerType.APPLICATION, application.getPublicId(), componentCopyrightDTO);

    assertThat(returnedComponentCopyrightDTO.getId()).isNotNull();
    assertThat(returnedComponentCopyrightDTO.getCopyrightOverrides()).hasSize(2);
    returnedComponentCopyrightDTO.getCopyrightOverrides().forEach(co -> assertThat(co.getId()).isNotNull());
    assertThat(returnedComponentCopyrightDTO.getLastUpdatedAt()).isNotNull();
    assertThat(returnedComponentCopyrightDTO.getLastUpdatedByUsername()).isEqualTo(USERNAME);
    assertThat(returnedComponentCopyrightDTO.getComponentIdentifier()).usingRecursiveComparison()
        .isEqualTo(componentIdentifier);

    CopyrightOverrideDTO copyrightOverrideDTO0 = returnedComponentCopyrightDTO.getCopyrightOverrides().get(0);
    CopyrightOverrideDTO copyrightOverrideDTO1 = returnedComponentCopyrightDTO.getCopyrightOverrides().get(1);

    assertThat(copyrightOverrideDTO0.getOriginalContentHash()).isEqualTo("originalContentHash");
    assertThat(copyrightOverrideDTO0.getContent()).isEqualTo("content");
    assertThat(copyrightOverrideDTO0.getStatus()).isEqualTo(ComponentLegalPartStatus.ENABLED);

    assertThat(copyrightOverrideDTO1.getOriginalContentHash()).isEqualTo("originalContentHash2");
    assertThat(copyrightOverrideDTO1.getContent()).isEqualTo("content2");
    assertThat(copyrightOverrideDTO1.getStatus()).isEqualTo(ComponentLegalPartStatus.DISABLED);
  }

  @Test
  public void testSaveNewComponentCopyright_PackageURL() {
    Application application = tempEntity.newApplicationWithParent();

    ComponentCopyrightDTO componentCopyrightDTO = new ComponentCopyrightDTO();
    componentCopyrightDTO.setCopyrightOverrides(Lists.newArrayList(new CopyrightOverrideDTO(
        null,
        "originalContentHash",
        "content",
        ComponentLegalPartStatus.ENABLED),
        new CopyrightOverrideDTO(
            null,
            "originalContentHash2",
            "content2",
            ComponentLegalPartStatus.DISABLED),
        new CopyrightOverrideDTO(
            null,
            null,
            null,
            ComponentLegalPartStatus.ENABLED)));
    String packageURL = "pkg:maven/g1/a1@v1";
    componentCopyrightDTO.setPackageUrl(packageURL);
    ApiComponentIdentifierDTOV2 componentIdentifier = ApiComponentIdentifierDTOV2
        .fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"));

    ComponentCopyrightDTO returnedComponentCopyrightDTO =
        componentLegalService
            .saveComponentCopyright(OwnerType.APPLICATION, application.getPublicId(), componentCopyrightDTO);

    assertThat(returnedComponentCopyrightDTO.getId()).isNotNull();
    assertThat(returnedComponentCopyrightDTO.getCopyrightOverrides()).hasSize(2);
    returnedComponentCopyrightDTO.getCopyrightOverrides().forEach(co -> assertThat(co.getId()).isNotNull());
    assertThat(returnedComponentCopyrightDTO.getLastUpdatedAt()).isNotNull();
    assertThat(returnedComponentCopyrightDTO.getLastUpdatedByUsername()).isEqualTo(USERNAME);
    assertThat(returnedComponentCopyrightDTO.getComponentIdentifier()).usingRecursiveComparison()
        .isEqualTo(componentIdentifier);

    assertThat(returnedComponentCopyrightDTO.getPackageUrl()).isEqualTo(packageURL);
    CopyrightOverrideDTO copyrightOverrideDTO0 = returnedComponentCopyrightDTO.getCopyrightOverrides().get(0);
    CopyrightOverrideDTO copyrightOverrideDTO1 = returnedComponentCopyrightDTO.getCopyrightOverrides().get(1);

    assertThat(copyrightOverrideDTO0.getOriginalContentHash()).isEqualTo("originalContentHash");
    assertThat(copyrightOverrideDTO0.getContent()).isEqualTo("content");
    assertThat(copyrightOverrideDTO0.getStatus()).isEqualTo(ComponentLegalPartStatus.ENABLED);

    assertThat(copyrightOverrideDTO1.getOriginalContentHash()).isEqualTo("originalContentHash2");
    assertThat(copyrightOverrideDTO1.getContent()).isEqualTo("content2");
    assertThat(copyrightOverrideDTO1.getStatus()).isEqualTo(ComponentLegalPartStatus.DISABLED);
  }

  @Test
  public void testUpdatedExistingComponentCopyright() {
    Application application = tempEntity.newApplicationWithParent();
    Organization organization = tempEntity.newOrganization();

    ApiComponentIdentifierDTOV2 componentIdentifier = ApiComponentIdentifierDTOV2
        .fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"));

    ComponentCopyrightDTO componentCopyrightDTO = new ComponentCopyrightDTO(
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

    // Persist original componentCopyright
    ComponentCopyrightDTO existingComponentCopyright =
        componentLegalService
            .saveComponentCopyright(OwnerType.APPLICATION, application.getPublicId(), componentCopyrightDTO);

    // Modify certain properties
    existingComponentCopyright.getCopyrightOverrides().get(0).setContent("updated content");
    existingComponentCopyright.getCopyrightOverrides()
        .add(
            new CopyrightOverrideDTO(
                null,
                null,
                "content3",
                ComponentLegalPartStatus.ENABLED));
    assertThat(componentCopyrightDAO
        .getByOwnerIdAndComponentIdentifier(application.getId(), componentIdentifier.toComponentIdentifier()))
            .isNotNull();

    // Persist the updated values
    ComponentCopyrightDTO updatedComponentCopyrightDTO =
        componentLegalService
            .saveComponentCopyright(OwnerType.ORGANIZATION, organization.getPublicId(), existingComponentCopyright);

    assertThat(updatedComponentCopyrightDTO.getId()).isNotNull();
    assertThat(updatedComponentCopyrightDTO.getCopyrightOverrides()).hasSize(3);
    updatedComponentCopyrightDTO.getCopyrightOverrides().forEach(co -> assertThat(co.getId()).isNotNull());
    assertThat(updatedComponentCopyrightDTO.getComponentIdentifier()).usingRecursiveComparison()
        .isEqualTo(componentIdentifier);

    CopyrightOverrideDTO copyrightOverrideDTO0 = updatedComponentCopyrightDTO.getCopyrightOverrides().get(0);
    CopyrightOverrideDTO copyrightOverrideDTO1 = updatedComponentCopyrightDTO.getCopyrightOverrides().get(1);
    CopyrightOverrideDTO copyrightOverrideDTO2 = updatedComponentCopyrightDTO.getCopyrightOverrides().get(2);

    assertThat(copyrightOverrideDTO0.getOriginalContentHash()).isEqualTo("originalContentHash");
    assertThat(copyrightOverrideDTO0.getContent()).isEqualTo("updated content");
    assertThat(copyrightOverrideDTO0.getStatus()).isEqualTo(ComponentLegalPartStatus.ENABLED);

    assertThat(copyrightOverrideDTO1.getOriginalContentHash()).isEqualTo("originalContentHash2");
    assertThat(copyrightOverrideDTO1.getContent()).isEqualTo("content2");
    assertThat(copyrightOverrideDTO1.getStatus()).isEqualTo(ComponentLegalPartStatus.DISABLED);

    assertThat(copyrightOverrideDTO2.getOriginalContentHash()).isNull();
    assertThat(copyrightOverrideDTO2.getContent()).isEqualTo("content3");
    assertThat(copyrightOverrideDTO2.getStatus()).isEqualTo(ComponentLegalPartStatus.ENABLED);

    ComponentCopyright componentCopyright = componentCopyrightDAO.getById(updatedComponentCopyrightDTO.getId());
    assertThat(componentCopyright.getOwnerId()).isEqualTo(organization.getId());
    assertThat(componentCopyrightDAO
        .getByOwnerIdAndComponentIdentifier(application.getId(), componentIdentifier.toComponentIdentifier())).isNull();
  }

  /**
   * Removing a CopyrightOverride from an existing ComponentCopyright should delete
   */
  @Test
  public void testBlankCopyrightOverrideInsert() {
    Application application = tempEntity.newApplicationWithParent();
    ApiComponentIdentifierDTOV2 componentIdentifier = ApiComponentIdentifierDTOV2
        .fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"));

    ComponentCopyrightDTO componentCopyrightDTO = new ComponentCopyrightDTO(
        null,
        componentIdentifier,
        Lists.newArrayList(new CopyrightOverrideDTO(
            null,
            null,
            "content",
            ComponentLegalPartStatus.ENABLED)),
        null,
        null);

    ComponentCopyrightDTO returnedComponentCopyrightDTO =
        componentLegalService
            .saveComponentCopyright(OwnerType.APPLICATION, application.getPublicId(), componentCopyrightDTO);

    returnedComponentCopyrightDTO.getCopyrightOverrides().get(0).setContent("");

    ComponentCopyrightDTO updatedComponentCopyrightDTO =
        componentLegalService
            .saveComponentCopyright(OwnerType.APPLICATION, application.getPublicId(), returnedComponentCopyrightDTO);

    assertThat(updatedComponentCopyrightDTO.getCopyrightOverrides()).isEmpty();
  }

  /**
   * The scenario is the following: - a ComponentCopyright with ID A exists at the OrgScope - a ComponentCopyright with
   * ID B exists at the ApplicationScope - user modifies ComponentCopyright B to OrgScope. ComponentCopyright A is
   * updated to match ComponentCopyright B except in scope and ComponentCopyright A is deleted.
   */
  @Test
  public void testConflictingComponentCopyrightWhileUpdating() {
    ApiComponentIdentifierDTOV2 componentIdentifier = ApiComponentIdentifierDTOV2
        .fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"));

    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());

    ComponentCopyright orgComponentCopyright =
        tempEntity.newComponentCopyright(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"),
            organization.getId(), "lch");
    ComponentCopyright appComponentCopyright =
        tempEntity.newComponentCopyright(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"),
            application.getId(), "lch");

    assertThat(componentCopyrightDAO.getById(appComponentCopyright.getId())).isNotNull();

    ComponentCopyrightDTO componentCopyrightDTO = new ComponentCopyrightDTO(
        appComponentCopyright.getId(),
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

    ComponentCopyrightDTO returnedComponentCopyrightDTO = componentLegalService
        .saveComponentCopyright(OwnerType.ORGANIZATION, organization.getPublicId(), componentCopyrightDTO);

    assertThat(componentCopyrightDAO.getById(appComponentCopyright.getId())).isNull();
    assertThat(returnedComponentCopyrightDTO.getCopyrightOverrides()).hasSize(2);
    assertThat(returnedComponentCopyrightDTO.getId()).isEqualTo(orgComponentCopyright.getId());

    ComponentCopyright persistedComponentCopyright =
        componentCopyrightDAO.getById(returnedComponentCopyrightDTO.getId());
    assertThat(persistedComponentCopyright.getOwnerId()).isEqualTo(organization.getId());
  }

  /**
   * The scenario is the following: Inserting a new ComponentCopyright at an existing scope. A ComponentCopyright with
   * ID A exists at the OrgScope. The user inserts a new ComponentCopyright from the application scope at the OrgScope.
   * There is a conflict. The ComponentCopyright A is updated to match the new ComponentCopyright.
   */
  @Test
  public void testConflictingComponentCopyrightWhileInserting() {
    ApiComponentIdentifierDTOV2 componentIdentifier = ApiComponentIdentifierDTOV2
        .fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"));

    Organization organization = tempEntity.newOrganization();

    tempEntity.newComponentCopyright(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"),
        organization.getId(), "lch");

    ComponentCopyrightDTO componentCopyrightDTO = new ComponentCopyrightDTO(
        null, // null ID signifies we are creating a new ComponentCopyright
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

    ComponentCopyrightDTO returnedComponentCopyrightDTO = componentLegalService
        .saveComponentCopyright(OwnerType.ORGANIZATION, organization.getPublicId(), componentCopyrightDTO);

    assertThat(componentCopyrightDAO.getAll()).hasSize(1);
    assertThat(returnedComponentCopyrightDTO.getCopyrightOverrides()).hasSize(2);

    ComponentCopyright persistedComponentCopyright =
        componentCopyrightDAO.getById(returnedComponentCopyrightDTO.getId());
    assertThat(persistedComponentCopyright.getOwnerId()).isEqualTo(organization.getId());
  }

  @Test
  public void testInvalidComponentIdentifier() {
    assertThrows(InvalidComponentIdentifierException.class, () -> {
      Application application = tempEntity.newApplicationWithParent();
      ApiComponentIdentifierDTOV2 componentIdentifier = ApiComponentIdentifierDTOV2
          .fromComponentIdentifier(JsonUtils.parse("{}", ComponentIdentifier.class));

      ComponentCopyrightDTO componentCopyrightDTO = new ComponentCopyrightDTO(
          null,
          componentIdentifier,
          Lists.newArrayList(new CopyrightOverrideDTO(
              null,
              null,
              "content",
              ComponentLegalPartStatus.ENABLED)),
          null,
          null);

      componentLegalService
          .saveComponentCopyright(OwnerType.APPLICATION, application.getPublicId(), componentCopyrightDTO);
    });
  }

  @Test
  public void testInvalidComponentCopyright() {
    assertThrows(InvalidComponentCopyrightException.class, () -> {
      Application application = tempEntity.newApplicationWithParent();
      ApiComponentIdentifierDTOV2 componentIdentifier = ApiComponentIdentifierDTOV2
          .fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"));

      ComponentCopyrightDTO componentCopyrightDTO = new ComponentCopyrightDTO(
          null,
          componentIdentifier,
          Lists.newArrayList(new CopyrightOverrideDTO(
              null,
              null,
              "content",
              null)),
          null,
          null);

      componentLegalService
          .saveComponentCopyright(OwnerType.APPLICATION, application.getPublicId(), componentCopyrightDTO);
    });
  }

  @Test
  public void testInvalidComponentCopyright_PackageUrl() {
    assertThrows(InvalidComponentCopyrightException.class, () -> {
      Application application = tempEntity.newApplicationWithParent();

      ComponentCopyrightDTO componentCopyrightDTO = new ComponentCopyrightDTO();
      componentCopyrightDTO.setCopyrightOverrides(Lists.newArrayList(new CopyrightOverrideDTO(
          null,
          null,
          "content",
          null)));
      componentCopyrightDTO.setPackageUrl("pkg:maven/g/a@v");
      componentLegalService
          .saveComponentCopyright(OwnerType.APPLICATION, application.getPublicId(), componentCopyrightDTO);
    });
  }

  @Test
  public void testSaveComponentLegalFile_Unlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.ADVANCED_LEGAL_PACK);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> componentLegalService.saveComponentLegalFile(null, null, null));
  }

  @Test
  public void testSaveComponentLegalFile_NullComponentIdentifier() {
    Application application = tempEntity.newApplicationWithParent();
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> componentLegalService.saveComponentLegalFile(
        application.getType(),
        application.getPublicId(),
        new ComponentLegalFileDTO())).withMessageContaining("The component identifier cannot be null.");
  }

  @Test
  public void testSaveComponentLegalFile_InvalidComponentIdentifier() {
    Application application = tempEntity.newApplicationWithParent();
    ComponentLegalFileDTO componentLegalFileDTO = new ComponentLegalFileDTO();
    componentLegalFileDTO.setComponentIdentifier(new ApiComponentIdentifierDTOV2());
    componentLegalFileDTO.setLegalFileType(LegalFileType.NOTICE);
    assertThatExceptionOfType(InvalidComponentIdentifierException.class)
        .isThrownBy(() -> componentLegalService.saveComponentLegalFile(
            application.getType(),
            application.getPublicId(),
            componentLegalFileDTO));
  }

  @Test
  public void testSaveComponentLegalFile_ComponentLegalFileNullLegalFileType() {
    Application application = tempEntity.newApplicationWithParent();
    ComponentLegalFileDTO componentLegalFileDTO = new ComponentLegalFileDTO();
    componentLegalFileDTO.setComponentIdentifier(
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g", "a", "v")));
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> componentLegalService.saveComponentLegalFile(
        application.getType(),
        application.getPublicId(),
        componentLegalFileDTO)).withMessageContaining("ComponentLegalFileDTO must have a legal file type.");
  }

  @Test
  public void testSaveComponentLegalFilePackageUrl_ComponentLegalFileNullLegalFileType() {
    Application application = tempEntity.newApplicationWithParent();
    ComponentLegalFileDTO componentLegalFileDTO = new ComponentLegalFileDTO();
    componentLegalFileDTO.setPackageUrl("pkg:maven/g/a@v");
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> componentLegalService.saveComponentLegalFile(
        application.getType(),
        application.getPublicId(),
        componentLegalFileDTO)).withMessageContaining("ComponentLegalFileDTO must have a legal file type.");
  }

  @Test
  public void testSaveComponentLegalFile_LegalFileOverrideNullStatus() {
    Application application = tempEntity.newApplicationWithParent();
    ComponentLegalFileDTO componentLegalFileDTO = new ComponentLegalFileDTO();
    componentLegalFileDTO.setLegalFileType(LegalFileType.NOTICE);
    componentLegalFileDTO.setComponentIdentifier(
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g", "a", "v")));
    LegalFileOverrideDTO legalFileOverrideDTO = new LegalFileOverrideDTO();
    componentLegalFileDTO.setLegalFileOverrides(Collections.singletonList(legalFileOverrideDTO));
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> componentLegalService.saveComponentLegalFile(
        application.getType(),
        application.getPublicId(),
        componentLegalFileDTO)).withMessageContaining("LegalFileOverride must have a status.");
  }

  @Test
  public void testSaveComponentLegalFilePackageUrl_LegalFileOverrideNullStatus() {
    Application application = tempEntity.newApplicationWithParent();
    ComponentLegalFileDTO componentLegalFileDTO = new ComponentLegalFileDTO();
    componentLegalFileDTO.setLegalFileType(LegalFileType.NOTICE);
    componentLegalFileDTO.setPackageUrl("pkg:maven/g/a@v");
    LegalFileOverrideDTO legalFileOverrideDTO = new LegalFileOverrideDTO();
    componentLegalFileDTO.setLegalFileOverrides(Collections.singletonList(legalFileOverrideDTO));
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> componentLegalService.saveComponentLegalFile(
        application.getType(),
        application.getPublicId(),
        componentLegalFileDTO)).withMessageContaining("LegalFileOverride must have a status.");
  }

  @Test
  public void testSaveComponentLegalFile_New_Notices() {
    Application app = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentLegalFileDTO componentLegalFileDTO = new ComponentLegalFileDTO();
    componentLegalFileDTO.setLegalFileType(LegalFileType.NOTICE);
    componentLegalFileDTO
        .setComponentIdentifier(ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier));
    LegalFileOverrideDTO notice1 = new LegalFileOverrideDTO("originalContentHash1", "content1",
        ComponentLegalPartStatus.ENABLED);
    LegalFileOverrideDTO notice2 = new LegalFileOverrideDTO(null, "content2",
        ComponentLegalPartStatus.DISABLED);
    LegalFileOverrideDTO notice3 = new LegalFileOverrideDTO("originalContentHash2", null,
        ComponentLegalPartStatus.ENABLED);
    LegalFileOverrideDTO notice4 = new LegalFileOverrideDTO(null, null,
        ComponentLegalPartStatus.DISABLED);
    componentLegalFileDTO.setLegalFileOverrides(
        Arrays.asList(notice1, notice2, notice3, notice4));
    Date date = new Date();

    ComponentLegalFileDTO resultDto =
        componentLegalService
            .saveComponentLegalFile(app.getType(), app.getPublicId(), componentLegalFileDTO);

    assertComponentLegalFile(resultDto, componentLegalFileDTO, app.getId(), date);
    assertComponentLegalFile(resultDto,
        new ComponentLegalFileDTO(componentLegalFileDAO.getById(resultDto.getId()), Collections.emptyList()),
        app.getId(), date);
    notice3.setContent("");
    assertThat(resultDto.getLegalFileOverrides()).usingRecursiveFieldByFieldElementComparator(
        RecursiveComparisonConfiguration.builder().withIgnoredFields("id").build())
        .containsExactlyInAnyOrder(notice1, notice2, notice3);
    for (LegalFileOverrideDTO legalFileOverride : resultDto.getLegalFileOverrides()) {
      assertThat(legalFileOverride.getId()).isNotNull();
      assertLegalFileOverride(new LegalFileOverrideDTO(legalFileOverrideDAO.getById(legalFileOverride.getId())),
          legalFileOverride);
    }
  }

  @Test
  public void testSaveComponentLegalFile_New_Notices_PackageURL() {
    Application app = tempEntity.newApplicationWithParent();
    ComponentLegalFileDTO componentLegalFileDTO = new ComponentLegalFileDTO();
    componentLegalFileDTO.setLegalFileType(LegalFileType.NOTICE);
    String packageURL = "pkg:maven/g/a@v";
    componentLegalFileDTO.setPackageUrl(packageURL);
    LegalFileOverrideDTO notice1 = new LegalFileOverrideDTO("originalContentHash1", "content1",
        ComponentLegalPartStatus.ENABLED);
    LegalFileOverrideDTO notice2 = new LegalFileOverrideDTO(null, "content2",
        ComponentLegalPartStatus.DISABLED);
    LegalFileOverrideDTO notice3 = new LegalFileOverrideDTO("originalContentHash2", null,
        ComponentLegalPartStatus.ENABLED);
    LegalFileOverrideDTO notice4 = new LegalFileOverrideDTO(null, null,
        ComponentLegalPartStatus.DISABLED);
    componentLegalFileDTO.setLegalFileOverrides(
        Arrays.asList(notice1, notice2, notice3, notice4));
    Date date = new Date();

    ComponentLegalFileDTO resultDto =
        componentLegalService
            .saveComponentLegalFile(app.getType(), app.getPublicId(), componentLegalFileDTO);

    ComponentIdentifier componentIdentifier = ComponentIdentifier
        .createMavenCoordinates("g", "a", "v");
    componentLegalFileDTO.setComponentIdentifier(
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier));
    assertComponentLegalFile(resultDto, componentLegalFileDTO, app.getId(), date);
    assertComponentLegalFile(resultDto,
        new ComponentLegalFileDTO(componentLegalFileDAO.getById(resultDto.getId()), Collections.emptyList()),
        app.getId(), date);
    notice3.setContent("");
    assertThat(resultDto.getLegalFileOverrides()).usingRecursiveFieldByFieldElementComparator(
        RecursiveComparisonConfiguration.builder().withIgnoredFields("id").build())
        .containsExactlyInAnyOrder(notice1, notice2, notice3);
    for (LegalFileOverrideDTO legalFileOverride : resultDto.getLegalFileOverrides()) {
      assertThat(legalFileOverride.getId()).isNotNull();
      assertLegalFileOverride(new LegalFileOverrideDTO(legalFileOverrideDAO.getById(legalFileOverride.getId())),
          legalFileOverride);
    }
  }

  @Test
  public void testSaveComponentLegalFile_New_Licenses() {
    Application app = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentLegalFileDTO componentLegalFileDTO = new ComponentLegalFileDTO();
    componentLegalFileDTO.setLegalFileType(LegalFileType.LICENSE);
    componentLegalFileDTO
        .setComponentIdentifier(ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier));
    LegalFileOverrideDTO license1 = new LegalFileOverrideDTO("originalContentHash3", "content3",
        ComponentLegalPartStatus.ENABLED);
    LegalFileOverrideDTO license2 = new LegalFileOverrideDTO(null, "content4",
        ComponentLegalPartStatus.DISABLED);
    LegalFileOverrideDTO license3 = new LegalFileOverrideDTO("originalContentHash4", null,
        ComponentLegalPartStatus.ENABLED);
    LegalFileOverrideDTO license4 = new LegalFileOverrideDTO(null, null,
        ComponentLegalPartStatus.DISABLED);
    componentLegalFileDTO.setLegalFileOverrides(
        Arrays.asList(license1, license2, license3, license4));
    Date date = new Date();

    ComponentLegalFileDTO resultDto =
        componentLegalService
            .saveComponentLegalFile(app.getType(), app.getPublicId(), componentLegalFileDTO);

    assertComponentLegalFile(resultDto, componentLegalFileDTO, app.getId(), date);
    assertComponentLegalFile(resultDto,
        new ComponentLegalFileDTO(componentLegalFileDAO.getById(resultDto.getId()), Collections.emptyList()),
        app.getId(), date);
    license3.setContent("");
    assertThat(resultDto.getLegalFileOverrides()).usingRecursiveFieldByFieldElementComparator(
        RecursiveComparisonConfiguration.builder().withIgnoredFields("id").build())
        .containsExactlyInAnyOrder(license1, license2, license3);
    for (LegalFileOverrideDTO legalFileOverride : resultDto.getLegalFileOverrides()) {
      assertThat(legalFileOverride.getId()).isNotNull();
      assertLegalFileOverride(new LegalFileOverrideDTO(legalFileOverrideDAO.getById(legalFileOverride.getId())),
          legalFileOverride);
    }
  }

  @Test
  public void testSaveComponentLegalFile_NewConflict_Notice() {
    Organization org = tempEntity.newOrganization();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentLegalFile componentLegalFile =
        tempEntity.newComponentLegalFile(componentIdentifier, org.getId(), LegalFileType.NOTICE, "legalContentHash");
    LegalFileOverride noticeOverride =
        tempEntity.newLegalFileOverride("originalContentHash1", "hash1",
            "content1", ComponentLegalPartStatus.ENABLED, componentLegalFile.getId());
    ComponentLegalFileDTO componentLegalFileDTO = new ComponentLegalFileDTO();
    componentLegalFileDTO.setLegalFileType(LegalFileType.NOTICE);
    componentLegalFileDTO
        .setComponentIdentifier(ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier));
    LegalFileOverrideDTO notice1 = new LegalFileOverrideDTO(noticeOverride);
    LegalFileOverrideDTO notice2 = new LegalFileOverrideDTO("originalContentHash2", "content2",
        ComponentLegalPartStatus.ENABLED);
    componentLegalFileDTO.setLegalFileOverrides(Arrays.asList(notice1, notice2));
    Date date = new Date();

    ComponentLegalFileDTO resultDto = componentLegalService
        .saveComponentLegalFile(org.getType(), org.getPublicId(), componentLegalFileDTO);

    componentLegalFileDTO.setId(componentLegalFile.getId());
    assertComponentLegalFile(resultDto, componentLegalFileDTO, org.getId(), date);
    assertComponentLegalFile(resultDto,
        new ComponentLegalFileDTO(componentLegalFileDAO.getById(resultDto.getId()), Collections.emptyList()),
        org.getId(), date);
    assertThat(resultDto.getLegalFileOverrides()).usingRecursiveFieldByFieldElementComparator(
        RecursiveComparisonConfiguration.builder().withIgnoredFields("id").build())
        .containsExactlyInAnyOrder(notice1, notice2);
    assertThat(legalFileOverrideDAO
        .getByOwnerIdAndComponentIdentifierAndType(org.getId(), componentIdentifier, LegalFileType.NOTICE)).hasSize(2);
    for (LegalFileOverrideDTO legalFileOverride : resultDto.getLegalFileOverrides()) {
      assertThat(legalFileOverride.getId()).isNotNull();
      assertLegalFileOverride(new LegalFileOverrideDTO(legalFileOverrideDAO.getById(legalFileOverride.getId())),
          legalFileOverride);
    }
  }

  @Test
  public void testSaveComponentLegalFile_NewConflict_License() {
    Organization org = tempEntity.newOrganization();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentLegalFile componentLegalFile =
        tempEntity.newComponentLegalFile(componentIdentifier, org.getId(), LegalFileType.LICENSE, "legalContentHash");
    LegalFileOverride licenseOverride =
        tempEntity.newLegalFileOverride("originalContentHash1", "hash1",
            "content1", ComponentLegalPartStatus.ENABLED, componentLegalFile.getId());
    ComponentLegalFileDTO componentLegalFileDTO = new ComponentLegalFileDTO();
    componentLegalFileDTO.setLegalFileType(LegalFileType.LICENSE);
    componentLegalFileDTO
        .setComponentIdentifier(ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier));
    LegalFileOverrideDTO license1 = new LegalFileOverrideDTO(licenseOverride);
    LegalFileOverrideDTO license2 = new LegalFileOverrideDTO("originalContentHash2", "content2",
        ComponentLegalPartStatus.ENABLED);
    componentLegalFileDTO.setLegalFileOverrides(Arrays.asList(license1, license2));
    Date date = new Date();

    ComponentLegalFileDTO resultDto = componentLegalService
        .saveComponentLegalFile(org.getType(), org.getPublicId(), componentLegalFileDTO);

    componentLegalFileDTO.setId(componentLegalFile.getId());
    assertComponentLegalFile(resultDto, componentLegalFileDTO, org.getId(), date);
    assertComponentLegalFile(resultDto,
        new ComponentLegalFileDTO(componentLegalFileDAO.getById(resultDto.getId()), Collections.emptyList()),
        org.getId(), date);
    assertThat(resultDto.getLegalFileOverrides()).usingRecursiveFieldByFieldElementComparator(
        RecursiveComparisonConfiguration.builder().withIgnoredFields("id").build())
        .containsExactlyInAnyOrder(license1, license2);
    assertThat(legalFileOverrideDAO
        .getByOwnerIdAndComponentIdentifierAndType(org.getId(), componentIdentifier, LegalFileType.LICENSE)).hasSize(2);
    for (LegalFileOverrideDTO legalFileOverride : resultDto.getLegalFileOverrides()) {
      assertThat(legalFileOverride.getId()).isNotNull();
      assertLegalFileOverride(new LegalFileOverrideDTO(legalFileOverrideDAO.getById(legalFileOverride.getId())),
          legalFileOverride);
    }
  }

  @Test
  public void testSaveComponentLegalFile_ExistingConflict() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentLegalFile orgComponentLegalFile =
        tempEntity.newComponentLegalFile(componentIdentifier, app.getOrganizationId(), LegalFileType.NOTICE,
            "legalContentHash1");
    ComponentLegalFile appComponentLegalFile =
        tempEntity.newComponentLegalFile(componentIdentifier, app.getId(), LegalFileType.NOTICE, "legalContentHash2");
    ComponentLegalFileDTO componentLegalFileDTO = new ComponentLegalFileDTO();
    componentLegalFileDTO.setId(appComponentLegalFile.getId());
    componentLegalFileDTO.setLegalFileType(LegalFileType.NOTICE);
    componentLegalFileDTO
        .setComponentIdentifier(ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier));
    LegalFileOverrideDTO notice1 = new LegalFileOverrideDTO("originalContentHash1", "content1",
        ComponentLegalPartStatus.ENABLED);
    LegalFileOverrideDTO notice2 = new LegalFileOverrideDTO("originalContentHash2", "content2",
        ComponentLegalPartStatus.ENABLED);
    componentLegalFileDTO.setLegalFileOverrides(Arrays.asList(notice1, notice2));
    Date date = new Date();

    ComponentLegalFileDTO resultDto = componentLegalService
        .saveComponentLegalFile(org.getType(), org.getPublicId(), componentLegalFileDTO);

    assertThat(componentLegalFileDAO.getById(appComponentLegalFile.getId())).isNull();
    componentLegalFileDTO.setId(orgComponentLegalFile.getId());
    assertComponentLegalFile(resultDto, componentLegalFileDTO, org.getId(), date);
    assertComponentLegalFile(resultDto,
        new ComponentLegalFileDTO(componentLegalFileDAO.getById(resultDto.getId()), Collections.emptyList()),
        org.getId(), date);
    assertThat(resultDto.getLegalFileOverrides()).usingRecursiveFieldByFieldElementComparator(
        RecursiveComparisonConfiguration.builder().withIgnoredFields("id").build())
        .containsExactlyInAnyOrder(notice1, notice2);
    for (LegalFileOverrideDTO legalFileOverride : resultDto.getLegalFileOverrides()) {
      assertThat(legalFileOverride.getId()).isNotNull();
      assertLegalFileOverride(new LegalFileOverrideDTO(legalFileOverrideDAO.getById(legalFileOverride.getId())),
          legalFileOverride);
    }
  }

  private void assertComponentLegalFile(
      ComponentLegalFileDTO actual,
      ComponentLegalFileDTO expected,
      String expectedOwnerId,
      Date expectedDate)
  {
    if (expected.getId() == null) {
      assertThat(actual.getId()).isNotNull();
    }
    else {
      assertThat(actual.getId()).isEqualTo(expected.getId());
    }
    assertThat(actual.getComponentIdentifier()).usingRecursiveComparison().isEqualTo(expected.getComponentIdentifier());
    assertThat(actual.getOwnerId()).isEqualTo(expectedOwnerId);
    assertThat(actual.getLegalFileType()).isEqualTo(expected.getLegalFileType());
    assertThat(actual.getLastUpdatedAt()).isAfterOrEqualTo(expectedDate);
    assertThat(actual.getLastUpdatedByUsername()).isEqualTo(USERNAME);
  }

  private void assertLegalFileOverride(LegalFileOverrideDTO actual, LegalFileOverrideDTO expected) {
    if (expected.getId() == null) {
      assertThat(actual.getId()).isNotNull();
    }
    else {
      assertThat(actual.getId()).isEqualTo(expected.getId());
    }
    assertThat(actual.getOriginalContentHash()).isEqualTo(expected.getOriginalContentHash());
    assertThat(actual.getContent()).isEqualTo(StringUtils.trimToEmpty(expected.getContent()));
    assertThat(actual.getStatus()).isEqualTo(expected.getStatus());
  }

  @Test
  public void testSaveComponentLegalFile_Existing_UpdatesAllFields() {
    Application app = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    ComponentLegalFile componentLegalFile =
        tempEntity.newComponentLegalFile(componentIdentifier1, app.getId(), LegalFileType.NOTICE, "legalContentHash1");
    LegalFileOverride legalFileOverride =
        tempEntity.newLegalFileOverride("originalContentHash", "hash",
            "", ComponentLegalPartStatus.ENABLED, componentLegalFile.getId());
    ComponentLegalFileDTO componentLegalFileDTO = new ComponentLegalFileDTO();
    componentLegalFileDTO.setId(componentLegalFile.getId());
    componentLegalFileDTO.setLegalFileType(LegalFileType.NOTICE);
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2");
    componentLegalFileDTO
        .setComponentIdentifier(ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier2));
    LegalFileOverrideDTO legalFileOverrideDTO = new LegalFileOverrideDTO(null, "content",
        ComponentLegalPartStatus.DISABLED);
    legalFileOverrideDTO.setId(legalFileOverride.getId());
    componentLegalFileDTO.setLegalFileOverrides(Collections.singletonList(legalFileOverrideDTO));

    ComponentLegalFileDTO resultDto =
        componentLegalService.saveComponentLegalFile(app.getType(), app.getPublicId(), componentLegalFileDTO);

    assertComponentLegalFile(resultDto, componentLegalFileDTO, app.getId(), componentLegalFile.getLastUpdatedAt());
    assertComponentLegalFile(resultDto,
        new ComponentLegalFileDTO(componentLegalFileDAO.getById(resultDto.getId()), Collections.emptyList()),
        app.getId(), componentLegalFile.getLastUpdatedAt());
    assertThat(resultDto.getLegalFileOverrides()).usingRecursiveFieldByFieldElementComparator(
        RecursiveComparisonConfiguration.builder().build())
        .containsExactly(legalFileOverrideDTO);
    assertLegalFileOverride(new LegalFileOverrideDTO(legalFileOverrideDAO.getById(legalFileOverride.getId())),
        legalFileOverrideDTO);
  }

  @Test
  public void testSaveComponentLegalFile_Existing_DeletesCustomOverridesWithNoContent() {
    Application app = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    ComponentLegalFile componentLegalFile =
        tempEntity.newComponentLegalFile(componentIdentifier, app.getId(), LegalFileType.NOTICE, "legalContentHash1");
    LegalFileOverride legalFileOverride1 = tempEntity.newLegalFileOverride("originalContentHash1",
        "hash1", "content", ComponentLegalPartStatus.ENABLED, componentLegalFile.getId());
    LegalFileOverride legalFileOverride2 = tempEntity.newLegalFileOverride("originalContentHash2",
        "hash2", "", ComponentLegalPartStatus.ENABLED, componentLegalFile.getId());
    LegalFileOverride legalFileOverride3 = tempEntity.newLegalFileOverride(null,
        "hash1", "content", ComponentLegalPartStatus.ENABLED, componentLegalFile.getId());
    ComponentLegalFileDTO componentLegalFileDTO = new ComponentLegalFileDTO();
    componentLegalFileDTO.setId(componentLegalFile.getId());
    componentLegalFileDTO.setLegalFileType(LegalFileType.NOTICE);
    componentLegalFileDTO
        .setComponentIdentifier(ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier));
    LegalFileOverrideDTO legalFileOverrideDTO1 = new LegalFileOverrideDTO(legalFileOverride1);
    legalFileOverrideDTO1.setContent("");
    LegalFileOverrideDTO legalFileOverrideDTO2 = new LegalFileOverrideDTO(legalFileOverride2);
    legalFileOverrideDTO2.setContent("");
    LegalFileOverrideDTO legalFileOverrideDTO3 = new LegalFileOverrideDTO(legalFileOverride3);
    legalFileOverrideDTO3.setContent("");
    componentLegalFileDTO
        .setLegalFileOverrides(Arrays.asList(legalFileOverrideDTO1, legalFileOverrideDTO2, legalFileOverrideDTO3));

    ComponentLegalFileDTO resultDto = componentLegalService
        .saveComponentLegalFile(app.getType(), app.getPublicId(), componentLegalFileDTO);

    assertComponentLegalFile(resultDto, componentLegalFileDTO, app.getId(), componentLegalFile.getLastUpdatedAt());
    assertComponentLegalFile(resultDto,
        new ComponentLegalFileDTO(componentLegalFileDAO.getById(resultDto.getId()), Collections.emptyList()),
        app.getId(), componentLegalFile.getLastUpdatedAt());
    assertThat(resultDto.getLegalFileOverrides()).usingRecursiveFieldByFieldElementComparator(
        RecursiveComparisonConfiguration.builder().build())
        .containsExactlyInAnyOrder(legalFileOverrideDTO1, legalFileOverrideDTO2);
    assertLegalFileOverride(new LegalFileOverrideDTO(legalFileOverrideDAO.getById(legalFileOverrideDTO1.getId())),
        legalFileOverrideDTO1);
    assertLegalFileOverride(new LegalFileOverrideDTO(legalFileOverrideDAO.getById(legalFileOverrideDTO2.getId())),
        legalFileOverrideDTO2);
    assertThat(legalFileOverrideDAO.getById(legalFileOverrideDTO3.getId())).isNull();
  }

  @Test
  public void testSaveComponentObligations_Attribution_Unlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.ADVANCED_LEGAL_PACK);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> componentLegalService.saveComponentObligationAttribution(null, null, null));
  }

  @Test
  public void testSaveComponentObligations_Attribution_NullComponentIdentifier() {
    Application application = tempEntity.newApplicationWithParent();
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> componentLegalService.saveComponentObligationAttribution(
            application.getType(),
            application.getPublicId(),
            new ComponentObligationAttributionDTO()))
        .withMessageContaining("The component identifier cannot be null.");
  }

  @Test
  public void testSaveComponentObligations_Attribution_InvalidComponentIdentifier() {
    Application application = tempEntity.newApplicationWithParent();
    ComponentObligationAttributionDTO componentObligationAttributionDTO = new ComponentObligationAttributionDTO();
    componentObligationAttributionDTO.setComponentIdentifier(new ApiComponentIdentifierDTOV2());
    assertThatExceptionOfType(InvalidComponentIdentifierException.class)
        .isThrownBy(() -> componentLegalService.saveComponentObligationAttribution(
            application.getType(),
            application.getPublicId(),
            componentObligationAttributionDTO));
  }

  @Test
  public void testSaveComponentObligations_Attribution_BlankContent() {
    Application application = tempEntity.newApplicationWithParent();
    ComponentObligationAttributionDTO componentObligationAttributionDTO = new ComponentObligationAttributionDTO();
    componentObligationAttributionDTO.setComponentIdentifier(
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g", "a", "v")));
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> componentLegalService.saveComponentObligationAttribution(
            application.getType(),
            application.getPublicId(),
            componentObligationAttributionDTO))
        .withMessageContaining("ComponentObligationAttribution must have content.");
    componentObligationAttributionDTO.setContent("");
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> componentLegalService.saveComponentObligationAttribution(
            application.getType(),
            application.getPublicId(),
            componentObligationAttributionDTO))
        .withMessageContaining("ComponentObligationAttribution must have content.");
    componentObligationAttributionDTO.setContent(" ");
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> componentLegalService.saveComponentObligationAttribution(
            application.getType(),
            application.getPublicId(),
            componentObligationAttributionDTO))
        .withMessageContaining("ComponentObligationAttribution must have content.");

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentObligationAttribution componentObligationAttribution = tempEntity.newComponentObligationAttribution(
        componentIdentifier, application.getId(), "obligationName", "content",
        ComponentLegalService.NOT_IMPLEMENTED);
    componentObligationAttributionDTO.setId(componentObligationAttribution.getId());
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> componentLegalService.saveComponentObligationAttribution(
            application.getType(),
            application.getPublicId(),
            componentObligationAttributionDTO))
        .withMessageContaining("ComponentObligationAttribution must have content.");
    componentObligationAttributionDTO.setContent("");
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> componentLegalService.saveComponentObligationAttribution(
            application.getType(),
            application.getPublicId(),
            componentObligationAttributionDTO))
        .withMessageContaining("ComponentObligationAttribution must have content.");
    componentObligationAttributionDTO.setContent(" ");
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> componentLegalService.saveComponentObligationAttribution(
            application.getType(),
            application.getPublicId(),
            componentObligationAttributionDTO))
        .withMessageContaining("ComponentObligationAttribution must have content.");
  }

  @Test
  public void testSaveComponentObligations_Attribution_Create() {
    Application application = tempEntity.newApplicationWithParent();
    ComponentObligationAttributionDTO componentObligationAttributionDTO = new ComponentObligationAttributionDTO();
    componentObligationAttributionDTO.setComponentIdentifier(
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g", "a", "v")));
    componentObligationAttributionDTO.setObligationName("obligationName");
    componentObligationAttributionDTO.setContent("content");

    ComponentObligationAttributionDTO result = componentLegalService.saveComponentObligationAttribution(
        application.getType(),
        application.getPublicId(),
        componentObligationAttributionDTO);

    List<ComponentObligationAttribution> componentObligationAttributions =
        componentObligationAttributionDAO.getByOwnerId(application.getId());
    assertThat(componentObligationAttributions).hasSize(1);
    ComponentObligationAttribution componentObligationAttribution = componentObligationAttributions.get(0);
    assertComponentObligationAttribution(componentObligationAttribution, application,
        componentObligationAttributionDTO);
    assertComponentObligationAttribution(result, componentObligationAttribution);
  }

  @Test
  public void testSaveComponentObligations_Attribution_Create_PackageURL() {
    Application application = tempEntity.newApplicationWithParent();
    ComponentObligationAttributionDTO componentObligationAttributionDTO = new ComponentObligationAttributionDTO();
    ApiComponentIdentifierDTOV2 componentIdentifier = ApiComponentIdentifierDTOV2
        .fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g", "a", "v"));
    componentObligationAttributionDTO.setObligationName("obligationName");
    componentObligationAttributionDTO.setContent("content");
    String packageURL = "pkg:maven/g/a@v";
    componentObligationAttributionDTO.setPackageUrl(packageURL);
    ComponentObligationAttributionDTO result = componentLegalService.saveComponentObligationAttribution(
        application.getType(),
        application.getPublicId(),
        componentObligationAttributionDTO);
    componentObligationAttributionDTO.setComponentIdentifier(componentIdentifier);
    List<ComponentObligationAttribution> componentObligationAttributions =
        componentObligationAttributionDAO.getByOwnerId(application.getId());
    assertThat(componentObligationAttributions).hasSize(1);
    ComponentObligationAttribution componentObligationAttribution = componentObligationAttributions.get(0);
    assertComponentObligationAttribution(componentObligationAttribution, application,
        componentObligationAttributionDTO);
    assertComponentObligationAttribution(result, componentObligationAttribution);
  }

  @Test
  public void testSaveComponentObligations_Attribution_Update() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    String obligationName = "obligationName1";
    String content = "content1";
    ComponentObligationAttribution oldComponentObligationAttribution = tempEntity
        .newComponentObligationAttribution(componentIdentifier, organization.getPublicId(), obligationName,
            content, ComponentLegalService.NOT_IMPLEMENTED);
    ComponentObligationAttributionDTO componentObligationAttributionDTO = new ComponentObligationAttributionDTO();
    componentObligationAttributionDTO.setId(oldComponentObligationAttribution.getId());
    componentObligationAttributionDTO.setComponentIdentifier(
        ApiComponentIdentifierDTOV2
            .fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2")));
    componentObligationAttributionDTO.setObligationName("obligationName2");
    componentObligationAttributionDTO.setContent("content2");

    ComponentObligationAttributionDTO result = componentLegalService.saveComponentObligationAttribution(
        OwnerType.APPLICATION,
        application.getPublicId(),
        componentObligationAttributionDTO);

    List<ComponentObligationAttribution> componentObligationAttributions =
        componentObligationAttributionDAO.getByOwnerId(application.getId());
    assertThat(componentObligationAttributions).hasSize(1);
    ComponentObligationAttribution componentObligationAttribution = componentObligationAttributions.get(0);
    assertComponentObligationAttribution(componentObligationAttribution, application,
        componentObligationAttributionDTO);
    assertThat(componentObligationAttribution.getLastUpdatedAt())
        .isAfterOrEqualTo(oldComponentObligationAttribution.getLastUpdatedAt());
    assertComponentObligationAttribution(result, componentObligationAttribution);
  }

  @Test
  public void testSaveComponentObligations_Attribution_Update_DoesNotExist() {
    Application application = tempEntity.newApplicationWithParent();
    ComponentObligationAttributionDTO componentObligationAttributionDTO = new ComponentObligationAttributionDTO();
    componentObligationAttributionDTO.setId("doesNotExist");
    componentObligationAttributionDTO.setComponentIdentifier(
        ApiComponentIdentifierDTOV2
            .fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g", "a", "v")));
    componentObligationAttributionDTO.setContent("content");

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> componentLegalService.saveComponentObligationAttribution(
            application.getType(),
            application.getPublicId(),
            componentObligationAttributionDTO))
        .withMessageContaining(
            "ComponentObligationAttribution with ID " + componentObligationAttributionDTO.getId() + " does not exist.");
  }

  @Test
  public void testSaveComponentCopyright_Update_NotFound() {
    Application application = tempEntity.newApplicationWithParent();
    ComponentCopyrightDTO componentCopyrightDTO = new ComponentCopyrightDTO();
    componentCopyrightDTO.setId("doesNotExist");
    componentCopyrightDTO.setComponentIdentifier(
        ApiComponentIdentifierDTOV2
            .fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g", "a", "v")));

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> componentLegalService.saveComponentCopyright(
        application.getType(),
        application.getPublicId(),
        componentCopyrightDTO))
        .withMessageContaining("ComponentCopyright with ID " + componentCopyrightDTO.getId() + " does not exist.");
  }

  @Test
  public void testSaveComponentCopyright_Update_OverrideNotFound() {
    Application application = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentCopyright componentCopyright = tempEntity
        .newComponentCopyright(componentIdentifier, application.getId(), "legalContentHash");
    ComponentCopyrightDTO componentCopyrightDTO = new ComponentCopyrightDTO();
    componentCopyrightDTO.setId(componentCopyright.getId());
    componentCopyrightDTO
        .setComponentIdentifier(ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier));
    CopyrightOverrideDTO copyrightOverrideDTO = new CopyrightOverrideDTO();
    copyrightOverrideDTO.setId("doesNotExist");
    copyrightOverrideDTO.setContent("content");
    copyrightOverrideDTO.setStatus(ComponentLegalPartStatus.ENABLED);
    componentCopyrightDTO.setCopyrightOverrides(Collections.singletonList(copyrightOverrideDTO));

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> componentLegalService.saveComponentCopyright(
        application.getType(),
        application.getPublicId(),
        componentCopyrightDTO))
        .withMessageContaining("CopyrightOverride with ID " + copyrightOverrideDTO.getId() + " does not exist.");
  }

  @Test
  public void testSaveComponentLegalFile_Update_NotFound() {
    Application application = tempEntity.newApplicationWithParent();
    ComponentLegalFileDTO componentLegalFileDTO = new ComponentLegalFileDTO();
    componentLegalFileDTO.setId("doesNotExist");
    componentLegalFileDTO.setLegalFileType(LegalFileType.NOTICE);
    componentLegalFileDTO.setComponentIdentifier(
        ApiComponentIdentifierDTOV2
            .fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g", "a", "v")));

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> componentLegalService.saveComponentLegalFile(
        application.getType(),
        application.getPublicId(),
        componentLegalFileDTO))
        .withMessageContaining("ComponentLegalFile with ID " + componentLegalFileDTO.getId() + " does not exist.");
  }

  @Test
  public void testSaveComponentLegalFile_Update_OverrideNotFound() {
    Application application = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentLegalFile componentLegalFile = tempEntity
        .newComponentLegalFile(componentIdentifier, application.getId(), LegalFileType.NOTICE, "legalContentHash");
    ComponentLegalFileDTO componentLegalFileDTO = new ComponentLegalFileDTO();
    componentLegalFileDTO.setId(componentLegalFile.getId());
    componentLegalFileDTO.setLegalFileType(LegalFileType.NOTICE);
    componentLegalFileDTO
        .setComponentIdentifier(ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier));
    LegalFileOverrideDTO legalFileOverrideDTO = new LegalFileOverrideDTO();
    legalFileOverrideDTO.setId("doesNotExist");
    legalFileOverrideDTO.setContent("content");
    legalFileOverrideDTO.setStatus(ComponentLegalPartStatus.ENABLED);
    componentLegalFileDTO.setLegalFileOverrides(Collections.singletonList(legalFileOverrideDTO));

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> componentLegalService.saveComponentLegalFile(
        application.getType(),
        application.getPublicId(),
        componentLegalFileDTO))
        .withMessageContaining("LegalFileOverride with ID " + legalFileOverrideDTO.getId() + " does not exist.");
  }

  @Test
  public void testDeleteComponentObligationAttribution_Unlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.ADVANCED_LEGAL_PACK);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> componentLegalService.deleteComponentObligationAttribution(null));
  }

  @Test
  public void testDeleteComponentObligationAttribution() {
    Application application = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentObligationAttribution componentObligationAttribution = tempEntity.newComponentObligationAttribution(
        componentIdentifier, application.getId(), "obligationName", "content",
        ComponentLegalService.NOT_IMPLEMENTED);
    ComponentObligationAttributionDTO componentObligationAttributionDTO = new ComponentObligationAttributionDTO();
    componentObligationAttributionDTO.setId(componentObligationAttribution.getId());
    componentObligationAttributionDTO.setComponentIdentifier(
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier));

    componentLegalService.deleteComponentObligationAttribution(componentObligationAttribution.getId());

    assertThat(componentObligationAttributionDAO.getByOwnerId(application.getId())).isEmpty();

    componentObligationAttribution = tempEntity.newComponentObligationAttribution(componentIdentifier,
        application.getId(), "obligationName", "content", ComponentLegalService.NOT_IMPLEMENTED);
    componentObligationAttributionDTO.setId(componentObligationAttribution.getId());
    componentObligationAttributionDTO.setContent("");

    componentLegalService.deleteComponentObligationAttribution(componentObligationAttribution.getId());

    assertThat(componentObligationAttributionDAO.getByOwnerId(application.getId())).isEmpty();

    componentObligationAttribution = tempEntity.newComponentObligationAttribution(componentIdentifier,
        application.getId(), "obligationName", "content", ComponentLegalService.NOT_IMPLEMENTED);
    componentObligationAttributionDTO.setId(componentObligationAttribution.getId());
    componentObligationAttributionDTO.setContent(" ");

    componentLegalService.deleteComponentObligationAttribution(componentObligationAttribution.getId());

    assertThat(componentObligationAttributionDAO.getByOwnerId(application.getId())).isEmpty();
  }

  @Test
  public void testDeleteComponentObligationAttribution_DoesNotExist() {
    String id = "doesNotExist";

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> componentLegalService.deleteComponentObligationAttribution(id))
        .withMessageContaining("ComponentObligationAttribution with ID " + id + " does not exist.");
  }

  @Test
  public void testGetComponentObligationAttributions_Unlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.ADVANCED_LEGAL_PACK);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> componentLegalService.getComponentObligationAttributions(null, null, null, null));
  }

  @Test
  public void testGetComponentObligationAttributions() {
    Application application = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    String obligationName = "obligationName";

    ComponentObligationAttribution componentObligationAttribution = tempEntity.newComponentObligationAttribution(
        componentIdentifier, Organization.ROOT_ORGANIZATION_ID, obligationName, "content",
        ComponentLegalService.NOT_IMPLEMENTED);

    assertThat(componentLegalService.getComponentObligationAttributions(application.getType(),
        application.getPublicId(), componentIdentifier, obligationName))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(new ComponentObligationAttributionDTO(componentObligationAttribution));

    ComponentObligationAttribution override = tempEntity.newComponentObligationAttribution(
        componentIdentifier, application.getId(), obligationName, "override",
        ComponentLegalService.NOT_IMPLEMENTED);

    assertThat(componentLegalService.getComponentObligationAttributions(application.getType(),
        application.getPublicId(), componentIdentifier, obligationName))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(new ComponentObligationAttributionDTO(override));
  }

  @Test
  public void testSaveComponentObligations_Unlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.ADVANCED_LEGAL_PACK);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> componentLegalService.saveComponentObligations(null, null, null));
  }

  @Test
  public void testSaveComponentObligations_NullComponentIdentifier() {
    Application app = tempEntity.newApplicationWithParent();
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> componentLegalService
        .saveComponentObligations(app.getType(), app.getPublicId(),
            Lists.newArrayList(new ApiLicenseLegalObligationDTO())))
        .withMessageContaining("The component identifier cannot be null.");
  }

  @Test
  public void testSaveComponentObligations_InvalidComponentIdentifier() {
    Application app = tempEntity.newApplicationWithParent();
    ApiLicenseLegalObligationDTO dto = new ApiLicenseLegalObligationDTO();
    dto.setComponentIdentifier(new ApiComponentIdentifierDTOV2());
    assertThatExceptionOfType(InvalidComponentIdentifierException.class)
        .isThrownBy(() -> componentLegalService
            .saveComponentObligations(app.getType(), app.getPublicId(), Lists.newArrayList(dto)));
  }

  @Test
  public void testSaveComponentObligations_BlankObligationName() {
    Application app = tempEntity.newApplicationWithParent();
    ApiLicenseLegalObligationDTO dto = createMinimalComponentObligationDTO();
    dto.setName(null);
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> componentLegalService
            .saveComponentObligations(app.getType(), app.getPublicId(), Lists.newArrayList(dto)))
        .withMessageContaining("ComponentObligation must have a name.");
    dto.setName("");
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> componentLegalService
            .saveComponentObligations(app.getType(), app.getPublicId(), Lists.newArrayList(dto)))
        .withMessageContaining("ComponentObligation must have a name.");
    dto.setName(" ");
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> componentLegalService
            .saveComponentObligations(app.getType(), app.getPublicId(), Lists.newArrayList(dto)))
        .withMessageContaining("ComponentObligation must have a name.");
  }

  @Test
  public void testSaveComponentObligations_NullStatus() {
    Application app = tempEntity.newApplicationWithParent();
    ApiLicenseLegalObligationDTO componentObligationDTO = createMinimalComponentObligationDTO();
    componentObligationDTO.setStatus(null);
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> componentLegalService
            .saveComponentObligations(app.getType(), app.getPublicId(), Lists.newArrayList(componentObligationDTO)))
        .withMessageContaining("ComponentObligation must have a status.");
  }

  @Test
  public void testSaveComponentObligations_Create() {
    Application app = tempEntity.newApplicationWithParent();
    ApiLicenseLegalObligationDTO dto = createMinimalComponentObligationDTO();

    List<ApiLicenseLegalObligationDTO> resultDtos =
        componentLegalService.saveComponentObligations(app.getType(), app.getPublicId(), Lists.newArrayList(dto));

    assertThat(resultDtos).hasSize(1);
    ComponentObligation componentObligation = componentObligationDAO.getByIdNotNull(resultDtos.get(0).getId());
    assertComponentObligation(componentObligation, app, dto);
    assertComponentObligation(resultDtos.get(0), componentObligation);
  }

  @Test
  public void testSaveComponentObligations_Update_DoesNotExist() {
    Application app = tempEntity.newApplicationWithParent();
    ApiLicenseLegalObligationDTO dto = createMinimalComponentObligationDTO();
    String id = "doesNotExist";
    dto.setId(id);
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> componentLegalService
            .saveComponentObligations(app.getType(), app.getPublicId(), Lists.newArrayList(dto)))
        .withMessageContaining("ComponentObligation with ID " + id + " does not exist.");
  }

  @Test
  public void testSaveComponentObligations_Update() {
    Application app = tempEntity.newApplicationWithParent();
    ApiLicenseLegalObligationDTO dto = createMinimalComponentObligationDTO();
    ComponentObligation componentObligation = tempEntity.newComponentObligation(
        dto.getComponentIdentifier().toComponentIdentifier().createAlternativeVersion("v0"), app.getOrganizationId(),
        "original" + dto.getName(), "original" + dto.getComment(), ObligationStatus.FLAGGED,
        ComponentLegalService.NOT_IMPLEMENTED);
    dto.setId(componentObligation.getId());

    List<ApiLicenseLegalObligationDTO> resultDtos =
        componentLegalService.saveComponentObligations(app.getType(), app.getPublicId(), Lists.newArrayList(dto));

    assertThat(resultDtos).hasSize(1);
    componentObligation = componentObligationDAO.getByIdNotNull(resultDtos.get(0).getId());
    assertComponentObligation(componentObligation, app, dto);
    assertComponentObligation(resultDtos.get(0), componentObligation);
  }

  @Test
  public void testDeleteComponentObligation_Unlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.ADVANCED_LEGAL_PACK);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> componentLegalService.deleteComponentObligations(null));
  }

  @Test
  public void testDeleteComponentObligation_DoesNotExist() {
    String id = "doesNotExist";
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> componentLegalService.deleteComponentObligations(Collections.singletonList(id)))
        .withMessageContaining("ComponentObligation with ID " + id + " does not exist.");
  }

  @Test
  public void testDeleteComponentObligation() {
    Application app = tempEntity.newApplicationWithParent();
    ApiLicenseLegalObligationDTO dto = createMinimalComponentObligationDTO();
    ComponentObligation componentObligation = tempEntity.newComponentObligation(
        dto.getComponentIdentifier().toComponentIdentifier(), app.getId(), dto.getName(), dto.getComment(),
        dto.getStatus(), ComponentLegalService.NOT_IMPLEMENTED);

    componentLegalService.deleteComponentObligations(Collections.singletonList(componentObligation.getId()));

    assertThat(componentObligationDAO.getById(componentObligation.getId())).isNull();
  }

  @Test
  public void testGetComponentObligation_Unlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.ADVANCED_LEGAL_PACK);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> componentLegalService.getComponentObligation(null, null, null, null));
  }

  @Test
  public void testGetComponentObligation_NullComponentIdentifier() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> componentLegalService
            .getComponentObligation(OwnerType.APPLICATION, "ownerId", null, "obligationName"))
        .withMessageContaining("The component identifier cannot be null.");
  }

  @Test
  public void testGetComponentObligation_InvalidComponentIdentifier() {
    assertThatExceptionOfType(InvalidComponentIdentifierException.class).isThrownBy(() -> componentLegalService
        .getComponentObligation(OwnerType.APPLICATION, "ownerId",
            new ApiComponentIdentifierDTOV2().toComponentIdentifier(), "obligationName"));
  }

  @Test
  public void testGetComponentObligation_ReturnsSameScope() {
    Application app = tempEntity.newApplicationWithParent();
    ApiLicenseLegalObligationDTO dto = createMinimalComponentObligationDTO();
    ComponentObligation componentObligation = tempEntity.newComponentObligation(
        dto.getComponentIdentifier().toComponentIdentifier(), app.getId(), dto.getName(), dto.getComment(),
        dto.getStatus(), ComponentLegalService.NOT_IMPLEMENTED);

    ApiLicenseLegalObligationDTO resultDto = componentLegalService.getComponentObligation(app.getType(),
        app.getPublicId(), dto.getComponentIdentifier().toComponentIdentifier(), dto.getName());

    assertComponentObligation(resultDto, componentObligation);
  }

  @Test
  public void testGetComponentObligation_ReturnsHigherScope() {
    Application app = tempEntity.newApplicationWithParent();
    ApiLicenseLegalObligationDTO dto = createMinimalComponentObligationDTO();
    ComponentObligation componentObligation = tempEntity.newComponentObligation(
        dto.getComponentIdentifier().toComponentIdentifier(), app.getOrganizationId(), dto.getName(), dto.getComment(),
        dto.getStatus(), ComponentLegalService.NOT_IMPLEMENTED);

    ApiLicenseLegalObligationDTO resultDto = componentLegalService.getComponentObligation(app.getType(),
        app.getPublicId(), dto.getComponentIdentifier().toComponentIdentifier(), dto.getName());

    assertComponentObligation(resultDto, componentObligation);
  }

  @Test
  public void testGetComponentObligation_NotFound() {
    ApiLicenseLegalObligationDTO dto = createMinimalComponentObligationDTO();
    Application app = tempEntity.newApplicationWithParent();

    ApiLicenseLegalObligationDTO resultDto = componentLegalService.getComponentObligation(app.getType(),
        app.getPublicId(), dto.getComponentIdentifier().toComponentIdentifier(), dto.getName());

    assertThat(resultDto).isNull();
  }

  @Test
  public void testGetComponentCopyrightWithHierarchy() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");

    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());

    ComponentCopyright orgComponentCopyright =
        tempEntity.newComponentCopyright(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"),
            organization.getId(), "lch");
    ComponentCopyright appComponentCopyright =
        tempEntity.newComponentCopyright(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"),
            application.getId(), "lch");

    ComponentCopyrightWithOwnerDTO componentCopyrightWithOwnerDTO = componentLegalService
        .getComponentCopyrightWithHierarchy(OwnerType.APPLICATION, application.getPublicId(), componentIdentifier);

    assertThat(componentCopyrightWithOwnerDTO.getComponentCopyrightDTO().getId())
        .isEqualTo(appComponentCopyright.getId());

    componentCopyrightWithOwnerDTO = componentLegalService
        .getComponentCopyrightWithHierarchy(OwnerType.ORGANIZATION, organization.getPublicId(), componentIdentifier);

    assertThat(componentCopyrightWithOwnerDTO.getComponentCopyrightDTO().getId())
        .isEqualTo(orgComponentCopyright.getId());
  }

  @Test
  public void testGetComponentCopyrightWithHierarchy_higherScope() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");

    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());

    ComponentCopyright orgComponentCopyright =
        tempEntity.newComponentCopyright(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"),
            organization.getId(), "lch");

    ComponentCopyrightWithOwnerDTO componentCopyrightWithOwnerDTO = componentLegalService
        .getComponentCopyrightWithHierarchy(OwnerType.APPLICATION, application.getPublicId(), componentIdentifier);

    assertThat(componentCopyrightWithOwnerDTO.getComponentCopyrightDTO().getId())
        .isEqualTo(orgComponentCopyright.getId());
    assertThat(componentCopyrightWithOwnerDTO.getOwnerId())
        .isEqualTo(organization.getId());
  }

  @Test
  public void testGetComponentCopyrightWithHierarchy_notFound() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    Organization org = tempEntity.newOrganization();

    assertThat(componentLegalService
        .getComponentCopyrightWithHierarchy(OwnerType.ORGANIZATION, org.getPublicId(), componentIdentifier)).isNull();
  }

  @Test
  public void testGetComponentCopyrightWithHierarchy_Unlicensed() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    testProductLicense.setMissingFeatures(LicensedFeature.ADVANCED_LEGAL_PACK);
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() -> componentLegalService
        .getComponentCopyrightWithHierarchy(OwnerType.APPLICATION, "n/a", componentIdentifier));
  }

  @Test
  public void testGetComponentLegalFile_Unlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.ADVANCED_LEGAL_PACK);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> componentLegalService.getComponentLegalFile(null, null, null, null));
  }

  @Test
  public void testGetComponentLegalFile_NullComponentIdentifier() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> componentLegalService.getComponentLegalFile(null, null, null, null))
        .withMessageContaining("The component identifier cannot be null.");
  }

  @Test
  public void testGetComponentLegalFile_InvalidComponentIdentifier() {
    assertThatExceptionOfType(InvalidComponentIdentifierException.class).isThrownBy(() -> componentLegalService
        .getComponentLegalFile(null, null, new ApiComponentIdentifierDTOV2().toComponentIdentifier(), null));
  }

  @Test
  public void testGetComponentLegalFile_OwnerDoesNotExist() {
    String id = "doesNotExist";
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> componentLegalService
        .getComponentLegalFile(OwnerType.APPLICATION, id, ComponentIdentifier.createMavenCoordinates("g", "a", "v"),
            null))
        .withMessageContaining("Application with ID " + id + " does not exist.");
  }

  @Test
  public void testGetComponentLegalFile() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentLegalFile rootComponentNotice = tempEntity.newComponentLegalFile(componentIdentifier,
        Organization.ROOT_ORGANIZATION_ID, LegalFileType.NOTICE, "legalContentHash1");
    LegalFileOverride rootLegalFileOverride1 = tempEntity.newLegalFileOverride(null, "hash1",
        "content1", ComponentLegalPartStatus.ENABLED, rootComponentNotice.getId());
    ComponentLegalFile orgComponentNotice =
        tempEntity.newComponentLegalFile(componentIdentifier, org.getId(), LegalFileType.NOTICE, "legalContentHash2");
    LegalFileOverride orgLegalFileOverride1 = tempEntity.newLegalFileOverride(null, "hash3",
        "content3", ComponentLegalPartStatus.ENABLED, orgComponentNotice.getId());
    ComponentLegalFile appComponentNotice =
        tempEntity.newComponentLegalFile(componentIdentifier, app.getId(), LegalFileType.NOTICE, "legalContentHash3");
    LegalFileOverride appLegalFileOverride1 = tempEntity.newLegalFileOverride(null, "hash5",
        "content5", ComponentLegalPartStatus.ENABLED, appComponentNotice.getId());

    ComponentLegalFile rootComponentLicense = tempEntity.newComponentLegalFile(componentIdentifier,
        Organization.ROOT_ORGANIZATION_ID, LegalFileType.LICENSE, "legalContentHash1");
    LegalFileOverride rootLegalFileOverride2 = tempEntity.newLegalFileOverride(null, "hash2",
        "content2", ComponentLegalPartStatus.ENABLED, rootComponentLicense.getId());
    ComponentLegalFile orgComponentLicense =
        tempEntity.newComponentLegalFile(componentIdentifier, org.getId(), LegalFileType.LICENSE, "legalContentHash2");
    LegalFileOverride orgLegalFileOverride2 = tempEntity.newLegalFileOverride(null, "hash4",
        "content4", ComponentLegalPartStatus.ENABLED, orgComponentLicense.getId());
    ComponentLegalFile appComponentLicense =
        tempEntity.newComponentLegalFile(componentIdentifier, app.getId(), LegalFileType.LICENSE, "legalContentHash3");
    LegalFileOverride appLegalFileOverride2 = tempEntity.newLegalFileOverride(null, "hash6",
        "content6", ComponentLegalPartStatus.ENABLED, appComponentLicense.getId());

    assertThat(componentLegalService.getComponentLegalFile(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID,
        componentIdentifier, LegalFileType.NOTICE)).usingRecursiveComparison()
            .isEqualTo(
                new ComponentLegalFileDTO(rootComponentNotice, Collections.singletonList(rootLegalFileOverride1)));
    assertThat(componentLegalService.getComponentLegalFile(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID,
        componentIdentifier, LegalFileType.LICENSE)).usingRecursiveComparison()
            .isEqualTo(
                new ComponentLegalFileDTO(rootComponentLicense, Collections.singletonList(rootLegalFileOverride2)));
    assertThat(componentLegalService.getComponentLegalFile(org.getType(), org.getId(), componentIdentifier,
        LegalFileType.NOTICE)).usingRecursiveComparison()
            .isEqualTo(
                new ComponentLegalFileDTO(orgComponentNotice, Collections.singletonList(orgLegalFileOverride1)));
    assertThat(componentLegalService.getComponentLegalFile(org.getType(), org.getId(), componentIdentifier,
        LegalFileType.LICENSE)).usingRecursiveComparison()
            .isEqualTo(
                new ComponentLegalFileDTO(orgComponentLicense, Collections.singletonList(orgLegalFileOverride2)));
    assertThat(componentLegalService.getComponentLegalFile(app.getType(), app.getId(), componentIdentifier,
        LegalFileType.NOTICE)).usingRecursiveComparison()
            .isEqualTo(
                new ComponentLegalFileDTO(appComponentNotice, Collections.singletonList(appLegalFileOverride1)));
    assertThat(componentLegalService.getComponentLegalFile(app.getType(), app.getId(), componentIdentifier,
        LegalFileType.LICENSE)).usingRecursiveComparison()
            .isEqualTo(
                new ComponentLegalFileDTO(appComponentLicense, Collections.singletonList(appLegalFileOverride2)));

    componentLegalFileDAO.delete(appComponentNotice);
    componentLegalFileDAO.delete(appComponentLicense);

    assertThat(componentLegalService.getComponentLegalFile(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID,
        componentIdentifier, LegalFileType.NOTICE)).usingRecursiveComparison()
            .isEqualTo(
                new ComponentLegalFileDTO(rootComponentNotice, Collections.singletonList(rootLegalFileOverride1)));
    assertThat(componentLegalService.getComponentLegalFile(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID,
        componentIdentifier, LegalFileType.LICENSE)).usingRecursiveComparison()
            .isEqualTo(
                new ComponentLegalFileDTO(rootComponentLicense, Collections.singletonList(rootLegalFileOverride2)));
    assertThat(componentLegalService.getComponentLegalFile(org.getType(), org.getId(), componentIdentifier,
        LegalFileType.NOTICE)).usingRecursiveComparison()
            .isEqualTo(
                new ComponentLegalFileDTO(orgComponentNotice, Collections.singletonList(orgLegalFileOverride1)));
    assertThat(componentLegalService.getComponentLegalFile(org.getType(), org.getId(), componentIdentifier,
        LegalFileType.LICENSE)).usingRecursiveComparison()
            .isEqualTo(
                new ComponentLegalFileDTO(orgComponentLicense, Collections.singletonList(orgLegalFileOverride2)));
    assertThat(componentLegalService.getComponentLegalFile(app.getType(), app.getId(), componentIdentifier,
        LegalFileType.NOTICE)).usingRecursiveComparison()
            .isEqualTo(
                new ComponentLegalFileDTO(orgComponentNotice, Collections.singletonList(orgLegalFileOverride1)));
    assertThat(componentLegalService.getComponentLegalFile(app.getType(), app.getId(), componentIdentifier,
        LegalFileType.LICENSE)).usingRecursiveComparison()
            .isEqualTo(
                new ComponentLegalFileDTO(orgComponentLicense, Collections.singletonList(orgLegalFileOverride2)));

    componentLegalFileDAO.delete(orgComponentNotice);
    componentLegalFileDAO.delete(orgComponentLicense);

    assertThat(componentLegalService.getComponentLegalFile(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID,
        componentIdentifier, LegalFileType.NOTICE)).usingRecursiveComparison()
            .isEqualTo(
                new ComponentLegalFileDTO(rootComponentNotice, Collections.singletonList(rootLegalFileOverride1)));
    assertThat(componentLegalService.getComponentLegalFile(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID,
        componentIdentifier, LegalFileType.LICENSE)).usingRecursiveComparison()
            .isEqualTo(
                new ComponentLegalFileDTO(rootComponentLicense, Collections.singletonList(rootLegalFileOverride2)));
    assertThat(componentLegalService.getComponentLegalFile(org.getType(), org.getId(), componentIdentifier,
        LegalFileType.NOTICE)).usingRecursiveComparison()
            .isEqualTo(
                new ComponentLegalFileDTO(rootComponentNotice, Collections.singletonList(rootLegalFileOverride1)));
    assertThat(componentLegalService.getComponentLegalFile(org.getType(), org.getId(), componentIdentifier,
        LegalFileType.LICENSE)).usingRecursiveComparison()
            .isEqualTo(
                new ComponentLegalFileDTO(rootComponentLicense, Collections.singletonList(rootLegalFileOverride2)));
    assertThat(componentLegalService.getComponentLegalFile(app.getType(), app.getId(), componentIdentifier,
        LegalFileType.NOTICE)).usingRecursiveComparison()
            .isEqualTo(
                new ComponentLegalFileDTO(rootComponentNotice, Collections.singletonList(rootLegalFileOverride1)));
    assertThat(componentLegalService.getComponentLegalFile(app.getType(), app.getId(), componentIdentifier,
        LegalFileType.LICENSE)).usingRecursiveComparison()
            .isEqualTo(
                new ComponentLegalFileDTO(rootComponentLicense, Collections.singletonList(rootLegalFileOverride2)));

    componentLegalFileDAO.delete(rootComponentNotice);
    componentLegalFileDAO.delete(rootComponentLicense);

    assertThat(componentLegalService.getComponentLegalFile(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID,
        componentIdentifier, LegalFileType.NOTICE)).isNull();
    assertThat(componentLegalService.getComponentLegalFile(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID,
        componentIdentifier, LegalFileType.LICENSE)).isNull();
    assertThat(componentLegalService.getComponentLegalFile(org.getType(), org.getId(), componentIdentifier,
        LegalFileType.NOTICE)).isNull();
    assertThat(componentLegalService.getComponentLegalFile(org.getType(), org.getId(), componentIdentifier,
        LegalFileType.LICENSE)).isNull();
    assertThat(componentLegalService.getComponentLegalFile(app.getType(), app.getId(), componentIdentifier,
        LegalFileType.NOTICE)).isNull();
    assertThat(componentLegalService.getComponentLegalFile(app.getType(), app.getId(), componentIdentifier,
        LegalFileType.LICENSE)).isNull();
  }

  @Test
  public void testGetComponentLegalFile_OnlyGetsGivenType() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentLegalFile rootComponentNotice =
        tempEntity.newComponentLegalFile(componentIdentifier, Organization.ROOT_ORGANIZATION_ID, LegalFileType.NOTICE,
            "legalContentHash1");
    LegalFileOverride rootLegalFileOverride1 = tempEntity.newLegalFileOverride(null, "hash2",
        "content2", ComponentLegalPartStatus.ENABLED, rootComponentNotice.getId());
    ComponentLegalFile orgComponentNotice =
        tempEntity.newComponentLegalFile(componentIdentifier, org.getId(), LegalFileType.NOTICE, "legalContentHash2");
    LegalFileOverride orgLegalFileOverride1 = tempEntity.newLegalFileOverride(null, "hash4",
        "content4", ComponentLegalPartStatus.ENABLED, orgComponentNotice.getId());
    ComponentLegalFile appComponentNotice =
        tempEntity.newComponentLegalFile(componentIdentifier, app.getId(), LegalFileType.NOTICE, "legalContentHash3");
    LegalFileOverride appLegalFileOverride1 = tempEntity.newLegalFileOverride(null, "hash6",
        "content6", ComponentLegalPartStatus.ENABLED, appComponentNotice.getId());

    ComponentLegalFile rootComponentLicense =
        tempEntity.newComponentLegalFile(componentIdentifier, Organization.ROOT_ORGANIZATION_ID, LegalFileType.LICENSE,
            "legalContentHash4");
    LegalFileOverride rootLegalFileOverride2 = tempEntity.newLegalFileOverride(null, "hash2",
        "content2", ComponentLegalPartStatus.ENABLED, rootComponentLicense.getId());
    ComponentLegalFile orgComponentLicense =
        tempEntity.newComponentLegalFile(componentIdentifier, org.getId(), LegalFileType.LICENSE, "legalContentHash5");
    LegalFileOverride orgLegalFileOverride2 = tempEntity.newLegalFileOverride(null, "hash4",
        "content4", ComponentLegalPartStatus.ENABLED, orgComponentLicense.getId());
    ComponentLegalFile appComponentLicense =
        tempEntity.newComponentLegalFile(componentIdentifier, app.getId(), LegalFileType.LICENSE, "legalContentHash6");
    LegalFileOverride appLegalFileOverride2 = tempEntity.newLegalFileOverride(null, "hash6",
        "content6", ComponentLegalPartStatus.ENABLED, appComponentLicense.getId());

    assertThat(componentLegalService.getComponentLegalFile(app.getType(), app.getId(), componentIdentifier,
        LegalFileType.NOTICE)).usingRecursiveComparison()
            .isEqualTo(
                new ComponentLegalFileDTO(appComponentNotice, Collections.singletonList(appLegalFileOverride1)));

    legalFileOverrideDAO.delete(appLegalFileOverride1);

    assertThat(componentLegalService.getComponentLegalFile(app.getType(), app.getId(), componentIdentifier,
        LegalFileType.NOTICE)).usingRecursiveComparison()
            .isEqualTo(
                new ComponentLegalFileDTO(orgComponentNotice, Collections.singletonList(orgLegalFileOverride1)));

    legalFileOverrideDAO.delete(orgLegalFileOverride1);

    assertThat(componentLegalService.getComponentLegalFile(app.getType(), app.getId(), componentIdentifier,
        LegalFileType.NOTICE)).usingRecursiveComparison()
            .isEqualTo(
                new ComponentLegalFileDTO(rootComponentNotice, Collections.singletonList(rootLegalFileOverride1)));

    legalFileOverrideDAO.delete(rootLegalFileOverride1);

    assertThat(componentLegalService.getComponentLegalFile(app.getType(), app.getId(), componentIdentifier,
        LegalFileType.NOTICE)).isNull();

    assertThat(componentLegalService.getComponentLegalFile(app.getType(), app.getId(), componentIdentifier,
        LegalFileType.LICENSE)).usingRecursiveComparison()
            .isEqualTo(
                new ComponentLegalFileDTO(appComponentLicense, Collections.singletonList(appLegalFileOverride2)));

    legalFileOverrideDAO.delete(appLegalFileOverride2);

    assertThat(componentLegalService.getComponentLegalFile(app.getType(), app.getId(), componentIdentifier,
        LegalFileType.LICENSE)).usingRecursiveComparison()
            .isEqualTo(
                new ComponentLegalFileDTO(orgComponentLicense, Collections.singletonList(orgLegalFileOverride2)));

    legalFileOverrideDAO.delete(orgLegalFileOverride2);

    assertThat(componentLegalService.getComponentLegalFile(app.getType(), app.getId(), componentIdentifier,
        LegalFileType.LICENSE)).usingRecursiveComparison()
            .isEqualTo(
                new ComponentLegalFileDTO(rootComponentLicense, Collections.singletonList(rootLegalFileOverride2)));

    legalFileOverrideDAO.delete(rootLegalFileOverride2);

    assertThat(componentLegalService.getComponentLegalFile(app.getType(), app.getId(), componentIdentifier,
        LegalFileType.LICENSE)).isNull();
  }

  @Test
  public void testGetComponentCopyright_Order() {
    Application app = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentCopyright componentCopyright =
        tempEntity.newComponentCopyright(componentIdentifier, app.getId(), "legalContentHash1");
    CopyrightOverride copyrightOverride1 = tempEntity.newCopyrightOverride(null, "hash1", "y",
        ComponentLegalPartStatus.ENABLED, componentCopyright.getId());
    CopyrightOverride copyrightOverride2 = tempEntity.newCopyrightOverride(null, "hash2", "b",
        ComponentLegalPartStatus.ENABLED, componentCopyright.getId());
    CopyrightOverride copyrightOverride3 = tempEntity.newCopyrightOverride("originalHash1", "hash3", "z",
        ComponentLegalPartStatus.ENABLED, componentCopyright.getId());
    CopyrightOverride copyrightOverride4 = tempEntity.newCopyrightOverride("originalHash2", "hash4", "a",
        ComponentLegalPartStatus.ENABLED, componentCopyright.getId());

    ComponentCopyrightWithOwnerDTO expected = new ComponentCopyrightWithOwnerDTO(
        ComponentCopyrightDTO.fromComponentCopyright(componentCopyright,
            Arrays.asList(CopyrightOverrideDTO.fromCopyrightOverride(copyrightOverride4),
                CopyrightOverrideDTO.fromCopyrightOverride(copyrightOverride3),
                CopyrightOverrideDTO.fromCopyrightOverride(copyrightOverride2),
                CopyrightOverrideDTO.fromCopyrightOverride(copyrightOverride1))),
        componentCopyright.getOwnerId());
    assertThat(componentLegalService.getComponentCopyrightWithHierarchy(app.getType(), app.getPublicId(),
        componentIdentifier)).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  public void testGetComponentLegalFile_Order() {
    Application app = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentLegalFile componentLegalFile =
        tempEntity.newComponentLegalFile(componentIdentifier, app.getId(), LegalFileType.NOTICE, "legalContentHash1");
    LegalFileOverride legalFileOverride1 = tempEntity.newLegalFileOverride(null,
        "hash1", "y", ComponentLegalPartStatus.ENABLED, componentLegalFile.getId());
    LegalFileOverride legalFileOverride2 = tempEntity.newLegalFileOverride(null,
        "hash2", "b", ComponentLegalPartStatus.ENABLED, componentLegalFile.getId());
    LegalFileOverride legalFileOverride3 = tempEntity.newLegalFileOverride("originalHash1",
        "hash3", "z", ComponentLegalPartStatus.ENABLED, componentLegalFile.getId());
    LegalFileOverride legalFileOverride4 = tempEntity.newLegalFileOverride("originalHash2",
        "hash4", "a", ComponentLegalPartStatus.ENABLED, componentLegalFile.getId());

    assertThat(componentLegalService
        .getComponentLegalFile(app.getType(), app.getPublicId(), componentIdentifier, LegalFileType.NOTICE))
            .usingRecursiveComparison()
            .isEqualTo(new ComponentLegalFileDTO(componentLegalFile,
                Arrays.asList(legalFileOverride4, legalFileOverride3, legalFileOverride2, legalFileOverride1)));
  }

  @Test
  public void testSaveComponentCopyright_DeletesExistingOverridesIfNeeded() {
    Application app = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentCopyright existingComponentCopyright =
        tempEntity.newComponentCopyright(componentIdentifier, app.getId(), "legalContentHash");
    CopyrightOverride existingCopyrightOverride1 = tempEntity.newCopyrightOverride(null, "hash1", "content1",
        ComponentLegalPartStatus.ENABLED, existingComponentCopyright.getId());
    CopyrightOverride existingCopyrightOverride2 = tempEntity.newCopyrightOverride(null, "hash2", "content2",
        ComponentLegalPartStatus.ENABLED, existingComponentCopyright.getId());
    ComponentCopyrightDTO componentCopyrightDTO = new ComponentCopyrightDTO(existingComponentCopyright.getId(),
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier),
        Collections.singletonList(CopyrightOverrideDTO.fromCopyrightOverride(existingCopyrightOverride1)), null, null);

    componentLegalService.saveComponentCopyright(app.getType(), app.getPublicId(), componentCopyrightDTO);

    assertThat(copyrightOverrideDAO.getById(existingCopyrightOverride2.getId())).isNull();
  }

  @Test
  public void testSaveComponentCopyrightPackageUrl_DeletesExistingOverridesIfNeeded() {
    Application app = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentCopyright existingComponentCopyright =
        tempEntity.newComponentCopyright(componentIdentifier, app.getId(), "legalContentHash");
    CopyrightOverride existingCopyrightOverride1 = tempEntity.newCopyrightOverride(null, "hash1", "content1",
        ComponentLegalPartStatus.ENABLED, existingComponentCopyright.getId());
    CopyrightOverride existingCopyrightOverride2 = tempEntity.newCopyrightOverride(null, "hash2", "content2",
        ComponentLegalPartStatus.ENABLED, existingComponentCopyright.getId());

    ComponentCopyrightDTO componentCopyrightDTO = new ComponentCopyrightDTO();
    componentCopyrightDTO.setId(existingComponentCopyright.getId());
    componentCopyrightDTO.setCopyrightOverrides(
        Collections.singletonList(CopyrightOverrideDTO.fromCopyrightOverride(existingCopyrightOverride1)));
    componentCopyrightDTO.setPackageUrl("pkg:maven/g/a@v");

    componentLegalService.saveComponentCopyright(app.getType(), app.getPublicId(), componentCopyrightDTO);

    assertThat(copyrightOverrideDAO.getById(existingCopyrightOverride2.getId())).isNull();
  }

  @Test
  public void testSaveComponentLegalFile_DeletesExistingOverridesIfNeeded() {
    Application app = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    ComponentLegalFile existingComponentLegalFile =
        tempEntity.newComponentLegalFile(componentIdentifier, app.getId(), LegalFileType.NOTICE, "legalContentHash");
    LegalFileOverride existingLegalFileOverride1 = tempEntity.newLegalFileOverride(null, "hash1", "content1",
        ComponentLegalPartStatus.ENABLED, existingComponentLegalFile.getId());
    LegalFileOverride existingLegalFileOverride2 = tempEntity.newLegalFileOverride(null, "hash2", "content2",
        ComponentLegalPartStatus.ENABLED, existingComponentLegalFile.getId());
    ComponentLegalFileDTO componentLegalFileDTO =
        new ComponentLegalFileDTO(existingComponentLegalFile, Collections.singletonList(existingLegalFileOverride1));

    componentLegalService.saveComponentLegalFile(app.getType(), app.getPublicId(), componentLegalFileDTO);

    assertThat(copyrightOverrideDAO.getById(existingLegalFileOverride2.getId())).isNull();
  }

  @Test
  public void testSaveComponentObligations_Update_Conflict() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentObligation orgScopeExisting = tempEntity
        .newComponentObligation(componentIdentifier, org.getId(), "name", "comment1", ObligationStatus.OPEN,
            ComponentLegalService.NOT_IMPLEMENTED);
    ComponentObligation appScopeExisting = tempEntity
        .newComponentObligation(componentIdentifier, app.getId(), "name", "comment2", ObligationStatus.OPEN,
            ComponentLegalService.NOT_IMPLEMENTED);
    ApiLicenseLegalObligationDTO dto = new ApiLicenseLegalObligationDTO(appScopeExisting);

    List<ApiLicenseLegalObligationDTO> result =
        componentLegalService.saveComponentObligations(org.getType(), org.getPublicId(), Lists.newArrayList(dto));

    assertThat(result).hasSize(1);
    assertThat(componentObligationDAO.getById(appScopeExisting.getId())).isNull();
    ComponentObligation componentObligation = componentObligationDAO.getByIdNotNull(result.get(0).getId());
    dto.setId(orgScopeExisting.getId());
    assertComponentObligation(componentObligation, org, dto);
    assertComponentObligation(result.get(0), componentObligation);
  }

  @Test
  public void testSaveComponentObligations_Attribution_Update_Conflict() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    ComponentObligationAttribution orgScopeExisting = tempEntity
        .newComponentObligationAttribution(componentIdentifier, org.getId(), "name", "content1",
            ComponentLegalService.NOT_IMPLEMENTED);
    ComponentObligationAttribution appScopeExisting = tempEntity
        .newComponentObligationAttribution(componentIdentifier, app.getId(), "name", "content2",
            ComponentLegalService.NOT_IMPLEMENTED);
    ComponentObligationAttributionDTO dto = new ComponentObligationAttributionDTO(appScopeExisting);

    ComponentObligationAttributionDTO result =
        componentLegalService.saveComponentObligationAttribution(org.getType(), org.getPublicId(), dto);

    assertThat(componentObligationAttributionDAO.getById(appScopeExisting.getId())).isNull();
    ComponentObligationAttribution componentObligation =
        componentObligationAttributionDAO.getByIdNotNull(result.getId());
    dto.setId(orgScopeExisting.getId());
    assertComponentObligationAttribution(componentObligation, org, dto);
    assertComponentObligationAttribution(result, componentObligation);
  }

  @Test
  public void testSaveComponentCopyright_MaintainsFormatting() {
    Application app = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentCopyrightDTO componentCopyrightDTO = new ComponentCopyrightDTO();
    componentCopyrightDTO
        .setComponentIdentifier(ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier));
    CopyrightOverrideDTO copyrightOverrideDTO = new CopyrightOverrideDTO();
    copyrightOverrideDTO.setContent("   Copyright with some formatting   ");
    copyrightOverrideDTO.setStatus(ComponentLegalPartStatus.ENABLED);
    componentCopyrightDTO.setCopyrightOverrides(Collections.singletonList(copyrightOverrideDTO));

    componentLegalService.saveComponentCopyright(app.getType(), app.getPublicId(), componentCopyrightDTO);

    assertThat(copyrightOverrideDAO.getByOwnerIdAndComponentIdentifier(app.getId(), componentIdentifier)
        .get(0)
        .getContent()).isEqualTo(copyrightOverrideDTO.getContent());
  }

  @Test
  public void testSaveComponentCopyright_TrimsBlank() {
    Application app = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentCopyrightDTO componentCopyrightDTO = new ComponentCopyrightDTO();
    componentCopyrightDTO
        .setComponentIdentifier(ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier));
    CopyrightOverrideDTO copyrightOverrideDTO = new CopyrightOverrideDTO();
    copyrightOverrideDTO.setOriginalContentHash("originalContentHash");
    copyrightOverrideDTO.setContent("      ");
    copyrightOverrideDTO.setStatus(ComponentLegalPartStatus.ENABLED);
    componentCopyrightDTO.setCopyrightOverrides(Collections.singletonList(copyrightOverrideDTO));

    componentLegalService.saveComponentCopyright(app.getType(), app.getPublicId(), componentCopyrightDTO);

    assertThat(copyrightOverrideDAO.getByOwnerIdAndComponentIdentifier(app.getId(), componentIdentifier)
        .get(0)
        .getContent()).isEmpty();
  }

  @Test
  public void testSaveComponentLegalFile_MaintainsFormatting() {
    Application app = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentLegalFileDTO componentLegalFileDTO = new ComponentLegalFileDTO();
    componentLegalFileDTO.setLegalFileType(LegalFileType.NOTICE);
    componentLegalFileDTO
        .setComponentIdentifier(ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier));
    LegalFileOverrideDTO legalFileOverrideDTO = new LegalFileOverrideDTO();
    legalFileOverrideDTO.setContent("   Legal file with some formatting   ");
    legalFileOverrideDTO.setStatus(ComponentLegalPartStatus.ENABLED);
    componentLegalFileDTO.setLegalFileOverrides(Collections.singletonList(legalFileOverrideDTO));

    componentLegalService.saveComponentLegalFile(app.getType(), app.getPublicId(), componentLegalFileDTO);

    assertThat(legalFileOverrideDAO.getByOwnerIdAndComponentIdentifierAndType(app.getId(), componentIdentifier,
        LegalFileType.NOTICE).get(0).getContent()).isEqualTo(legalFileOverrideDTO.getContent());
  }

  @Test
  public void testSaveComponentLegalFile_TrimsBlank() {
    Application app = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentLegalFileDTO componentLegalFileDTO = new ComponentLegalFileDTO();
    componentLegalFileDTO.setLegalFileType(LegalFileType.NOTICE);
    componentLegalFileDTO
        .setComponentIdentifier(ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier));
    LegalFileOverrideDTO legalFileOverrideDTO = new LegalFileOverrideDTO();
    legalFileOverrideDTO.setOriginalContentHash("originalContentHash");
    legalFileOverrideDTO.setContent("      ");
    legalFileOverrideDTO.setStatus(ComponentLegalPartStatus.ENABLED);
    componentLegalFileDTO.setLegalFileOverrides(Collections.singletonList(legalFileOverrideDTO));

    componentLegalService.saveComponentLegalFile(app.getType(), app.getPublicId(), componentLegalFileDTO);

    assertThat(legalFileOverrideDAO.getByOwnerIdAndComponentIdentifierAndType(app.getId(), componentIdentifier,
        LegalFileType.NOTICE).get(0).getContent()).isEmpty();
  }

  private ApiLicenseLegalObligationDTO createMinimalComponentObligationDTO() {
    ApiLicenseLegalObligationDTO componentObligationDTO = new ApiLicenseLegalObligationDTO();
    componentObligationDTO.setComponentIdentifier(
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g", "a", "v")));
    componentObligationDTO.setName("obligationName");
    componentObligationDTO.setStatus(ObligationStatus.OPEN);
    return componentObligationDTO;
  }

  private void assertComponentObligationAttribution(
      ComponentObligationAttribution actual,
      Owner expectedOwner,
      ComponentObligationAttributionDTO expected)
  {
    if (expected.getId() == null) {
      assertThat(actual.getId()).isNotNull();
    }
    else {
      assertThat(actual.getId()).isEqualTo(expected.getId());
    }
    assertThat(actual.getOwnerId()).isEqualTo(expectedOwner.getId());
    assertThat(actual.getComponentIdentifier()).isEqualTo(expected.getComponentIdentifier().toComponentIdentifier());
    assertThat(actual.getObligationName()).isEqualTo(expected.getObligationName());
    assertThat(actual.getContent()).isEqualTo(expected.getContent());
    assertThat(actual.getLegalContentHash()).isEqualTo(ComponentLegalService.NOT_IMPLEMENTED);
    assertThat(actual.getLastUpdatedAt()).isNotNull();
    assertThat(actual.getLastUpdatedByUsername()).isEqualTo(USERNAME);
  }

  private void assertComponentObligationAttribution(
      ComponentObligationAttributionDTO actual,
      ComponentObligationAttribution expected)
  {
    assertThat(actual.getId()).isEqualTo(expected.getId());
    assertThat(actual.getComponentIdentifier().toComponentIdentifier()).isEqualTo(expected.getComponentIdentifier());
    assertThat(actual.getObligationName()).isEqualTo(expected.getObligationName());
    assertThat(actual.getContent()).isEqualTo(expected.getContent());
    assertThat(actual.getLastUpdatedAt()).isEqualTo(expected.getLastUpdatedAt());
    assertThat(actual.getLastUpdatedByUsername()).isEqualTo(expected.getLastUpdatedByUsername());
  }

  private void assertComponentObligation(
      ComponentObligation actual,
      Owner expectedOwner,
      ApiLicenseLegalObligationDTO expected)
  {
    if (expected.getId() == null) {
      assertThat(actual.getId()).isNotNull();
    }
    else {
      assertThat(actual.getId()).isEqualTo(expected.getId());
    }
    assertThat(actual.getOwnerId()).isEqualTo(expectedOwner.getId());
    assertThat(actual.getComponentIdentifier()).isEqualTo(expected.getComponentIdentifier().toComponentIdentifier());
    assertThat(actual.getObligationName()).isEqualTo(expected.getName());
    assertThat(actual.getComment()).isEqualTo(expected.getComment());
    assertThat(actual.getStatus()).isEqualTo(expected.getStatus());
    assertThat(actual.getLegalContentHash()).isEqualTo(ComponentLegalService.NOT_IMPLEMENTED);
    assertThat(actual.getLastUpdatedAt()).isNotNull();
    assertThat(actual.getLastUpdatedByUsername()).isEqualTo(USERNAME);
  }

  private void assertComponentObligation(
      ApiLicenseLegalObligationDTO actual,
      ComponentObligation expected)
  {
    assertThat(actual.getId()).isEqualTo(expected.getId());
    assertThat(actual.getComponentIdentifier().toComponentIdentifier()).isEqualTo(expected.getComponentIdentifier());
    assertThat(actual.getOwnerId()).isEqualTo(expected.getOwnerId());
    assertThat(actual.getName()).isEqualTo(expected.getObligationName());
    assertThat(actual.getStatus()).isEqualTo(expected.getStatus());
    assertThat(actual.getComment()).isEqualTo(expected.getComment());
    assertThat(actual.getLastUpdatedByUsername()).isEqualTo(expected.getLastUpdatedByUsername());
    assertThat(actual.getLastUpdatedAt()).isEqualTo(expected.getLastUpdatedAt());
  }

  @Test
  public void testSaveComponentSourceLink_Unlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.ADVANCED_LEGAL_PACK);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> componentLegalService.saveComponentSourceLink(null, null, null));
  }

  @Test
  public void testSaveNewComponentSourceLink() {
    Application application = tempEntity.newApplicationWithParent();
    ApiComponentIdentifierDTOV2 componentIdentifier = ApiComponentIdentifierDTOV2
        .fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"));

    ComponentSourceLinkDTO componentSourceLinkDTO = new ComponentSourceLinkDTO(
        null,
        componentIdentifier,
        Lists.newArrayList(new SourceLinkOverrideDTO(
            null,
            "content",
            ComponentLegalPartStatus.ENABLED),
            new SourceLinkOverrideDTO(
                null,
                "content2",
                ComponentLegalPartStatus.DISABLED),
            new SourceLinkOverrideDTO(
                null,
                null,
                ComponentLegalPartStatus.ENABLED)),
        null,
        null);

    ComponentSourceLinkDTO returnedComponentSourceLinkDTO =
        componentLegalService
            .saveComponentSourceLink(OwnerType.APPLICATION, application.getPublicId(), componentSourceLinkDTO);

    assertThat(returnedComponentSourceLinkDTO.getId()).isNotNull();
    assertThat(returnedComponentSourceLinkDTO.getSourceLinkOverrides()).hasSize(2);
    returnedComponentSourceLinkDTO.getSourceLinkOverrides().forEach(co -> assertThat(co.getId()).isNotNull());
    assertThat(returnedComponentSourceLinkDTO.getLastUpdatedAt()).isNotNull();
    assertThat(returnedComponentSourceLinkDTO.getLastUpdatedByUsername()).isEqualTo(USERNAME);
    assertThat(returnedComponentSourceLinkDTO.getComponentIdentifier()).usingRecursiveComparison()
        .isEqualTo(componentIdentifier);

    SourceLinkOverrideDTO sourceLinkOverrideDTO0 = returnedComponentSourceLinkDTO.getSourceLinkOverrides().get(0);
    SourceLinkOverrideDTO sourceLinkOverrideDTO1 = returnedComponentSourceLinkDTO.getSourceLinkOverrides().get(1);

    assertThat(sourceLinkOverrideDTO0.getContent()).isEqualTo("content");
    assertThat(sourceLinkOverrideDTO0.getStatus()).isEqualTo(ComponentLegalPartStatus.ENABLED);

    assertThat(sourceLinkOverrideDTO1.getContent()).isEqualTo("content2");
    assertThat(sourceLinkOverrideDTO1.getStatus()).isEqualTo(ComponentLegalPartStatus.DISABLED);
  }

  @Test
  public void testSaveNewComponentSourceLink_PackageURL() {
    Application application = tempEntity.newApplicationWithParent();
    ApiComponentIdentifierDTOV2 componentIdentifier = ApiComponentIdentifierDTOV2
        .fromComponentIdentifier(ComponentIdentifier
            .createMavenCoordinates("g1", "a1", "v1", "c1", "e1"));

    ComponentSourceLinkDTO componentSourceLinkDTO = new ComponentSourceLinkDTO();
    componentSourceLinkDTO.setSourceLinkOverrides(
        Lists.newArrayList(new SourceLinkOverrideDTO(
            null,
            "content",
            ComponentLegalPartStatus.ENABLED),
            new SourceLinkOverrideDTO(
                null,
                "content2",
                ComponentLegalPartStatus.DISABLED),
            new SourceLinkOverrideDTO(
                null,
                null,
                ComponentLegalPartStatus.ENABLED)));
    String packageURL = "pkg:maven/g1/a1@v1?classifier=c1&type=e1";
    componentSourceLinkDTO.setPackageUrl(packageURL);

    ComponentSourceLinkDTO returnedComponentSourceLinkDTO =
        componentLegalService
            .saveComponentSourceLink(OwnerType.APPLICATION, application.getPublicId(), componentSourceLinkDTO);

    assertThat(returnedComponentSourceLinkDTO.getId()).isNotNull();
    assertThat(returnedComponentSourceLinkDTO.getSourceLinkOverrides()).hasSize(2);
    returnedComponentSourceLinkDTO.getSourceLinkOverrides().forEach(co -> assertThat(co.getId()).isNotNull());
    assertThat(returnedComponentSourceLinkDTO.getLastUpdatedAt()).isNotNull();
    assertThat(returnedComponentSourceLinkDTO.getLastUpdatedByUsername()).isEqualTo(USERNAME);
    assertThat(returnedComponentSourceLinkDTO.getComponentIdentifier()).usingRecursiveComparison()
        .isEqualTo(componentIdentifier);
    assertThat(returnedComponentSourceLinkDTO.getPackageUrl()).isEqualTo(packageURL);

    SourceLinkOverrideDTO sourceLinkOverrideDTO0 = returnedComponentSourceLinkDTO.getSourceLinkOverrides().get(0);
    SourceLinkOverrideDTO sourceLinkOverrideDTO1 = returnedComponentSourceLinkDTO.getSourceLinkOverrides().get(1);

    assertThat(sourceLinkOverrideDTO0.getContent()).isEqualTo("content");
    assertThat(sourceLinkOverrideDTO0.getStatus()).isEqualTo(ComponentLegalPartStatus.ENABLED);

    assertThat(sourceLinkOverrideDTO1.getContent()).isEqualTo("content2");
    assertThat(sourceLinkOverrideDTO1.getStatus()).isEqualTo(ComponentLegalPartStatus.DISABLED);
  }

  @Test
  public void testUpdatedExistingComponentSourceLink() {
    ApiComponentIdentifierDTOV2 componentIdentifier = ApiComponentIdentifierDTOV2
        .fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"));

    ComponentSourceLinkDTO componentSourceLinkDTO = new ComponentSourceLinkDTO(
        null,
        componentIdentifier,
        Lists.newArrayList(new SourceLinkOverrideDTO(
            null,
            "content",
            ComponentLegalPartStatus.ENABLED),
            new SourceLinkOverrideDTO(
                null,
                "content2",
                ComponentLegalPartStatus.DISABLED)),
        null,
        null);

    // Persist original componentSourceLink
    ComponentSourceLinkDTO existingComponentSourceLink =
        componentLegalService
            .saveComponentSourceLink(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID, componentSourceLinkDTO);

    // Modify certain properties
    existingComponentSourceLink.getSourceLinkOverrides().get(0).setContent("updated content");
    existingComponentSourceLink.getSourceLinkOverrides()
        .add(
            new SourceLinkOverrideDTO(
                null,
                "content3",
                ComponentLegalPartStatus.ENABLED));
    assertThat(componentSourceLinkDAO
        .getByOwnerIdAndComponentIdentifier(Organization.ROOT_ORGANIZATION_ID,
            componentIdentifier.toComponentIdentifier()))
                .isNotNull();

    // Persist the updated values
    ComponentSourceLinkDTO updatedComponentSourceLinkDTO = componentLegalService.saveComponentSourceLink(
        OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID, existingComponentSourceLink);

    assertThat(updatedComponentSourceLinkDTO.getId()).isNotNull();
    assertThat(updatedComponentSourceLinkDTO.getSourceLinkOverrides()).hasSize(3);
    updatedComponentSourceLinkDTO.getSourceLinkOverrides().forEach(co -> assertThat(co.getId()).isNotNull());
    assertThat(updatedComponentSourceLinkDTO.getComponentIdentifier()).usingRecursiveComparison()
        .isEqualTo(componentIdentifier);

    SourceLinkOverrideDTO sourceLinkOverrideDTO0 = updatedComponentSourceLinkDTO.getSourceLinkOverrides().get(0);
    SourceLinkOverrideDTO sourceLinkOverrideDTO1 = updatedComponentSourceLinkDTO.getSourceLinkOverrides().get(1);
    SourceLinkOverrideDTO sourceLinkOverrideDTO2 = updatedComponentSourceLinkDTO.getSourceLinkOverrides().get(2);

    assertThat(sourceLinkOverrideDTO0.getContent()).isEqualTo("updated content");
    assertThat(sourceLinkOverrideDTO0.getStatus()).isEqualTo(ComponentLegalPartStatus.ENABLED);

    assertThat(sourceLinkOverrideDTO1.getContent()).isEqualTo("content2");
    assertThat(sourceLinkOverrideDTO1.getStatus()).isEqualTo(ComponentLegalPartStatus.DISABLED);

    assertThat(sourceLinkOverrideDTO2.getContent()).isEqualTo("content3");

    ComponentSourceLink componentSourceLink = componentSourceLinkDAO.getById(updatedComponentSourceLinkDTO.getId());
    assertThat(componentSourceLink.getOwnerId()).isEqualTo(Organization.ROOT_ORGANIZATION_ID);
  }

  /**
   * Removing a SourceLinkOverride from an existing ComponentSourceLink should delete
   */
  @Test
  public void testBlankSourceLinkOverrideInsert() {
    Application application = tempEntity.newApplicationWithParent();
    ApiComponentIdentifierDTOV2 componentIdentifier = ApiComponentIdentifierDTOV2
        .fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"));

    ComponentSourceLinkDTO componentSourceLinkDTO = new ComponentSourceLinkDTO(
        null,
        componentIdentifier,
        Lists.newArrayList(new SourceLinkOverrideDTO(
            null,
            "content",
            ComponentLegalPartStatus.ENABLED)),
        null,
        null);

    ComponentSourceLinkDTO returnedComponentSourceLinkDTO =
        componentLegalService
            .saveComponentSourceLink(OwnerType.APPLICATION, application.getPublicId(), componentSourceLinkDTO);

    returnedComponentSourceLinkDTO.getSourceLinkOverrides().get(0).setContent("");

    ComponentSourceLinkDTO updatedComponentSourceLinkDTO =
        componentLegalService
            .saveComponentSourceLink(OwnerType.APPLICATION, application.getPublicId(), returnedComponentSourceLinkDTO);

    assertThat(updatedComponentSourceLinkDTO.getSourceLinkOverrides()).isEmpty();
  }

  /**
   * The scenario is the following: Inserting a new ComponentSourceLink at an existing scope. A ComponentSourceLink with
   * ID A exists at the OrgScope. The user inserts a new ComponentSourceLink from the application scope at the OrgScope.
   * There is a conflict. The ComponentSourceLink A is updated to match the new ComponentSourceLink.
   */
  @Test
  public void testConflictingComponentSourceLinkWhileInserting() {
    ApiComponentIdentifierDTOV2 componentIdentifier = ApiComponentIdentifierDTOV2
        .fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"));

    tempEntity.newComponentSourceLink(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"),
        Organization.ROOT_ORGANIZATION_ID);

    ComponentSourceLinkDTO componentSourceLinkDTO = new ComponentSourceLinkDTO(
        null, // null ID signifies we are creating a new ComponentSourceLink
        componentIdentifier,
        Lists.newArrayList(new SourceLinkOverrideDTO(
            null,
            "content",
            ComponentLegalPartStatus.ENABLED),
            new SourceLinkOverrideDTO(
                null,
                "content2",
                ComponentLegalPartStatus.DISABLED)),
        null,
        null);

    ComponentSourceLinkDTO returnedComponentSourceLinkDTO = componentLegalService
        .saveComponentSourceLink(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID, componentSourceLinkDTO);

    assertThat(componentSourceLinkDAO.getAll()).hasSize(1);
    assertThat(returnedComponentSourceLinkDTO.getSourceLinkOverrides()).hasSize(2);

    ComponentSourceLink persistedComponentSourceLink =
        componentSourceLinkDAO.getById(returnedComponentSourceLinkDTO.getId());
    assertThat(persistedComponentSourceLink.getOwnerId()).isEqualTo(Organization.ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testInvalidComponentIdentifierSourceLink() {
    assertThrows(InvalidComponentIdentifierException.class, () -> {
      Application application = tempEntity.newApplicationWithParent();
      ApiComponentIdentifierDTOV2 componentIdentifier = ApiComponentIdentifierDTOV2
          .fromComponentIdentifier(JsonUtils.parse("{}", ComponentIdentifier.class));

      ComponentSourceLinkDTO componentSourceLinkDTO = new ComponentSourceLinkDTO(
          null,
          componentIdentifier,
          Lists.newArrayList(new SourceLinkOverrideDTO(
              null,
              "content",
              ComponentLegalPartStatus.ENABLED)),
          null,
          null);

      componentLegalService
          .saveComponentSourceLink(OwnerType.APPLICATION, application.getPublicId(), componentSourceLinkDTO);
    });
  }

  @Test
  public void testInvalidComponentSourceLink() {
    assertThrows(InvalidComponentSourceLinkException.class, () -> {
      Application application = tempEntity.newApplicationWithParent();
      ApiComponentIdentifierDTOV2 componentIdentifier = ApiComponentIdentifierDTOV2
          .fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"));

      ComponentSourceLinkDTO componentSourceLinkDTO = new ComponentSourceLinkDTO(
          null,
          componentIdentifier,
          Lists.newArrayList(new SourceLinkOverrideDTO(
              null,
              "content",
              null)),
          null,
          null);

      componentLegalService
          .saveComponentSourceLink(OwnerType.APPLICATION, application.getPublicId(), componentSourceLinkDTO);
    });
  }

  @Test
  public void testSaveComponentSourceLink_Update_NotFound() {
    Application application = tempEntity.newApplicationWithParent();
    ComponentSourceLinkDTO componentSourceLinkDTO = new ComponentSourceLinkDTO();
    componentSourceLinkDTO.setId("doesNotExist");
    componentSourceLinkDTO.setComponentIdentifier(
        ApiComponentIdentifierDTOV2
            .fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g", "a", "v")));
    OwnerType ownerType = application.getType();
    String publicId = application.getPublicId();
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> componentLegalService.saveComponentSourceLink(
        ownerType,
        publicId,
        componentSourceLinkDTO))
        .withMessageContaining("ComponentSourceLink with ID " + componentSourceLinkDTO.getId() + " does not exist.");
  }

  @Test
  public void testSaveComponentSourceLink_Update_OverrideNotFound() {
    Application application = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentSourceLink componentSourceLink = tempEntity
        .newComponentSourceLink(componentIdentifier, application.getId());
    ComponentSourceLinkDTO componentSourceLinkDTO = new ComponentSourceLinkDTO();
    componentSourceLinkDTO.setId(componentSourceLink.getId());
    componentSourceLinkDTO
        .setComponentIdentifier(ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier));
    SourceLinkOverrideDTO sourceLinkOverrideDTO = new SourceLinkOverrideDTO();
    sourceLinkOverrideDTO.setId("doesNotExist");
    sourceLinkOverrideDTO.setContent("content");
    sourceLinkOverrideDTO.setStatus(ComponentLegalPartStatus.ENABLED);
    componentSourceLinkDTO.setSourceLinkOverrides(Collections.singletonList(sourceLinkOverrideDTO));
    OwnerType ownerType = application.getType();
    String publicId = application.getPublicId();
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> componentLegalService.saveComponentSourceLink(
        ownerType,
        publicId,
        componentSourceLinkDTO))
        .withMessageContaining("SourceLinkOverride with ID " + sourceLinkOverrideDTO.getId() + " does not exist.");
  }

  @Test
  public void testSaveComponentSourceLink_DeletesExistingOverridesIfNeeded() {
    Application app = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentSourceLink existingComponentSourceLink =
        tempEntity.newComponentSourceLink(componentIdentifier, app.getId());
    SourceLinkOverride existingSourceLinkOverride1 = tempEntity.newSourceLinkOverride("content1",
        ComponentLegalPartStatus.ENABLED, existingComponentSourceLink.getId());
    SourceLinkOverride existingSourceLinkOverride2 = tempEntity.newSourceLinkOverride("content2",
        ComponentLegalPartStatus.ENABLED, existingComponentSourceLink.getId());
    ComponentSourceLinkDTO componentSourceLinkDTO = new ComponentSourceLinkDTO(existingComponentSourceLink.getId(),
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier),
        Collections.singletonList(SourceLinkOverrideDTO.fromSourceLinkOverride(existingSourceLinkOverride1)), null,
        null);
    componentLegalService.saveComponentSourceLink(app.getType(), app.getPublicId(), componentSourceLinkDTO);
    assertThat(sourceLinkOverrideDAO.getById(existingSourceLinkOverride2.getId())).isNull();
  }

  @Test
  public void testGetSourceLinkOverridesFromComponentIdentifier() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentSourceLink componentSourceLink =
        tempEntity.newComponentSourceLink(componentIdentifier, Organization.ROOT_ORGANIZATION_ID);
    SourceLinkOverride sourceLinkOverride =
        tempEntity.newSourceLinkOverride("contentA", ComponentLegalPartStatus.ENABLED, componentSourceLink.getId());
    SourceLinkOverride sourceLinkOverrideTwo =
        tempEntity.newSourceLinkOverride("contentB", ComponentLegalPartStatus.ENABLED, componentSourceLink.getId());
    assertThat(componentLegalService.getSourceLinksOverridesFromComponentIdentifier(Organization.ROOT_ORGANIZATION_ID,
        componentSourceLink.getComponentIdentifier())).hasSize(2)
            .containsExactly(new LegalSourceLinkDTO(sourceLinkOverride), new LegalSourceLinkDTO(sourceLinkOverrideTwo));
  }
}
