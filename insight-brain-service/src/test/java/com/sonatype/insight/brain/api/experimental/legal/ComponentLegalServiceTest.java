/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.InvalidComponentIdentifierException;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalObligationDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentCopyrightWithOwnerDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentCopyrightDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentLegalFileDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentObligationAttributionDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.CopyrightOverrideDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.LegalFileOverrideDTO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentCopyrightDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentLegalFileDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentObligationAttributionDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentObligationDAO;
import com.sonatype.insight.brain.dataaccess.legal.LegalFileOverrideDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.legal.ComponentCopyright;
import com.sonatype.insight.brain.model.legal.ComponentLegalFile;
import com.sonatype.insight.brain.model.legal.ComponentLegalPartStatus;
import com.sonatype.insight.brain.model.legal.ComponentObligation;
import com.sonatype.insight.brain.model.legal.ComponentObligationAttribution;
import com.sonatype.insight.brain.model.legal.LegalFileOverride;
import com.sonatype.insight.brain.model.legal.LegalFileType;
import com.sonatype.insight.brain.model.legal.ObligationStatus;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.model.LicensedFeature;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ComponentLegalServiceTest
    extends AbstractComponentTest
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
  private TestProductLicense testProductLicense;

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

    ComponentCopyrightDTO returnedComponentCopyrightDTO =
        componentLegalService
            .saveComponentCopyright(OwnerType.APPLICATION, application.getPublicId(), componentCopyrightDTO);

    assertThat(returnedComponentCopyrightDTO.getId()).isNotNull();
    assertThat(returnedComponentCopyrightDTO.getCopyrightOverrides()).hasSize(2);
    returnedComponentCopyrightDTO.getCopyrightOverrides().forEach(co ->
        assertThat(co.getId()).isNotNull());
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

    //Persist original componentCopyright
    ComponentCopyrightDTO existingComponentCopyright =
        componentLegalService
            .saveComponentCopyright(OwnerType.APPLICATION, application.getPublicId(), componentCopyrightDTO);

    //Modify certain properties
    existingComponentCopyright.getCopyrightOverrides().get(0).setContent("updated content");
    existingComponentCopyright.getCopyrightOverrides().add(
        new CopyrightOverrideDTO(
            null,
            null,
            "content3",
            ComponentLegalPartStatus.ENABLED
        ));
    assertThat(componentCopyrightDAO
        .getByOwnerIdAndComponentIdentifier(application.getId(), componentIdentifier.toComponentIdentifier()))
        .isNotNull();

    //Persist the updated values
    ComponentCopyrightDTO updatedComponentCopyrightDTO =
        componentLegalService
            .saveComponentCopyright(OwnerType.ORGANIZATION, organization.getPublicId(), existingComponentCopyright);

    assertThat(updatedComponentCopyrightDTO.getId()).isNotNull();
    assertThat(updatedComponentCopyrightDTO.getCopyrightOverrides()).hasSize(3);
    updatedComponentCopyrightDTO.getCopyrightOverrides().forEach(co ->
        assertThat(co.getId()).isNotNull());
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
                ComponentLegalPartStatus.ENABLED
            )
        ),
        null,
        null
    );

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
   * ID B exists at the ApplicationScope - user modifies ComponentCopyright B to OrgScope. The ComponentCopyright A is
   * deleted, now we only have ComponentCopyright B at the OrgScope.
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

    ComponentCopyrightDTO returnedComponentCopyrightDTO = componentLegalService
        .saveComponentCopyright(OwnerType.ORGANIZATION, organization.getPublicId(), componentCopyrightDTO);

    assertThat(componentCopyrightDAO.getById(orgComponentCopyright.getId())).isNull();
    assertThat(returnedComponentCopyrightDTO.getCopyrightOverrides()).hasSize(2);
    assertThat(returnedComponentCopyrightDTO.getId()).isEqualTo(appComponentCopyright.getId());

    ComponentCopyright persistedComponentCopyright =
        componentCopyrightDAO.getById(returnedComponentCopyrightDTO.getId());
    assertThat(persistedComponentCopyright.getOwnerId()).isEqualTo(organization.getId());
  }

  /**
   * The scenario is the following: Inserting a new ComponentCopyright at an existing scope. A ComponentCopyright with
   * ID A exists at the OrgScope. The user inserts a new ComponentCopyright from the application scope at the OrgScope.
   * There is a conflict. The ComponentCopyright A is deleted, now we only have ComponentCopyright B at the OrgScope.
   */
  @Test
  public void testConflictingComponentCopyrightWhileInserting() {
    ApiComponentIdentifierDTOV2 componentIdentifier = ApiComponentIdentifierDTOV2
        .fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"));

    Organization organization = tempEntity.newOrganization();

    ComponentCopyright orgComponentCopyright =
        tempEntity.newComponentCopyright(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"),
            organization.getId(), "lch");

    ComponentCopyrightDTO componentCopyrightDTO = new ComponentCopyrightDTO(
        null, //null ID signifies we are creating a new ComponentCopyright
        componentIdentifier,
        Lists.newArrayList(new CopyrightOverrideDTO(
                "123",
                "originalContentHash",
                "content",
                ComponentLegalPartStatus.ENABLED
            ),
            new CopyrightOverrideDTO(
                "456",
                "originalContentHash2",
                "content2",
                ComponentLegalPartStatus.DISABLED
            )
        ),
        null,
        null
    );

    ComponentCopyrightDTO returnedComponentCopyrightDTO = componentLegalService
        .saveComponentCopyright(OwnerType.ORGANIZATION, organization.getPublicId(), componentCopyrightDTO);

    assertThat(componentCopyrightDAO.getById(orgComponentCopyright.getId())).isNull();
    assertThat(returnedComponentCopyrightDTO.getCopyrightOverrides()).hasSize(2);

    ComponentCopyright persistedComponentCopyright =
        componentCopyrightDAO.getById(returnedComponentCopyrightDTO.getId());
    assertThat(persistedComponentCopyright.getOwnerId()).isEqualTo(organization.getId());
  }

  @Test(expected = InvalidComponentIdentifierException.class)
  public void testInvalidComponentIdentifier() throws IOException {
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
                ComponentLegalPartStatus.ENABLED
            )
        ),
        null,
        null
    );

    componentLegalService
        .saveComponentCopyright(OwnerType.APPLICATION, application.getPublicId(), componentCopyrightDTO);
  }

  @Test(expected = InvalidComponentCopyrightException.class)
  public void testInvalidComponentCopyright() {
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
                null
            )
        ),
        null,
        null
    );

    componentLegalService
        .saveComponentCopyright(OwnerType.APPLICATION, application.getPublicId(), componentCopyrightDTO);
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
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() ->
        componentLegalService.saveComponentLegalFile(
            application.getType(),
            application.getPublicId(),
            new ComponentLegalFileDTO()
        )
    ).withMessageContaining("The component identifier cannot be null.");
  }

  @Test
  public void testSaveComponentLegalFile_InvalidComponentIdentifier() {
    Application application = tempEntity.newApplicationWithParent();
    ComponentLegalFileDTO componentLegalFileDTO = new ComponentLegalFileDTO();
    componentLegalFileDTO.setComponentIdentifier(new ApiComponentIdentifierDTOV2());
    assertThatExceptionOfType(InvalidComponentIdentifierException.class).isThrownBy(() ->
        componentLegalService.saveComponentLegalFile(
            application.getType(),
            application.getPublicId(),
            componentLegalFileDTO
        )
    );
  }

  @Test
  public void testSaveComponentLegalFile_LegalFileOverrideNullLegalFileType() {
    Application application = tempEntity.newApplicationWithParent();
    ComponentLegalFileDTO componentLegalFileDTO = new ComponentLegalFileDTO();
    componentLegalFileDTO.setComponentIdentifier(
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g", "a", "v")));
    LegalFileOverrideDTO legalFileOverrideDTO = new LegalFileOverrideDTO();
    componentLegalFileDTO.setLegalFileOverrides(Collections.singletonList(legalFileOverrideDTO));
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() ->
        componentLegalService.saveComponentLegalFile(
            application.getType(),
            application.getPublicId(),
            componentLegalFileDTO
        )
    ).withMessageContaining("LegalFileOverride must have a legal file type.");
  }

  @Test
  public void testSaveComponentLegalFile_LegalFileOverrideNullStatus() {
    Application application = tempEntity.newApplicationWithParent();
    ComponentLegalFileDTO componentLegalFileDTO = new ComponentLegalFileDTO();
    componentLegalFileDTO.setComponentIdentifier(
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g", "a", "v")));
    LegalFileOverrideDTO legalFileOverrideDTO = new LegalFileOverrideDTO();
    legalFileOverrideDTO.setLegalFileType(LegalFileType.NOTICE);
    componentLegalFileDTO.setLegalFileOverrides(Collections.singletonList(legalFileOverrideDTO));
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() ->
        componentLegalService.saveComponentLegalFile(
            application.getType(),
            application.getPublicId(),
            componentLegalFileDTO
        )
    ).withMessageContaining("LegalFileOverride must have a status.");
  }

  @Test
  public void testSaveComponentLegalFile_New() {
    Application app = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentLegalFileDTO componentLegalFileDTO = new ComponentLegalFileDTO();
    componentLegalFileDTO
        .setComponentIdentifier(ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier));
    LegalFileOverrideDTO notice1 = new LegalFileOverrideDTO(LegalFileType.NOTICE, "originalContentHash1", "content1",
        ComponentLegalPartStatus.ENABLED);
    LegalFileOverrideDTO notice2 = new LegalFileOverrideDTO(LegalFileType.NOTICE, null, "content2",
        ComponentLegalPartStatus.DISABLED);
    LegalFileOverrideDTO notice3 = new LegalFileOverrideDTO(LegalFileType.NOTICE, "originalContentHash2", null,
        ComponentLegalPartStatus.ENABLED);
    LegalFileOverrideDTO notice4 = new LegalFileOverrideDTO(LegalFileType.NOTICE, null, null,
        ComponentLegalPartStatus.DISABLED);
    LegalFileOverrideDTO license1 = new LegalFileOverrideDTO(LegalFileType.LICENSE, "originalContentHash3", "content3",
        ComponentLegalPartStatus.ENABLED);
    LegalFileOverrideDTO license2 = new LegalFileOverrideDTO(LegalFileType.LICENSE, null, "content4",
        ComponentLegalPartStatus.DISABLED);
    LegalFileOverrideDTO license3 = new LegalFileOverrideDTO(LegalFileType.LICENSE, "originalContentHash4", null,
        ComponentLegalPartStatus.ENABLED);
    LegalFileOverrideDTO license4 = new LegalFileOverrideDTO(LegalFileType.LICENSE, null, null,
        ComponentLegalPartStatus.DISABLED);
    componentLegalFileDTO.setLegalFileOverrides(
        Arrays.asList(notice1, notice2, notice3, notice4, license1, license2, license3, license4));
    Date date = new Date();

    ComponentLegalFileDTO resultDto =
        componentLegalService.saveComponentLegalFile(app.getType(), app.getPublicId(), componentLegalFileDTO);

    assertComponentLegalFile(resultDto, componentLegalFileDTO, app.getId(), date);
    assertComponentLegalFile(resultDto,
        new ComponentLegalFileDTO(componentLegalFileDAO.getById(resultDto.getId()), Collections.emptyList()),
        app.getId(), date);
    notice3.setContent("");
    license3.setContent("");
    assertThat(resultDto.getLegalFileOverrides()).usingRecursiveFieldByFieldElementComparator(
        RecursiveComparisonConfiguration.builder().withIgnoredFields("id").build())
        .containsExactlyInAnyOrder(notice1, notice2, notice3, license1, license2, license3);
    for (LegalFileOverrideDTO legalFileOverride : resultDto.getLegalFileOverrides()) {
      assertThat(legalFileOverride.getId()).isNotNull();
      assertLegalFileOverride(new LegalFileOverrideDTO(legalFileOverrideDAO.getById(legalFileOverride.getId())),
          legalFileOverride);
    }
  }

  @Test
  public void testSaveComponentLegalFile_NewConflict() {
    Organization org = tempEntity.newOrganization();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentLegalFile componentLegalFile =
        tempEntity.newComponentLegalFile(componentIdentifier, org.getId(), "legalContentHash");
    LegalFileOverride noticeOverride =
        tempEntity.newLegalFileOverride(LegalFileType.NOTICE, "originalContentHash1", "hash1",
            "content1", ComponentLegalPartStatus.ENABLED, componentLegalFile.getId());
    ComponentLegalFileDTO componentLegalFileDTO = new ComponentLegalFileDTO();
    componentLegalFileDTO
        .setComponentIdentifier(ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier));
    LegalFileOverrideDTO notice = new LegalFileOverrideDTO(noticeOverride);
    LegalFileOverrideDTO license = new LegalFileOverrideDTO(LegalFileType.LICENSE, "originalContentHash2", "content2",
        ComponentLegalPartStatus.ENABLED);
    componentLegalFileDTO.setLegalFileOverrides(Arrays.asList(notice, license));
    Date date = new Date();

    ComponentLegalFileDTO resultDto = componentLegalService
        .saveComponentLegalFile(org.getType(), org.getPublicId(), componentLegalFileDTO);

    assertThat(componentLegalFileDAO.getById(componentLegalFile.getId())).isNull();
    assertComponentLegalFile(resultDto, componentLegalFileDTO, org.getId(), date);
    assertComponentLegalFile(resultDto,
        new ComponentLegalFileDTO(componentLegalFileDAO.getById(resultDto.getId()), Collections.emptyList()),
        org.getId(), date);
    assertThat(resultDto.getLegalFileOverrides()).usingRecursiveFieldByFieldElementComparator(
        RecursiveComparisonConfiguration.builder().withIgnoredFields("id").build())
        .containsExactlyInAnyOrder(notice, license);
    for (LegalFileOverrideDTO legalFileOverride : resultDto.getLegalFileOverrides()) {
      assertThat(legalFileOverride.getId()).isNotNull().isNotEqualTo(noticeOverride.getId());
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
        tempEntity.newComponentLegalFile(componentIdentifier, app.getOrganizationId(), "legalContentHash1");
    ComponentLegalFile appComponentLegalFile =
        tempEntity.newComponentLegalFile(componentIdentifier, app.getId(), "legalContentHash2");
    ComponentLegalFileDTO componentLegalFileDTO = new ComponentLegalFileDTO();
    componentLegalFileDTO.setId(appComponentLegalFile.getId());
    componentLegalFileDTO
        .setComponentIdentifier(ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier));
    LegalFileOverrideDTO notice1 = new LegalFileOverrideDTO(LegalFileType.NOTICE, "originalContentHash1", "content1",
        ComponentLegalPartStatus.ENABLED);
    LegalFileOverrideDTO license1 = new LegalFileOverrideDTO(LegalFileType.LICENSE, "originalContentHash2", "content2",
        ComponentLegalPartStatus.ENABLED);
    componentLegalFileDTO.setLegalFileOverrides(Arrays.asList(notice1, license1));
    Date date = new Date();

    ComponentLegalFileDTO resultDto = componentLegalService
        .saveComponentLegalFile(org.getType(), org.getPublicId(), componentLegalFileDTO);

    assertThat(componentLegalFileDAO.getById(orgComponentLegalFile.getId())).isNull();
    assertComponentLegalFile(resultDto, componentLegalFileDTO, org.getId(), date);
    assertComponentLegalFile(resultDto,
        new ComponentLegalFileDTO(componentLegalFileDAO.getById(resultDto.getId()), Collections.emptyList()),
        org.getId(), date);
    assertThat(resultDto.getLegalFileOverrides()).usingRecursiveFieldByFieldElementComparator(
        RecursiveComparisonConfiguration.builder().withIgnoredFields("id").build())
        .containsExactlyInAnyOrder(notice1, license1);
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
    assertThat(actual.getLegalFileType()).isEqualTo(expected.getLegalFileType());
    assertThat(actual.getOriginalContentHash()).isEqualTo(expected.getOriginalContentHash());
    assertThat(actual.getContent()).isEqualTo(StringUtils.trimToEmpty(expected.getContent()));
    assertThat(actual.getStatus()).isEqualTo(expected.getStatus());
  }

  @Test
  public void testSaveComponentLegalFile_Existing_UpdatesAllFields() {
    Application app = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    ComponentLegalFile componentLegalFile =
        tempEntity.newComponentLegalFile(componentIdentifier1, app.getId(), "legalContentHash1");
    LegalFileOverride legalFileOverride =
        tempEntity.newLegalFileOverride(LegalFileType.NOTICE, "originalContentHash", "hash",
            "", ComponentLegalPartStatus.ENABLED, componentLegalFile.getId());
    ComponentLegalFileDTO componentLegalFileDTO = new ComponentLegalFileDTO();
    componentLegalFileDTO.setId(componentLegalFile.getId());
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2");
    componentLegalFileDTO
        .setComponentIdentifier(ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier2));
    LegalFileOverrideDTO legalFileOverrideDTO = new LegalFileOverrideDTO(LegalFileType.LICENSE, null, "content",
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
        tempEntity.newComponentLegalFile(componentIdentifier, app.getId(), "legalContentHash1");
    LegalFileOverride legalFileOverride1 = tempEntity.newLegalFileOverride(LegalFileType.NOTICE, "originalContentHash1",
        "hash1", "content", ComponentLegalPartStatus.ENABLED, componentLegalFile.getId());
    LegalFileOverride legalFileOverride2 = tempEntity.newLegalFileOverride(LegalFileType.NOTICE, "originalContentHash2",
        "hash2", "", ComponentLegalPartStatus.ENABLED, componentLegalFile.getId());
    LegalFileOverride legalFileOverride3 = tempEntity.newLegalFileOverride(LegalFileType.NOTICE, null,
        "hash1", "content", ComponentLegalPartStatus.ENABLED, componentLegalFile.getId());
    ComponentLegalFileDTO componentLegalFileDTO = new ComponentLegalFileDTO();
    componentLegalFileDTO.setId(componentLegalFile.getId());
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

    ComponentLegalFileDTO resultDto =
        componentLegalService.saveComponentLegalFile(app.getType(), app.getPublicId(), componentLegalFileDTO);

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
  public void testSaveComponentObligationAttribution_Unlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.ADVANCED_LEGAL_PACK);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> componentLegalService.saveComponentObligationAttribution(null, null, null));
  }

  @Test
  public void testSaveComponentObligationAttribution_NullComponentIdentifier() {
    Application application = tempEntity.newApplicationWithParent();
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() ->
        componentLegalService.saveComponentObligationAttribution(
            application.getType(),
            application.getPublicId(),
            new ComponentObligationAttributionDTO()
        )
    ).withMessageContaining("The component identifier cannot be null.");
  }

  @Test
  public void testSaveComponentObligationAttribution_InvalidComponentIdentifier() {
    Application application = tempEntity.newApplicationWithParent();
    ComponentObligationAttributionDTO componentObligationAttributionDTO = new ComponentObligationAttributionDTO();
    componentObligationAttributionDTO.setComponentIdentifier(new ApiComponentIdentifierDTOV2());
    assertThatExceptionOfType(InvalidComponentIdentifierException.class).isThrownBy(() ->
        componentLegalService.saveComponentObligationAttribution(
            application.getType(),
            application.getPublicId(),
            componentObligationAttributionDTO
        )
    );
  }

  @Test
  public void testSaveComponentObligationAttribution_BlankContent() {
    Application application = tempEntity.newApplicationWithParent();
    ComponentObligationAttributionDTO componentObligationAttributionDTO = new ComponentObligationAttributionDTO();
    componentObligationAttributionDTO.setComponentIdentifier(
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g", "a", "v")));
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() ->
        componentLegalService.saveComponentObligationAttribution(
            application.getType(),
            application.getPublicId(),
            componentObligationAttributionDTO
        )
    ).withMessageContaining("ComponentObligationAttribution must have content.");
    componentObligationAttributionDTO.setContent("");
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() ->
        componentLegalService.saveComponentObligationAttribution(
            application.getType(),
            application.getPublicId(),
            componentObligationAttributionDTO
        )
    ).withMessageContaining("ComponentObligationAttribution must have content.");
    componentObligationAttributionDTO.setContent(" ");
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() ->
        componentLegalService.saveComponentObligationAttribution(
            application.getType(),
            application.getPublicId(),
            componentObligationAttributionDTO
        )
    ).withMessageContaining("ComponentObligationAttribution must have content.");

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentObligationAttribution componentObligationAttribution = tempEntity.newComponentObligationAttribution(
        componentIdentifier, application.getId(), "obligationName", "content",
        ComponentLegalService.NOT_IMPLEMENTED);
    componentObligationAttributionDTO.setId(componentObligationAttribution.getId());
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() ->
        componentLegalService.saveComponentObligationAttribution(
            application.getType(),
            application.getPublicId(),
            componentObligationAttributionDTO
        )
    ).withMessageContaining("ComponentObligationAttribution must have content.");
    componentObligationAttributionDTO.setContent("");
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() ->
        componentLegalService.saveComponentObligationAttribution(
            application.getType(),
            application.getPublicId(),
            componentObligationAttributionDTO
        )
    ).withMessageContaining("ComponentObligationAttribution must have content.");
    componentObligationAttributionDTO.setContent(" ");
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() ->
        componentLegalService.saveComponentObligationAttribution(
            application.getType(),
            application.getPublicId(),
            componentObligationAttributionDTO
        )
    ).withMessageContaining("ComponentObligationAttribution must have content.");
  }

  @Test
  public void testSaveComponentObligationAttribution_Create() {
    Application application = tempEntity.newApplicationWithParent();
    ComponentObligationAttributionDTO componentObligationAttributionDTO = new ComponentObligationAttributionDTO();
    componentObligationAttributionDTO.setComponentIdentifier(
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g", "a", "v")));
    componentObligationAttributionDTO.setObligationName("obligationName");
    componentObligationAttributionDTO.setContent("content");

    ComponentObligationAttributionDTO result = componentLegalService.saveComponentObligationAttribution(
        application.getType(),
        application.getPublicId(),
        componentObligationAttributionDTO
    );

    List<ComponentObligationAttribution> componentObligationAttributions =
        componentObligationAttributionDAO.getByOwnerId(application.getId());
    assertThat(componentObligationAttributions).hasSize(1);
    ComponentObligationAttribution componentObligationAttribution = componentObligationAttributions.get(0);
    assertComponentObligationAttribution(componentObligationAttribution, application,
        componentObligationAttributionDTO);
    assertComponentObligationAttribution(result, componentObligationAttribution);
  }

  @Test
  public void testSaveComponentObligationAttribution_Update() {
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
        componentObligationAttributionDTO
    );

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
  public void testSaveComponentObligationAttribution_Update_DoesNotExist() {
    Application application = tempEntity.newApplicationWithParent();
    ComponentObligationAttributionDTO componentObligationAttributionDTO = new ComponentObligationAttributionDTO();
    componentObligationAttributionDTO.setId("doesNotExist");
    componentObligationAttributionDTO.setComponentIdentifier(
        ApiComponentIdentifierDTOV2
            .fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g", "a", "v")));
    componentObligationAttributionDTO.setContent("content");

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() ->
        componentLegalService.saveComponentObligationAttribution(
            application.getType(),
            application.getPublicId(),
            componentObligationAttributionDTO
        )
    ).withMessageContaining(
        "ComponentObligationAttribution with ID " + componentObligationAttributionDTO.getId() + " does not exist.");
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
  public void testSaveComponentObligation_Unlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.ADVANCED_LEGAL_PACK);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> componentLegalService.saveComponentObligation(null, null, null));
  }

  @Test
  public void testSaveComponentObligation_NullComponentIdentifier() {
    Application app = tempEntity.newApplicationWithParent();
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> componentLegalService
        .saveComponentObligation(app.getType(), app.getPublicId(), new ApiLicenseLegalObligationDTO()))
        .withMessageContaining("The component identifier cannot be null.");
  }

  @Test
  public void testSaveComponentObligation_InvalidComponentIdentifier() {
    Application app = tempEntity.newApplicationWithParent();
    ApiLicenseLegalObligationDTO dto = new ApiLicenseLegalObligationDTO();
    dto.setComponentIdentifier(new ApiComponentIdentifierDTOV2());
    assertThatExceptionOfType(InvalidComponentIdentifierException.class)
        .isThrownBy(() -> componentLegalService.saveComponentObligation(app.getType(), app.getPublicId(), dto));
  }

  @Test
  public void testSaveComponentObligation_BlankObligationName() {
    Application app = tempEntity.newApplicationWithParent();
    ApiLicenseLegalObligationDTO dto = createMinimalComponentObligationDTO();
    dto.setName(null);
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> componentLegalService.saveComponentObligation(app.getType(), app.getPublicId(), dto))
        .withMessageContaining("ComponentObligation must have a name.");
    dto.setName("");
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> componentLegalService.saveComponentObligation(app.getType(), app.getPublicId(), dto))
        .withMessageContaining("ComponentObligation must have a name.");
    dto.setName(" ");
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> componentLegalService.saveComponentObligation(app.getType(), app.getPublicId(), dto))
        .withMessageContaining("ComponentObligation must have a name.");
  }

  @Test
  public void testSaveComponentObligation_NullStatus() {
    Application app = tempEntity.newApplicationWithParent();
    ApiLicenseLegalObligationDTO componentObligationDTO = createMinimalComponentObligationDTO();
    componentObligationDTO.setStatus(null);
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> componentLegalService.saveComponentObligation(app.getType(), app.getPublicId(), componentObligationDTO))
        .withMessageContaining("ComponentObligation must have a status.");
  }

  @Test
  public void testSaveComponentObligation_Create() {
    Application app = tempEntity.newApplicationWithParent();
    ApiLicenseLegalObligationDTO dto = createMinimalComponentObligationDTO();

    ApiLicenseLegalObligationDTO resultDto =
        componentLegalService.saveComponentObligation(app.getType(), app.getPublicId(), dto);

    ComponentObligation componentObligation = componentObligationDAO.getByIdNotNull(resultDto.getId());
    assertComponentObligation(componentObligation, app, dto);
    assertComponentObligation(resultDto, componentObligation);
  }

  @Test
  public void testSaveComponentObligation_Update_DoesNotExist() {
    Application app = tempEntity.newApplicationWithParent();
    ApiLicenseLegalObligationDTO dto = createMinimalComponentObligationDTO();
    String id = "doesNotExist";
    dto.setId(id);
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> componentLegalService.saveComponentObligation(app.getType(), app.getPublicId(), dto))
        .withMessageContaining("ComponentObligation with ID " + id + " does not exist.");
  }

  @Test
  public void testSaveComponentObligation_Update() {
    Application app = tempEntity.newApplicationWithParent();
    ApiLicenseLegalObligationDTO dto = createMinimalComponentObligationDTO();
    ComponentObligation componentObligation = tempEntity.newComponentObligation(
        dto.getComponentIdentifier().toComponentIdentifier().createAlternativeVersion("v0"), app.getOrganizationId(),
        "original" + dto.getName(), "original" + dto.getComment(), ObligationStatus.FLAGGED,
        ComponentLegalService.NOT_IMPLEMENTED);
    dto.setId(componentObligation.getId());

    ApiLicenseLegalObligationDTO resultDto =
        componentLegalService.saveComponentObligation(app.getType(), app.getPublicId(), dto);

    componentObligation = componentObligationDAO.getByIdNotNull(resultDto.getId());
    assertComponentObligation(componentObligation, app, dto);
    assertComponentObligation(resultDto, componentObligation);
  }

  @Test
  public void testDeleteComponentObligation_Unlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.ADVANCED_LEGAL_PACK);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> componentLegalService.deleteComponentObligation(null));
  }

  @Test
  public void testDeleteComponentObligation_DoesNotExist() {
    String id = "doesNotExist";
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> componentLegalService.deleteComponentObligation(id))
        .withMessageContaining("ComponentObligation with ID " + id + " does not exist.");
  }

  @Test
  public void testDeleteComponentObligation() {
    Application app = tempEntity.newApplicationWithParent();
    ApiLicenseLegalObligationDTO dto = createMinimalComponentObligationDTO();
    ComponentObligation componentObligation = tempEntity.newComponentObligation(
        dto.getComponentIdentifier().toComponentIdentifier(), app.getId(), dto.getName(), dto.getComment(),
        dto.getStatus(), ComponentLegalService.NOT_IMPLEMENTED);

    componentLegalService.deleteComponentObligation(componentObligation.getId());

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

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> componentLegalService
        .getComponentCopyrightWithHierarchy(OwnerType.ORGANIZATION, org.getPublicId(), componentIdentifier))
    .withMessageContaining("No component copyright");
  }

  @Test
  public void testGetComponentCopyrightWithHierarchy_Unlicensed() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    testProductLicense.setMissingFeatures(LicensedFeature.ADVANCED_LEGAL_PACK);
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() ->
        componentLegalService.getComponentCopyrightWithHierarchy(OwnerType.APPLICATION, "n/a", componentIdentifier));
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
}
